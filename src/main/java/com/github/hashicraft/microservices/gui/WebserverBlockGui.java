package com.github.hashicraft.microservices.gui;

import com.github.hashicraft.microservices.blocks.WebserverBlockEntity;
import com.github.hashicraft.microservices.events.WebserverGuiCallback;

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

public class WebserverBlockGui extends LightweightGuiDescription {

  public WebserverBlockGui(WebserverBlockEntity entity, WebserverGuiCallback callback) {
    WGridPanel root = new WGridPanel();

    setRootPanel(root);
    root.setInsets(Insets.ROOT_PANEL);

    WLabel labelSettings = new WLabel(Text.literal("Settings"));
    root.add(labelSettings, 0, 0, 0, 1);

    WGridPanel panel = new WGridPanel();
    WScrollPanel scrollPanel = new WScrollPanel(panel);
    root.add(scrollPanel, 0, 1, 20, 8);

    WLabel labelPort = new WLabel(Text.literal("Port"));
    panel.add(labelPort, 0, 0, 4, 1);

    WTextField portField;
    portField = new WTextField(Text.literal("The port to listen on"));
    panel.add(portField, 0, 1, 16, 2);
    portField.setMaxLength(255);

    WLabel labelPath = new WLabel(Text.literal("Path"));
    panel.add(labelPath, 0, 3, 4, 1);

    WTextField pathField;
    pathField = new WTextField(Text.literal("The path for the request"));
    panel.add(pathField, 0, 4, 16, 2);
    pathField.setMaxLength(255);

    WLabel labelMethod = new WLabel(Text.literal("Method"));
    panel.add(labelMethod, 0, 6, 4, 1);

    WTextField methodField;
    methodField = new WTextField(Text.literal("The HTTP method for the request"));
    panel.add(methodField, 0, 7, 16, 2);
    methodField.setMaxLength(255);

    WButton button = new WButton(Text.literal("Save"));
    panel.add(button, 0, 10, 16, 1);

    // save the details to the entity
    button.setOnClick(() -> {
      entity.setServerPort(portField.getText());
      entity.setServerPath(pathField.getText());
      entity.setServerMethod(methodField.getText());

      callback.onSave();

      MinecraftClient client = MinecraftClient.getInstance();
      client.player.closeScreen();
      MinecraftClient.getInstance().setScreen((Screen) null);
    });

    WLabel labelOutput = new WLabel(Text.literal("Output"));
    root.add(labelOutput, 0, 10, 4, 1);

    WText textOutput = new WText(Text.literal("Output and error messages from the server"));
    root.add(textOutput, 0, 11, 20, 2);

    // populate the fields
    String serverPort = entity.getServerPort();
    String serverMethod = entity.getServerMethod();
    String serverPath = entity.getServerPath();
    String result = entity.getResult();

    if (serverPort != null) {
      portField.setText(String.valueOf(serverPort));
    }

    if (serverMethod != null) {
      methodField.setText(serverMethod);
    }

    if (serverPath != null) {
      pathField.setText(serverPath);
    }

    if (result != null) {
      textOutput.setText(Text.literal(result));
    }

    root.validate(this);
  }
}