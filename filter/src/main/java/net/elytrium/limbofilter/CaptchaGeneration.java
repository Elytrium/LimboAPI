/*
 * This file is part of Velocity-BotFilter, licensed under the AGPLv3 License (AGPLv3).
 *
 * Copyright (C) 2021 Vjatšeslav Maspanov <Leymooo>
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.elytrium.elytraproxy.botfilter.cache.CachedCaptcha;
import net.elytrium.elytraproxy.botfilter.generator.CaptchaPainter;
import net.elytrium.elytraproxy.botfilter.generator.map.CraftMapCanvas;
import net.elytrium.elytraproxy.botfilter.generator.map.MapPalette;
import net.elytrium.elytraproxy.config.Settings;
import net.elytrium.elytraproxy.virtual.protocol.packet.MapDataPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Leymooo
 * @author hevav
 */
@UtilityClass
public class CaptchaGeneration {

  private static final CraftMapCanvas cachedBackgroundMap = new CraftMapCanvas();
  private final Logger logger = LogManager.getLogger("ElytraProxy");
  private final CaptchaPainter painter = new CaptchaPainter();
  private final List<Font> fonts = new ArrayList<>();
  private final AtomicInteger fontCounter = new AtomicInteger(0);
  private final AtomicInteger colorCounter = new AtomicInteger(0);

  public void init() {
    try {
      if (!Settings.IMP.MAIN.CAPTCHA_GENERATOR.BACKPLATE_PATH.equals("")) {
        cachedBackgroundMap.drawImage(0, 0,
            ImageIO.read(new File(Settings.IMP.MAIN.CAPTCHA_GENERATOR.BACKPLATE_PATH)));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    int fontSize = Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONT_SIZE;

    fonts.clear();
    if (Settings.IMP.MAIN.CAPTCHA_GENERATOR.USE_STANDARD_FONTS) {
      fonts.add(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
      fonts.add(new Font(Font.SERIF, Font.PLAIN, fontSize));
      fonts.add(new Font(Font.MONOSPACED, Font.BOLD, fontSize));
    }

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

    if (Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONTS_PATH != null) {
      Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONTS_PATH.forEach(fontFile -> {
        try {
          if (!fontFile.equals("")) {
            logger.info("Loading font " + fontFile);
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontFile));
            ge.registerFont(font);
            fonts.add(font.deriveFont(Font.PLAIN).deriveFont((float) fontSize));
          }
        } catch (FontFormatException | IOException e) {
          e.printStackTrace();
        }
      });
    }

    new Thread(CaptchaGeneration::generateImages).start();
  }

  @SneakyThrows
  @SuppressWarnings("StatementWithEmptyBody")
  public void generateImages() {
    ThreadPoolExecutor ex =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
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

  public void genNewPacket() {
    String answer = randomAnswer();
    final CraftMapCanvas map = new CraftMapCanvas(cachedBackgroundMap.getCanvas());
    int fontNumber = fontCounter.getAndIncrement();
    if (fontNumber >= fonts.size()) {
      fontNumber = 0;
      fontCounter.set(0);
    }
    BufferedImage image =
        painter.draw(fonts.get(fontNumber), randomNotWhiteColor(), answer);
    map.drawImage(0, 0, image);
    MapDataPacket packet = new MapDataPacket(0, (byte) 0, map.getMapData());
    CachedCaptcha.createCaptchaPacket(packet, answer);
  }

  private Color randomNotWhiteColor() {
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

  private String randomAnswer() {
    int length = Settings.IMP.MAIN.CAPTCHA_GENERATOR.LENGTH;
    String pattern = Settings.IMP.MAIN.CAPTCHA_GENERATOR.PATTERN;

    char[] text = new char[length];
    for (int i = 0; i < length; i++) {
      text[i] = pattern.charAt(ThreadLocalRandom.current().nextInt(pattern.length()));
    }
    return new String(text);
  }
}
