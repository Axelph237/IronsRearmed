package com.mumu17.ironsarms.network;

import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.utils.GunMana;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ManaDrawPacket() implements CustomPacketPayload {
    public static final Type<ManaDrawPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IronsArms.MODID, "mana_draw"));
    public static final StreamCodec<ByteBuf, ManaDrawPacket> STREAM_CODEC = StreamCodec.unit(new ManaDrawPacket());

    @Override
    public Type<ManaDrawPacket> type() {
        return TYPE;
    }

    public static void handle(ManaDrawPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack gun = player.getMainHandItem();
        if (GunMana.inscribedSpell(gun).isEmpty() || GunMana.manaDrawLevel(gun) <= 0) return;
        if (GunMana.charge(player, gun) > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
