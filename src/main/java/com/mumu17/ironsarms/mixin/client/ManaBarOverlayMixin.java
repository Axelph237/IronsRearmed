package com.mumu17.ironsarms.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mumu17.ironsarms.registry.ModEnchantments;
import com.mumu17.ironsarms.utils.GunMana;
import io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(value = ManaBarOverlay.class, remap = false)
public class ManaBarOverlayMixin {
    @WrapOperation(method = "shouldShowManaBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isHolding(Ljava/util/function/Predicate;)Z", remap = true))
    private static boolean ironsarms$showWhenHoldingManaGun(Player player, Predicate<ItemStack> isCastingItem, Operation<Boolean> original) {
        return original.call(player, isCastingItem)
                || player.isHolding(stack -> stack.getEnchantmentLevel(ModEnchantments.CONDUCTIVE.get()) > 0 || GunMana.manaDrawLevel(stack) > 0);
    }
}
