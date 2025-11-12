package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.limboapi.api.world.item.datacomponent.type.AttributeModifiers;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.ComponentHolderCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface AttributeModifiersCodec {

  StreamCodec<AttributeModifiers> CODEC = StreamCodec.composite(
      ByteBufCodecs.collection(EntryCodec.CODEC), AttributeModifiers::modifiers,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL, true), AttributeModifiers::showInTooltip,
      AttributeModifiers::new
  );

  interface EntryCodec {

    StreamCodec<AttributeModifiers.Entry> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AttributeModifiers.Entry::attribute,
        AttributeModifierCodec.CODEC, AttributeModifiers.Entry::modifier,
        ByteBufCodecs.VAR_INT, AttributeModifiers.Entry::slot,
        StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_6, DisplayCodec.CODEC, AttributeModifiers.Display.Default.INSTANCE), AttributeModifiers.Entry::display,
        AttributeModifiers.Entry::new
    );

    interface AttributeModifierCodec {

      StreamCodec<AttributeModifiers.AttributeModifier> CODEC = StreamCodec.composite(
          StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21, ByteBufCodecs.UUID), AttributeModifiers.AttributeModifier::uuid,
          ByteBufCodecs.STRING_UTF8, AttributeModifiers.AttributeModifier::name,
          ByteBufCodecs.DOUBLE, AttributeModifiers.AttributeModifier::amount,
          ByteBufCodecs.VAR_INT, AttributeModifiers.AttributeModifier::operation,
          AttributeModifiers.AttributeModifier::new
      );
    }

    interface DisplayCodec {

      StreamCodec<AttributeModifiers.Display> CODEC = new StreamCodec<>() {

        @Override
        public AttributeModifiers.Display decode(ByteBuf buf, ProtocolVersion version) {
          int id = ProtocolUtils.readVarInt(buf);
          return switch (id) {
            case 0 -> AttributeModifiers.Display.Default.INSTANCE;
            case 1 -> AttributeModifiers.Display.Hidden.INSTANCE;
            case 2 -> new AttributeModifiers.Display.OverrideText(ComponentHolderCodec.CODEC.decode(buf, version));
            default -> throw new IllegalStateException("Unexpected value: " + id);
          };
        }

        @Override
        public void encode(ByteBuf buf, ProtocolVersion version, AttributeModifiers.Display value) {
          if (value instanceof AttributeModifiers.Display.Default) {
            ProtocolUtils.writeVarInt(buf, 0);
          } else if (value instanceof AttributeModifiers.Display.Hidden) {
            ProtocolUtils.writeVarInt(buf, 1);
          } else if (value instanceof AttributeModifiers.Display.OverrideText overrideText) {
            ProtocolUtils.writeVarInt(buf, 2);
            ComponentHolderCodec.CODEC.encode(buf, version, overrideText.component());
          } else {
            throw new IllegalArgumentException(value.getClass().getName());
          }
        }
      };
    }
  }
}
