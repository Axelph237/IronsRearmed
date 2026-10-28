package com.mumu17.ironsarms.event;

import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.utils.GunTags;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronsArms.MODID)
public class GunManaChargeEvents {
    private static final int CHARGE_INTERVAL_TICKS = 20;
    private static final int CHARGE_AMOUNT = 100;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % CHARGE_INTERVAL_TICKS != 0) return;
        for (ItemStack stack : player.getInventory().items) {
            if (!GunTags.isTargetItem(stack) || stack.getTag() == null || !stack.getTag().contains("InscribedSpell")) continue;
            chargeGun(player, stack);
            return;
        }
    }

    private static void chargeGun(ServerPlayer player, ItemStack gun) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        int charge = Math.min(CHARGE_AMOUNT, GunTags.MAX_MANA - GunTags.getMana(gun));
        if (charge <= 0 || magicData.getMana() < CHARGE_AMOUNT) return;
        magicData.addMana(-charge);
        GunTags.addMana(gun, charge);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }
}
