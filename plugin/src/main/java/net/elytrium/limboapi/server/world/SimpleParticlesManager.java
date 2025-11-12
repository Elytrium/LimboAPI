package net.elytrium.limboapi.server.world;

import com.google.gson.internal.LinkedTreeMap;
import com.velocitypowered.api.network.ProtocolVersion;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.EnumMap;
import java.util.Locale;
import net.elytrium.limboapi.LimboAPI;
import net.elytrium.limboapi.utils.JsonUtil;

// TODO legacy (<=1.12.2) + api
public class SimpleParticlesManager {

  private static final EnumMap<ProtocolVersion, Int2ReferenceOpenHashMap<Particle>> PROTOCOL_2_PARTICLE = new EnumMap<>(ProtocolVersion.class);

  static {
    var particlesMappings = JsonUtil.<LinkedTreeMap<String, Number>>parse(LimboAPI.class.getResourceAsStream("/mappings/particle_types_mappings.json"));
    particlesMappings.forEach((modernId, versions) -> {
      Particle particle = Particle.valueOf(modernId.substring(10/*"minecraft:".length()*/).toUpperCase(Locale.US));
      versions.forEach((version, id) -> SimpleParticlesManager.PROTOCOL_2_PARTICLE.computeIfAbsent(
          ProtocolVersion.getProtocolVersion(Integer.parseInt(version)),
          key -> new Int2ReferenceOpenHashMap<>(versions.size())
      ).put(id.intValue(), particle));
    });
    SimpleParticlesManager.PROTOCOL_2_PARTICLE.values().forEach(Int2ReferenceOpenHashMap::trim);
  }

  public static Particle fromProtocolId(ProtocolVersion version, int id) {
    return SimpleParticlesManager.PROTOCOL_2_PARTICLE.get(version).get(id);
  }
}
