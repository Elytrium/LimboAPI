/*
 * Copyright (C) 2021 Elytrium
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

package net.elytrium.limboapi.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.elytrium.limboapi.LimboAPI;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

public class Config {

  private static final Logger LOGGER = LimboAPI.getInstance().getLogger();
  private String oldPrefix = "";
  private String currentPrefix = "";

  /**
   * Set the value of a specific node. Probably throws some error if you supply non-existing keys or invalid values.
   *
   * @param key   config node
   * @param value value
   */
  private void set(String key, Object value, Class<?> root) {
    String[] split = key.split("\\.");
    Object instance = this.getInstance(split, root);
    if (instance != null) {
      Field field = this.getField(split, instance);
      if (field != null) {
        try {
          if (field.getAnnotation(Final.class) != null) {
            return;
          }
          if (field.getType() == String.class && !(value instanceof String)) {
            value = value + "";
          }
          field.set(instance, value);
          return;
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    }

    LOGGER.debug("Failed to set config option: " + key + ": " + value + " | " + instance + " | " + root.getSimpleName() + ".yml");
  }

  @SuppressWarnings("unchecked")
  public void set(Map<String, Object> input, String oldPath) {
    for (Map.Entry<String, Object> entry : input.entrySet()) {
      String key = oldPath + (oldPath.isEmpty() ? "" : ".") + entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map) {
        this.set((Map<String, Object>) value, key);
      } else if (value instanceof String) {
        if (key.equalsIgnoreCase("prefix") && !this.currentPrefix.equals(value)) {
          this.currentPrefix = (String) value;
        }

        this.set(key, ((String) value).replace("{NL}", "\n").replace("{PRFX}", this.currentPrefix), this.getClass());
      } else {
        this.set(key, value, this.getClass());
      }
    }
  }

  public boolean load(File file, String prefix) {
    this.oldPrefix = this.currentPrefix.isEmpty() ? prefix : this.currentPrefix;
    this.currentPrefix = prefix;
    if (!file.exists()) {
      return false;
    }

    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      this.set(new Yaml().load(reader), "");
    } catch (IOException e) {
      LOGGER.warn("Unable to load config ", e);
      return false;
    }

    return true;
  }

  /**
   * Indicates that a field should be instantiated / created.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD})
  public @interface Create {

  }

  /**
   * Indicates that a field cannot be modified.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD})
  public @interface Final {

  }

  /**
   * Creates a comment.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface Comment {

    String[] value();
  }

  /**
   * Any field or class with is not part of the config.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface Ignore {

  }

  private String toYamlString(Object value, String spacing, String fieldName) {
    if (value instanceof List) {
      Collection<?> listValue = (Collection<?>) value;
      if (listValue.isEmpty()) {
        return "[]";
      }
      StringBuilder m = new StringBuilder();
      for (Object obj : listValue) {
        m.append(System.lineSeparator()).append(spacing).append("- ").append(this.toYamlString(obj, spacing, fieldName));
      }

      return m.toString();
    }

    if (value instanceof String) {
      String stringValue = (String) value;
      if (stringValue.isEmpty()) {
        return "\"\"";
      }

      String quoted = "\"" + stringValue + "\"";
      if (fieldName.equalsIgnoreCase("prefix")) {
        return quoted;
      } else {
        return quoted.replace("\n", "{NL}").replace(this.currentPrefix.equals(this.oldPrefix) ? this.oldPrefix : this.currentPrefix, "{PRFX}");
      }
    }

    return value != null ? value.toString() : "null";
  }

  /**
   * Set all values in the file (load first to avoid overwriting).
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public void save(File file) {
    try {
      if (!file.exists()) {
        File parent = file.getParentFile();
        if (parent != null) {
          file.getParentFile().mkdirs();
        }
        file.createNewFile();
      }

      PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8);
      Object instance = this;
      this.save(writer, this.getClass(), instance, 0);
      writer.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void save(PrintWriter writer, Class<?> clazz, final Object instance, int indent) {
    try {
      String lineSeparator = System.lineSeparator();
      String spacing = this.repeat(" ", indent);

      for (Field field : clazz.getFields()) {
        if (field.getAnnotation(Ignore.class) != null) {
          continue;
        }
        Class<?> current = field.getType();
        if (field.getAnnotation(Ignore.class) != null) {
          continue;
        }

        Comment comment = field.getAnnotation(Comment.class);
        if (comment != null) {
          for (String commentLine : comment.value()) {
            writer.write(spacing + "# " + commentLine + lineSeparator);
          }
        }

        Create create = field.getAnnotation(Create.class);
        if (create != null) {
          Object value = field.get(instance);
          this.setAccessible(field);
          if (indent == 0) {
            writer.write(lineSeparator);
          }
          comment = current.getAnnotation(Comment.class);
          if (comment != null) {
            for (String commentLine : comment.value()) {
              writer.write(spacing + "# " + commentLine + lineSeparator);
            }
          }
          writer.write(spacing + this.toNodeName(current.getSimpleName()) + ":" + lineSeparator);
          if (value == null) {
            field.set(instance, value = current.getDeclaredConstructor().newInstance());
          }
          this.save(writer, current, value, indent + 2);
        } else {
          String value = this.toYamlString(field.get(instance), spacing, field.getName());
          writer.write(spacing + this.toNodeName(field.getName() + ": ") + value + lineSeparator);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the field for a specific config node and instance.
   *
   * <p>As expiry can have multiple blocks there will be multiple instances
   *
   * @param split    the node (split by period)
   * @param instance the instance
   */
  private Field getField(String[] split, Object instance) {
    try {
      Field field = instance.getClass().getField(this.toFieldName(split[split.length - 1]));
      this.setAccessible(field);
      return field;
    } catch (Throwable ignored) {
      LOGGER.debug("Invalid config field: " + this.join(split, ".") + " for " + this.toNodeName(instance.getClass().getSimpleName()));
      return null;
    }
  }

  /**
   * Get the instance for a specific config node.
   *
   * @param split the node (split by period)
   * @return The instance or null
   */
  private Object getInstance(String[] split, Class<?> root) {
    try {
      Class<?> clazz = root == null ? MethodHandles.lookup().lookupClass() : root;
      Object instance = this;
      while (split.length > 0) {
        if (split.length == 1) {
          return instance;
        } else {
          Class<?> found = null;
          if (clazz == null) {
            return null;
          }

          Class<?>[] classes = clazz.getDeclaredClasses();
          for (Class<?> current : classes) {
            if (Objects.equals(current.getSimpleName(), this.toFieldName(split[0]))) {
              found = current;
              break;
            }
          }

          if (found == null) {
            return null;
          }

          try {
            Field instanceField = clazz.getDeclaredField(this.toFieldName(split[0]));
            this.setAccessible(instanceField);
            Object value = instanceField.get(instance);
            if (value == null) {
              value = found.getDeclaredConstructor().newInstance();
              instanceField.set(instance, value);
            }

            clazz = found;
            instance = value;
            split = Arrays.copyOfRange(split, 1, split.length);
            continue;
          } catch (NoSuchFieldException e) {
            //
          }

          split = Arrays.copyOfRange(split, 1, split.length);
          clazz = found;
          instance = clazz.getDeclaredConstructor().newInstance();
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Translate a node to a java field name.
   */
  private String toFieldName(String node) {
    return node.toUpperCase(Locale.ROOT).replaceAll("-", "_");
  }

  /**
   * Translate a field to a config node.
   */
  private String toNodeName(String field) {
    return field.toLowerCase(Locale.ROOT).replace("_", "-");
  }

  /**
   * Set some field to be accessible.
   */
  private void setAccessible(Field field) throws NoSuchFieldException, IllegalAccessException {
    field.setAccessible(true);
    if (Modifier.isFinal(field.getModifiers())) {
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private String repeat(String s, int n) {
    return IntStream.range(0, n).mapToObj(i -> s).collect(Collectors.joining());
  }

  @SuppressWarnings("SameParameterValue")
  private String join(Object[] array, String delimiter) {
    switch (array.length) {
      case 0: {
        return "";
      }
      case 1: {
        return array[0].toString();
      }
      default: {
        final StringBuilder result = new StringBuilder();
        for (int i = 0, j = array.length; i < j; ++i) {
          if (i > 0) {
            result.append(delimiter);
          }
          result.append(array[i]);
        }

        return result.toString();
      }
    }
  }
}
