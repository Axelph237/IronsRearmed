package com.mumu17.ironsarms.event;

import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.utils.GunMana;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.EntityKineticBullet;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.spells.ender.StarfallSpell;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = IronsArms.MODID)
public class IronsArmsBulletEvents {
    private static final Map<UUID, CompoundTag> pendingSpells = new ConcurrentHashMap<>();
    private static Boolean calamityRingExists = null;
    private static Item ringItemInstance = null;

    private static final List<UUID> castingPlayers = new ArrayList<>(List.of());

    private static final double FALL_HEIGHT = 4.0;
    private static final double STATIONARY_SPEED = 0.1;
    private static Skyfall skyfall = null;

    private record Impact(Vec3 hit, Vec3 aim, Vec3 approach) {
        static Impact onEntity(LivingEntity target) {
            return new Impact(target.position(), target.getBoundingBox().getCenter(), new Vec3(0, FALL_HEIGHT, 0));
        }

        static Impact onBlock(BlockHitResult hitResult) {
            Vec3 normal = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal());
            // The underside of a block cannot be reached from above, so approach it from below.
            Vec3 approach = normal.y < 0 ? normal.scale(FALL_HEIGHT) : new Vec3(normal.x, FALL_HEIGHT, normal.z);
            return new Impact(hitResult.getLocation(), hitResult.getLocation().add(normal.scale(0.05)), approach);
        }

        Skyfall skyfallFor(Player caster) {
            Vec3 top = aim.add(approach);
            BlockHitResult blocked = caster.level().clip(new ClipContext(aim, top, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            Vec3 start = blocked.getType() == HitResult.Type.MISS ? top
                    : aim.add(approach.normalize().scale(Math.max(0, blocked.getLocation().distanceTo(aim) - 0.5)));
            Vec3 dir = start.equals(aim) ? approach.normalize().reverse() : aim.subtract(start).normalize();
            return new Skyfall(caster, aim, start, dir);
        }
    }

    private record Skyfall(Player caster, Vec3 aim, Vec3 start, Vec3 dir) {
        void redirect(Projectile projectile) {
            // Spells like firecracker spawn a stationary projectile that bursts where it stands; put it on the target instead.
            if (projectile.getDeltaMovement().length() < STATIONARY_SPEED) {
                projectile.setPos(aim.subtract(0, projectile.getBbHeight() / 2, 0));
                return;
            }
            projectile.setPos(start.subtract(0, projectile.getBbHeight() / 2, 0));
            if (projectile instanceof AbstractMagicProjectile magicProjectile) {
                magicProjectile.shoot(dir);
            } else {
                projectile.setDeltaMovement(dir.scale(projectile.getDeltaMovement().length()));
            }
            if (projectile instanceof AbstractHurtingProjectile hurting) {
                double power = new Vec3(hurting.xPower, hurting.yPower, hurting.zPower).length();
                hurting.xPower = dir.x * power;
                hurting.yPower = dir.y * power;
                hurting.zPower = dir.z * power;
            }
            projectile.setYRot((float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG));
            projectile.setXRot((float) (Mth.atan2(dir.y, dir.horizontalDistance()) * Mth.RAD_TO_DEG));
            projectile.yRotO = projectile.getYRot();
            projectile.xRotO = projectile.getXRot();
        }
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) return;
        if (!(event.getShooter() instanceof ServerPlayer player)) return;

        ItemStack gunStack = event.getGunItemStack();
        Optional<GunMana.InscribedSpell> inscribedSpell = GunMana.inscribedSpell(gunStack);

        AbstractSpell spell = null;
        int level = 1;

        if (inscribedSpell.isPresent()) {
            spell = inscribedSpell.get().spell();
            level = inscribedSpell.get().level();
        } else if (ISpellContainer.isSpellContainer(gunStack)) {
            ISpellContainer container = ISpellContainer.get(gunStack);
            if (!container.isEmpty()) {
                SpellData spellData = container.getSpellAtIndex(0);
                spell = spellData.getSpell();
                level = spellData.getLevel();
            }
        }

        if (spell == null || spell == SpellRegistry.none()) return;

        ModifySpellLevelEvent levelEvent = new ModifySpellLevelEvent(spell, player, level, level);
        MinecraftForge.EVENT_BUS.post(levelEvent);
        level = levelEvent.getLevel();

        MagicData magicData = MagicData.getPlayerMagicData(player);
        String spellId = spell.getSpellResource().toString();
        boolean isRecasting = magicData.getPlayerRecasts().hasRecastForSpell(spellId);
        boolean hasCalamityRing = checkForCalamityRing(player);

        if (!player.isCreative() && !hasCalamityRing && !isRecasting) {
            if (!GunMana.pay(player, gunStack, spell.getManaCost(level))) return;
        }

        CompoundTag dataToPass = new CompoundTag();
        dataToPass.putString("SpellID", spell.getSpellId());
        dataToPass.putInt("Level", level);
        pendingSpells.put(player.getUUID(), dataToPass);
        castingPlayers.add(player.getUUID());
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (event.getCastSource() != CastSource.SWORD || !event.getCastSource().consumesMana() || event.getEntity().isCreative()) return;
        ItemStack gun = event.getEntity().getMainHandItem();
        boolean gunCastsThisSpell = GunMana.inscribedSpell(gun)
                .filter(inscribed -> inscribed.spell().getSpellId().equals(event.getSpellId()))
                .isPresent();
        if (gunCastsThisSpell && GunMana.payFromGun(gun, event.getManaCost())) {
            event.setManaCost(0);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof EntityKineticBullet bullet) {
            if (bullet.getOwner() instanceof Player player) {
                if (!castingPlayers.contains(player.getUUID())) return;
                CompoundTag reservedData = pendingSpells.get(player.getUUID());
                if (reservedData != null) {
                    bullet.getPersistentData().put("MagicData", reservedData);
                }
            }
        } else if (skyfall != null && event.getEntity() instanceof Projectile projectile && projectile.getOwner() == skyfall.caster()) {
            // Redirect before the spawn packet goes out, so clients see the fall from the first frame.
            skyfall.redirect(projectile);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            pendingSpells.clear();
        }
    }

