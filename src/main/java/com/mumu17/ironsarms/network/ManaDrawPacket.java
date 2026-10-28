package com.mumu17.ironsarms.network;

import com.mumu17.ironsarms.utils.GunMana;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManaDrawPacket {
    public ManaDrawPacket() {}

    public ManaDrawPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        ItemStack gun = player.getMainHandItem();
        if (GunMana.inscribedSpell(gun).isEmpty() || GunMana.manaDrawLevel(gun) <= 0) return;
        if (GunMana.charge(player, gun) > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
