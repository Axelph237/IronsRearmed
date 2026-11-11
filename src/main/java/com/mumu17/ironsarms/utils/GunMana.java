package com.mumu17.ironsarms.utils;

import com.mumu17.ironsarms.config.ManaDrawConfig;
import com.mumu17.ironsarms.registry.ModDataComponents;
import com.mumu17.ironsarms.registry.ModEnchantments;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public class GunMana {
    public static Optional<SpellData> inscribedSpell(ItemStack gun) {
        return Optional.ofNullable(gun.get(ModDataComponents.INSCRIBED_SPELL))
                .filter(spellData -> spellData.getSpell() != SpellRegistry.none());
    }

    public static int manaDrawLevel(ItemStack gun) {
        return ModEnchantments.level(gun, ModEnchantments.MANA_DRAW);
    }

    public static int stored(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.STORED_MANA, 0);
    }

    private static void setStored(ItemStack gun, int mana) {
        gun.set(ModDataComponents.STORED_MANA, mana);
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

    public static int charge(ServerPlayer player, ItemStack gun) {
        ManaDrawConfig.ManaDrawTier tier = ManaDrawConfig.tier(manaDrawLevel(gun));
        int room = tier.cap() - stored(gun);
        if (room <= 0) return 0;
        MagicData magicData = MagicData.getPlayerMagicData(player);
        int playerSpend = Math.min((int) magicData.getMana(), (int) Math.ceil(room / tier.efficiency()));
        int gained = Math.min(room, (int) Math.floor(playerSpend * tier.efficiency()));
        if (gained <= 0) return 0;
        magicData.setMana(magicData.getMana() - playerSpend);
        setStored(gun, stored(gun) + gained);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        return gained;
    }
}
