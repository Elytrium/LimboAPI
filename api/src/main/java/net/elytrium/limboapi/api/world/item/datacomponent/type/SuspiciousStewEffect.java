package net.elytrium.limboapi.api.world.item.datacomponent.type;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SuspiciousStewEffect(int/*Holder<MobEffect>*/ effect, int duration) {

}
