package com.github.hashicraft.microservices.http;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.microservices.ModBlocks;
import com.github.hashicraft.microservices.ModItems;
import com.github.hashicraft.microservices.blocks.WebserverBlock;
import com.github.hashicraft.microservices.blocks.WebserverContext;

import fi.iki.elonen.NanoHTTPD;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickPriority;

public class NanoHTTP extends NanoHTTPD {
  private static final Logger LOGGER = LoggerFactory.getLogger(NanoHTTP.class);

  private final ServerWorld world;
  private final WebserverContext ctx;

  public NanoHTTP(int port, ServerWorld world, WebserverContext ctx) throws IOException {
    super(port);

    this.world = world;
    this.ctx = ctx;
  }

  public void startServer() throws IOException {
    LOGGER.info("Starting NanoHTTP server on port {}", getListeningPort());
    this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
  }

  public void stopServer() {
    LOGGER.info("Stopping NanoHTTP server on port {}", getListeningPort());
    this.closeAllConnections();
    this.stop();
  }

  @Override
  public Response serve(IHTTPSession session) {
    LOGGER.info("Received request {} {}", session.getMethod(), session.getUri());

    // find the handler for this request
    var handler = ctx.getHandlers().stream()
        .filter(h -> h.getPath().equals(session.getUri())
            && h.getMethod().equals(session.getMethod().toString()))
        .findFirst();

    // if we don't have a handler, return 404
    if (handler.isEmpty()) {
      LOGGER.error("No handler found for request");

      return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    BlockPos pos = handler.get().getBlockPos();

    BlockState state = world.getBlockState(pos);
    state = state.with(WebserverBlock.POWERED, true);
    world.setBlockState(pos, state, Block.NOTIFY_ALL);

    // schedule a block tick to update the block so it can disable
    world.scheduleBlockTick(pos, ModBlocks.WEBSERVER_BLOCK, 40, TickPriority.NORMAL);

    // create a data item
    ItemStack data = new ItemStack(ModItems.DATA_ITEM);

    // create a dispense location
    Direction direction = world.getBlockState(pos).get(WebserverBlock.FACING);

    // generate a request id
    String requestId = java.util.UUID.randomUUID().toString();

    // set the request properties
    NbtCompound req = data.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    req.putString("request_id", requestId);
    req.putString("request_path", session.getUri());
    req.putString("request_method", session.getMethod().toString());

    if (!session.getParameters().isEmpty()) {
      NbtCompound queryMap = new NbtCompound();
      session.getParameters().forEach((key, values) -> {
        if (!values.isEmpty()) {
          queryMap.putString(key, values.getFirst());
        }
      });
      req.put("request_query", queryMap);
    }

    // get the request body
    String body = "";
    try {
      if (session.getHeaders().get("content-length") == null) {
        LOGGER.warn("No content-length header found, assuming no body");
      } else {
        int contentLength = Integer.valueOf(session.getHeaders().get("content-length"));
        if (contentLength > 0) {
          body = new String(session.getInputStream().readNBytes(contentLength)); // the request body
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    req.putString("data", new String(body));

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
        if (WebserverBlock.RESPONSES.containsKey(requestId)) {
          LOGGER.info("Sending response {}", requestId);
          var response = WebserverBlock.RESPONSES.get(requestId);

          NanoHTTPD.Response.IStatus status = NanoHTTPD.Response.Status.lookup(response.getStatusCode());
          if (status == null) {
            status = NanoHTTPD.Response.Status.OK;
          }

          return newFixedLengthResponse(status, "text/plain", response.getData());
        }

        Thread.sleep(10);
      }

      // if we reach here, we timed out
      throw new InterruptedException("Timeout waiting for response");
    } catch (InterruptedException e) {
      LOGGER.error("Error waiting for response {}", e.getMessage());
      return newFixedLengthResponse(Response.Status.REQUEST_TIMEOUT, "text/plain", "Timeout waiting for response");
    }
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
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset + (double) side.getOffsetX() * g,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset + 0.20000000298023224D,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset + (double) side.getOffsetZ() * g);
    world.spawnEntity(entity);
  }
}
