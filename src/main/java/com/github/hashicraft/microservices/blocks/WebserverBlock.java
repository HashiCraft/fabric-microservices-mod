package com.github.hashicraft.microservices.blocks;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.microservices.ModBlocks;
import com.github.hashicraft.microservices.ModItems;
import com.github.hashicraft.microservices.events.WebserverBlockClicked;
import com.github.hashicraft.microservices.events.WebserverBlockRemovedPacket;
import com.github.hashicraft.microservices.events.WebserverBlockUpdatedPacket;
import com.github.hashicraft.microservices.interpolation.Interpolate;
import com.github.hashicraft.stateful.blocks.StatefulBlock;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.TickPriority;

public class WebserverBlock extends StatefulBlock {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebserverBlock.class);

  public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
  public static final BooleanProperty POWERED = Properties.POWERED;

  // keeps a map of registered database blocks so we can check for updates
  // on server tick
  private static Webservers SERVERS = new Webservers();
  private static Boolean initialized = false;
  private static final Object INIT_LOCK = new Object();

  public static final HashMap<String, String> RESPONSES = new HashMap<String, String>();

  private static ExecutorService service = new ThreadPoolExecutor(4, 1000, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

  public WebserverBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
  }

  @Override
  protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
      BlockHitResult hit) {

    WebserverBlockEntity blockEntity = (WebserverBlockEntity) world.getBlockEntity(pos);

    if (world.isClient()) {
      WebserverBlockClicked.EVENT.invoker().interact(blockEntity, () -> {
        blockEntity.markForUpdate();

        // notify that the server has been reconfigured
        // we need to wait until the block state has synced so wait here
        service.submit(() -> {
          try {
            Thread.sleep(1000);
            LOGGER.info("Sending webserver block updated packet for {}", pos);
            ClientPlayNetworking.send(new WebserverBlockUpdatedPacket(pos));
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
    return new WebserverBlockEntity(pos, state, this);
  }

  @Override
  public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
    if (world.isClient()) {
      ClientPlayNetworking.send(new WebserverBlockRemovedPacket(pos));
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
  protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
    super.onStateReplaced(state, world, pos, moved);
    world.updateNeighborsAlways(pos, state.getBlock(), null);
  }

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
    PayloadTypeRegistry.playC2S().register(WebserverBlockRemovedPacket.PACKET_ID,
        WebserverBlockRemovedPacket.PACKET_CODEC);

    PayloadTypeRegistry.playC2S().register(WebserverBlockUpdatedPacket.PACKET_ID,
        WebserverBlockUpdatedPacket.PACKET_CODEC);

    ServerPlayNetworking.registerGlobalReceiver(WebserverBlockRemovedPacket.PACKET_ID, (payload, context) -> {
      handleBlockRemove(payload.pos());
    });

    ServerPlayNetworking.registerGlobalReceiver(WebserverBlockUpdatedPacket.PACKET_ID, (payload, context) -> {
      handleBlockUpdate(payload.pos(), context.server().getOverworld());
    });

    ServerTickEvents.START_SERVER_TICK.register(server -> {
      synchronized (INIT_LOCK) {
        if (initialized) {
          return;
        }

        initialized = true;

        SERVERS = Webservers.loadFromConfig();

        // start all the servers
        MicroservicesMod.LOGGER.info("Starting webservers from config");
        SERVERS.getContexts().forEach((ctx) -> {
          startServer(server.getOverworld(), ctx);
        });
      }
    });
  }

  public static void handleBlockRemove(BlockPos pos) {
    MicroservicesMod.LOGGER.info("Received remove_webserver message {}", pos);
    var server = SERVERS.getAtLocation(pos);
    if (server.isEmpty()) {
      MicroservicesMod.LOGGER.info("No server found at {}", pos);
      return;
    }

    server.get().removeHandler(pos);

    // if there are no more handlers for this server, remove it
    if (server.get().getHandlers().isEmpty()) {
      MicroservicesMod.LOGGER.info("Removed server at {}", pos);
      server.get().getServer().stop();
      SERVERS.removeAtLocation(pos);
    }
  }

  public static void handleBlockUpdate(BlockPos pos, ServerWorld world) {
    WebserverBlockEntity blockEntity = (WebserverBlockEntity) world.getBlockEntity(pos);

    String port = Interpolate.getValue(blockEntity.getPort());
    String timeout = Interpolate.getValue(blockEntity.getTimeout());
    String path = Interpolate.getValue(blockEntity.getPath());
    String method = Interpolate.getValue(blockEntity.getMethod());

    var ctx = SERVERS.getOrDefault(port);

    MicroservicesMod.LOGGER.info(
        "configure server {} port: {} path: {} method: {}",
        pos,
        blockEntity.getPort(),
        blockEntity.getPath(),
        blockEntity.getMethod());

    // create a new handler if it doesn't exist
    var handler = ctx.getHandlerForPos(pos);
    handler.setPath(path);
    handler.setMethod(method);
    handler.setTimeout(timeout);
    ctx.updateHandler(handler);

    // set the TLS cert and key
    ctx.setTlsCert(blockEntity.getTlsCert());
    ctx.setTlsKey(blockEntity.getTlsKey());

    startServer(world, ctx);

    // serialize the servers collection
    try {
      SERVERS.writeToConfig();
    } catch (IOException e) {
      MicroservicesMod.LOGGER.info("unable to write config {}", e.getMessage());
    }
  }

  public static void startServer(ServerWorld world, WebserverContext ctx) {
    // get the port as an integer
    int serverPort = 0;
    try {
      serverPort = Integer.parseInt(ctx.getPort());
    } catch (NumberFormatException e) {
      MicroservicesMod.LOGGER.error("invalid port {}, unable to start server, error:{}", ctx.getPort(), e);
      return;
    }

    // if the server exists, stop it
    if (ctx.getServer() != null) {
      ctx.getServer().stop();
    }

    // create the server and set the port
    LOGGER.info("Starting webserver for port: {}", serverPort);
    Undertow server = Undertow.builder()
        .addHttpListener(serverPort, "0.0.0.0")
        .setHandler(exchange -> handleRequest(exchange, world, ctx))
        .build();

    // start the server async so we don't block the main thread
    service.submit(() -> {
      server.start();
    });

    // set the server
    ctx.setServer(server);
  }

  public static void handleRequest(HttpServerExchange exchange, ServerWorld world, WebserverContext ctx) {
    LOGGER.info("Received request {} {}", exchange.getRequestMethod(), exchange.getRequestPath());

    // find the handler for this request
    var handler = ctx.getHandlers().stream()
        .filter(h -> h.getPath().equals(exchange.getRequestPath())
            && h.getMethod().equals(exchange.getRequestMethod().toString()))
        .findFirst();

    // if we don't have a handler, return 404
    if (handler.isEmpty()) {
      LOGGER.error("No handler found for request {} {}", exchange.getRequestMethod(), exchange.getRequestPath());
      exchange.setStatusCode(404);
      exchange.getResponseSender().send("Not Found");
      return;
    }

    BlockPos pos = handler.get().getBlockPos();

    BlockState state = world.getBlockState(pos);
    state = state.with(DatabaseBlock.POWERED, true);
    world.setBlockState(pos, state, Block.NOTIFY_ALL);

    // schedule a block tick to update the block so it can disable
    world.scheduleBlockTick(pos, ModBlocks.WEBSERVER_BLOCK, 40, TickPriority.NORMAL);

    // create a data item
    ItemStack data = new ItemStack(ModItems.DATA_ITEM);

    // create a dispense location
    Direction direction = world.getBlockState(pos).get(FACING);

    // generate a request id
    String requestId = java.util.UUID.randomUUID().toString();

    // set the request properties
    NbtCompound req = data.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    req.putString("request_id", requestId);
    req.putString("request_path", exchange.getRequestPath());
    req.putString("request_method", exchange.getRequestMethod().toString());

    if (!exchange.getQueryParameters().isEmpty()) {
      NbtCompound queryMap = new NbtCompound();
      exchange.getQueryParameters().forEach((key, values) -> {
        if (!values.isEmpty()) {
          queryMap.putString(key, values.getFirst());
        }
      });
      req.put("request_query", queryMap);
    }

    // get the request body
    exchange.getRequestReceiver().receiveFullString((e, m) -> {
      req.putString("data", m);
    });

    data.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(req));

    // dispense the block
    dispense(world, pos, data, 1, direction);

    // wait until we have a response or the timeout is reached
    long timeout;
    try {
      timeout = Long.parseLong(handler.get().getTimeout());
    } catch (NumberFormatException e) {
      LOGGER.error("Invalid timeout value for handler {}, using default 5000ms", handler.get().getTimeout());
      timeout = 5000;
    }

    // wait until we have a response or the timeout is reached
    try {
      long start = System.currentTimeMillis();
      long end = start + timeout;
      LOGGER.info("Wait for response {}", requestId);

      while (System.currentTimeMillis() < end) {
        // check if we have a response for this request id
        if (RESPONSES.containsKey(requestId)) {
          LOGGER.info("Sending response {}", requestId);
          exchange.getResponseSender().send(RESPONSES.get(requestId));
          return;
        }

        Thread.sleep(10);
      }

      // if we reach here, we timed out
      throw new InterruptedException("Timeout waiting for response");
    } catch (InterruptedException e) {
      LOGGER.error("Error waiting for response {}", e.getMessage());
      exchange.setStatusCode(408);
      exchange.getResponseSender().send("Request Timeout");
    }
  }

  public static void dispense(World world, BlockPos pos, ItemStack stack, int offset, Direction side) {
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
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset + (double) side.getOffsetX() * g,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset + 0.20000000298023224D,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset + (double) side.getOffsetZ() * g);
    world.spawnEntity(entity);
  }
}