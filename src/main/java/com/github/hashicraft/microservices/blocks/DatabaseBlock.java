package com.github.hashicraft.microservices.blocks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

public class DatabaseBlock extends StatefulBlock {
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

        // ensure that the state is synced with the server
        blockEntity.markForUpdate();
      });
    }

    return ActionResult.SUCCESS;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    // pass a reference to self so that neighbors can be updated later
    return new DatabaseBlockEntity(pos, state, this);
  }

  @Override
  public boolean emitsRedstonePower(BlockState state) {
    return true;
  }

  @Override
  public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
    DatabaseBlockEntity blockEntity = (DatabaseBlockEntity) world.getBlockEntity(pos);
    return blockEntity.redstonePower;
  }

  @Override
  public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
    DatabaseBlockEntity blockEntity = (DatabaseBlockEntity) world.getBlockEntity(pos);
    return blockEntity.redstonePower;
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
  public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
    DatabaseBlockEntity blockEntity = (DatabaseBlockEntity) world.getBlockEntity(pos);

      MicroservicesMod.LOGGER.info("power on");
    if (blockEntity.powered) {
      // increase the tick count that the block has been powered for
      blockEntity.onCount++;

      // if powered for 5 ticks then turn off
      if (blockEntity.onCount > 5) {
        MicroservicesMod.LOGGER.info("power off");

        blockEntity.powered = false;
        blockEntity.redstonePower = 0;
        blockEntity.markForUpdate();
        return;
      }

      return;
    }

    // check if the block is powered
    int power = world.getReceivedRedstonePower(pos);

    if (power > 0) {
      if (!executeSQLStatement(blockEntity)) {
        MicroservicesMod.LOGGER.info("sql failed");
        return;
      }

      MicroservicesMod.LOGGER.info("power on");

      // set that the block is powered
      blockEntity.onCount = 0;
      blockEntity.redstonePower = 16;
      blockEntity.powered = true;
      blockEntity.markForUpdate();
    }
  }

  private Boolean executeSQLStatement(DatabaseBlockEntity blockEntity) {

    // get the database details from the block entity
    String address = blockEntity.getDbAddress();
    String username = blockEntity.getUsername();
    String password = blockEntity.getPassword();
    String database = blockEntity.getDatabase();
    String sql = blockEntity.getSQLStatement();

    // execute the SQL statement
    try {
      Connection conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s/%s", address, database),
          username, password);
      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery(sql);
      rs.close();
      st.close();

    } catch (SQLException e) {
      // set the result on the block entity
      blockEntity.setResult(e.getMessage());
      blockEntity.markForUpdate();
      return false;
    }

    return true;

    // String result = Database.execute(address, username, password, database, sql);

    // set the result on the block entity
    // blockEntity.setResult(result);
  }

}