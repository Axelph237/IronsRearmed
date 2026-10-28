package com.mumu17.ironsarms.registry;

import com.mumu17.ironsarms.IronsArms;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final EnchantmentCategory GUN = EnchantmentCategory.create("IRONSARMS_GUN", item -> item instanceof IGun);

    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, IronsArms.MODID);

    public static final RegistryObject<Enchantment> CONDUCTIVE = ENCHANTMENTS.register("conductive",
            () -> new GunEnchantment(Enchantment.Rarity.UNCOMMON, 1, 15, 0, 50));
    public static final RegistryObject<Enchantment> MANA_DRAW = ENCHANTMENTS.register("mana_draw",
            () -> new GunEnchantment(Enchantment.Rarity.RARE, 5, 1, 9, 15));

    private static class GunEnchantment extends Enchantment {
        private final int maxLevel;
        private final int baseCost;
        private final int costPerLevel;
        private final int costSpan;

        GunEnchantment(Rarity rarity, int maxLevel, int baseCost, int costPerLevel, int costSpan) {
            super(rarity, GUN, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
            this.maxLevel = maxLevel;
            this.baseCost = baseCost;
            this.costPerLevel = costPerLevel;
            this.costSpan = costSpan;
        }

        @Override
        public int getMaxLevel() {
            return maxLevel;
        }

        @Override
        public int getMinCost(int level) {
            return baseCost + (level - 1) * costPerLevel;
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + costSpan;
        }
    }
}
