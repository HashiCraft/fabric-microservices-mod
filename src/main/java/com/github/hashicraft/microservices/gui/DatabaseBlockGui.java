package com.github.hashicraft.microservices.gui;

import com.github.hashicraft.microservices.blocks.DatabaseBlockEntity;
import com.github.hashicraft.microservices.events.DatabaseGuiCallback;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WScrollPanel;
import io.github.cottonmc.cotton.gui.widget.WText;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class DatabaseBlockGui extends LightweightGuiDescription {

  public DatabaseBlockGui(DatabaseBlockEntity entity, DatabaseGuiCallback callback) {
    WGridPanel root = new WGridPanel();

    setRootPanel(root);
    root.setInsets(Insets.ROOT_PANEL);

    WLabel labelSettings = new WLabel(Text.literal("Settings"));
    root.add(labelSettings, 0, 0, 0, 1);

    WGridPanel panel = new WGridPanel();
    WScrollPanel scrollPanel = new WScrollPanel(panel);
    root.add(scrollPanel, 0, 1, 20, 8);

    WLabel labelAddr = new WLabel(Text.literal("Address"));
    panel.add(labelAddr, 0, 0, 4, 1);

    WTextField addressField;
    addressField = new WTextField(Text.literal("The Database server address"));
    panel.add(addressField, 0, 1, 16, 2);
    addressField.setMaxLength(255);

    WLabel labelUser = new WLabel(Text.literal("Username"));
    panel.add(labelUser, 0, 3, 4, 1);

    WTextField userField;
    userField = new WTextField(Text.literal("The Database server username"));
    panel.add(userField, 0, 4, 16, 2);
    userField.setMaxLength(255);

    WLabel labelPass = new WLabel(Text.literal("Password"));
    panel.add(labelPass, 0, 6, 4, 1);

    WTextField passField;
    passField = new WTextField(Text.literal("The Database server password"));
    panel.add(passField, 0, 7, 16, 2);
    passField.setMaxLength(255);

    WLabel labelDatabase = new WLabel(Text.literal("Database"));
    panel.add(labelDatabase, 0, 9, 4, 1);

    WTextField databaseField;
    databaseField = new WTextField(Text.literal("The Database server database"));
    panel.add(databaseField, 0, 10, 16, 2);
    databaseField.setMaxLength(255);

    WLabel labelSql = new WLabel(Text.literal("SQL Statement"));
    panel.add(labelSql, 0, 12, 4, 1);

    WTextField sqlField;
    sqlField = new WTextField(Text.literal("SQL Statement to execute"));
    panel.add(sqlField, 0, 13, 16, 6);
    sqlField.setMaxLength(4096);

    WButton button = new WButton(Text.literal("Save"));
    panel.add(button, 0, 15, 16, 1);

    // save the details to the entity
    button.setOnClick(() -> {
      entity.setDbAddress(addressField.getText());
      entity.setUsername(userField.getText());
      entity.setPassword(passField.getText());
      entity.setDatabase(databaseField.getText());
      entity.setSQLStatement(sqlField.getText());

      callback.onSave();

      MinecraftClient client = MinecraftClient.getInstance();
      client.player.closeScreen();
      MinecraftClient.getInstance().setScreen((Screen) null);
    });

    WLabel labelOutput = new WLabel(Text.literal("Output"));
    root.add(labelOutput, 0, 10, 4, 1);

    WText textOutput = new WText(Text.literal("Output and error messages from the database"));
    root.add(textOutput, 0, 11, 20, 2);

    // populate the fields
    String dbAddress = entity.getDbAddress();
    String dbUsername = entity.getUsername();
    String dbPassword = entity.getPassword();
    String dbDatabase = entity.getDatabase();
    String dbSql = entity.getSQLStatement();
    String result = entity.getResult();

    if (dbAddress != null) {
      addressField.setText(dbAddress);
    }

    if (dbUsername != null) {
      userField.setText(dbUsername);
    }

    if (dbPassword != null) {
      passField.setText(dbPassword);
    }

    if (dbDatabase != null) {
      databaseField.setText(dbDatabase);
    }

    if (dbSql != null) {
      sqlField.setText(dbSql);
    }

    if (result != null) {
      textOutput.setText(Text.literal(result));
    }

    root.validate(this);
  }
}