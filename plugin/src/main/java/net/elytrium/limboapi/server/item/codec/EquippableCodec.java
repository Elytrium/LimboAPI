package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Equippable;
import net.elytrium.limboapi.api.world.item.datacomponent.type.data.Holder;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderSetCodec;
import net.elytrium.limboapi.server.item.codec.data.SoundEventCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EquippableCodec {

  StreamCodec<Equippable> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, Equippable::slot,
      SoundEventCodec.HOLDER_CODEC, Equippable::equipSound,
      ByteBufCodecs.OPTIONAL_STRING_UTF8, Equippable::assetId,
      ByteBufCodecs.OPTIONAL_STRING_UTF8, Equippable::cameraOverlay,
      HolderSetCodec.OPTIONAL_CODEC, Equippable::allowedEntities,
      ByteBufCodecs.BOOL, Equippable::dispensable,
      ByteBufCodecs.BOOL, Equippable::swappable,
      ByteBufCodecs.BOOL, Equippable::damageOnHurt,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL, true), Equippable::equipOnInteract,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_6, ByteBufCodecs.BOOL, false), Equippable::canBeSheared,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_6, SoundEventCodec.HOLDER_CODEC, Holder.ref(0)), Equippable::shearingSound,
      Equippable::new
  );
}
