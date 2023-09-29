package com.github.hashicraft.microservices.blocks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.microservices.Client;
import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.microservices.events.DatabaseBlockClicked;
import com.github.hashicraft.microservices.events.Messages;
import com.github.hashicraft.microservices.interpolation.Interpolate;
import com.github.hashicraft.stateful.blocks.StatefulBlock;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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
import net.minecraft.world.tick.TickPriority;

public class DatabaseBlock extends StatefulBlock {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBlock.class);

  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
  public static final BooleanProperty POWERED = Properties.POWERED;

  // keeps a map of registered database blocks so we can check for updates
  // on server tick
  private static HashMap<BlockPos, Boolean> DATABASES = new HashMap<BlockPos, Boolean>();
  // background thread service
  private static ExecutorService service = new ThreadPoolExecutor(4, 1000, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

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
  public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
    if (world.isClient()) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBlockPos(pos);

      ClientPlayNetworking.send(Messages.DATABASE_BLOCK_REMOVE, buf);
    }
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    LOGGER.info("createBlockEntity {} {}", pos, Client.isClient());

    if (Client.isClient()) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBlockPos(pos);

      ClientPlayNetworking.send(Messages.DATABASE_BLOCK_REGISTER, buf);
    }

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
  public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    super.onStateReplaced(state, world, pos, newState, moved);
    world.updateNeighborsAlways(pos, state.getBlock());
  }

  @Override
  // scheduledTick is called after the sql statement has been executed
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
    ServerPlayNetworking.registerGlobalReceiver(Messages.DATABASE_BLOCK_REGISTER,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();
          handleBlockRegister(pos);
        });

    ServerPlayNetworking.registerGlobalReceiver(Messages.DATABASE_BLOCK_REMOVE,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();
          handleBlockRemove(pos);

        });

    ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
      server.execute(() -> {
        handleServerTick(server.getOverworld());
      });
    });

  }

  public static void handleBlockRegister(BlockPos pos) {
    MicroservicesMod.LOGGER.info("Received register_database message {}", pos);
    if (!DATABASES.containsKey(pos)) {
      DATABASES.put(pos, false);
    }
  }

  public static void handleBlockRemove(BlockPos pos) {
    MicroservicesMod.LOGGER.info("Received remove_database message {}", pos);
    if (DATABASES.containsKey(pos)) {
      DATABASES.remove(pos);
    }
  }

  public static void handleServerTick(ServerWorld world) {
    // check if the block is powered
    for (Entry<BlockPos, Boolean> entry : DATABASES.entrySet()) {
      BlockPos pos = entry.getKey();
      DatabaseBlockEntity blockEntity = (DatabaseBlockEntity) world.getBlockEntity(pos);
      int power = world.getReceivedRedstonePower(pos);

      if (power > 0 && !entry.getValue()) {
        MicroservicesMod.LOGGER.info("block {} power on {}", entry.getKey(), power);
        DATABASES.put(pos, true);

        executeDBQuery(world, blockEntity);
      }

      // when power is off reset the block
      if (power == 0 && entry.getValue()) {
        DATABASES.put(pos, false);
      }
    }
  }

  private static void executeDBQuery(ServerWorld world, DatabaseBlockEntity blockEntity) {
    service.submit(() -> {
      try {
        LOGGER.info("Executing SQL statement {}", blockEntity.getSQLStatement());

        String result = executeSQLStatement(blockEntity);
        blockEntity.result = result;

        BlockState state = blockEntity.getCachedState().with(DatabaseBlock.POWERED, true);
        world.setBlockState(blockEntity.getPos(), state, Block.NOTIFY_ALL);
      } catch (SQLException e) {
        LOGGER.error("Error executing SQL statement {}", e);
        blockEntity.result = e.getMessage();
      }

      // update the block
      blockEntity.setPropertiesToState();
      blockEntity.serverStateUpdated(blockEntity.serverState);

      // schedule a block tick to update the block so it can disable
      world.scheduleBlockTick(blockEntity.getPos(), MicroservicesMod.DATABASE_BLOCK, 40, TickPriority.NORMAL);
    });
  }

  private static String executeSQLStatement(DatabaseBlockEntity blockEntity) throws SQLException {
    // get the database details from the block entity gui
    // we will substitute any environment variables that may be embedded in here
    String address = Interpolate.getValue(blockEntity.getDbAddress());
    String username = Interpolate.getValue(blockEntity.getUsername());
    String password = Interpolate.getValue(blockEntity.getPassword());
    String database = Interpolate.getValue(blockEntity.getDatabase());
    String sql = Interpolate.getValue(blockEntity.getSQLStatement());

    // execute the SQL statement
    Connection conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s/%s", address, database),
        username, password);
    Statement st = conn.createStatement();
    st.execute(sql);
    st.close();

    return "success";
  }
}