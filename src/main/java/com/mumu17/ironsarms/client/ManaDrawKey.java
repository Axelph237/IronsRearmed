package com.mumu17.ironsarms.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.network.ManaDrawPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = IronsArms.MODID, value = Dist.CLIENT)
public class ManaDrawKey {
    public static final KeyMapping KEY = new KeyMapping("key.ironsarms.mana_draw", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.ironsarms");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @EventBusSubscriber(modid = IronsArms.MODID, value = Dist.CLIENT)
    public static class Presses {
        private static boolean wasDown = false;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            boolean clicked = false;
            while (KEY.consumeClick()) clicked = true;
            // Held keys send OS key-repeat events, which Minecraft counts as clicks; only a fresh press charges.
            if (clicked && !wasDown) PacketDistributor.sendToServer(new ManaDrawPacket());
            wasDown = KEY.isDown();
        }
    }
}
