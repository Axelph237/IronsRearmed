package com.mumu17.ironsarms.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.network.ManaDrawPacket;
import com.mumu17.ironsarms.network.ModNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = IronsArms.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ManaDrawKey {
    public static final KeyMapping KEY = new KeyMapping("key.ironsarms.mana_draw", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.ironsarms");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @Mod.EventBusSubscriber(modid = IronsArms.MODID, value = Dist.CLIENT)
    public static class Presses {
        private static boolean wasDown = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            boolean clicked = false;
            while (KEY.consumeClick()) clicked = true;
            // Held keys send OS key-repeat events, which Minecraft counts as clicks; only a fresh press charges.
            if (clicked && !wasDown) ModNetworking.INSTANCE.sendToServer(new ManaDrawPacket());
            wasDown = KEY.isDown();
        }
    }
}
