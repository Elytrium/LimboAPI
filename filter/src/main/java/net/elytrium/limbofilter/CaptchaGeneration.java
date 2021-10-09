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

package net.elytrium.limbofilter;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.elytrium.limboapi.protocol.map.CraftMapCanvas;
import net.elytrium.limboapi.protocol.map.MapPalette;
import net.elytrium.limboapi.protocol.packet.MapDataPacket;
import net.elytrium.limbofilter.config.Settings;
import net.elytrium.limbofilter.generator.CaptchaPainter;
import org.slf4j.Logger;

/**
 * @author Leymooo
 * @author hevav
 */
public final class CaptchaGeneration {

  private static final CraftMapCanvas cachedBackgroundMap = new CraftMapCanvas();
  private static final FilterPlugin plugin = FilterPlugin.getInstance();
  private static final Logger logger = FilterPlugin.getInstance().getLogger();
  private static final CaptchaPainter painter = new CaptchaPainter();
  private static final List<Font> fonts = new ArrayList<>();
  private static final AtomicInteger fontCounter = new AtomicInteger(0);
  private static final AtomicInteger colorCounter = new AtomicInteger(0);

  private CaptchaGeneration() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static void init() {
    try {
      if (!Settings.IMP.MAIN.CAPTCHA_GENERATOR.BACKPLATE_PATH.equals("")) {
        cachedBackgroundMap.drawImage(0, 0, ImageIO.read(new File(Settings.IMP.MAIN.CAPTCHA_GENERATOR.BACKPLATE_PATH)));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    fonts.clear();

    int fontSize = Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONT_SIZE;
    Map<TextAttribute, Object> textSettings = Map.of(
        TextAttribute.TRACKING,
        Settings.IMP.MAIN.CAPTCHA_GENERATOR.LETTER_SPACING,
        TextAttribute.SIZE,
        (float) fontSize,
        TextAttribute.STRIKETHROUGH,
        Settings.IMP.MAIN.CAPTCHA_GENERATOR.STRIKETHROUGH,
        TextAttribute.UNDERLINE,
        Settings.IMP.MAIN.CAPTCHA_GENERATOR.UNDERLINE
    );

    if (Settings.IMP.MAIN.CAPTCHA_GENERATOR.USE_STANDARD_FONTS) {
      fonts.add(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize).deriveFont(textSettings));
      fonts.add(new Font(Font.SERIF, Font.PLAIN, fontSize).deriveFont(textSettings));
      fonts.add(new Font(Font.MONOSPACED, Font.PLAIN, fontSize).deriveFont(textSettings));
    }

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

    if (Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONTS_PATH != null) {
      Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONTS_PATH.forEach(fontFile -> {
        try {
          if (!fontFile.equals("")) {
            logger.info("Loading font " + fontFile);
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontFile));
            ge.registerFont(font);
            fonts.add(font.deriveFont(textSettings));
          }
        } catch (FontFormatException | IOException e) {
          e.printStackTrace();
        }
      });
    }

    new Thread(CaptchaGeneration::generateImages).start();
  }

  @SuppressWarnings("StatementWithEmptyBody")
  public static void generateImages() {
    ThreadPoolExecutor ex = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    for (int i = 100; i <= 999; i++) {
      ex.execute(CaptchaGeneration::genNewPacket);
    }

    long start = System.currentTimeMillis();
    while (ex.getActiveCount() != 0) {
      // Busy wait
    }

    logger.info("Captcha generated in " + (System.currentTimeMillis() - start) + " ms.");
    ex.shutdownNow();
    System.gc();
  }

  public static void genNewPacket() {
    String answer = randomAnswer();
    final CraftMapCanvas map = new CraftMapCanvas(cachedBackgroundMap.getCanvas());
    int fontNumber = fontCounter.getAndIncrement();
    if (fontNumber >= fonts.size()) {
      fontNumber = 0;
      fontCounter.set(0);
    }
    BufferedImage image = painter.draw(fonts.get(fontNumber), randomNotWhiteColor(), answer);
    map.drawImage(0, 0, image);
    MapDataPacket packet = new MapDataPacket(0, (byte) 0, map.getMapData());
    plugin.getCachedCaptcha().createCaptchaPacket(packet, answer);
  }

  private static Color randomNotWhiteColor() {
    MapPalette.Color[] colors = MapPalette.getColors();

    int index;
    do {
      index = colorCounter.getAndIncrement();
      if (index >= colors.length) {
        index = 0;
        colorCounter.set(0);
      }
    } while (colors[index].getRed() >= 200 && colors[index].getGreen() >= 200 && colors[index].getBlue() >= 200);

    return colors[index].toJava();
  }

  private static String randomAnswer() {
    int length = Settings.IMP.MAIN.CAPTCHA_GENERATOR.LENGTH;
    String pattern = Settings.IMP.MAIN.CAPTCHA_GENERATOR.PATTERN;

    char[] text = new char[length];
    for (int i = 0; i < length; i++) {
      text[i] = pattern.charAt(ThreadLocalRandom.current().nextInt(pattern.length()));
    }

    return new String(text);
  }
}
