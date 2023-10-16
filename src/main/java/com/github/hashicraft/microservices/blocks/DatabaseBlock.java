package com.github.hashicraft.microservices.blocks;

import java.io.IOException;
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
  private static Databases DATABASES = new Databases();
  private static boolean initialized = false;

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

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);

        // notify that the server has been reconfigured
        // we need to wait until the block state has synced so wait here
        service.submit(() -> {
          try {
            Thread.sleep(1000);
            ClientPlayNetworking.send(Messages.DATABASE_BLOCK_UPDATED, buf);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        });
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
    ServerPlayNetworking.registerGlobalReceiver(Messages.DATABASE_BLOCK_REMOVE,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();
          handleBlockRemove(pos);
        });

    ServerPlayNetworking.registerGlobalReceiver(Messages.DATABASE_BLOCK_UPDATED,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();

          // this needs to happen on the server thread or the block entity will not exist
          server.execute(() -> {
            handleBlockUpdate(pos, server.getOverworld());
          });
        });

    ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
      server.execute(() -> {
        if (!initialized) {
          initialized = true;
          DATABASES = Databases.loadFromConfig();
        }

        handleServerTick(server.getOverworld());
      });
    });
  }

  public static void handleBlockUpdate(BlockPos pos, ServerWorld world) {
    DatabaseBlockEntity blockEntity = (DatabaseBlockEntity) world.getBlockEntity(pos);

    MicroservicesMod.LOGGER.info("Received update_database message {}", pos);

    DatabaseContext context = new DatabaseContext();
    context.setAddress(blockEntity.getDbAddress());
    context.setUsername(blockEntity.getUsername());
    context.setPassword(blockEntity.getPassword());
    context.setDatabase(blockEntity.getDatabase());
    context.setSQL(blockEntity.getSQLStatement());

    DATABASES.add(pos, context);

    try {
      DATABASES.writeToConfig();
    } catch (IOException e) {
      MicroservicesMod.LOGGER.info("Unable to write config {}", e.getMessage());
    }
  }

  public static void handleBlockRemove(BlockPos pos) {
    MicroservicesMod.LOGGER.info("Received remove_database message {}", pos);
    if (DATABASES.exists(pos)) {
      DATABASES.remove(pos);
    }
  }

  public static void handleServerTick(ServerWorld world) {
    // check if the block is powered
    for (Entry<BlockPos, DatabaseContext> entry : DATABASES.entrySet()) {
      BlockPos pos = entry.getKey();
      int power = world.getReceivedRedstonePower(pos);

      DatabaseContext db = entry.getValue();

      if (power > 0 && !db.getActive()) {
        MicroservicesMod.LOGGER.info("block {} power on {}", entry.getKey(), power);

        // update active before executing the query as the query may take multiple
        // ticks
        db.setActive(true);
        DATABASES.add(pos, db);

        executeDBQuery(world, pos, db);
      }

      // when power is off reset the block
      if (power == 0 && db.getActive()) {
        db.setActive(false);
        DATABASES.add(pos, db);
      }
    }
  }

  private static void executeDBQuery(ServerWorld world, BlockPos pos, DatabaseContext ctx) {
    service.submit(() -> {
      try {
        LOGGER.info("Executing SQL statement {}", ctx.getSQL());

        String result = executeSQLStatement(ctx);
        // blockEntity.result = result;

        // everything is ok emit redstone power
        BlockState state = world.getBlockState(pos);
        state = state.with(DatabaseBlock.POWERED, true);
        world.setBlockState(pos, state, Block.NOTIFY_ALL);

        // schedule a block tick to update the block so it can disable
        world.scheduleBlockTick(pos, MicroservicesMod.DATABASE_BLOCK, 40, TickPriority.NORMAL);
      } catch (SQLException e) {
        LOGGER.error("Error executing SQL statement {}", e);
        // blockEntity.result = e.getMessage();
      }
    });
  }

  private static String executeSQLStatement(DatabaseContext ctx) throws SQLException {
    // get the database details from the block entity gui
    // we will substitute any environment variables that may be embedded in here
    String address = Interpolate.getValue(ctx.getAddress());
    String username = Interpolate.getValue(ctx.getUsername());
    String password = Interpolate.getValue(ctx.getPassword());
    String database = Interpolate.getValue(ctx.getDatabase());
    String sql = Interpolate.getValue(ctx.getSQL());

    // execute the SQL statement
    Connection conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s/%s", address, database),
        username, password);
    Statement st = conn.createStatement();
    st.execute(sql);
    st.close();

    return "success";
  }
}