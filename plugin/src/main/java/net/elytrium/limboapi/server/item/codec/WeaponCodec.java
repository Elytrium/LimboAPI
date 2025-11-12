package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.Weapon;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface WeaponCodec {

  StreamCodec<Weapon> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, Weapon::itemDamagePerAttack,
      ByteBufCodecs.FLOAT, Weapon::disableBlockingForSeconds,
      Weapon::new
  );
}
