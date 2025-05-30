package com.github.hashicraft.microservices.gui;

import java.util.ArrayList;

import com.github.hashicraft.microservices.blocks.WasmBlockEntity;
import com.github.hashicraft.microservices.events.WasmGuiCallback;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WScrollPanel;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class WasmBlockGui extends LightweightGuiDescription {
  private WasmBlockEntity currentEntity;

  private int moduleCount = 1;
  private int rowNumber = 0;

  WGridPanel root = new WGridPanel();
  WScrollPanel scroll;
  WLabel label = new WLabel(Text.literal("§lConfigure Wasm Module"));

  ArrayList<WTextField> moduleLocFields = new ArrayList<WTextField>();
  ArrayList<WTextField> moduleNameFields = new ArrayList<WTextField>();

  WTextField functionField;
  WTextField inputField1;
  WTextField inputField2;
  WTextField resultField;

  public WasmBlockGui(WasmBlockEntity entity, WasmGuiCallback callback) {
    setRootPanel(root);
    root.setInsets(Insets.ROOT_PANEL);

    moduleCount = (entity.modules == null || entity.modules.size() == 0) ? 1
        : entity.modules.size();

    currentEntity = entity;
    drawPanel(callback);
    populateValues();
  }

  private void drawPanel(WasmGuiCallback callback) {
    root.add(label, 0, 0, 4, 1);

    if (scroll != null) {
      root.remove(scroll);
    }

    WGridPanel panel = new WGridPanel();

    scroll = new WScrollPanel(panel);
    root.add(scroll, 0, 1, 20, 10);

    WLabel label = new WLabel(Text.literal("modules to load:"));
    panel.add(label, 0, 0, 10, 2);

    WButton plus = new WButton(Text.literal("+"));
    panel.add(plus, 7, 0, 1, 2);

    plus.setOnClick(() -> {
      setValues();
      moduleCount++;
      drawPanel(callback);
      populateValues();
    });

    if (moduleCount > 1) {
      WButton minus = new WButton(Text.literal("-"));
      panel.add(minus, 9, 0, 1, 2);

      minus.setOnClick(() -> {
        moduleCount--;
        drawPanel(callback);
        populateValues();
      });
    }

    rowNumber = 2;

    // add the modules
    addModuleFields(panel);

    // add the function name dialog
    WLabel function = new WLabel(Text.literal("function to execute:"));
    panel.add(function, 0, rowNumber, 4, 1);

    rowNumber = rowNumber + 1;

    functionField = new WTextField(Text.literal("e.g. sum"));
    panel.add(functionField, 0, rowNumber, 9, 2);

    rowNumber = rowNumber + 2;

    // add the save button
    WButton button = new WButton(Text.literal("Save"));
    button.setOnClick(() -> {
      System.out.println("Save clicked");
      setValues();

      callback.onSave();

      MinecraftClient client = MinecraftClient.getInstance();
      client.player.closeScreen();
      MinecraftClient.getInstance().setScreen((Screen) null);
    });

    panel.add(button, 0, rowNumber, 19, 1);

    root.validate(this);
  }

  private void addModuleFields(WGridPanel panel) {
    moduleLocFields.clear();
    moduleNameFields.clear();

    for (int n = 0; n < moduleCount; n++) {
      WTextField wtf;

      wtf = new WTextField(Text.literal("module location (c:\\module.wat)"));
      panel.add(wtf, 0, rowNumber, 19, 2);
      wtf.setMaxLength(255);

      moduleLocFields.add(wtf);

      wtf = new WTextField(Text.literal("module name (leave blank if main)"));
      panel.add(wtf, 0, rowNumber + 2, 10, 2);
      wtf.setMaxLength(255);

      moduleNameFields.add(wtf);

      rowNumber = rowNumber + 4;
    }
  }

  // populates the values from the entity
  private void populateValues() {
    var modules = currentEntity.getModules();
    var names = currentEntity.getModuleNames();
    var function = currentEntity.getFunction();

    if (modules != null) {
      for (int n = 0; n < modules.size(); n++) {
        moduleLocFields.get(n).setText(modules.get(n));
      }
    }

    if (names != null) {
      for (int n = 0; n < names.size(); n++) {
        moduleNameFields.get(n).setText(names.get(n));
      }
    }

    if (function != null) {
      functionField.setText(function);
    }
  }

  // sets the values to the entity
  private void setValues() {
    var modules = new ArrayList<String>();
    var names = new ArrayList<String>();

    for (int n = 0; n < moduleLocFields.size(); n++) {
      modules.add(moduleLocFields.get(n).getText());
      names.add(moduleNameFields.get(n).getText());
    }

    currentEntity.setModules(modules);
    currentEntity.setModuleNames(names);
    currentEntity.setFunction(functionField.getText());
  }
}