package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.elytrium.limboapi.api.protocol.data.ItemStack;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.SimpleDataComponentMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ItemStackCodec {

  StreamCodec<ItemStack> OPTIONAL_CODEC = new StreamCodec<>() {

    @Override
    public ItemStack decode(ByteBuf buf, ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        int amount = ProtocolUtils.readVarInt(buf);
        return amount > 0
            ? new ItemStack(ProtocolUtils.readVarInt(buf), amount, SimpleDataComponentMap.read(buf, version))
            : ItemStack.EMPTY;
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
        return buf.readBoolean()
            ? new ItemStack(ProtocolUtils.readVarInt(buf), buf.readByte(), ByteBufCodecs.OPTIONAL_COMPOUND_TAG.decode(buf, version))
            : ItemStack.EMPTY;
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
        short material = buf.readShort();
        return material >= 0
            ? new ItemStack(material, buf.readByte(), ByteBufCodecs.OPTIONAL_COMPOUND_TAG.decode(buf, version))
            : ItemStack.EMPTY;
      } else {
        short material = buf.readShort();
        return material >= 0
            ? new ItemStack(material, buf.readByte(), buf.readShort(), ByteBufCodecs.OPTIONAL_COMPOUND_TAG.decode(buf, version))
            : ItemStack.EMPTY;
      }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, ItemStack value) {
      boolean hasDamage = version.noGreaterThan(ProtocolVersion.MINECRAFT_1_12_2);
      if (value.isEmpty(hasDamage)) {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
          ProtocolUtils.writeVarInt(buf, 0);
        } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
          buf.writeBoolean(false);
        } else {
          buf.writeShort(-1);
        }
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        ProtocolUtils.writeVarInt(buf, value.amount());
        ProtocolUtils.writeVarInt(buf, value.material());
        var map = value.map();
        if (map == null) {
          ProtocolUtils.writeVarInt(buf, 0); // added
          ProtocolUtils.writeVarInt(buf, 0); // removed
        } else {
          map.write(buf, version);
        }
      } else {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13_2)) {
          buf.writeBoolean(true);
          ProtocolUtils.writeVarInt(buf, value.material());
        } else {
          buf.writeShort(value.material());
        }

        buf.writeByte(value.amount());
        if (hasDamage) {
          buf.writeShort(value.data());
        }

        ByteBufCodecs.OPTIONAL_COMPOUND_TAG.encode(buf, version, value.nbt());
      }
    }
  };
  StreamCodec<ItemStack> CODEC = new StreamCodec<>() {

    @Override
    public ItemStack decode(ByteBuf buf, ProtocolVersion version) {
      ItemStack itemStack = ItemStackCodec.OPTIONAL_CODEC.decode(buf, version);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5) && itemStack.isEmpty(false)) {
        throw new DecoderException("Empty ItemStack not allowed");
      } else {
        return itemStack;
      }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, ItemStack value) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5) && value.isEmpty(false)) {
        throw new EncoderException("Empty ItemStack not allowed");
      } else {
        ItemStackCodec.OPTIONAL_CODEC.encode(buf, version, value);
      }
    }
  };
}
