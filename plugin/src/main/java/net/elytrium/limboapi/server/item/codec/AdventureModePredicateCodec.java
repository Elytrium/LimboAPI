package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import net.elytrium.limboapi.api.world.item.datacomponent.DataComponentType;
import net.elytrium.limboapi.api.world.item.datacomponent.type.AdventureModePredicate;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.DataComponentRegistry;
import net.elytrium.limboapi.server.item.codec.data.HolderSetCodec;
import net.elytrium.limboapi.utils.Unit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface AdventureModePredicateCodec {

  StreamCodec<AdventureModePredicate> CODEC = StreamCodec.composite(
      ByteBufCodecs.collection(BlockPredicateCodec.CODEC), AdventureModePredicate::predicates,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL, true), AdventureModePredicate::showInTooltip,
      AdventureModePredicate::new
  );

  interface BlockPredicateCodec {

    StreamCodec<AdventureModePredicate.BlockPredicate> CODEC = StreamCodec.composite(
        HolderSetCodec.OPTIONAL_CODEC, AdventureModePredicate.BlockPredicate::blocks,
        ByteBufCodecs.optional(ByteBufCodecs.collection(PropertyMatcherCodec.CODEC)), AdventureModePredicate.BlockPredicate::properties,
        ByteBufCodecs.optional(ByteBufCodecs.COMPOUND_TAG), AdventureModePredicate.BlockPredicate::nbt,
        DataComponentMatchersCodec.CODEC, AdventureModePredicate.BlockPredicate::components,
        AdventureModePredicate.BlockPredicate::new
    );

    interface PropertyMatcherCodec {

      StreamCodec<AdventureModePredicate.PropertyMatcher> CODEC = StreamCodec.composite(
          ByteBufCodecs.STRING_UTF8, AdventureModePredicate.PropertyMatcher::name,
          ValueMatcherCodec.CODEC, AdventureModePredicate.PropertyMatcher::valueMatcher,
          AdventureModePredicate.PropertyMatcher::new
      );

      interface ValueMatcherCodec {

        StreamCodec<AdventureModePredicate.ValueMatcher.Exact> EXACT_MATCHER_CODEC = ByteBufCodecs.STRING_UTF8.map(
            AdventureModePredicate.ValueMatcher.Exact::new, AdventureModePredicate.ValueMatcher.Exact::value
        );
        StreamCodec<AdventureModePredicate.ValueMatcher.Ranged> RANGED_MATCHER_CODEC = StreamCodec.composite(
            ByteBufCodecs.OPTIONAL_STRING_UTF8, AdventureModePredicate.ValueMatcher.Ranged::minValue,
            ByteBufCodecs.OPTIONAL_STRING_UTF8, AdventureModePredicate.ValueMatcher.Ranged::maxValue,
            AdventureModePredicate.ValueMatcher.Ranged::new
        );
        StreamCodec<AdventureModePredicate.ValueMatcher> CODEC = new StreamCodec<>() {

          @Override
          public AdventureModePredicate.ValueMatcher decode(ByteBuf buf, ProtocolVersion version) {
            return buf.readBoolean() ? ValueMatcherCodec.EXACT_MATCHER_CODEC.decode(buf, version) : ValueMatcherCodec.RANGED_MATCHER_CODEC.decode(buf, version);
          }

          @Override
          public void encode(ByteBuf buf, ProtocolVersion version, AdventureModePredicate.ValueMatcher value) {
            if (value instanceof AdventureModePredicate.ValueMatcher.Exact exact) {
              buf.writeBoolean(true);
              ValueMatcherCodec.EXACT_MATCHER_CODEC.encode(buf, version, exact);
            } else if (value instanceof AdventureModePredicate.ValueMatcher.Ranged ranged) {
              buf.writeBoolean(false);
              ValueMatcherCodec.RANGED_MATCHER_CODEC.encode(buf, version, ranged);
            } else {
              throw new IllegalArgumentException(value.getClass().getName());
            }
          }
        };
      }
    }

    interface DataComponentMatchersCodec {

      StreamCodec<Collection<AdventureModePredicate.DataComponentExactPredicate>> EXACT_PREDICATE_CODEC = new StreamCodec<>() {

        @Override
        @SuppressWarnings("unchecked")
        public Collection<AdventureModePredicate.DataComponentExactPredicate> decode(ByteBuf buf, ProtocolVersion version) {
          int count = ProtocolUtils.readVarInt(buf);
          var result = new ArrayList<AdventureModePredicate.DataComponentExactPredicate>(count);
          for (int i = 0; i < count; ++i) {
            DataComponentType type = DataComponentRegistry.getType(ProtocolUtils.readVarInt(buf), version);
            Object value = ((DataComponentRegistry.DataComponentTypeImpl<?>) type).codec().decode(buf, version);
            result.add(value == Unit.INSTANCE
                ? new AdventureModePredicate.DataComponentExactPredicate.NonValued((DataComponentType.NonValued) type)
                : new AdventureModePredicate.DataComponentExactPredicate.Valued<>((DataComponentType.Valued<@NonNull Object>) type, value)
            );
          }

          return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void encode(ByteBuf buf, ProtocolVersion version, Collection<AdventureModePredicate.DataComponentExactPredicate> value) {
          Object[] result = value.stream().map(predicate -> {
            int id = DataComponentRegistry.getId(predicate.type(), version);
            if (id == Integer.MIN_VALUE) {
              return null;
            }

            return IntObjectImmutablePair.of(id, predicate);
          }).filter(Objects::nonNull).toArray();
          ProtocolUtils.writeVarInt(buf, result.length);
          for (Object _pair : result) {
            var pair = (IntObjectImmutablePair<AdventureModePredicate.DataComponentExactPredicate>) _pair;
            ProtocolUtils.writeVarInt(buf, pair.leftInt());
            AdventureModePredicate.DataComponentExactPredicate predicate = pair.right();
            ((DataComponentRegistry.DataComponentTypeImpl<Object>) predicate.type()).codec().encode(buf, version, predicate instanceof AdventureModePredicate.DataComponentExactPredicate.NonValued
                ? Unit.INSTANCE
                : ((AdventureModePredicate.DataComponentExactPredicate.Valued<@NonNull Object>) predicate).value()
            );
          }
        }
      };
      StreamCodec<AdventureModePredicate.DataComponentPartialPredicate> PARTIAL_PREDICATE_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, AdventureModePredicate.DataComponentPartialPredicate::id,
          ByteBufCodecs.TAG, AdventureModePredicate.DataComponentPartialPredicate::predicate,
          AdventureModePredicate.DataComponentPartialPredicate::new
      );
      StreamCodec<AdventureModePredicate.DataComponentMatchers> CODEC = StreamCodec.composite(
          DataComponentMatchersCodec.EXACT_PREDICATE_CODEC, AdventureModePredicate.DataComponentMatchers::exact,
          ByteBufCodecs.collection(DataComponentMatchersCodec.PARTIAL_PREDICATE_CODEC, 64), AdventureModePredicate.DataComponentMatchers::partial,
          AdventureModePredicate.DataComponentMatchers::new
      );
    }
  }
}
