package com.github.hashicraft.microservices.events;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record WebserverBlockUpdatedPacket(BlockPos pos) implements CustomPayload {
  public static final CustomPayload.Id<WebserverBlockUpdatedPacket> PACKET_ID = new CustomPayload.Id<>(
      Identifier.of("webserver_block_updated"));
  public static final PacketCodec<RegistryByteBuf, WebserverBlockUpdatedPacket> PACKET_CODEC = BlockPos.PACKET_CODEC
      .xmap(WebserverBlockUpdatedPacket::new, WebserverBlockUpdatedPacket::pos).cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return PACKET_ID;
  }

}
