package com.github.hashicraft.microservices.blocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.microservices.Client;
import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.microservices.events.DatabaseBlockClicked;
import com.github.hashicraft.microservices.events.Messages;
import com.github.hashicraft.stateful.blocks.StatefulBlock;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class DatabaseBlock extends StatefulBlock {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBlock.class);

  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
  public static final BooleanProperty POWERED = Properties.POWERED;

  public DatabaseBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
  }

  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
      BlockHitResult hit) {

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
    LOGGER.info("createBlockEntity {} {}", pos, Client.isClient());

    return new DatabaseBlockEntity(pos, state, this);
  }

  @Override
  public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
    if (world.isClient()) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBlockPos(pos);

      ClientPlayNetworking.send(Messages.DATABASE_BLOCK_REMOVE, buf);
    }
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
  public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    super.onStateReplaced(state, world, pos, newState, moved);
    world.updateNeighborsAlways(pos, state.getBlock());
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
    ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
      server.execute(() -> {
        handleServerTick(server.getOverworld());
      });
    });
  }

  public static void handleServerTick(ServerWorld world) {
  }
}