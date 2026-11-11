package com.mumu17.ironsarms.registry;

import com.mumu17.ironsarms.IronsArms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> CONDUCTIVE = key("conductive");
    public static final ResourceKey<Enchantment> MANA_DRAW = key("mana_draw");

    public static int level(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return stack.getTagEnchantments().entrySet().stream()
                .filter(entry -> entry.getKey().is(enchantment))
                .mapToInt(entry -> entry.getIntValue())
                .findFirst().orElse(0);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(IronsArms.MODID, path));
    }
}
