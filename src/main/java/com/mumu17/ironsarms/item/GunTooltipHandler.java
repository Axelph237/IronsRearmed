package com.mumu17.ironsarms.item;

import com.mumu17.ironsarms.IronsArms;
import com.mumu17.ironsarms.registry.ModEnchantments;
import com.mumu17.ironsarms.utils.GunMana;
import com.tacz.guns.api.item.IGun;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = IronsArms.MODID, value = Dist.CLIENT)
public class GunTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IGun)) return;
        GunMana.inscribedSpell(stack).ifPresent(inscribed -> {
            AbstractSpell spell = inscribed.getSpell();
            Player player = event.getEntity();
            List<Component> lines = new ArrayList<>();

            Component level = player != null
                    ? TooltipsUtils.getLevelComponenet(inscribed, player)
                    : Component.literal(String.valueOf(inscribed.getLevel()));
            lines.add(indent(Component.translatable("tooltip.irons_spellbooks.selected_spell", spell.getDisplayName(player), level)
                    .withStyle(spell.getSchoolType().getDisplayName().getStyle())));
            if (player != null) {
                spell.getUniqueInfo(inscribed.getLevel(), player).forEach(line ->
                        lines.add(indent(line.withStyle(line.getStyle().applyTo(TooltipsUtils.getStyleFor(player, spell))))));
            }
            int cost = spell.getManaCost(inscribed.getLevel());
            lines.add(indent(GunMana.manaDrawLevel(stack) > 0
                    ? Component.translatable("tooltip." + IronsArms.MODID + ".mana", GunMana.stored(stack), cost).withStyle(ChatFormatting.BLUE)
                    : Component.translatable("tooltip." + IronsArms.MODID + ".mana_cost", cost).withStyle(ChatFormatting.BLUE)));

            List<Component> tooltip = event.getToolTip();
            tooltip.addAll(indexAfterConductive(tooltip), lines);
        });
    }

    private static int indexAfterConductive(List<Component> tooltip) {
        String conductiveKey = Util.makeDescriptionId("enchantment", ModEnchantments.CONDUCTIVE.location());
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
