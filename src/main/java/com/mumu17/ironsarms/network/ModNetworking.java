package com.mumu17.ironsarms.network;

import com.mumu17.ironsarms.IronsArms;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    public static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(IronsArms.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    public static void register() {
        INSTANCE.messageBuilder(ManaDrawPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ManaDrawPacket::encode)
                .decoder(ManaDrawPacket::new)
                .consumerMainThread(ManaDrawPacket::handle)
                .add();
    }
}
