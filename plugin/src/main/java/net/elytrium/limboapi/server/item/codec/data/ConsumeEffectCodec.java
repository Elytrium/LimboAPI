package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.ConsumeEffect;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConsumeEffectCodec {

  // TODO registry based
  StreamCodec<ConsumeEffect> CODEC = new StreamCodec<>() {

    @Override
    public ConsumeEffect decode(ByteBuf buf, ProtocolVersion version) {
      int id = ProtocolUtils.readVarInt(buf);
      return switch (id) {
        case 0 -> new ConsumeEffect.ApplyStatusEffects(MobEffectCodec.COLLECTION_CODEC.decode(buf, version), buf.readFloat());
        case 1 -> new ConsumeEffect.RemoveStatusEffects(HolderSetCodec.CODEC.decode(buf, version));
        case 2 -> ConsumeEffect.ClearAllStatusEffects.INSTANCE;
        case 3 -> new ConsumeEffect.TeleportRandomly(buf.readFloat());
        case 4 -> new ConsumeEffect.PlaySound(SoundEventCodec.HOLDER_CODEC.decode(buf, version));
        default -> throw new IllegalStateException("Unexpected value: " + id);
      };
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, ConsumeEffect value) {
      if (value instanceof ConsumeEffect.ApplyStatusEffects applyStatusEffects) {
        ProtocolUtils.writeVarInt(buf, 0);
        MobEffectCodec.COLLECTION_CODEC.encode(buf, version, applyStatusEffects.effects());
        buf.writeFloat(applyStatusEffects.probability());
      } else if (value instanceof ConsumeEffect.RemoveStatusEffects removeStatusEffects) {
        ProtocolUtils.writeVarInt(buf, 1);
        HolderSetCodec.CODEC.encode(buf, version, removeStatusEffects.effects());
      } else if (value instanceof ConsumeEffect.ClearAllStatusEffects) {
        ProtocolUtils.writeVarInt(buf, 2);
      } else if (value instanceof ConsumeEffect.TeleportRandomly teleportRandomly) {
        ProtocolUtils.writeVarInt(buf, 3);
        buf.writeFloat(teleportRandomly.diameter());
      } else if (value instanceof ConsumeEffect.PlaySound playSound) {
        ProtocolUtils.writeVarInt(buf, 4);
        SoundEventCodec.HOLDER_CODEC.encode(buf, version, playSound.sound());
      } else {
        throw new IllegalArgumentException(value.getClass().getName());
      }
    }
  };
  StreamCodec<Collection<ConsumeEffect>> COLLECTION_CODEC = ByteBufCodecs.collection(ConsumeEffectCodec.CODEC);
}
