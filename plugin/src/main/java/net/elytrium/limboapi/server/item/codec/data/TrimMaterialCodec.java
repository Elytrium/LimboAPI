package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.TrimMaterial;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TrimMaterialCodec {

  StreamCodec<TrimMaterial> CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, TrimMaterial::assetName,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.VAR_INT, 0), TrimMaterial::ingredient,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_4, ByteBufCodecs.FLOAT, 0.0F), TrimMaterial::itemModelIndex,
      new StreamCodec<>() {

        private static final StreamCodec<Map<Object, String>> STRING_2_STRING_MAP = ByteBufCodecs.map(Object2ObjectOpenHashMap::new, new StreamCodec<>() {

          @Override
          public Object decode(ByteBuf buf, ProtocolVersion version) {
            return ByteBufCodecs.STRING_UTF8.decode(buf, version);
          }

          @Override
          public void encode(ByteBuf buf, ProtocolVersion version, Object value) {
            ByteBufCodecs.STRING_UTF8.encode(buf, version, String.valueOf(value));
          }
        }, ByteBufCodecs.STRING_UTF8);
        private static final StreamCodec<Map<Object, String>> INT_2_STRING_MAP = ByteBufCodecs.map(Object2ObjectOpenHashMap::new, new StreamCodec<>() {

          @Override
          public Object decode(ByteBuf buf, ProtocolVersion version) {
            return ByteBufCodecs.VAR_INT.decode(buf, version);
          }

          @Override
          public void encode(ByteBuf buf, ProtocolVersion version, Object value) {
            ProtocolUtils.writeVarInt(buf, value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value)));
          }
        }, ByteBufCodecs.STRING_UTF8);

        @Override
        public Map<Object, String> decode(ByteBuf buf, ProtocolVersion version) {
          return version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2) ? STRING_2_STRING_MAP.decode(buf, version) : INT_2_STRING_MAP.decode(buf, version);
        }

        @Override
        public void encode(ByteBuf buf, ProtocolVersion version, Map<Object, String> value) {
          (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2) ? STRING_2_STRING_MAP : INT_2_STRING_MAP).encode(buf, version, value);
        }
      }, TrimMaterial::overrides,
      ComponentHolderCodec.CODEC, TrimMaterial::description,
      TrimMaterial::new
  );
  StreamCodec<Holder<TrimMaterial>> HOLDER_CODEC = HolderCodec.codec(TrimMaterialCodec.CODEC);
}