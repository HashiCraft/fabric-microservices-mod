package com.github.hashicraft.microservices.items;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class DataItem extends Item {
  public DataItem(Settings settings) {
    super(settings);
  }

  @Override
  public ActionResult useOnBlock(ItemUsageContext context) {
    return ActionResult.SUCCESS;
  }

  @Override
  public String getTranslationKey() {
    return "item.microservices.data";
  }

  @Override
  public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
    if (!itemStack.hasNbt()) {
      tooltip.add(Text.literal("Empty").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
      return;
    }

    NbtCompound identity = itemStack.getOrCreateNbt();
    String name = identity.getString("name");
    String policies = identity.getString("policies");

    tooltip.add(Text.literal("Name").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
    tooltip.add(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
    tooltip.add(Text.literal(""));
    tooltip.add(Text.literal("Policies").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
    tooltip.add(Text.literal(policies).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
  }

  @Override
  public Text getName() {
    return Text.translatable(this.getTranslationKey());
  }

  @Override
  public Text getName(ItemStack stack) {
    NbtCompound identity = stack.getOrCreateNbt();
    String name = identity.getString("name");

    MutableText text = Text.translatable(this.getTranslationKey());

    if (name != "") {
      text.append(Text.literal(" (" + name + ")").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
    }

    return text;
  }
}