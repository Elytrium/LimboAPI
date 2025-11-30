package net.elytrium.limboapi.utils;

import net.elytrium.limboapi.protocol.codec.StreamCodec;

public enum Unit {

  INSTANCE;

  public static final StreamCodec<Unit> CODEC = StreamCodec.unit(Unit.INSTANCE);
}
