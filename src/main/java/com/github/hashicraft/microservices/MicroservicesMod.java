package com.github.hashicraft.microservices;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.microservices.blocks.DatabaseBlock;
import com.github.hashicraft.microservices.blocks.DatabaseBlockEntity;
import com.github.hashicraft.microservices.events.Messages;
import com.github.hashicraft.stateful.blocks.EntityServerState;
import com.ibm.icu.impl.locale.LocaleDistance.Data;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.TickPriority;

public class MicroservicesMod implements ModInitializer {
  public static final String MODID = "microservices";
  public static final Logger LOGGER = LoggerFactory.getLogger(MicroservicesMod.class);

  public static final Identifier DATABASE_BLOCK_ID = new Identifier(MODID, "database_block");
  public static final Identifier DATABASE_ENTITY_ID = new Identifier(MODID, "database_entity");
  public static final DatabaseBlock DATABASE_BLOCK = new DatabaseBlock(FabricBlockSettings.create().strength(4.0f)
      .nonOpaque().solid());

  public static BlockEntityType<DatabaseBlockEntity> DATABASE_BLOCK_ENTITY;
  public static final Item DATABASE_ITEM = new BlockItem(DATABASE_BLOCK, new Item.Settings());

  public static final RegistryKey<ItemGroup> ITEM_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP,
      new Identifier(MODID, "microservices"));

  public static HashMap<BlockPos, Boolean> DATABASES = new HashMap<BlockPos, Boolean>();

  // background thread service
  private ExecutorService service = new ThreadPoolExecutor(4, 1000, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    System.out.println("Microservices v1.0.0 loading...");

    Registry.register(Registries.BLOCK, DATABASE_BLOCK_ID, DATABASE_BLOCK);
    Registry.register(Registries.ITEM, DATABASE_BLOCK_ID, DATABASE_ITEM);
    Registry.register(Registries.ITEM_GROUP, ITEM_GROUP, FabricItemGroup.builder()
        .icon(() -> new ItemStack(DATABASE_BLOCK))
        .displayName(Text.translatable("microservices.database"))
        .build());

    DATABASE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, DATABASE_ENTITY_ID,
        FabricBlockEntityTypeBuilder.create(DatabaseBlockEntity::new, DATABASE_BLOCK).build());

    ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> {
      content.add(DATABASE_ITEM);
    });

    // register for block events
    listenForEvents();

    EntityServerState.RegisterStateUpdates();
  }

  private void listenForEvents() {
    ServerPlayNetworking.registerGlobalReceiver(Messages.DATABASE_BLOCK_REGISTER,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();

          LOGGER.info("Received register_database message {}", pos);
          if (!DATABASES.containsKey(pos)) {
            DATABASES.put(pos, false);
          }
        });

    ServerPlayNetworking.registerGlobalReceiver(Messages.DATABASE_BLOCK_REMOVE,
        (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) -> {
          BlockPos pos = buf.readBlockPos();

          LOGGER.info("Received remove_database message {}", pos);
          if (DATABASES.containsKey(pos)) {
            DATABASES.remove(pos);
          }
        });

    ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
      server.execute(() -> {
        // check if the block is powered
        for (Entry<BlockPos, Boolean> entry : DATABASES.entrySet()) {
          ServerWorld world = server.getOverworld();
          BlockPos pos = entry.getKey();
          DatabaseBlockEntity blockEntity = (DatabaseBlockEntity) world.getBlockEntity(pos);
          int power = world.getReceivedRedstonePower(pos);

          if (power > 0 && !entry.getValue()) {
            LOGGER.info("block {} power on {}", entry.getKey(), power);
            DATABASES.put(pos, true);

            executeDBQuery(world, blockEntity);
          }

          // when power is off reset the block
          if (power == 0 && entry.getValue()) {
            DATABASES.put(pos, false);
          }
        }
      });
    });
  }

  private void executeDBQuery(ServerWorld world, DatabaseBlockEntity blockEntity) {
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
      world.scheduleBlockTick(blockEntity.getPos(), DATABASE_BLOCK, 40, TickPriority.NORMAL);
    });
  }

  private String executeSQLStatement(DatabaseBlockEntity blockEntity) throws SQLException {
    // get the database details from the block entity
    String address = blockEntity.getDbAddress();
    String username = blockEntity.getUsername();
    String password = blockEntity.getPassword();
    String database = blockEntity.getDatabase();
    String sql = blockEntity.getSQLStatement();

    // execute the SQL statement
    Connection conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s/%s", address, database),
        username, password);
    Statement st = conn.createStatement();
    st.execute(sql);
    st.close();

    return "success";
  }
}