package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.BlocksAttacks;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderSetCodec;
import net.elytrium.limboapi.server.item.codec.data.SoundEventCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BlocksAttacksCodec {

  StreamCodec<BlocksAttacks> CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, BlocksAttacks::blockDelaySeconds,
      ByteBufCodecs.FLOAT, BlocksAttacks::disableCooldownScale,
      ByteBufCodecs.collection(DamageReductionCodec.CODEC), BlocksAttacks::damageReductions,
      ItemDamageFunctionCodec.CODEC, BlocksAttacks::itemDamage,
      ByteBufCodecs.OPTIONAL_STRING_UTF8, BlocksAttacks::bypassedBy,
      SoundEventCodec.OPTIONAL_HOLDER_CODEC, BlocksAttacks::blockSound,
      SoundEventCodec.OPTIONAL_HOLDER_CODEC, BlocksAttacks::disableSound,
      BlocksAttacks::new
  );

  interface DamageReductionCodec {

    StreamCodec<BlocksAttacks.DamageReduction> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, BlocksAttacks.DamageReduction::horizontalBlockingAngle,
        HolderSetCodec.OPTIONAL_CODEC, BlocksAttacks.DamageReduction::type,
        ByteBufCodecs.FLOAT, BlocksAttacks.DamageReduction::base,
        ByteBufCodecs.FLOAT, BlocksAttacks.DamageReduction::factor,
        BlocksAttacks.DamageReduction::new
    );
  }

  interface ItemDamageFunctionCodec {

    StreamCodec<BlocksAttacks.ItemDamageFunction> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, BlocksAttacks.ItemDamageFunction::threshold,
        ByteBufCodecs.FLOAT, BlocksAttacks.ItemDamageFunction::base,
        ByteBufCodecs.FLOAT, BlocksAttacks.ItemDamageFunction::factor,
        BlocksAttacks.ItemDamageFunction::new
    );
  }
}
