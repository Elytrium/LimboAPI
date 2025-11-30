package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import java.util.Collection;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface ConsumeEffect permits ConsumeEffect.ApplyStatusEffects, ConsumeEffect.RemoveStatusEffects, ConsumeEffect.ClearAllStatusEffects, ConsumeEffect.TeleportRandomly, ConsumeEffect.PlaySound {

  record ApplyStatusEffects(Collection<MobEffect> effects, float probability) implements ConsumeEffect {

  }

  record RemoveStatusEffects(HolderSet effects) implements ConsumeEffect {

  }

  record ClearAllStatusEffects() implements ConsumeEffect {

    public static final ClearAllStatusEffects INSTANCE = new ClearAllStatusEffects();
  }

  record TeleportRandomly(float diameter) implements ConsumeEffect {

  }

  record PlaySound(Holder<SoundEvent> sound) implements ConsumeEffect {

  }
}
