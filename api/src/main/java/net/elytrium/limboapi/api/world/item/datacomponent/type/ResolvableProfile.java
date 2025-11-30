package net.elytrium.limboapi.api.world.item.datacomponent.type;

import java.util.Collection;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * @param skinPatch Added in version 1.21.9
 */
@NullMarked
public record ResolvableProfile(GameProfile gameProfile, PlayerSkinPatch skinPatch) {

  public ResolvableProfile(GameProfile gameProfile) {
    this(gameProfile, PlayerSkinPatch.EMPTY);
  }

  public sealed interface GameProfile permits ResolvedGameProfile, PartialGameProfile {

    UUID id();

    String name();

    Collection<Property> properties();
  }

  /**
   * @sinceMinecraft 1.21.9
   */
  public record ResolvedGameProfile(UUID id, String name, Collection<Property> properties) implements GameProfile {

  }

  public record PartialGameProfile(@Nullable String name, @Nullable UUID id, Collection<Property> properties) implements GameProfile {

  }

  public record Property(String name, String value, @Nullable String signature) {

    public Property(String name, String value) {
      this(name, value, null);
    }
  }

  /**
   * @param model {@code true} if the model is slim; {@code false} if the model is wide.
   */
  public record PlayerSkinPatch(@Nullable String body, @Nullable String cape, @Nullable String elytra, @Nullable Boolean model) {

    public static final PlayerSkinPatch EMPTY = new PlayerSkinPatch(null, null, null, null);
  }
}
