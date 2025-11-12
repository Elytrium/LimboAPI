package net.elytrium.limboapi.api.protocol.data;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.ServiceLoader;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;

public class ComponentHolder {

  public static final Codec CODEC = ServiceLoader.load(Codec.class, ComponentHolder.class.getClassLoader()).iterator().next();

  private final Component component;
  private final String json;
  private final BinaryTag binaryTag;

  public ComponentHolder(Component component) {
    this.component = component;
    this.json = null;
    this.binaryTag = null;
  }

  public ComponentHolder(String json) {
    this.component = null;
    this.json = json;
    this.binaryTag = null;
  }

  public ComponentHolder(BinaryTag binaryTag) {
    this.component = null;
    this.json = null;
    this.binaryTag = binaryTag;
  }

  public Component getComponent(ProtocolVersion version) {
    Component component = this.component;
    if (component == null) {
      if (this.json != null) {
        return ComponentHolder.CODEC.json2Component(version, this.json);
      } else if (this.binaryTag != null) {
        return ComponentHolder.CODEC.binaryTag2Component(version, this.binaryTag);
      }
    }

    return component;
  }

  public String getJson(ProtocolVersion version) {
    String json = this.json;
    return json == null ? ComponentHolder.CODEC.component2Json(version, this.getComponent(version)) : json;
  }

  public BinaryTag getBinaryTag(ProtocolVersion version) {
    BinaryTag binaryTag = this.binaryTag;
    return binaryTag == null ? ComponentHolder.CODEC.component2BinaryTag(version, this.getComponent(version)) : binaryTag;
  }

  public interface Codec {

    Component json2Component(ProtocolVersion version, String json);

    Component binaryTag2Component(ProtocolVersion version, BinaryTag binaryTag);

    String component2Json(ProtocolVersion version, Component component);

    BinaryTag component2BinaryTag(ProtocolVersion version, Component component);
  }
}
