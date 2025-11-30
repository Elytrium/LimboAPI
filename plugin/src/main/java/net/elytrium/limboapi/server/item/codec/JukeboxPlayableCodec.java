package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.JukeboxPlayable;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.ComponentHolderCodec;
import net.elytrium.limboapi.server.item.codec.data.EitherCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderCodec;
import net.elytrium.limboapi.server.item.codec.data.SoundEventCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface JukeboxPlayableCodec {

  StreamCodec<JukeboxPlayable> CODEC = StreamCodec.composite(
      EitherCodec.codec(HolderCodec.codec(JukeboxSongCodec.CODEC), ByteBufCodecs.STRING_UTF8), JukeboxPlayable::song,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL, true), JukeboxPlayable::showInTooltip,
      JukeboxPlayable::new
  );

  interface JukeboxSongCodec {

    StreamCodec<JukeboxPlayable.JukeboxSong> CODEC = StreamCodec.composite(
        SoundEventCodec.HOLDER_CODEC, JukeboxPlayable.JukeboxSong::soundEvent,
        ComponentHolderCodec.CODEC, JukeboxPlayable.JukeboxSong::description,
        ByteBufCodecs.FLOAT, JukeboxPlayable.JukeboxSong::lengthInSeconds,
        ByteBufCodecs.VAR_INT, JukeboxPlayable.JukeboxSong::comparatorOutput,
        JukeboxPlayable.JukeboxSong::new
    );
  }
}
