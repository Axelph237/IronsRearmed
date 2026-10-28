package com.mumu17.ironsarms.utils;

import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.registry.ModEnchantments;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class GunMana {
    private static final String MANA = IronsArms.MODID + ":Mana";
    private static final String INSCRIBED_SPELL = "InscribedSpell";

    public record InscribedSpell(AbstractSpell spell, int level) {}

    public static Optional<InscribedSpell> inscribedSpell(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(INSCRIBED_SPELL)) return Optional.empty();
        CompoundTag spellTag = tag.getCompound(INSCRIBED_SPELL);
        AbstractSpell spell = SpellRegistry.getSpell(spellTag.getString("SpellID"));
        if (spell == null || spell == SpellRegistry.none()) return Optional.empty();
        return Optional.of(new InscribedSpell(spell, spellTag.getInt("Level")));
    }

    public static int manaDrawLevel(ItemStack gun) {
        return gun.getEnchantmentLevel(ModEnchantments.MANA_DRAW.get());
    }

    public static int stored(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        return tag == null ? 0 : tag.getInt(MANA);
    }

    private static void setStored(ItemStack gun, int mana) {
        gun.getOrCreateTag().putInt(MANA, mana);
    }

    public static boolean pay(ServerPlayer player, ItemStack gun, int cost) {
        if (payFromGun(gun, cost)) return true;
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData.getMana() < cost) return false;
        magicData.setMana(magicData.getMana() - cost);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        return true;
    }

    public static boolean payFromGun(ItemStack gun, int cost) {
        if (manaDrawLevel(gun) <= 0 || stored(gun) < cost) return false;
        setStored(gun, stored(gun) - cost);
        return true;
    }
}
