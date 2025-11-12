/*
 * Copyright (C) 2021 - 2025 Elytrium
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
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.elytrium.limboapi.api.world.item.datacomponent.type.Enchantments;
import net.elytrium.limboapi.protocol.codec.ByteBufCodecs;
import net.elytrium.limboapi.protocol.codec.StreamCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EnchantmentsCodec {

  StreamCodec<Enchantments> CODEC = StreamCodec.composite(
      ByteBufCodecs.map(Int2IntOpenHashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT), Enchantments::enchantments,
      StreamCodec.lt(ProtocolVersion.MINECRAFT_1_21_5, ByteBufCodecs.BOOL, true), Enchantments::showInTooltip,
      Enchantments::new
  );
}
