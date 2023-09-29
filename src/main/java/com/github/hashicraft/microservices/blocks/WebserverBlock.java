package com.github.hashicraft.microservices.blocks;

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

  private static ExecutorService service = new ThreadPoolExecutor(4, 1000, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
  public static final BooleanProperty POWERED = Properties.POWERED;

  // keeps a map of registered database blocks so we can check for updates
  // on server tick
  private static HashMap<BlockPos, WebserverContext> SERVERS = new HashMap<BlockPos, WebserverContext>();

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
        ClientPlayNetworking.send(Messages.WEBSERVER_BLOCK_UPDATED, buf);
      });
    }

    return ActionResult.SUCCESS;
  }

  @Override
  public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
    if (world.isClient()) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBlockPos(pos);

      ClientPlayNetworking.send(Messages.WEBSERVER_BLOCK_REMOVE, buf);
    }
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    LOGGER.info("createBlockEntity {} {}", pos, Client.isClient());

    if (Client.isClient()) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBlockPos(pos);

      ClientPlayNetworking.send(Messages.WEBSERVER_BLOCK_REGISTER, buf);
    }

    return new WebserverBlockEntity(pos, state, this);
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
    ServerPlayNetworking.registerGlobalReceiver(Messages.WEBSERVER_BLOCK_REGISTER,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();
          handleBlockRegister(pos);

          // ensure that blocks are configured on the server thread
          // and that the server is started when configured
          server.execute(() -> {
            handleConfigureServer(pos, server.getOverworld());
          });
        });

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

          server.execute(() -> {
            handleConfigureServer(pos, server.getOverworld());
          });
        });

    ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
      server.execute(() -> {
        handleServerTick(server.getOverworld());
      });
    });

  }

  public static void handleBlockRegister(BlockPos pos) {
    MicroservicesMod.LOGGER.info("Received register_webserver message {}", pos);
    if (!SERVERS.containsKey(pos)) {
      SERVERS.put(pos, new WebserverContext(null, false));
    }
  }

  public static void handleBlockRemove(BlockPos pos) {
    MicroservicesMod.LOGGER.info("Received remove_webserver message {}", pos);
    if (SERVERS.containsKey(pos)) {
      var val = SERVERS.get(pos);

      // when removing a block ensure the server is stopped
      if (val.server != null) {
        val.server.close();
      }

      SERVERS.remove(pos);
    }
  }

  public static void handleConfigureServer(BlockPos pos, ServerWorld world) {
    WebserverBlockEntity blockEntity = (WebserverBlockEntity) world.getBlockEntity(pos);
    var val = SERVERS.get(pos);

    String port = Interpolate.getValue(blockEntity.getServerPort());
    int iPort = 0;

    try {
      iPort = Integer.parseInt(port);
    } catch (NumberFormatException e) {
      MicroservicesMod.LOGGER.error("invalid port {}", port);
    }

    String path = Interpolate.getValue(blockEntity.getServerPath());
    String method = Interpolate.getValue(blockEntity.getServerMethod());

    // no server port set, do nothing
    if (iPort == 0 || path.isEmpty() || method.isEmpty()) {
      return;
    }

    MicroservicesMod.LOGGER.info("configure server {} port: {} path: {} method: {}", pos, port, path, method);

    // if we already have a server stop it and reconfigure
    if (val.server != null) {
      val.server.close();
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
          javalin.get(path, ctx -> handleRequest(ctx, world, blockEntity));
          break;
        case "POST":
          javalin.post(path, ctx -> handleRequest(ctx, world, blockEntity));
          break;
        case "PUT":
          javalin.put(path, ctx -> handleRequest(ctx, world, blockEntity));
          break;
        case "DELETE":
          javalin.delete(path, ctx -> handleRequest(ctx, world, blockEntity));
      }

      // start the server if the block is powered
      int power = world.getReceivedRedstonePower(pos);
      if (power > 0) {
        javalin.start();
      }
    });

    // set the server
    val.server = javalin;
    SERVERS.put(pos, val);
  }

  public static void handleServerTick(ServerWorld world) {
    // check if the block is powered
    for (Entry<BlockPos, WebserverContext> entry : SERVERS.entrySet()) {
      BlockPos pos = entry.getKey();
      int power = world.getReceivedRedstonePower(pos);

      var val = entry.getValue();
      if (power > 0 && !val.powered) {

        // if there is no server do nothing
        if (val.server == null) {
          continue;
        }

        // start the server async
        service.submit(() -> {
          val.server.start();
          MicroservicesMod.LOGGER.info("block {} power on {}, starting server", entry.getKey(), power);
        });

        val.powered = true;

        SERVERS.put(pos, val);
      }

      // when power is off stop the server
      if (power == 0 && val.powered) {
        MicroservicesMod.LOGGER.info("block {} power off {}, stopping server", entry.getKey(), power);
        val.powered = false;
        val.server.stop();

        SERVERS.put(pos, val);
      }
    }
  }

  public static Context handleRequest(Context ctx, ServerWorld world, WebserverBlockEntity blockEntity) {
    LOGGER.info("Received request {} {}", ctx.method(), ctx.path());

    BlockState state = blockEntity.getCachedState().with(DatabaseBlock.POWERED, true);
    world.setBlockState(blockEntity.getPos(), state, Block.NOTIFY_ALL);

    // update the block
    blockEntity.setPropertiesToState();
    blockEntity.serverStateUpdated(blockEntity.serverState);

    // schedule a block tick to update the block so it can disable
    world.scheduleBlockTick(blockEntity.getPos(), MicroservicesMod.WEBSERVER_BLOCK, 40, TickPriority.NORMAL);

    return ctx.result("Hello World");
  }
}