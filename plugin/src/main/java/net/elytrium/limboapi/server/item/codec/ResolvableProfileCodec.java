/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboapi.server.item.codec;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.elytrium.limboapi.api.world.item.datacomponent.type.ResolvableProfile;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ResolvableProfileCodec {

  StreamCodec<ResolvableProfile.Property> GAME_PROFILE_PROPERTY_CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(64), ResolvableProfile.Property::name,
      ByteBufCodecs.STRING_UTF8, ResolvableProfile.Property::value,
      ByteBufCodecs.OPTIONAL_STRING_UTF8_1024, ResolvableProfile.Property::signature,
      ResolvableProfile.Property::new
  );
  StreamCodec<Collection<ResolvableProfile.Property>> GAME_PROFILE_PROPERTY_COLLECTION_CODEC = ByteBufCodecs.collection(ResolvableProfileCodec.GAME_PROFILE_PROPERTY_CODEC, 16);
  StreamCodec<ResolvableProfile.GameProfile> RESOLVED_GAME_PROFILE_CODEC = StreamCodec.composite(
      ByteBufCodecs.UUID, ResolvableProfile.GameProfile::id,
      ByteBufCodecs.PLAYER_NAME, ResolvableProfile.GameProfile::name,
      ResolvableProfileCodec.GAME_PROFILE_PROPERTY_COLLECTION_CODEC, ResolvableProfile.GameProfile::properties,
      ResolvableProfile.ResolvedGameProfile::new
  );
  StreamCodec<ResolvableProfile.GameProfile> PARTIAL_GAME_PROFILE_CODEC = StreamCodec.composite(
      ByteBufCodecs.OPTIONAL_PLAYER_NAME, ResolvableProfile.GameProfile::name,
      ByteBufCodecs.OPTIONAL_UUID, ResolvableProfile.GameProfile::id,
      ResolvableProfileCodec.GAME_PROFILE_PROPERTY_COLLECTION_CODEC, ResolvableProfile.GameProfile::properties,
      ResolvableProfile.PartialGameProfile::new
  );
  StreamCodec<ResolvableProfile.PlayerSkinPatch> PLAYER_SKIN_PATCH_CODEC = StreamCodec.composite(
      ByteBufCodecs.OPTIONAL_STRING_UTF8, ResolvableProfile.PlayerSkinPatch::body,
      ByteBufCodecs.OPTIONAL_STRING_UTF8, ResolvableProfile.PlayerSkinPatch::cape,
      ByteBufCodecs.OPTIONAL_STRING_UTF8, ResolvableProfile.PlayerSkinPatch::elytra,
      ByteBufCodecs.optional(ByteBufCodecs.BOOL), ResolvableProfile.PlayerSkinPatch::model,
      ResolvableProfile.PlayerSkinPatch::new
  );
  StreamCodec<ResolvableProfile> CODEC = new StreamCodec<>() {

    @Override
    public ResolvableProfile decode(ByteBuf buf, ProtocolVersion version) {
      ResolvableProfile.GameProfile profile;
      ResolvableProfile.PlayerSkinPatch patch;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_9)) {
        profile = buf.readBoolean()
            ? ResolvableProfileCodec.RESOLVED_GAME_PROFILE_CODEC.decode(buf, version)
            : ResolvableProfileCodec.PARTIAL_GAME_PROFILE_CODEC.decode(buf, version);
        patch = ResolvableProfileCodec.PLAYER_SKIN_PATCH_CODEC.decode(buf, version);
      } else {
        profile = ResolvableProfileCodec.PARTIAL_GAME_PROFILE_CODEC.decode(buf, version);
        patch = ResolvableProfile.PlayerSkinPatch.EMPTY;
      }

      return new ResolvableProfile(profile, patch);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolVersion version, ResolvableProfile value) {
      ResolvableProfile.GameProfile profile = value.gameProfile();
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_9)) {
        if (profile instanceof ResolvableProfile.ResolvedGameProfile) {
          buf.writeBoolean(true);
          ResolvableProfileCodec.RESOLVED_GAME_PROFILE_CODEC.encode(buf, version, profile);
        } else {
          buf.writeBoolean(false);
          ResolvableProfileCodec.PARTIAL_GAME_PROFILE_CODEC.encode(buf, version, profile);
        }
        ResolvableProfileCodec.PLAYER_SKIN_PATCH_CODEC.encode(buf, version, value.skinPatch());
      } else {
        ResolvableProfileCodec.PARTIAL_GAME_PROFILE_CODEC.encode(buf, version, profile);
      }
    }
  };
}
