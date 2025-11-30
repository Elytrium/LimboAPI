package net.elytrium.limboapi.server.item.codec.data;

import com.velocitypowered.api.network.ProtocolVersion;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Function;
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
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_2,
          ByteBufCodecs.<Object, String, Map<Object, String>>map(Object2ObjectOpenHashMap::new,
              ByteBufCodecs.VAR_INT.map(Function.identity(), value -> value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value))),
              ByteBufCodecs.STRING_UTF8
          ),
          ByteBufCodecs.<Object, String, Map<Object, String>>map(Object2ObjectOpenHashMap::new,
              ByteBufCodecs.STRING_UTF8.map(Function.identity(), Object::toString),
              ByteBufCodecs.STRING_UTF8
          )
      ), TrimMaterial::overrides,
      ComponentHolderCodec.CODEC, TrimMaterial::description,
      TrimMaterial::new
  );
  StreamCodec<Holder<TrimMaterial>> HOLDER_CODEC = HolderCodec.codec(TrimMaterialCodec.CODEC);
}
