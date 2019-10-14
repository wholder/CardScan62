import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageViewer extends JFrame {
  static class Surface extends JPanel {
    private Image img;

    Surface(Image img) {
      this.img = img;
      setPreferredSize(new Dimension(img.getWidth(null), img.getHeight(null)));
    }

    @Override
    public void paintComponent(Graphics g) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.drawImage(img, 0, 0, null);
    }
  }

  public ImageViewer (Image img) {
    setTitle("ImageViewer");
    add(new Surface(img));
    pack();
    setLocationRelativeTo(null);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);
  }
}