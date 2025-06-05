package com.github.hashicraft.microservices.blocks;

import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.microservices.events.DatabaseBlockClicked;
import com.github.hashicraft.stateful.blocks.StatefulBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class DatabaseBlock extends StatefulBlock {
  public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
  public static final BooleanProperty POWERED = Properties.POWERED;

  public DatabaseBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
  }

  @Override
  protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {

    DatabaseBlockEntity blockEntity = (DatabaseBlockEntity) world.getBlockEntity(pos);

    if (world.isClient()) {
      DatabaseBlockClicked.EVENT.invoker().interact(blockEntity, () -> {
        blockEntity.markForUpdate();
      });
    }

    return ActionResult.SUCCESS;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new DatabaseBlockEntity(pos, state, this);
  }

  public boolean emitsRedstonePower(BlockState state) {
    return true;
  }

  public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
    return state.get(POWERED) != false ? 15 : 0;
  }

  @Override
  public BlockRenderType getRenderType(BlockState state) {
    return BlockRenderType.MODEL;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
    stateManager.add(FACING);
    stateManager.add(POWERED);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
  }

  @Override
  protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
    super.onStateReplaced(state, world, pos, moved);
    world.updateNeighborsAlways(pos, state.getBlock(), null); // Replace null with appropriate WireOrientation if needed
  }

  // called after the database query is executed to disable the power output
  @Override
  public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    MicroservicesMod.LOGGER.info("scheduledTick {}", pos);
    if (!state.get(POWERED).booleanValue()) {
      return;
    }

    BlockState newState = state.with(POWERED, false);
    world.setBlockState(pos, newState, Block.NOTIFY_ALL);
  }

  // register this class to listen to server play networkiing events
  public static void registerEvents() {
  }
}