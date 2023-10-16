package com.github.hashicraft.microservices.blocks;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.microservices.Client;
import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.microservices.events.Messages;
import com.github.hashicraft.microservices.events.WebserverBlockClicked;
import com.github.hashicraft.microservices.interpolation.Interpolate;
import com.github.hashicraft.stateful.blocks.StatefulBlock;

import io.javalin.Javalin;
import io.javalin.http.Context;
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

public class WebserverBlock extends StatefulBlock {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebserverBlock.class);

  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
  public static final BooleanProperty POWERED = Properties.POWERED;

  // keeps a map of registered database blocks so we can check for updates
  // on server tick
  private static Webservers SERVERS = new Webservers();
  private static boolean initialized = false;

  private static ExecutorService service = new ThreadPoolExecutor(4, 1000, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

  public WebserverBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
  }

  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
      BlockHitResult hit) {

    WebserverBlockEntity blockEntity = (WebserverBlockEntity) world.getBlockEntity(pos);

    if (world.isClient()) {
      WebserverBlockClicked.EVENT.invoker().interact(blockEntity, () -> {
        blockEntity.markForUpdate();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);

        // notify that the server has been reconfigured
        // we need to wait until the block state has synced so wait here
        service.submit(() -> {
          try {
            Thread.sleep(1000);
            ClientPlayNetworking.send(Messages.WEBSERVER_BLOCK_UPDATED, buf);
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

    return new WebserverBlockEntity(pos, state, this);
  }

  @Override
  public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
    if (world.isClient()) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBlockPos(pos);

      ClientPlayNetworking.send(Messages.WEBSERVER_BLOCK_REMOVE, buf);
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
    ServerPlayNetworking.registerGlobalReceiver(Messages.WEBSERVER_BLOCK_REMOVE,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();
          handleBlockRemove(pos);
        });

    ServerPlayNetworking.registerGlobalReceiver(Messages.WEBSERVER_BLOCK_UPDATED,
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

          SERVERS = Webservers.loadFromConfig();

          // start all the servers
          for (Entry<BlockPos, WebserverContext> entry : SERVERS.entrySet()) {
            server.execute(() -> {
              startServer(entry.getKey(), server.getOverworld(), entry.getValue());
            });
          }
        }
      });
    });
  }

  public static void handleBlockRemove(BlockPos pos) {
    MicroservicesMod.LOGGER.info("Received remove_webserver message {}", pos);
    if (SERVERS.exists(pos)) {
      var val = SERVERS.get(pos);

      // when removing a block ensure the server is stopped
      if (val.getServer() != null) {
        val.getServer().close();
      }

      SERVERS.remove(pos);
    }
  }

  public static void handleBlockUpdate(BlockPos pos, ServerWorld world) {
    WebserverBlockEntity blockEntity = (WebserverBlockEntity) world.getBlockEntity(pos);
    var val = SERVERS.get(pos);

    // we might not have created the instance yet
    if (val == null) {
      val = new WebserverContext();
    }

    MicroservicesMod.LOGGER.info("configure server {} port: {} path: {} method: {}", pos, blockEntity.getServerPort(),
        blockEntity.getServerPath(), blockEntity.getServerMethod());

    // update the context
    val.setServerPort(blockEntity.getServerPort());
    val.setServerPath(blockEntity.getServerPath());
    val.setServerMethod(blockEntity.getServerMethod());

    startServer(pos, world, val);

    // serialize the servers collection
    try {
      SERVERS.writeToConfig();
    } catch (IOException e) {
      MicroservicesMod.LOGGER.info("unable to write config {}", e.getMessage());
    }
  }

  public static void startServer(BlockPos pos, ServerWorld world, WebserverContext wctx) {
    // read the values from interpolation
    String port = Interpolate.getValue(wctx.getServerPort());
    String path = Interpolate.getValue(wctx.getServerPath());
    String method = Interpolate.getValue(wctx.getServerMethod());

    // get the port as an integer
    int iPort = 0;
    try {
      iPort = Integer.parseInt(port);
    } catch (NumberFormatException e) {
      MicroservicesMod.LOGGER.error("invalid port, not starting {}", e);
      return;
    }

    if (path.isEmpty() || method.isEmpty()) {
      MicroservicesMod.LOGGER.error("path or method is empty, not starting");
      return;
    }

    if (wctx.getServer() != null) {
      wctx.getServer().close();
    }

    // create the server and set the port
    Javalin javalin = Javalin.create();
    javalin.jettyServer().setServerPort(iPort);
    javalin.jettyServer().setServerHost("0.0.0.0");

    // start the server async
    service.submit(() -> {
      // set the method
      switch (method) {
        case "GET":
          javalin.get(path, ctx -> handleRequest(ctx, world, pos));
          break;
        case "POST":
          javalin.post(path, ctx -> handleRequest(ctx, world, pos));
          break;
        case "PUT":
          javalin.put(path, ctx -> handleRequest(ctx, world, pos));
          break;
        case "DELETE":
          javalin.delete(path, ctx -> handleRequest(ctx, world, pos));
      }
    });

    // start the server
    javalin.start();

    // set the server
    wctx.setServer(javalin);
    SERVERS.add(pos, wctx);
  }

  public static Context handleRequest(Context ctx, ServerWorld world, BlockPos pos) {
    LOGGER.info("Received request {} {} {}", ctx.method(), ctx.path(), pos);

    BlockState state = world.getBlockState(pos);
    state = state.with(DatabaseBlock.POWERED, true);
    world.setBlockState(pos, state, Block.NOTIFY_ALL);

    // schedule a block tick to update the block so it can disable
    world.scheduleBlockTick(pos, MicroservicesMod.WEBSERVER_BLOCK, 40, TickPriority.NORMAL);

    LOGGER.info("Sending response");
    return ctx.result("Hello World");
  }
}