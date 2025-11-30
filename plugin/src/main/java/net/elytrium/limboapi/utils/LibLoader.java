package net.elytrium.limboapi.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibLoader {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(LibLoader.class);

  private static final String REPOSITORY = "https://repo.maven.apache.org/maven2/";

  private static final long TWO_WEEKS_MILLIS = 2/*weeks*/ * 7/*days*/ * 24/*hours*/ * 60/*minutes*/ * 60/*seconds*/ * 1000/*millis*/;
  private static final int SHA1_HEX_LENGTH = 20 << 1;

  private static final int MAX_ATTEMPTS = 3;

  private static boolean fastutil_loaded;

  public static void resolveAndLoad(Object plugin, ProxyServer server, String[] indexes) { // TODO versioning
    try {
      final Path librariesDirectory = Path.of("libraries");
      for (final String library : indexes) {
        //final String library = BuildConfig.LIBRARIES[index];
        //// Make sure this library won't be loaded again
        //if (library == null) {
        //  continue;
        //}
        //BuildConfig.LIBRARIES[index] = null;

        final Path jarPath = librariesDirectory.resolve(library);
        final Path sha1Path = librariesDirectory.resolve(library + ".sha1");
        boolean notExists = Files.notExists(jarPath);
        if (notExists || Files.notExists(sha1Path)
            || System.currentTimeMillis() - sha1Path.toFile().lastModified() >= LibLoader.TWO_WEEKS_MILLIS
            || LibLoader.notMatches(Files.readAllBytes(sha1Path), jarPath)) {
          final String jarUrl = LibLoader.REPOSITORY + library;
          final String sha1Url = LibLoader.REPOSITORY + library + ".sha1";

          Files.createDirectories(jarPath.getParent());

          LibLoader.LOGGER.info("Fetching {}", sha1Url);
          byte[] expectedHash;
          try (InputStream inputStream = new URI(sha1Url).toURL().openStream()) {
            expectedHash = inputStream.readNBytes(LibLoader.SHA1_HEX_LENGTH);
            Files.deleteIfExists(sha1Path);
            Files.write(sha1Path, expectedHash);
          } catch (Throwable t) {
            if (t instanceof FileNotFoundException) {
              LibLoader.LOGGER.warn("Couldn't fetch file from {}, no repositories left, shutting down the server", LibLoader.REPOSITORY);
              server.shutdown();
              return;
            } else {
              LibLoader.LOGGER.warn("Unable to fetch {}", sha1Url);
              expectedHash = null;
            }
          }

          if (notExists || LibLoader.notMatches(expectedHash, jarPath)) {
            LibLoader.LOGGER.info("Downloading {}", jarUrl);
            int attempt = 0;
            do {
              if (attempt == LibLoader.MAX_ATTEMPTS) {
                LibLoader.LOGGER.error("Download failed after " + LibLoader.MAX_ATTEMPTS + " times, shutting down the server");
                server.shutdown();
                return;
              } else if (attempt != 0) {
                LibLoader.LOGGER.info("Trying again");
              }

              try (InputStream inputStream = new URI(jarUrl).toURL().openStream()) {
                Files.copy(inputStream, jarPath, StandardCopyOption.REPLACE_EXISTING);
              } catch (Throwable t) {
                if (t instanceof FileNotFoundException) {
                  LibLoader.LOGGER.warn("Couldn't find file in {}, no repositories left, shutting down the server", LibLoader.REPOSITORY);
                  server.shutdown();
                  return;
                } else {
                  LibLoader.LOGGER.error("Failed to download", t);
                }
              }

              ++attempt;
            } while (LibLoader.notMatches(expectedHash, jarPath));
          }
        }

        if (library.startsWith("it/unimi/dsi/fastutil")) {
          // We're doing it because velocity already shades some fastutil classes, but it does so in a way that serves its own needs
          if (!LibLoader.fastutil_loaded) {
            ClassLoader classLoader = server.getClass().getClassLoader();
            Reflection.findVirtualVoid(classLoader.getClass(), "appendToClassPathForInstrumentation", String.class).invoke(classLoader, jarPath.toString());
            LibLoader.fastutil_loaded = true;
          }
        } else {
          server.getPluginManager().addToClasspath(plugin, jarPath);
        }

        LibLoader.LOGGER.info("Loaded library {}", jarPath.toAbsolutePath());
      }
    } catch (Throwable t) {
      throw new RuntimeException("An exception has occurred whilst loading libraries", t);
    }
  }

  private static boolean notMatches(byte[] expectedHash, Path jarPath) throws IOException {
    return expectedHash != null && !Arrays.equals(expectedHash, Hex.encodeBytes(Hashing.sha1(Files.readAllBytes(jarPath))));
  }
}
