package com.github.hashicraft.microservices.blocks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.hashicraft.microservices.ModBlockEntities;
import com.github.hashicraft.microservices.ModBlocks;
import com.github.hashicraft.microservices.ModItems;
import com.github.hashicraft.microservices.interpolation.Interpolate;
import com.github.hashicraft.stateful.blocks.StatefulBlockEntity;
import com.github.hashicraft.stateful.blocks.Syncable;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickPriority;

public class DatabaseBlockEntity extends StatefulBlockEntity implements DatabaseInventory {

  public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBlockEntity.class);

  // background thread service
  private static ExecutorService service = new ThreadPoolExecutor(4, 1000, 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

  private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);

  private final BlockPos pos;

  @Syncable
  public String result;

  @Syncable
  public String dbAddress;

  @Syncable
  public String dbUsername;

  @Syncable
  public String dbPassword;

  @Syncable
  public String dbDatabase;

  @Syncable
  public String sqlStatement;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getDbAddress() {
    return dbAddress;
  }

  public void setDbAddress(String address) {
    this.dbAddress = address;
  }

  public String getUsername() {
    return this.dbUsername;
  }

  public void setUsername(String name) {
    this.dbUsername = name;
  }

  public String getPassword() {
    return this.dbPassword;
  }

  public void setPassword(String password) {
    this.dbPassword = password;
  }

  public String getDatabase() {
    return this.dbDatabase;
  }

  public void setDatabase(String database) {
    this.dbDatabase = database;
  }

  public String getSQLStatement() {
    return this.sqlStatement;
  }

  public void setSQLStatement(String sql) {
    this.sqlStatement = sql;
  }

  public DatabaseBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.DATABASE_BLOCK_ENTITY, pos, state, null);
    this.pos = pos;
  }

  public DatabaseBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(ModBlockEntities.DATABASE_BLOCK_ENTITY, pos, state, parent);
    this.pos = pos;
  }

  // DatabaseInventory interface methods
  @Override
  public DefaultedList<ItemStack> getItems() {
    return items;
  }

  @Override
  public BlockPos getBlockPos() {
    return pos;
  }

  @Override
  public void executeDBQuery(String requestID, String data) {
    LOGGER.info("Executing SQL statement {} for request {} with data {}", this.getSQLStatement(), requestID, data);

    service.submit(() -> {

      try {
        String result = executeSQLStatement();

        // everything is ok emit redstone power
        BlockState state = world.getBlockState(pos);
        state = state.with(DatabaseBlock.POWERED, true);
        world.setBlockState(pos, state, Block.NOTIFY_ALL);

        // schedule a block tick to update the block so it can disable
        world.scheduleBlockTick(pos, ModBlocks.DATABASE_BLOCK, 40, TickPriority.NORMAL);

        // create a data item
        ItemStack card = new ItemStack(ModItems.DATA_ITEM);
        NbtComponent nbtComponent = card.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtComponent.copyNbt();
        nbt.putString("request_id", requestID);
        nbt.putString("data", result);
        card.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // create a dispense location and dispense the item
        Direction direction = world.getBlockState(pos).get(FACING);
        dispense(world, pos, card, 1, direction);
      } catch (SQLException e) {
        LOGGER.error("Error executing SQL statement {}", e);
        this.result = "error";
      }

      this.markForUpdate();
    });
  }

  private String executeSQLStatement() throws SQLException {
    // get the database details from the block entity gui
    // we will substitute any environment variables that may be embedded in here
    String address = Interpolate.getValue(this.getDbAddress());
    String username = Interpolate.getValue(this.getUsername());
    String password = Interpolate.getValue(this.getPassword());
    String database = Interpolate.getValue(this.getDatabase());
    String sql = Interpolate.getValue(this.getSQLStatement());

    // execute the SQL statement
    Connection conn = DriverManager.getConnection(
        String.format("jdbc:postgresql://%s/%s", address, database),
        username, password);

    String json = "";

    // split the SQL statement by the semi-colon and execute
    String[] statements = sql.split(";");
    for (String statement : statements) {
      if (statement.isEmpty()) {
        continue;
      }

      // execute the statement
      LOGGER.info("Execute SQL statement {}", statement);
      Statement st = conn.createStatement();
      st.execute(statement);

      // get the result set
      ResultSet results = st.getResultSet();
      if (results == null) {
        LOGGER.info("No results from the query");
      } else {
        json = resultSetToJson(results);
      }

      st.close();
    }

    return json;
  }

  private String resultSetToJson(ResultSet rs) {
    try {
      ResultSetMetaData md = rs.getMetaData();
      int numCols = md.getColumnCount();
      List<String> colNames = IntStream.range(0, numCols)
          .mapToObj(i -> {
            try {
              return md.getColumnName(i + 1);
            } catch (SQLException e) {
              e.printStackTrace();
              return "?";
            }
          })
          .collect(Collectors.toList());

      Gson gson = new Gson();
      JsonArray result = new JsonArray();
      while (rs.next()) {
        JsonObject row = new JsonObject();
        colNames.forEach(cn -> {
          try {
            JsonElement val = gson.toJsonTree(rs.getObject(cn));
            row.add(cn.toString(), val);
          } catch (SQLException e) {
            e.printStackTrace();
          }
        });

        result.add(row);
      }

      // if there is only one element do not return an array
      if (result.size() == 1) {
        return result.get(0).toString();
      }

      // convert the result set to a JSON string
      return result.toString();
    } catch (SQLException e) {
      e.printStackTrace();
      return "";
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
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset +
            (double) side.getOffsetX() * g,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset +
            0.20000000298023224D,
        world.random.nextGaussian() * 0.007499999832361937D * (double) offset +
            (double) side.getOffsetZ() * g);
    world.spawnEntity(entity);
  }
}