    @SubscribeEvent
    public static void onEntityHurtPre(EntityHurtByGunEvent.Pre event) {
        if (!(event.getAttacker() instanceof Player player)) return;
        if (!(event.getHurtEntity() instanceof LivingEntity target)) return;
        if (!castingPlayers.contains(player.getUUID())) return;
        castingPlayers.remove(player.getUUID());

        // getHitPos()を解決できないため、弾丸の現在位置を使用
        processImpact(player, target, Impact.onEntity(target), event.getBullet());
    }

    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        if (!(event.getAmmo().getOwner() instanceof Player player)) return;
        if (!castingPlayers.contains(player.getUUID())) return;
        castingPlayers.remove(player.getUUID());

        Impact impact = Impact.onBlock(event.getHitResult());
        Vec3 hitPos = impact.hit();

        ArmorStand dummy = new ArmorStand(EntityType.ARMOR_STAND, player.level());
        dummy.setPos(hitPos.x, hitPos.y, hitPos.z);
        dummy.setInvisible(true);
        dummy.setInvulnerable(true);
        dummy.setNoGravity(true);

        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Marker", true);
        tag.putBoolean("Small", true);
        dummy.readAdditionalSaveData(tag);

        player.level().addFreshEntity(dummy);

        processImpact(player, dummy, impact, event.getAmmo());

