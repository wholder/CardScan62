import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class RotateImage {
  public static BufferedImage rotate (BufferedImage img, double angle) {
    AffineTransform at1 = new AffineTransform();
    at1.scale(1, 1);
    at1.rotate(Math.toRadians(angle));
    Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight());
    Path2D.Double tShape = (Path2D.Double) at1.createTransformedShape(rect);
    Rectangle2D bnds = tShape.getBounds2D();
    Dimension dim = new Dimension((int) Math.round(bnds.getWidth()), (int) Math.round(bnds.getHeight()));
    AffineTransform at2 = new AffineTransform();
    at2.translate(-bnds.getX(), -bnds.getY());
    at2.scale(1, 1);
    at2.rotate(Math.toRadians(angle));
    BufferedImage bImg = new BufferedImage(dim.width, dim.height, img.getType());
    Graphics2D g2 = (Graphics2D) bImg.getGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    // Fill background
    g2.setColor(Color.lightGray);
    g2.fillRect(0, 0, dim.width, dim.height);
    // Draw rotated image
    g2.setTransform(at2);
    g2.drawImage(img, 0, 0, null);
    return bImg;
  }
}
