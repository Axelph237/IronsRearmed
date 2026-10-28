package com.mumu17.ironsarms.config;

import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ManaDrawConfig {
    public record ManaDrawTier(int cap, double efficiency) {}

    private record TierValues(ForgeConfigSpec.IntValue cap, ForgeConfigSpec.DoubleValue efficiency) {}

    private static final List<ManaDrawTier> DEFAULTS = List.of(
            new ManaDrawTier(100, 0.6),
            new ManaDrawTier(250, 0.7),
            new ManaDrawTier(500, 0.8),
            new ManaDrawTier(1000, 0.9),
            new ManaDrawTier(2000, 1.0));

    private static final List<TierValues> TIERS;
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        List<TierValues> tiers = new ArrayList<>();
        builder.comment("Mana Draw enchantment, one section per enchantment level").push("manaDraw");
        for (int i = 0; i < DEFAULTS.size(); i++) {
            ManaDrawTier defaults = DEFAULTS.get(i);
            builder.push("level" + (i + 1));
            tiers.add(new TierValues(
                    builder.comment("Most mana the gun can store").defineInRange("cap", defaults.cap(), 0, Integer.MAX_VALUE),
                    builder.comment("Gun mana gained per point of player mana spent").defineInRange("efficiency", defaults.efficiency(), 0.01, 1.0)));
            builder.pop();
        }
        builder.pop();
        TIERS = List.copyOf(tiers);
        SPEC = builder.build();
    }

    public static ManaDrawTier tier(int level) {
        TierValues values = TIERS.get(Mth.clamp(level, 1, TIERS.size()) - 1);
        return new ManaDrawTier(values.cap().get(), values.efficiency().get());
    }
}