        dummy.discard();
    }

    private static void processImpact(Player player, LivingEntity target, Impact impact, Entity bullet) {
        if (player.level().isClientSide) return;
        if (!bullet.getPersistentData().contains("MagicData")) return;
        CompoundTag magicTag = bullet.getPersistentData().getCompound("MagicData");

        AbstractSpell spell = SpellRegistry.getSpell(magicTag.getString("SpellID"));
        int level = magicTag.getInt("Level");
        if (spell == null || spell == SpellRegistry.none()) return;

        MagicData magicData = MagicData.getPlayerMagicData(player);
        String path = spell.getSpellResource().getPath();

        if (path.contains("starfall")) {
            magicData.setAdditionalCastData(new StarfallSpell.StarfallCastData(impact.hit()));
        } else if (path.contains("teleport")) {
            magicData.setAdditionalCastData(new TeleportSpell.TeleportData(impact.hit()));
        } else if (target != null) {
            magicData.setAdditionalCastData(new TargetEntityCastData(target));
        }

        Vec3 originalPos = player.position();
        float originalYRot = player.getYRot();
        float originalXRot = player.getXRot();

        try {
            Vec3 bulletMotion = bullet.getDeltaMovement().normalize();
            double d0 = bulletMotion.x;
            double d1 = bulletMotion.y;
            double d2 = bulletMotion.z;
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            float yaw = (float)(Math.atan2(d2, d0) * (180D / Math.PI)) - 90.0F;
            float pitch = (float)(-(Math.atan2(d1, d3) * (180D / Math.PI)));

            double offsetDistance = 2.2;
            Vec3 pseudoSpawnPos = impact.hit().subtract(bulletMotion.scale(offsetDistance));

            player.setPos(pseudoSpawnPos.x, pseudoSpawnPos.y, pseudoSpawnPos.z);
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.setYHeadRot(yaw);

            // setInitiatedCastSpellが解決できないため、initiateCast内で管理するか、
            // バージョンに応じて直接詠唱開始をシミュレート
            if (spell.getCastType() == CastType.CONTINUOUS) {
                int duration = continuousDurationTicks(player.getMainHandItem(), spell, level);
                magicData.initiateCast(spell, level, duration, CastSource.SWORD, spell.getSpellId());
                executeSpell(spell, level, player, magicData);
            } else {
                skyfall = impact.skyfallFor(player);
                try {
                    executeSpell(spell, level, player, magicData);
                } finally {
                    skyfall = null;
                }
            }

        } finally {
            player.setPos(originalPos.x, originalPos.y, originalPos.z);
            player.setYRot(originalYRot);
            player.setXRot(originalXRot);
            player.setYHeadRot(originalYRot);

            updateRecastSystems(spell, magicData);

            if (spell.getCastType() != CastType.CONTINUOUS) {
                magicData.resetCastingState();
                magicData.setAdditionalCastData(null);
            }
        }
    }

    private static int continuousDurationTicks(ItemStack gun, AbstractSpell spell, int level) {
        if (gun.getItem() instanceof IGun iGun) {
            return Math.max(1, Math.round(1200f / Math.max(1, iGun.getRPM(gun))));
        }
        int castTime = spell.getCastTime(level);
        return castTime > 0 ? castTime : 20;
    }

    private static void updateRecastSystems(AbstractSpell spell, MagicData magicData) {
        String spellIdString = spell.getSpellResource().toString();
        if (magicData.getPlayerRecasts().hasRecastForSpell(spellIdString)) {
            RecastInstance oldInstance = magicData.getPlayerRecasts().getRecastInstance(spellIdString);
            int nextRemaining = oldInstance.getRemainingRecasts() - 1;

            if (nextRemaining >= 0) {
                RecastInstance newInstance = new RecastInstance(
                        oldInstance.getSpellId(),
                        oldInstance.getSpellLevel(),
                        nextRemaining + 1,
                        oldInstance.getTicksToLive(),
                        oldInstance.getCastSource(),
                        oldInstance.getCastData()
                );
                magicData.getPlayerRecasts().addRecast(newInstance, magicData);
            } else {
                magicData.getPlayerRecasts().removeRecast(oldInstance, io.redspace.ironsspellbooks.capabilities.magic.RecastResult.USED_ALL_RECASTS);
            }
        }
    }

    private static void executeSpell(AbstractSpell spell, int level, Player player, MagicData magicData) {
        try {
            // 第3引数(LivingEntity)にplayerを渡すことで、弾丸の主人が詠唱したことにする
            spell.onCast(player.level(), level, player, CastSource.SWORD, magicData);
        } catch (Exception e) {
            IronsArms.LOGGER.error("Failed to cast spell: " + spell.getSpellResource().toString(), e);
        }
    }

    private static boolean checkForCalamityRing(Player player) {
        try {
            if (calamityRingExists == null) initializeCalamityRingDetection();
            if (!calamityRingExists || ringItemInstance == null) return false;
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object curiosHelper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            java.lang.reflect.Method findFirstCurio = curiosHelper.getClass().getMethod("findFirstCurio", LivingEntity.class, Item.class);
            Object result = findFirstCurio.invoke(curiosHelper, player, ringItemInstance);
            if (result instanceof java.util.Optional<?> optional) return optional.isPresent();
        } catch (Exception e) { return false; }
        return false;
    }

    private static void initializeCalamityRingDetection() {
        try {
            Class<?> itemRegistryClass = Class.forName("inovation_and_control.inovation_and_control.registry.ItemRegistry");
            java.lang.reflect.Field ringField = itemRegistryClass.getDeclaredField("RING_OF_CALAMITY");
            ringField.setAccessible(true);
            Object registryObject = ringField.get(null);
            java.lang.reflect.Method getMethod = registryObject.getClass().getMethod("get");
            Object item = getMethod.invoke(registryObject);
            if (item instanceof Item castedItem) {
                ringItemInstance = castedItem;
                calamityRingExists = true;
            }
        } catch (Exception e) { calamityRingExists = false; }
    }
}
