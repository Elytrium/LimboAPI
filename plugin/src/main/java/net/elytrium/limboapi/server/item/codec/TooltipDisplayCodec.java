package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.Set;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentType;
import net.elytrium.limboapi.api.world.item.datacomponent.type.TooltipDisplay;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.DataComponentRegistry;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TooltipDisplayCodec {

  StreamCodec<TooltipDisplay> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, TooltipDisplay::hideTooltip,
      new StreamCodec<>() {

        @Override
        public Set<DataComponentType> decode(ByteBuf buf, ProtocolVersion version) {
          int count = ProtocolUtils.readVarInt(buf);
          var result = new ReferenceLinkedOpenHashSet<DataComponentType>(count);
          for (int i = 0; i < count; ++i) {
            result.add(DataComponentRegistry.getType(ProtocolUtils.readVarInt(buf), version));
          }

          return result;
        }

        @Override
        public void encode(ByteBuf buf, ProtocolVersion version, Set<DataComponentType> value) {
          ProtocolUtils.writeVarIntArray(buf, value.stream().mapToInt(type -> DataComponentRegistry.getId(type, version)).filter(value1 -> value1 != Integer.MIN_VALUE).toArray());
        }
      }, TooltipDisplay::hiddenComponents,
      TooltipDisplay::new
  );
}
