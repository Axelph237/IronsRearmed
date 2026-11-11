package com.mumu17.ironsarms;

import com.mojang.logging.LogUtils;
import com.mumu17.ironsarms.config.ManaDrawConfig;
import com.mumu17.ironsarms.registry.ModDataComponents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(IronsArms.MODID)
public class IronsArms {
    public static final String MODID = "ironsarms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IronsArms(IEventBus modBus, ModContainer container) {
        if (ModList.get().isLoaded("arscurios")
                || ModList.get().isLoaded("armslib")
                || ModList.get().isLoaded("castlib")) {
            throw new ModLoadingException(ModLoadingIssue.error(
                    "This mod is incompatible with §eArsCurios, ArmsLib and CastLib§r. Please remove them."));
        }
        ModDataComponents.COMPONENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ManaDrawConfig.SPEC);
    }
}
