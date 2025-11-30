package net.elytrium.limboapi.server.item.codec.data;

import com.google.gson.JsonElement;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.api.protocol.data.ComponentHolder;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ComponentHolderCodec implements StreamCodec<ComponentHolder>, ComponentHolder.Codec {

  public static final ComponentHolderCodec CODEC = (ComponentHolderCodec) ComponentHolder.CODEC;
  public static final StreamCodec<@Nullable ComponentHolder> OPTIONAL_CODEC = ByteBufCodecs.optional(ComponentHolderCodec.CODEC);

  @Override
  public ComponentHolder decode(ByteBuf buf, ProtocolVersion version) {
    return version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)
        ? new ComponentHolder(ProtocolUtils.readBinaryTag(buf, version, null))
        : version.noLessThan(ProtocolVersion.MINECRAFT_1_13)
            ? new ComponentHolder(ProtocolUtils.readString(buf, 0x3FFFF))
            : new ComponentHolder(ProtocolUtils.readString(buf));
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version, ComponentHolder value) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      ProtocolUtils.writeBinaryTag(buf, version, value.getBinaryTag(version));
    } else {
      ProtocolUtils.writeString(buf, value.getJson(version));
    }
  }

  @Override
  public Component json2Component(ProtocolVersion version, String json) {
    return ProtocolUtils.getJsonChatSerializer(version).deserialize(json);
  }

  @Override
  public Component binaryTag2Component(ProtocolVersion version, BinaryTag binaryTag) {
    JsonElement json = null;
    try {
      return ProtocolUtils.getJsonChatSerializer(version).deserializeFromTree(json = com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder.deserialize(binaryTag));
    } catch (Exception e) {
      LimboAPI.getLogger().error("Error converting binary component to JSON component! Binary: {} JSON: {}", binaryTag, json, e);
      throw e;
    }
  }

  @Override
  public String component2Json(ProtocolVersion version, Component component) {
    return ProtocolUtils.getJsonChatSerializer(version).serialize(component);
  }

  @Override
  public BinaryTag component2BinaryTag(ProtocolVersion version, Component component) {
    return com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder.serialize(ProtocolUtils.getJsonChatSerializer(version).serializeToTree(component));
  }
}
