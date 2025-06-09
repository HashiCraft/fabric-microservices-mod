package com.github.hashicraft.microservices.blocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

import com.github.hashicraft.microservices.Client;
import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.microservices.events.WasmBlockClicked;
import com.github.hashicraft.microservices.wasm.WasmRuntime;
import com.github.hashicraft.stateful.blocks.StatefulBlock;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class WasmBlock extends StatefulBlock {
  private static final Logger LOGGER = LoggerFactory.getLogger(WasmBlock.class);

  public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
  public static final BooleanProperty POWERED = Properties.POWERED;
  public static final IntProperty POWER = Properties.POWER;

  public WasmBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    setDefaultState(getStateManager().getDefaultState().with(POWER, 15));
  }

  @Override
  protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    WasmBlockEntity blockEntity = (WasmBlockEntity) world.getBlockEntity(pos);

    if (world.isClient()) {
      WasmBlockClicked.EVENT.invoker().interact(blockEntity, () -> {
        blockEntity.markForUpdate();
      });
    }

    return ActionResult.SUCCESS;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    LOGGER.info("createBlockEntity {} {}", pos, Client.isClient());

    return new WasmBlockEntity(pos, state, this);
  }

  @Override
  public boolean emitsRedstonePower(BlockState state) {
    return true;
  }

  @Override
  public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
    return state.get(POWERED) != false ? state.get(Properties.POWER) : 0;
  }

  @Override
  public BlockRenderType getRenderType(BlockState state) {
    return BlockRenderType.MODEL;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
    stateManager.add(FACING);
    stateManager.add(POWERED);
    stateManager.add(POWER);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
  }

  @Override
  protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
    super.onStateReplaced(state, world, pos, moved);
    world.updateNeighborsAlways(pos, state.getBlock(), null);
  }

  // called after the wasm function is executed to disable the power output
  @Override
  public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    MicroservicesMod.LOGGER.info("scheduledTick {} {}", pos, state.get(POWER));

    if (!state.get(POWERED).booleanValue()) {
      return;
    }

    BlockState newState = state.with(POWERED, false);
    world.setBlockState(pos, newState, Block.NOTIFY_ALL);
  }

  public static void startRuntime() {
    MicroservicesMod.LOGGER.info("Starting Wasm Runtime");

    // create the wasm module filesystem directory if it does not exist
    File wasmPath = new File("./wasm_runtime");
    if (!wasmPath.exists()) {
      wasmPath.mkdirs();
    }

    WasmRuntime.getInstance().init(Paths.get("./wasm_runtime"));
  }

  public static void registerEvents() {
    ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
      server.execute(() -> {
        handleServerTick(server.getOverworld());
      });
    });
  }

  public static void handleServerTick(ServerWorld world) {
  }
}