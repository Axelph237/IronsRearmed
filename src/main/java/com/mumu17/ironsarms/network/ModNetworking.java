package com.mumu17.ironsarms.network;

import com.mumu17.ironsarms.IronsArms;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = IronsArms.MODID)
public class ModNetworking {
    public static final String PROTOCOL_VERSION = "2";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToServer(ManaDrawPacket.TYPE, ManaDrawPacket.STREAM_CODEC, ManaDrawPacket::handle);
    }
}
