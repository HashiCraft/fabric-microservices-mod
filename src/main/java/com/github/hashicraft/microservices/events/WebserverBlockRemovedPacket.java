package com.github.hashicraft.microservices.events;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record WebserverBlockRemovedPacket(BlockPos pos) implements CustomPayload {
  public static final CustomPayload.Id<WebserverBlockRemovedPacket> PACKET_ID = new CustomPayload.Id<WebserverBlockRemovedPacket>(
      Identifier.of("webserver_block_removed"));
  public static final PacketCodec<RegistryByteBuf, WebserverBlockRemovedPacket> PACKET_CODEC = BlockPos.PACKET_CODEC
      .xmap(WebserverBlockRemovedPacket::new, WebserverBlockRemovedPacket::pos).cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return PACKET_ID;
  }
}
