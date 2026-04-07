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

package net.elytrium.limboapi.mapping;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public final class MappingGenerator {

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final Gson PRETTY_GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
  private static final int JSON_WALK_DEPTH = 32;
  private static final int DOWNLOAD_ATTEMPTS = 4;
  private static final int DOWNLOAD_CONNECT_TIMEOUT_MILLIS = 30_000;
  private static final int DOWNLOAD_READ_TIMEOUT_MILLIS = 300_000;
  private static final long DOWNLOAD_RETRY_DELAY_MILLIS = 2_000L;

  private final Path outputDirectory;
  private final Path dataDirectory;
  private final Path seedsDirectory;
  private final Path versionManifestFile;
  private final String manifestUrl;
  private final long cacheValidMillis;

  public MappingGenerator(Path outputDirectory, Path dataDirectory, Path seedsDirectory, String manifestUrl, long cacheValidMillis) {
    this.outputDirectory = outputDirectory;
    this.dataDirectory = dataDirectory;
    this.seedsDirectory = seedsDirectory;
    this.versionManifestFile = dataDirectory.resolve("manifest.json");
    this.manifestUrl = manifestUrl;
    this.cacheValidMillis = cacheValidMillis;
  }

  public void generateAll() throws IOException, InterruptedException {
    Files.createDirectories(this.outputDirectory);
    Files.createDirectories(this.dataDirectory);

    this.downloadManifest();

    Path targetDir = this.outputDirectory.resolve("mapping");
    Files.createDirectories(targetDir);

    System.out.println("> Generating Minecraft data...");

    List<MinecraftVersion> supportedVersions = this.getSupportedGenerationVersions();
    MinecraftVersion latestGeneratedVersion = supportedVersions.get(supportedVersions.size() - 1);

    Map<MinecraftVersion, Path> generated = supportedVersions.stream()
        .collect(Collectors.toMap(
            version -> version,
            version -> this.uncheckedGenerateData(version),
            (left, right) -> left,
            LinkedHashMap::new
        ));

    Map<MinecraftVersion, JsonObject> blockReports = new LinkedHashMap<>();
    for (Map.Entry<MinecraftVersion, Path> entry : generated.entrySet()) {
      blockReports.put(entry.getKey(), this.readJsonObject(entry.getValue().resolve("reports/blocks.json")));
    }

    this.generateBlockMappings(targetDir, blockReports, latestGeneratedVersion);

    Map<MinecraftVersion, JsonObject> registriesReports = new LinkedHashMap<>();
    for (Map.Entry<MinecraftVersion, Path> entry : generated.entrySet()) {
      if (entry.getKey().isAtLeast(MinecraftVersion.MINECRAFT_1_14)) {
        registriesReports.put(entry.getKey(), this.readJsonObject(entry.getValue().resolve("reports/registries.json")));
      }
    }

    this.generateRegistryMappings(targetDir, registriesReports);

    Map<MinecraftVersion, Path> tagDirectories = new LinkedHashMap<>();
    for (Map.Entry<MinecraftVersion, Path> entry : generated.entrySet()) {
      tagDirectories.put(entry.getKey(), entry.getValue().resolve("data/minecraft/tags"));
    }

    this.generateTags(targetDir, tagDirectories);
  }

  private List<MinecraftVersion> getSupportedGenerationVersions() {
    int currentJavaFeature = Runtime.version().feature();
    List<MinecraftVersion> supported = Arrays.stream(MinecraftVersion.values())
        .filter(version -> version.isAtLeast(MinecraftVersion.MINECRAFT_1_13))
        .filter(version -> this.isSupportedOnCurrentJava(version, currentJavaFeature))
        .toList();

    if (supported.isEmpty()) {
      throw new IllegalStateException("No Minecraft versions are runnable on Java " + currentJavaFeature);
    }

    MinecraftVersion newestKnownVersion = MinecraftVersion.MAXIMUM_VERSION;
    MinecraftVersion newestRunnableVersion = supported.get(supported.size() - 1);
    if (newestRunnableVersion != newestKnownVersion) {
      System.out.println("> Skipping versions newer than " + newestRunnableVersion.getVersionName()
          + " because the current Java runtime is " + currentJavaFeature + ".");
    }

    return supported;
  }

  private boolean isSupportedOnCurrentJava(MinecraftVersion version, int currentJavaFeature) {
    if (version == MinecraftVersion.MINECRAFT_26_1) {
      return currentJavaFeature >= 25;
    }

    return true;
  }

  private Path uncheckedGenerateData(MinecraftVersion version) {
    try {
      return this.generateData(version);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while generating data for " + version.getVersionName(), e);
    }
  }

  private void downloadManifest() throws IOException {
    System.out.println("> Downloading version manifest...");
    Files.createDirectories(this.versionManifestFile.getParent());
    if (this.checkIsCacheValid(this.versionManifestFile)) {
      this.downloadToFileWithRetries(new URL(this.manifestUrl), this.versionManifestFile, "version manifest");
    }
  }

  private boolean checkIsCacheValid(Path file) {
    if (Files.exists(file)) {
      try {
        long age = System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis();
        if (age < this.cacheValidMillis) {
          System.out.println("> Found cached " + file.getFileName());
          return false;
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to check cache for " + file, e);
      }
    }

    return true;
  }

  private Path downloadVersionManifest(String version) throws IOException {
    System.out.println("> Downloading " + version + " manifest...");

    JsonObject manifest = this.readJsonObject(this.versionManifestFile);
    JsonObject resolved = null;
    for (JsonElement element : manifest.getAsJsonArray("versions")) {
      JsonObject candidate = element.getAsJsonObject();
      if (version.equals(candidate.get("id").getAsString())) {
        resolved = candidate;
        break;
      }
    }

    if (resolved == null) {
      throw new IllegalStateException("Couldn't find version: " + version);
    }

    Path output = this.dataDirectory.resolve(version).resolve("manifest.json");
    Files.createDirectories(output.getParent());
    this.downloadToFileWithRetries(new URL(resolved.get("url").getAsString()), output, version + " manifest");
    return output;
  }

  private Path getGeneratedCache(MinecraftVersion version) {
    Path generated = this.dataDirectory.resolve(version.getVersionName()).resolve("generated");
    Path registryOrItems = version.isAtLeast(MinecraftVersion.MINECRAFT_1_14)
        ? generated.resolve("reports/registries.json")
        : generated.resolve("reports/items.json");

    if (Files.exists(generated.resolve("reports/blocks.json"))
        && Files.exists(registryOrItems)
        && Files.exists(generated.resolve("data/minecraft/tags"))) {
      return generated;
    }

    return null;
  }

  private static boolean validateServer(Path file, String expected) throws IOException {
    if (file == null || !Files.exists(file)) {
      return false;
    }

    String hash = com.google.common.io.Files.asByteSource(file.toFile()).hash(Hashing.sha1()).toString();
    return hash.equals(expected);
  }

  private Path getServerJar(String version) throws IOException {
    Path manifestFile = this.downloadVersionManifest(version);
    JsonObject manifest = this.readJsonObject(manifestFile);

    JsonObject server = manifest.getAsJsonObject("downloads").getAsJsonObject("server");
    Path jarFile = this.dataDirectory.resolve(version).resolve("server.jar");
    if (!validateServer(jarFile, server.get("sha1").getAsString())) {
      System.out.println("> Downloading " + version + " server...");
      Files.createDirectories(jarFile.getParent());

      IOException lastException = null;
      for (int attempt = 1; attempt <= DOWNLOAD_ATTEMPTS; ++attempt) {
        try {
          this.downloadToFileWithRetries(URI.create(server.get("url").getAsString()).toURL(), jarFile, version + " server");
          if (validateServer(jarFile, server.get("sha1").getAsString())) {
            return jarFile;
          }

          Files.deleteIfExists(jarFile);
          lastException = new IOException("Downloaded server jar checksum mismatch for " + version);
        } catch (IOException e) {
          Files.deleteIfExists(jarFile);
          lastException = e;
        }

        if (attempt < DOWNLOAD_ATTEMPTS) {
          System.out.println("> Retrying " + version + " server download checksum validation (attempt "
              + (attempt + 1) + "/" + DOWNLOAD_ATTEMPTS + ")...");
          this.sleepBeforeRetry();
        }
      }

      throw Objects.requireNonNullElseGet(lastException,
          () -> new IOException("Failed to download server jar for " + version));
    }

    return jarFile;
  }

  private void downloadToFileWithRetries(URL url, Path target, String label) throws IOException {
    Files.createDirectories(Objects.requireNonNull(target.getParent()));
    Path tempFile = target.resolveSibling(target.getFileName() + ".part");

    IOException lastException = null;
    for (int attempt = 1; attempt <= DOWNLOAD_ATTEMPTS; ++attempt) {
      try {
        Files.deleteIfExists(tempFile);
        FileUtils.copyURLToFile(url, tempFile.toFile(), DOWNLOAD_CONNECT_TIMEOUT_MILLIS, DOWNLOAD_READ_TIMEOUT_MILLIS);
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return;
      } catch (IOException e) {
        lastException = e;
        Files.deleteIfExists(tempFile);

        if (attempt < DOWNLOAD_ATTEMPTS) {
          System.out.println("> Download failed for " + label + ", retrying (attempt " + (attempt + 1)
              + "/" + DOWNLOAD_ATTEMPTS + "): " + e.getMessage());
          this.sleepBeforeRetry();
        }
      }
    }

    throw new IOException("Failed to download " + label + " after " + DOWNLOAD_ATTEMPTS + " attempts", lastException);
  }

  private void sleepBeforeRetry() throws IOException {
    try {
      Thread.sleep(DOWNLOAD_RETRY_DELAY_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting to retry download", e);
    }
  }

  private Path generateData(MinecraftVersion version) throws IOException, InterruptedException {
    Path cache = this.getGeneratedCache(version);
    if (cache != null) {
      return cache;
    }

    Path jarFile = this.getServerJar(version.getVersionName());
    Path parent = jarFile.getParent();
    Path targetDir = parent.resolve("generated");

    FileUtils.deleteDirectory(targetDir.toFile());

    List<String> commandLine = new ArrayList<>();
    commandLine.add(this.resolveJavaExecutable().toString());
    if (version.isAtLeast(MinecraftVersion.MINECRAFT_1_18)) {
      commandLine.add("-DbundlerMainClass=net.minecraft.data.Main");
      commandLine.add("-jar");
      commandLine.add(jarFile.toString());
    } else {
      commandLine.add("-cp");
      commandLine.add(jarFile.toString());
      commandLine.add("net.minecraft.data.Main");
    }
    commandLine.add("--reports");
    commandLine.add("--server");

    Process process = new ProcessBuilder(commandLine)
        .directory(parent.toFile())
        .inheritIO()
        .start();

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IllegalStateException("Minecraft data generation failed for " + version.getVersionName() + " with exit code " + exitCode);
    }

    Files.deleteIfExists(jarFile);
    FileUtils.deleteDirectory(parent.resolve("logs").toFile());
    FileUtils.deleteDirectory(parent.resolve("libraries").toFile());
    FileUtils.deleteDirectory(parent.resolve("versions").toFile());

    Files.deleteIfExists(targetDir.resolve("reports/commands.json"));

    FileUtils.deleteDirectory(targetDir.resolve(".cache").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("reports/biome_parameters").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("reports/biomes").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("reports/worldgen").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("reports/minecraft/components/item").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/datapacks").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/advancements").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/advancement").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/recipes").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/recipe").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/loot_tables").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/loot_table").toFile());
    FileUtils.deleteDirectory(targetDir.resolve("data/minecraft/worldgen").toFile());

    this.minifyJsonFiles(parent);

    return targetDir;
  }

  private void generateBlockMappings(Path targetDir, Map<MinecraftVersion, JsonObject> blockReports,
      MinecraftVersion latestGeneratedVersion) throws IOException {
    Path defaultBlockPropertiesFile = targetDir.resolve("defaultblockproperties.json");
    Path blockStatesFile = targetDir.resolve("blockstates.json");
    Path blockStatesMappingFile = targetDir.resolve("blockstates_mapping.json");
    Path legacyBlocksFile = targetDir.resolve("legacyblocks.json");

    if (this.checkIsCacheValid(defaultBlockPropertiesFile)
        || this.checkIsCacheValid(blockStatesFile)
        || this.checkIsCacheValid(blockStatesMappingFile)
        || this.checkIsCacheValid(legacyBlocksFile)) {
      System.out.println("> Generating default block properties...");

      Map<MinecraftVersion, Map<String, Map<String, String>>> defaultProperties = new LinkedHashMap<>();
      for (Map.Entry<MinecraftVersion, JsonObject> entry : blockReports.entrySet()) {
        defaultProperties.put(entry.getKey(), getDefaultProperties(entry.getValue()));
      }

      this.writePrettyJson(defaultBlockPropertiesFile, toJsonObjectOfStringMaps(defaultProperties.get(latestGeneratedVersion)));

      System.out.println("> Generating blockstates...");

      Map<MinecraftVersion, Map<String, Integer>> mappings = loadLegacyMapping(this.seedsDirectory.resolve("mapping/legacyblockmapping.json"));
      for (Map.Entry<MinecraftVersion, JsonObject> entry : blockReports.entrySet()) {
        mappings.put(entry.getKey(), getBlockMappings(entry.getValue(), defaultProperties.get(entry.getKey())));
      }

      Map<String, Integer> blocks = mappings.get(latestGeneratedVersion);
      this.writePrettyJson(blockStatesFile, toJsonObjectFromIntegerMap(sortByIntegerValue(blocks)));

      System.out.println("> Generating blockstates mapping...");

      Map<MinecraftVersion, Map<String, String>> fallbackMapping = loadFallbackMapping(this.seedsDirectory.resolve("mapping/fallbackdata.json"));
      LinkedHashMap<String, JsonObject> blockStateMappingJson = new LinkedHashMap<>();
      for (Map.Entry<String, Integer> blockEntry : sortByIntegerValue(blocks).entrySet()) {
        LinkedHashMap<String, String> blockMapping = new LinkedHashMap<>();
        int lastId = -1;
        for (MinecraftVersion version : MinecraftVersion.values()) {
          int id = getBlockId(blockEntry.getKey(), mappings, defaultProperties, fallbackMapping, version, latestGeneratedVersion);
          if (lastId != id) {
            blockMapping.put(version.getVersionName(), String.valueOf(id));
            lastId = id;
          }
        }

        blockStateMappingJson.put(String.valueOf(blockEntry.getValue()), toJsonObject(blockMapping));
      }
      this.writePrettyJson(blockStatesMappingFile, toJsonObjectOfJsonObjects(blockStateMappingJson));

      System.out.println("> Generating legacy blocks...");

      JsonObject legacyData = this.readJsonObject(this.seedsDirectory.resolve("mapping/legacyblocks.json"));
      LinkedHashMap<String, String> resolvedLegacyData = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> entry : legacyData.entrySet()) {
        int modernId = getBlockId(entry.getValue().getAsString(), mappings, defaultProperties, fallbackMapping,
            latestGeneratedVersion, latestGeneratedVersion);
        resolvedLegacyData.put(entry.getKey(), String.valueOf(modernId));
      }
      this.writePrettyJson(legacyBlocksFile, toJsonObject(resolvedLegacyData));
    }
  }

  private void generateRegistryMappings(Path targetDir, Map<MinecraftVersion, JsonObject> registriesReports) throws IOException {
    this.generateRegistryMapping("item", targetDir, registriesReports.entrySet().stream()
        .filter(entry -> MinecraftVersion.WORLD_VERSIONS.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new)));

    this.generateRegistryMapping("block", targetDir, registriesReports);

    this.generateRegistryMapping("data_component_type", targetDir, registriesReports.entrySet().stream()
        .filter(entry -> entry.getKey().isAtLeast(MinecraftVersion.MINECRAFT_1_20_5))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new)));

    Path blockEntitiesMappingFile = targetDir.resolve("blockentities_mapping.json");
    if (this.checkIsCacheValid(blockEntitiesMappingFile)) {
      System.out.println("> Generating blockentities mapping...");

      LinkedHashMap<String, LinkedHashMap<String, String>> blockEntities = new LinkedHashMap<>();
      JsonObject legacyMappings = this.readJsonObject(this.seedsDirectory.resolve("mapping/legacy_blockentities_mapping.json"));
      for (Map.Entry<String, JsonElement> entry : legacyMappings.entrySet()) {
        blockEntities.put(entry.getKey(), toLinkedStringMap(entry.getValue().getAsJsonObject()));
      }

      for (Map.Entry<MinecraftVersion, JsonObject> entry : registriesReports.entrySet()) {
        if (!entry.getKey().isAtLeast(MinecraftVersion.MINECRAFT_1_19)) {
          continue;
        }

        JsonObject values = entry.getValue().getAsJsonObject("minecraft:block_entity_type").getAsJsonObject("entries");
        for (Map.Entry<String, JsonElement> valueEntry : values.entrySet()) {
          int id = valueEntry.getValue().getAsJsonObject().get("protocol_id").getAsInt();
          blockEntities.computeIfAbsent(valueEntry.getKey(), ignored -> new LinkedHashMap<>())
              .put(entry.getKey().getVersionName(), String.valueOf(id));
        }
      }

      this.writePrettyJson(blockEntitiesMappingFile, toJsonObjectOfStringMaps(sortRegistryMapping(blockEntities)));
    }
  }

  private void generateRegistryMapping(String target, Path targetDir, Map<MinecraftVersion, JsonObject> registriesReports) throws IOException {
    Path targetFile = targetDir.resolve(target + "s.json");
    Path targetMappingFile = targetDir.resolve(target + "s_mapping.json");
    if (this.checkIsCacheValid(targetFile) || this.checkIsCacheValid(targetMappingFile)) {
      System.out.println("> Generating " + target + "s...");

      Map<MinecraftVersion, Map<String, String>> idMap = new LinkedHashMap<>();
      for (Map.Entry<MinecraftVersion, JsonObject> entry : registriesReports.entrySet()) {
        JsonObject entries = entry.getValue().getAsJsonObject("minecraft:" + target).getAsJsonObject("entries");
        LinkedHashMap<String, String> ids = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> valueEntry : entries.entrySet()) {
          ids.put(valueEntry.getKey(), String.valueOf(valueEntry.getValue().getAsJsonObject().get("protocol_id").getAsInt()));
        }
        idMap.put(entry.getKey(), ids);
      }

      Map<String, String> modernIds = idMap.entrySet().stream()
          .max(Map.Entry.comparingByKey())
          .orElseThrow(() -> new IllegalStateException("No registry reports found for " + target))
          .getValue();

      this.writePrettyJson(targetFile, toJsonObjectFromIntegerMap(sortByIntegerValue(parseIntegerValues(modernIds))));

      System.out.println("> Generating " + target + "s mapping...");

      LinkedHashMap<String, LinkedHashMap<String, String>> mapping = new LinkedHashMap<>();
      JsonObject legacyMapping = this.readJsonObject(this.seedsDirectory.resolve("mapping/legacy_" + target + "s_mapping.json"));
      for (Map.Entry<String, JsonElement> entry : legacyMapping.entrySet()) {
        String modernId = modernIds.get(entry.getKey());
        if (modernId == null) {
          throw new IllegalStateException("No modern id found for " + entry.getKey());
        }

        mapping.put(modernId, toLinkedStringMap(entry.getValue().getAsJsonObject()));
      }

      for (Map.Entry<MinecraftVersion, Map<String, String>> versionEntry : idMap.entrySet()) {
        for (Map.Entry<String, String> idEntry : versionEntry.getValue().entrySet()) {
          if (!modernIds.containsKey(idEntry.getKey())) {
            continue;
          }

          mapping.computeIfAbsent(modernIds.get(idEntry.getKey()), ignored -> new LinkedHashMap<>())
              .put(versionEntry.getKey().getVersionName(), idEntry.getValue());
        }
      }

      LinkedHashMap<String, LinkedHashMap<String, String>> sortedMapping = sortRegistryMapping(mapping);
      this.writePrettyJson(targetMappingFile, toJsonObjectOfStringMaps(sortNestedMapByNumericKey(sortedMapping)));
    }
  }

  private void generateTags(Path targetDir, Map<MinecraftVersion, Path> tagDirectories) throws IOException {
    Path tagsFile = targetDir.resolve("tags.json");
    if (this.checkIsCacheValid(tagsFile)) {
      System.out.println("> Generating tags...");

      JsonObject tagTypesJson = this.readJsonObject(this.seedsDirectory.resolve("mapping/tag_types.json"));
      Map<String, String> tagTypes = toLinkedStringMap(tagTypesJson.getAsJsonObject("tag_types"));
      Set<String> supportedTagTypes = new HashSet<>();
      for (JsonElement element : tagTypesJson.getAsJsonArray("supported_tag_types")) {
        supportedTagTypes.add(element.getAsString());
      }

      Map<MinecraftVersion, Map<String, Map<String, List<String>>>> allTags = new LinkedHashMap<>();
      for (Map.Entry<MinecraftVersion, Path> entry : tagDirectories.entrySet()) {
        allTags.put(entry.getKey(), getTags(entry.getValue(), tagTypes));
      }

      Map<String, Map<String, Set<String>>> mergedTags = new LinkedHashMap<>();
      for (Map<String, Map<String, List<String>>> tags : allTags.values()) {
        for (Map.Entry<String, Map<String, List<String>>> typeEntry : tags.entrySet()) {
          if (!supportedTagTypes.contains(typeEntry.getKey())) {
            continue;
          }

          Map<String, Set<String>> mergedTypeTags = mergedTags.computeIfAbsent(typeEntry.getKey(), ignored -> new HashMap<>());
          for (Map.Entry<String, List<String>> tagEntry : typeEntry.getValue().entrySet()) {
            mergedTypeTags.computeIfAbsent(tagEntry.getKey(), ignored -> new HashSet<>()).addAll(tagEntry.getValue());
          }
        }
      }

      LinkedHashMap<String, JsonObject> mergedTagsJson = new LinkedHashMap<>();
      mergedTags.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(typeEntry -> {
            JsonObject typeJson = new JsonObject();
            typeEntry.getValue().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(tagEntry -> {
                  List<String> values = new ArrayList<>(tagEntry.getValue());
                  Collections.sort(values);
                  typeJson.add(tagEntry.getKey(), toJsonArray(values));
                });
            mergedTagsJson.put(typeEntry.getKey(), typeJson);
          });

      this.writePrettyJson(tagsFile, toJsonObjectOfJsonObjects(mergedTagsJson));
    }
  }

  private static Map<String, Map<String, String>> getDefaultProperties(JsonObject data) {
    LinkedHashMap<String, Map<String, String>> defaultProperties = new LinkedHashMap<>();

    for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
      JsonObject block = entry.getValue().getAsJsonObject();
      if (!block.has("properties")) {
        continue;
      }

      for (JsonElement stateElement : block.getAsJsonArray("states")) {
        JsonObject blockState = stateElement.getAsJsonObject();
        if (!blockState.has("default") || !blockState.get("default").getAsBoolean()) {
          continue;
        }

        defaultProperties.put(entry.getKey(), new TreeMap<>(toLinkedStringMap(blockState.getAsJsonObject("properties"))));
        break;
      }
    }

    return defaultProperties;
  }

  private static Map<MinecraftVersion, Map<String, String>> loadFallbackMapping(Path file) throws IOException {
    JsonObject mappings = readJsonObjectStatic(file);
    LinkedHashMap<MinecraftVersion, Map<String, String>> fallback = new LinkedHashMap<>();
    for (MinecraftVersion version : MinecraftVersion.values()) {
      JsonObject value = mappings.has(version.name()) ? mappings.getAsJsonObject(version.name()) : new JsonObject();
      fallback.put(version, toLinkedStringMap(value));
    }

    return fallback;
  }

  private static Map<MinecraftVersion, Map<String, Integer>> loadLegacyMapping(Path file) throws IOException {
    JsonObject mappings = readJsonObjectStatic(file);
    LinkedHashMap<MinecraftVersion, Map<String, Integer>> result = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
      LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> valueEntry : entry.getValue().getAsJsonObject().entrySet()) {
        values.put(valueEntry.getKey(), valueEntry.getValue().getAsInt());
      }
      result.put(MinecraftVersion.valueOf(entry.getKey()), values);
    }

    return result;
  }

  private static int getBlockId(
      String block,
      Map<MinecraftVersion, Map<String, Integer>> mappings,
      Map<MinecraftVersion, Map<String, Map<String, String>>> properties,
      Map<MinecraftVersion, Map<String, String>> fallback,
      MinecraftVersion version,
      MinecraftVersion latestGeneratedVersion
  ) {
    MinecraftVersion lookupVersion = mappings.containsKey(version) ? version : latestGeneratedVersion;

    Map<String, Map<String, String>> defaultProperties = lookupVersion.isAtLeast(MinecraftVersion.MINECRAFT_1_13)
        ? properties.get(lookupVersion)
        : properties.get(MinecraftVersion.MINECRAFT_1_18_2);

    String[] split = block.split("\\[", 2);
    String noArgBlock = split[0];

    MinecraftVersion fallbackVersion = latestGeneratedVersion;
    while (fallbackVersion != lookupVersion) {
      fallbackVersion = fallbackVersion.previous();
      noArgBlock = fallback.getOrDefault(fallbackVersion, Map.of()).getOrDefault(noArgBlock, noArgBlock);
    }

    Map<String, String> blockProperties = defaultProperties.get(noArgBlock);
    String targetBlockId;
    if (blockProperties == null) {
      targetBlockId = noArgBlock;
    } else {
      TreeMap<String, String> currentProperties = new TreeMap<>(blockProperties);
      if (split.length > 1) {
        String propertyText = split[1].replace("]", "");
        if (!propertyText.isBlank()) {
          for (String argument : propertyText.split(",")) {
            String[] parts = argument.split("=", 2);
            if (parts.length == 2 && currentProperties.containsKey(parts[0])) {
              currentProperties.put(parts[0], parts[1]);
            }
          }
        }
      }

      targetBlockId = formatStateId(noArgBlock, currentProperties);
    }

    Integer id = mappings.get(lookupVersion).get(targetBlockId);
    if (id == null && blockProperties != null) {
      id = mappings.get(lookupVersion).get(formatStateId(noArgBlock, new TreeMap<>(blockProperties)));
    }

    if (id == null) {
      System.err.println("No " + version.getVersionName() + " fallback data for " + noArgBlock + ", replacing with minecraft:stone");
      return 1;
    }

    return id;
  }

  private static Map<String, Integer> getBlockMappings(JsonObject data, Map<String, Map<String, String>> defaultPropertiesMap) {
    LinkedHashMap<String, Integer> mapping = new LinkedHashMap<>();

    for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
      String blockId = entry.getKey();
      JsonObject blockData = entry.getValue().getAsJsonObject();
      for (JsonElement stateElement : blockData.getAsJsonArray("states")) {
        JsonObject blockState = stateElement.getAsJsonObject();
        int protocolId = blockState.get("id").getAsInt();

        if (blockState.has("properties")) {
          TreeMap<String, String> properties = new TreeMap<>(defaultPropertiesMap.getOrDefault(blockId, Map.of()));
          properties.putAll(toLinkedStringMap(blockState.getAsJsonObject("properties")));
          mapping.put(formatStateId(blockId, properties), protocolId);
        } else {
          mapping.put(blockId, protocolId);
        }
      }
    }

    return mapping;
  }

  private static LinkedHashMap<String, LinkedHashMap<String, String>> sortRegistryMapping(Map<String, LinkedHashMap<String, String>> mapping) {
    Comparator<Map.Entry<String, String>> comparator = Comparator.comparing(entry -> {
      if (entry.getKey().contains(".")) {
        return MinecraftVersion.fromVersionName(entry.getKey());
      }

      return MinecraftVersion.MINIMUM_VERSION;
    });

    LinkedHashMap<String, LinkedHashMap<String, String>> sorted = new LinkedHashMap<>();
    mapping.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> {
          LinkedHashMap<String, String> sortedValues = entry.getValue().entrySet().stream()
              .sorted(comparator)
              .collect(Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (left, right) -> left,
                  LinkedHashMap::new
              ));
          sorted.put(entry.getKey(), sortedValues);
        });

    return sorted;
  }

  private static Map<String, Map<String, List<String>>> getTags(Path tagDir, Map<String, String> tagTypes) throws IOException {
    LinkedHashMap<String, Map<String, List<String>>> tags = new LinkedHashMap<>();

    for (Map.Entry<String, String> tagTypeEntry : tagTypes.entrySet()) {
      Path directory = tagDir.resolve(tagTypeEntry.getKey());
      if (!Files.exists(directory)) {
        continue;
      }

      Map<String, List<String>> typeTags = new HashMap<>();
      try (Stream<Path> pathStream = Files.walk(directory)) {
        for (Path file : pathStream.filter(Files::isRegularFile).toList()) {
          JsonObject json = readJsonObjectStatic(file);
          List<String> values = new ArrayList<>();
          for (JsonElement element : json.getAsJsonArray("values")) {
            values.add(element.getAsString());
          }

          Path relativePath = directory.relativize(file);
          String name = FilenameUtils.removeExtension(relativePath.toString()).replace('\\', '/');
          typeTags.put("minecraft:" + name, values);
        }
      }

      boolean flatten = false;
      while (!flatten) {
        flatten = true;
        Map<String, List<String>> tempTags = new HashMap<>();
        for (Map.Entry<String, List<String>> tagEntry : typeTags.entrySet()) {
          List<String> newTags = new ArrayList<>();
          for (String currentTag : tagEntry.getValue()) {
            if (currentTag.startsWith("#")) {
              newTags.addAll(typeTags.getOrDefault(currentTag.substring(1), List.of()));
              flatten = false;
            } else {
              newTags.add(currentTag);
            }
          }
          tempTags.put(tagEntry.getKey(), newTags);
        }
        typeTags = tempTags;
      }

      tags.put(tagTypeEntry.getValue(), typeTags);
    }

    return tags;
  }

  private void minifyJsonFiles(Path root) throws IOException {
    List<Path> jsonFiles;
    try (Stream<Path> pathStream = Files.walk(root, JSON_WALK_DEPTH)) {
      jsonFiles = pathStream
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .toList();
    }

    for (Path jsonFile : jsonFiles) {
      JsonElement json = this.readJson(jsonFile);
      Files.writeString(
          jsonFile,
          GSON.toJson(json),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE
      );
    }
  }

  private Path resolveJavaExecutable() {
    String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
    return Path.of(System.getProperty("java.home"), "bin", executable);
  }

  private JsonElement readJson(Path file) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader);
    }
  }

  private JsonObject readJsonObject(Path file) throws IOException {
    return readJsonObjectStatic(file);
  }

  private void writePrettyJson(Path file, JsonElement json) throws IOException {
    Files.createDirectories(Objects.requireNonNull(file.getParent()));
    Files.writeString(
        file,
        PRETTY_GSON.toJson(json),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
    );
  }

  private static JsonObject readJsonObjectStatic(Path file) throws IOException {
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    }
  }

  private static LinkedHashMap<String, Integer> sortByIntegerValue(Map<String, Integer> input) {
    return input.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (left, right) -> left,
            LinkedHashMap::new
        ));
  }

  private static LinkedHashMap<String, LinkedHashMap<String, String>> sortNestedMapByNumericKey(Map<String, LinkedHashMap<String, String>> input) {
    return input.entrySet().stream()
        .sorted(Comparator.comparingInt(entry -> Integer.parseInt(entry.getKey())))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (left, right) -> left,
            LinkedHashMap::new
        ));
  }

  private static LinkedHashMap<String, Integer> parseIntegerValues(Map<String, String> input) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : input.entrySet()) {
      result.put(entry.getKey(), Integer.parseInt(entry.getValue()));
    }
    return result;
  }

  private static String formatStateId(String blockId, Map<String, String> properties) {
    if (properties.isEmpty()) {
      return blockId;
    }

    return blockId + "[" + properties.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(",")) + "]";
  }

  private static LinkedHashMap<String, String> toLinkedStringMap(JsonObject object) {
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getAsString());
    }
    return result;
  }

  private static JsonObject toJsonObject(Map<String, String> values) {
    JsonObject result = new JsonObject();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      result.addProperty(entry.getKey(), entry.getValue());
    }
    return result;
  }

  private static JsonObject toJsonObjectFromIntegerMap(Map<String, Integer> values) {
    JsonObject result = new JsonObject();
    for (Map.Entry<String, Integer> entry : values.entrySet()) {
      result.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
    }
    return result;
  }

  private static JsonObject toJsonObjectOfStringMaps(Map<String, ? extends Map<String, String>> values) {
    JsonObject result = new JsonObject();
    for (Map.Entry<String, ? extends Map<String, String>> entry : values.entrySet()) {
      result.add(entry.getKey(), toJsonObject(entry.getValue()));
    }
    return result;
  }

  private static JsonObject toJsonObjectOfJsonObjects(Map<String, JsonObject> values) {
    JsonObject result = new JsonObject();
    for (Map.Entry<String, JsonObject> entry : values.entrySet()) {
      result.add(entry.getKey(), entry.getValue());
    }
    return result;
  }

  private static JsonArray toJsonArray(Collection<String> values) {
    JsonArray result = new JsonArray();
    for (String value : values) {
      result.add(value);
    }
    return result;
  }
}


