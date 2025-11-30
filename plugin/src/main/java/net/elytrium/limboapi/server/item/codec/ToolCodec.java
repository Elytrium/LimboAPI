package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Tool;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import net.elytrium.limboapi.server.item.codec.data.HolderSetCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ToolCodec {

  StreamCodec<Tool> CODEC = StreamCodec.composite(
      ByteBufCodecs.collection(RuleCodec.CODEC), Tool::rules,
      ByteBufCodecs.FLOAT, Tool::defaultMiningSpeed,
      ByteBufCodecs.VAR_INT, Tool::damagePerBlock,
      StreamCodec.ge(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL, true), Tool::canDestroyBlocksInCreative,
      Tool::new
  );

  interface RuleCodec {

    StreamCodec<Tool.Rule> CODEC = StreamCodec.composite(
        HolderSetCodec.CODEC, Tool.Rule::blocks,
        ByteBufCodecs.optional(ByteBufCodecs.FLOAT), Tool.Rule::speed,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL), Tool.Rule::correctForDrops,
        Tool.Rule::new
    );
  }
}
