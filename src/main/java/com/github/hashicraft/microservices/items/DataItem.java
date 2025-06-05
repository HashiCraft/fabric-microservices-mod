package com.github.hashicraft.microservices.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.item.tooltip.TooltipType;

import java.util.function.Consumer;

public class DataItem extends Item {
  public DataItem(Settings settings) {
    super(settings);
  }

  @Override
  public ActionResult useOnBlock(ItemUsageContext context) {
    return ActionResult.SUCCESS;
  }

  @Override
  public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
      Consumer<Text> textConsumer, TooltipType type) {
  }

  @Override
  public Text getName(ItemStack stack) {
    NbtCompound item = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    String name = item.getString("request_id", "");

    MutableText text = Text.translatable(this.getTranslationKey());

    if (name != "") {
      text.append(Text.literal(" (" + name + ")").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
    }

    return text;
  }
}