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

package net.elytrium.limbofilter.generator;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Random;
import net.elytrium.limbofilter.config.Settings;

public class CaptchaPainter {

  private static final int width = 128;
  private static final int height = 128;
  private final Color background = Color.WHITE;
  private final Random rnd = new Random();

  public BufferedImage draw(Font font, Color foreGround, String text) {
    if (font == null) {
      throw new IllegalArgumentException("Font can not be null.");
    }
    if (foreGround == null) {
      throw new IllegalArgumentException("Foreground color can not be null.");
    }
    if (text == null || text.length() < 1) {
      throw new IllegalArgumentException("No text given.");
    }

    BufferedImage img = this.createImage();

    final Graphics g = img.getGraphics();
    try {
      final Graphics2D g2 = this.configureGraphics(g, font, foreGround);
      this.draw(g2, text);
    } finally {
      g.dispose();
    }

    img = this.postProcess(img);

    return img;
  }

  protected void draw(Graphics2D g, String text) {
    final GlyphVector vector = g.getFont().createGlyphVector(g.getFontRenderContext(), text);

    this.transform(g, text, vector);

    final Rectangle bounds = vector.getPixelBounds(null, 0, height);
    final float bw = (float) bounds.getWidth();
    final float bh = (float) bounds.getHeight();

    final boolean outlineEnabled = Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONT_OUTLINE;

    final float wr = width / bw * (this.rnd.nextFloat() / 20 + (outlineEnabled ? 0.89f : 0.92f)) * 1;
    final float hr = height / bh * (this.rnd.nextFloat() / 20 + (outlineEnabled ? 0.68f : 0.75f)) * 1;
    g.translate((width - bw * wr) / 2, (height - bh * hr) / 2);
    g.scale(wr, hr);

    final float bx = (float) bounds.getX();
    final float by = (float) bounds.getY();
    if (outlineEnabled) {
      g.draw(vector.getOutline(Math.signum(this.rnd.nextFloat() - 0.5f) * 1
          * width / 200 - bx, Math.signum(this.rnd.nextFloat() - 0.5f) * 1
          * height / 70 + height - by));
    }

    g.drawGlyphVector(vector, -bx, height - by);
  }

  protected BufferedImage createImage() {
    return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
  }

  protected Graphics2D configureGraphics(Graphics g, Font font, Color foreGround) {
    if (!(g instanceof Graphics2D)) {
      throw new IllegalStateException("Graphics (" + g + ") that is not an instance of Graphics2D.");
    }
    final Graphics2D g2 = (Graphics2D) g;

    this.configureGraphicsQuality(g2);

    g2.setColor(foreGround);
    g2.setBackground(this.background);
    g2.setFont(font);

    g2.clearRect(0, 0, width, height);

    return g2;
  }

  protected void configureGraphicsQuality(Graphics2D g2) {
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
  }

  protected void transform(Graphics2D g, String text, GlyphVector v) {
    final int glyphNum = v.getNumGlyphs();

    Point2D prePos = null;
    Rectangle2D preBounds = null;

    double rotateCur = (this.rnd.nextDouble() - 0.5) * Math.PI / 8;
    double rotateStep = Math.signum(rotateCur) * (this.rnd.nextDouble() * 3 * Math.PI / 8 / glyphNum);
    final boolean rotateEnabled = Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONT_ROTATE;

    for (int fi = 0; fi < glyphNum; fi++) {
      if (rotateEnabled) {
        final AffineTransform tr = AffineTransform.getRotateInstance(rotateCur);
        if (this.rnd.nextDouble() < 0.25) {
          rotateStep *= -1;
        }
        rotateCur += rotateStep;
        v.setGlyphTransform(fi, tr);
      }
      final Point2D pos = v.getGlyphPosition(fi);
      final Rectangle2D bounds = v.getGlyphVisualBounds(fi).getBounds2D();
      Point2D newPos;
      if (prePos == null) {
        newPos = new Point2D.Double(pos.getX() - bounds.getX(), pos.getY());
      } else {
        newPos = new Point2D.Double(
            preBounds.getMaxX() + pos.getX() - bounds.getX() - Math.min(preBounds.getWidth(),
                bounds.getWidth()) * (this.rnd.nextDouble() / 20 + (rotateEnabled ? 0.27 : 0.1)), pos.getY());
      }
      v.setGlyphPosition(fi, newPos);
      prePos = newPos;
      preBounds = v.getGlyphVisualBounds(fi).getBounds2D();
    }
  }

  protected BufferedImage postProcess(BufferedImage img) {
    if (Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONT_RIPPLE) {
      final Rippler.AxisConfig vertical = new Rippler.AxisConfig(
          this.rnd.nextDouble() * 2 * Math.PI, (1 + this.rnd.nextDouble() * 2) * Math.PI, img.getHeight() / 10.0);
      final Rippler.AxisConfig horizontal = new Rippler.AxisConfig(
          this.rnd.nextDouble() * 2 * Math.PI, (2 + this.rnd.nextDouble() * 2) * Math.PI, img.getWidth() / 100.0);
      final Rippler op = new Rippler(vertical, horizontal);

      img = op.filter(img, this.createImage());
    }

    if (Settings.IMP.MAIN.CAPTCHA_GENERATOR.FONT_BLUR) {
      final float[] blurArray = new float[9];
      this.fillBlurArray(blurArray);
      final ConvolveOp op = new ConvolveOp(new Kernel(3, 3, blurArray), ConvolveOp.EDGE_NO_OP, null);

      img = op.filter(img, this.createImage());
    }

    return img;
  }

  protected void fillBlurArray(float[] array) {
    float sum = 0;
    for (int fi = 0; fi < array.length; fi++) {
      array[fi] = this.rnd.nextFloat();
      sum += array[fi];
    }

    for (int fi = 0; fi < array.length; fi++) {
      array[fi] /= sum;
    }
  }
}
