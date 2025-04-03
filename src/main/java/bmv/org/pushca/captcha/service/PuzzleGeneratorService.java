package bmv.org.pushca.captcha.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class PuzzleGeneratorService implements CaptchaGenerator {

  @Override
  public BufferedImage generateCaptchaImage(String text) {
    return generateCaptchaImage(text, "puzzle_image.png", true);
  }

  public BufferedImage generateCaptchaImage(String text, String outputFilePath, boolean randomColors) {
    int squareSideLength;
    try {
      squareSideLength = Integer.parseInt(text);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid input for square side length: " + text, e);
    }

    BufferedImage image = new BufferedImage(squareSideLength, squareSideLength, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = null;
    try {
      graphics = image.createGraphics();

      // Fill background
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, squareSideLength, squareSideLength);

      // Draw puzzle grid with curved lines
      int squareSize = squareSideLength / 4; // 4x4 grid
      graphics.setColor(Color.BLACK);
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          drawPuzzlePiece(graphics, i * squareSize, j * squareSize, squareSize, i, j);
        }
      }

      /*// Draw grid
      int squareSize = squareSideLength / 4; // 4x4 grid
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          if (randomColors) {
            graphics.setColor(new Color((int) (Math.random() * 0x1000000)));
          } else {
            graphics.setColor(Color.LIGHT_GRAY);
          }
          graphics.fillRect(i * squareSize, j * squareSize, squareSize, squareSize);
          graphics.setColor(Color.BLACK);
          graphics.drawRect(i * squareSize, j * squareSize, squareSize, squareSize);
        }
      }*/
    } finally {
      Optional.ofNullable(graphics).ifPresent(Graphics2D::dispose);
    }

    // Save the image
    try {
      ImageIO.write(image, "png", new File(outputFilePath));
    } catch (IOException e) {
      System.err.println("Error writing image to file: " + e.getMessage());
    }

    return image;
  }

  private void drawPuzzlePiece(Graphics2D graphics, int x, int y, int size, int row, int col) {
    Path2D path = new Path2D.Double();
    path.moveTo(x, y);

    // Top side
    if (row > 0) {
      drawSlot(path, x, y, size, true);
    } else {
      path.lineTo(x + size, y);
    }

    // Right side
    if (col < 3) {
      drawTab(path, x + size, y, size, false);
    } else {
      path.lineTo(x + size, y + size);
    }

    // Bottom side
    if (row < 3) {
      drawTab(path, x + size, y + size, size, true);
    } else {
      path.lineTo(x, y + size);
    }

    // Left side
    if (col > 0) {
      drawSlot(path, x, y + size, size, false);
    } else {
      path.lineTo(x, y);
    }

    path.closePath();
    graphics.draw(path);
  }

  private void drawTab(Path2D path, int x, int y, int size, boolean horizontal) {
    int tabSize = size / 4;
    if (horizontal) {
      path.lineTo(x + size * 0.25, y);
      path.curveTo(x + size * 0.35, y - tabSize, x + size * 0.65, y - tabSize, x + size * 0.75, y);
      path.lineTo(x + size, y);
    } else {
      path.lineTo(x, y + size * 0.25);
      path.curveTo(x + tabSize, y + size * 0.35, x + tabSize, y + size * 0.65, x, y + size * 0.75);
      path.lineTo(x, y + size);
    }
  }

  private void drawSlot(Path2D path, int x, int y, int size, boolean horizontal) {
    int slotSize = size / 4;
    if (horizontal) {
      path.lineTo(x + size * 0.25, y);
      path.curveTo(x + size * 0.35, y + slotSize, x + size * 0.65, y + slotSize, x + size * 0.75, y);
      path.lineTo(x + size, y);
    } else {
      path.lineTo(x, y + size * 0.25);
      path.curveTo(x - slotSize, y + size * 0.35, x - slotSize, y + size * 0.65, x, y + size * 0.75);
      path.lineTo(x, y + size);
    }
  }

}
