package com.mumu17.ironsarms.registry;

import com.mojang.serialization.Codec;
import com.mumu17.ironsarms.IronsArms;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IronsArms.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpellData>> INSCRIBED_SPELL = COMPONENTS.registerComponentType("inscribed_spell",
            builder -> builder.persistent(SpellData.CODEC)
                    .networkSynchronized(StreamCodec.<FriendlyByteBuf, SpellData>of(SpellData::writeToBuffer, SpellData::readFromBuffer).cast()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_MANA = COMPONENTS.registerComponentType("stored_mana",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
}
