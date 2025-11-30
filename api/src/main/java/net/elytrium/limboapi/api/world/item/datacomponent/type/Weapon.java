package net.elytrium.limboapi.api.world.item.datacomponent.type;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record Weapon(int itemDamagePerAttack, float disableBlockingForSeconds) {

}
