package com.mumu17.ironsarms.item;

import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.registry.ModEnchantments;
import com.mumu17.ironsarms.utils.GunMana;
import com.tacz.guns.api.item.IGun;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = IronsArms.MODID, value = Dist.CLIENT)
public class GunTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IGun)) return;
        GunMana.inscribedSpell(stack).ifPresent(inscribed -> {
            AbstractSpell spell = inscribed.spell();
            Player player = event.getEntity();
            List<Component> lines = new ArrayList<>();

            Component level = player != null
                    ? TooltipsUtils.getLevelComponenet(new SpellData(spell, inscribed.level()), player)
                    : Component.literal(String.valueOf(inscribed.level()));
            lines.add(indent(Component.translatable("tooltip.irons_spellbooks.selected_spell", spell.getDisplayName(player), level)
                    .withStyle(spell.getSchoolType().getDisplayName().getStyle())));
            if (player != null) {
                spell.getUniqueInfo(inscribed.level(), player).forEach(line ->
                        lines.add(indent(line.withStyle(line.getStyle().applyTo(TooltipsUtils.getStyleFor(player, spell))))));
            }
            int cost = spell.getManaCost(inscribed.level());
            lines.add(indent(GunMana.manaDrawLevel(stack) > 0
                    ? Component.translatable("tooltip." + IronsArms.MODID + ".mana", GunMana.stored(stack), cost).withStyle(ChatFormatting.BLUE)
                    : Component.translatable("tooltip." + IronsArms.MODID + ".mana_cost", cost).withStyle(ChatFormatting.BLUE)));

            List<Component> tooltip = event.getToolTip();
            tooltip.addAll(indexAfterConductive(tooltip), lines);
        });
    }

    private static int indexAfterConductive(List<Component> tooltip) {
        String conductiveKey = ModEnchantments.CONDUCTIVE.get().getDescriptionId();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getContents() instanceof TranslatableContents contents && contents.getKey().equals(conductiveKey)) {
                return i + 1;
            }
        }
        return tooltip.size();
    }

    private static Component indent(Component line) {
        return Component.literal(" ").append(line);
    }
}
