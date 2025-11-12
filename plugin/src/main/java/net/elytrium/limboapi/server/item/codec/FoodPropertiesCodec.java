package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.protocol.data.ItemStack;
import net.elytrium.limboapi.api.world.item.datacomponent.type.FoodProperties;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.ItemStackCodec;
import net.elytrium.limboapi.server.item.codec.data.MobEffectCodec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface FoodPropertiesCodec {

  StreamCodec<FoodProperties> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, FoodProperties::nutrition,
      ByteBufCodecs.FLOAT, FoodProperties::saturation,
      ByteBufCodecs.BOOL, FoodProperties::canAlwaysEat,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_2, ByteBufCodecs.FLOAT, 1.6F), FoodProperties::eatSeconds,
      StreamCodec.<@Nullable ItemStack>eq(ProtocolVersion.MINECRAFT_1_21, ByteBufCodecs.optional(ItemStackCodec.CODEC)), FoodProperties::usingConvertsTo,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_2, ByteBufCodecs.collection(PossibleEffectCodec.CODEC)), FoodProperties::effects,
      FoodProperties::new
  );

  interface PossibleEffectCodec {

    StreamCodec<FoodProperties.PossibleEffect> CODEC = StreamCodec.composite(
        MobEffectCodec.CODEC, FoodProperties.PossibleEffect::effect,
        ByteBufCodecs.FLOAT, FoodProperties.PossibleEffect::probability,
        FoodProperties.PossibleEffect::new
    );
  }
}
