package net.elytrium.limboapi.server.item.codec;

import net.elytrium.limboapi.api.world.item.datacomponent.type.LodestoneTracker;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.GlobalPosCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface LodestoneTrackerCodec {

  StreamCodec<LodestoneTracker> CODEC = StreamCodec.composite(
      GlobalPosCodec.OPTIONAL_CODEC, LodestoneTracker::target,
      ByteBufCodecs.BOOL, LodestoneTracker::tracked,
      LodestoneTracker::new
  );
}
