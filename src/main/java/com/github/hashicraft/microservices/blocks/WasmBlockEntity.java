package com.github.hashicraft.microservices.blocks;

import java.util.ArrayList;

import com.github.hashicraft.microservices.ModBlockEntities;
import com.github.hashicraft.microservices.ModBlocks;
import com.github.hashicraft.microservices.ModItems;
import com.github.hashicraft.microservices.wasm.WasmRuntime;
import com.github.hashicraft.stateful.blocks.StatefulBlockEntity;
import com.github.hashicraft.stateful.blocks.Syncable;

import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickPriority;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WasmBlockEntity extends StatefulBlockEntity implements WasmInventory {
  private static final Logger LOGGER = LoggerFactory.getLogger(WasmBlockEntity.class);

  public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

  private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);

  private final BlockPos pos;

  private static ExecutorService service = new ThreadPoolExecutor(4, 1000, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

  @Syncable
  public ArrayList<String> modules = new ArrayList<String>();

  @Syncable
  public ArrayList<String> names = new ArrayList<String>();

  @Syncable
  public String function;

  public void setFunction(String function) {
    this.function = function;
  }

  public String getFunction() {
    return function;
  }

  public void setModules(ArrayList<String> modules) {
    this.modules = modules;
  }

  public void setModuleNames(ArrayList<String> names) {
    this.names = names;
  }

  public ArrayList<String> getModules() {
    return modules;
  }

  public ArrayList<String> getModuleNames() {
    return names;
  }

  public WasmBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.WASM_BLOCK_ENTITY, pos, state, null);
    this.pos = pos;
  }

  public WasmBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(ModBlockEntities.WASM_BLOCK_ENTITY, pos, state, parent);
    this.pos = pos;
  }

  @Override
  public DefaultedList<ItemStack> getItems() {
    return items;
  }

  @Override
  public BlockPos getBlockPos() {
    return pos;
  }

  @Override
  public void executeWasmFunction(String requestID, String data) {
    LOGGER.info("Executing Wasm Function {} for request {} with data {}", function, requestID, data);

    service.submit(() -> {
      try {
        WasmRuntime runtime = WasmRuntime.getInstance();
        ArrayList<String> modules = new ArrayList<String>();

        var function = this.getFunction();

        if (function == null || function.isEmpty()) {
          LOGGER.error("No function specified to execute in the Wasm module");
          return;
        }

        if (this.getModules().isEmpty()) {
          LOGGER.error("No modules specified to execute the function in");
          return;
        }

        for (int n = 0; n < this.getModules().size(); n++) {
          modules.add(runtime.getModule(this.getModules().get(n), this.getModuleNames().get(n)));
        }

        Object fnResult = runtime.executeModuleFunction(String.class, modules.toArray(new String[modules.size()]),
            function, new String[] { data });

        // everything is ok emit redstone power
        BlockState state = world.getBlockState(pos);
        state = state.with(DatabaseBlock.POWERED, true);
        world.setBlockState(pos, state, Block.NOTIFY_ALL);

        // schedule a block tick to update the block so it can disable
        world.scheduleBlockTick(pos, ModBlocks.WASM_BLOCK, 40, TickPriority.NORMAL);

        // create a data item
        ItemStack card = new ItemStack(ModItems.DATA_ITEM);

        // create a dispense location
        Direction direction = world.getBlockState(pos).get(FACING);

        NbtCompound nbt = card.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putString("request_id", requestID);
        nbt.putString("data", fnResult.toString());
        card.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // dispense the block
        dispense(world, pos, card, 1, direction);
      } catch (Exception e) {
        LOGGER.error("Error executing Wasm function: {}", e.getMessage());
        e.printStackTrace();
      }

      this.markForUpdate();
    });
  }

  private void dispense(World world, BlockPos pos, ItemStack stack, int offset, Direction side) {
    // get the opposite side so that it dispenses from the read of the block
    side = side.getOpposite();

    double x = pos.getX() + 0.7D * (double) side.getOffsetX();
    double y = pos.getY() + 0.7D * (double) side.getOffsetY();
    double z = pos.getZ() + 0.7D * (double) side.getOffsetZ();

    if (side.getAxis() == Direction.Axis.Y) {
      y -= 0.425D;
    } else {
      y -= 0.45625D;
    }

    ItemEntity entity = new ItemEntity(world, x, y, z, stack);
    double g = world.random.nextDouble() * 0.1D + 0.2D;
    entity.setVelocity(
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset +
            (double) side.getOffsetX() * g,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset +
            0.20000000298023224D,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset +
            (double) side.getOffsetZ() * g);
    world.spawnEntity(entity);
  }

}