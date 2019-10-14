
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

//  Note: the model 60ii does not seem to respond to the same commands as the model 62, so this code does not work!

public class CardScan60 extends JFrame {
  private static final boolean  DEBUG = false;
  private static final short    VENDOR_ID     = 0x08F0;
  private static final short    PRODUCT_ID    = 0x1000;
  private static final byte     INTERFACE     = 0;
  private static final byte     OUT_ENDPOINT  = (byte) 0x03;
  private static final byte     IN_ENDPOINT   = (byte) 0x82;
  //                                             Cmd   lenL  lenH
  private static final byte[]   settings      = {0x48, 0x00, 0x00};
  private static final byte[]   calibration   = {0x45, 0x00, 0x00};
  private static final byte[]   status2       = {0x34, 0x00, 0x00};
  private static final byte[]   colorLamp     = {0x18, 0x07, 0x00, 0x00, 0x01, 0x60, 0x00, 0x61, 0x00, 0x07};
  private static final byte[]   colorScan     = {0x12, 0x06, 0x00, 0x01, 0x01, 0x60, 0x00, 0x18, 0x05};



  private static final byte[]   cmd1          = {0x14, 0x05, 0x00, (byte) 0x80, 0x1E, 0x28, 0x00, 0x0F};
                                              //                                ==== contrast (higher is more contrast) (was 0x1B)
                                              //                                0x7F is BW,
  private static final byte[]   paperCheck    = {0x35, 0x01, 0x00, 0x00};   // Paper status
  private static final byte[]   cmd2          = {0x22, 0x01, 0x00, 0x00};
  private static final byte[]   cmd3          = {0x1A, 0x01, 0x00, 0x66};
  private static final byte[]   cmd4          = {0x19, 0x03, 0x00, 0x51, 0x62, 0x49};
  private static final byte[]   grayLamp      = {0x12, 0x06, 0x00, 0x00, 0x01, 0x60, 0x00, 0x61, 0x00};
  private static final byte[]   cmd5          = {0x13, 0x01, 0x00, 0x28};
  private static final byte[]   grayScan      = {0x12, 0x06, 0x00, 0x01, 0x01, 0x00, 0x00, (byte) 0xC2, 0x02};
                                              //                   ==== 1 enables motor
                                              //                         ==== lines/scan
                                              //                               ====  ==== start (was 0x60)
                                              //                                           ====  ==== width (was 0x0518)
                                              // data[766] start = 0x00, width = 0x02C2 (706) width < 0x02C1 causes image wrap
  private static final byte[]   cmd6          = {0x35, 0x01, 0x00, (byte) 0xFF};
  private static final byte[]   status1       = {0x01, 0x01, 0x00, 0x00};
  private static final byte[]   powerDown     = {0x21, 0x02, 0x00, 0x0A, 0x00};

  /*
   *  Command       Response  Note: must unplug and replug
   *    cmd1          0 - sends responses after command sent 19 times...
   *    cmd2          0 - can send >= 32 times with no error
   *    cmd3          0 - error after command sent 25 times...
   *    cmd4          81 bytes (64 + 17), then error when command sent again
   *                    Send:
   *                        0x19, 0x03, 0x00, 0x51, 0x62, 0x49
   *                    Response:
   *                        81 bytes (64 + 17) all with value of 0x62
   *    cmd5          40 bytes - can send >= 32 times with no error
   *                    Send:
   *                        0x13, 0x01, 0x00, 0x28}
   *                    Response:
   *                        0x00, 0x00, 0x62, 0x02, 0x02, 0x00, 0x30, 0x06, 0x00, 0x01, 0x01, 0x00, 0x00, 0xE2, 0x02, 0x06,
   *                        0x12, 0x06, 0x00, 0x01, 0x01, 0x00, 0x00, 0x02, 0x02, 0x06, 0x12, 0x06, 0x00, 0x01, 0x01, 0x00,
   *                        0x00, 0x02, 0x02, 0x06, 0x12, 0x06, 0x00, 0x00,
   *    cmd6          255 bytes (all zeroes)  - can send >= 32 times with no error
   *                    Send:
   *                        0x35, 0x01, 0x00, 0xFF
   *                    Response:
   *                        255 bytes (64 + 64 + 64 + 63) all with value of 0x00
   *    status1       0 - can send >= 32 times with no error
   *    status2       0 - can send >= 32 times with no error
   *    paperCheck    0 - can send >= 32 times with no error
   *    settings      0 - can send >= 32 times with no error
   *    calibration   0 - error after command sent 2 times...
   *    powerDown     10 bytes (all zeroes) - unable to repeat with out power cycle
   *    grayLamp      0 - error after command sent 1 time
   *    grayScan      0 - can send >= 32 times with no error
   *    colorLamp     0 - error after command sent 1 time
   *    colorScan     0 - can send >= 32 times with no error
   */

  public static void main (String[] args) {
    USBIO usb = null;
    try {
      usb = new USBIO(VENDOR_ID, PRODUCT_ID, INTERFACE, OUT_ENDPOINT, IN_ENDPOINT);
      // Gobble up any leftovers from prior session
      byte[] dump;
      do {
        dump = usb.receive(100);
      } while (dump.length > 0);
      if (true) {
        byte[] cmd = cmd6;
        try {
          // Test code
          int sent = 0;
          for (int ii = 0; ii < 32; ii++) {
            System.out.println("send command");
            usb.send(cmd);
            sent += cmd.length;
            byte[] data;
            int bytes = 0;
            while ((data = usb.receive(100)).length > 0) {
              bytes += data.length;
              printRespnse(data);
            }
            System.out.println((ii + 1) + " - total sent: " + sent + " - total received: " + bytes);
          }
        } finally {
          if (usb != null) {
            System.out.println("closing");
            usb.close();
          }
        }
        System.exit(0);
      }

      while (false && !sendCmd(usb, "paperCheck", paperCheck)) {
        try {
          System.out.print(".");
          Thread.sleep(500);
        } catch (InterruptedException ex) {
          ex.printStackTrace();
        }
      }
                                              //                   ====  ====  ====  ====  ==== data
      sendCmd(usb, "cmd1", cmd1);             // 0x14, 0x05, 0x00, 0x80, 0x1B, 0x28, 0x00, 0x0F           needed
                                              //                         ==== contrast?
      sendCmd(usb, "cmd2", cmd2);             // 0x22, 0x01, 0x00, 0x00                                   needed
      sendCmd(usb, "cmd3", cmd3);             // 0x1A, 0x01, 0x00, 0x66                                   needed
      sendCmd(usb, "cmd4", cmd4);             // 0x19, 0x03, 0x00, 0x51, 0x62, 0x49                       needed
      sendCmd(usb, "grayLamp", grayLamp);     // 0x12, 0x06, 0x00, 0x00, 0x01, 0x60, 0x00, 0x61, 0x00     needed
      sendCmd(usb, "cmd5", cmd5);             // 0x13, 0x01, 0x00, 0x28                                   needed

      BufferedImage scan = scanPixels(usb);   // 0x12, 0x06, 0x00, 0x01, 0x10, 0x60, 0x00, 0x81, 0x05
                                              //                                                 ==M= motor speed (higher is slower)

      sendCmd(usb, "cmd6", cmd6);             // 0x35, 0x01, 0x00, 0xFF
      sendCmd(usb, "status1", status1);       // 0x34, 0x00, 0x00
      sendCmd(usb, "powerDown", powerDown);   // 0x21, 0x02, 0x00, 0x0A, 0x00

      EventQueue.invokeLater(() -> new ImageViewer(scan));

    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (usb != null) {
        System.out.println("closing");
        usb.close();
      }
    }
    System.out.println("done");
    //System.exit(0);
  }

  static class ScanLine {
    byte[] line;

    ScanLine (byte[] scan) {
      line = new byte[scan.length];
      for (int ii = 0; ii < line.length; ii++) {
        line[ii] = (byte) (scan[ii] << 1);
      }
    }
  }

  private static int getWord16 (byte[] ary, int idx) {
    return (ary[idx] & 0xFF) + ((ary[idx + 1] & 0xFF) << 8);
  }

  private static BufferedImage scanPixels (USBIO usb) throws IOException {
    List<ScanLine> lines = new ArrayList<>();
    int linesPerScan = 16;
    System.out.println("grayScan (linesPerScan = " + linesPerScan);
    byte[] temp = new byte[grayScan.length];
    System.arraycopy(grayScan, 0, temp, 0, grayScan.length);
    temp[4] = (byte) linesPerScan;
    boolean hasPaper;
    int extra = 140 / linesPerScan;
    // Extract scan offset and width from "grayScan" command
    int sOff = getWord16(grayScan, 5);  // 0
    int eOff = getWord16(grayScan, 7);  // 706
    int scanBytes = eOff - sOff;
    do {
      usb.send(temp);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      hasPaper = getResponse(usb, bout);
      byte[] data = bout.toByteArray();
      System.out.println(data.length + "\t" + hasPaper);
      if (false) {
        // usable pixel data is at 60 - 704 (inclusive)
        byte[] line = new byte[705 - 60];
        for (int ii = 0; ii < linesPerScan; ii++) {
          System.arraycopy(data, 60 + ii * scanBytes, line, 0, line.length);
          lines.add(new ScanLine(line));
        }
      }
      if (!hasPaper) {
        extra--;
      }
    } while (hasPaper || extra > 0);
    if (false) {
      // Generate grayscale image
      int pixelsPerLine = lines.get(0).line.length;
      BufferedImage image = new BufferedImage(pixelsPerLine, lines.size(), TYPE_BYTE_GRAY);
      WritableRaster raster = image.getRaster();
      int y = 0;
      for (ScanLine line : lines) {
        raster.setDataElements(0, y++, pixelsPerLine, 1, line.line);
      }
      System.out.println(image.getWidth() + ", " + image.getHeight());
      return image;
    } else {
      return new BufferedImage(64, 64, TYPE_BYTE_GRAY);
    }
  }

  private static boolean sendCmd (USBIO usb, String name, byte[] cmd) throws IOException {
    if (DEBUG) {
      System.out.println(name);
    }
    usb.send(cmd);
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    boolean hasPaper = getResponse(usb, bout);
    if (DEBUG) {
      printRespnse(bout.toByteArray());
    }
    return hasPaper;
  }

  private static boolean getResponse (USBIO usb, ByteArrayOutputStream bout) throws IOException {
    boolean hasPaper = false;
    byte[] rsp;
    int length = 0;
    do {
      rsp = usb.receive(100);
      if (rsp.length > 0) {
        if (length == 0) {
          length = ((rsp[3] & 0xFF) << 8) + (rsp[2] & 0xFF);
          bout.write(rsp, 4, rsp.length - 4);
          hasPaper = rsp[1] == 1;
          length -= rsp.length - 4;
        } else {
          bout.write(rsp);
          length -= rsp.length;
        }
      }
    } while (length > 0);
    return hasPaper;
  }

  private static void printRespnse (byte[] data) {
    for (int ii = 0; ii < data.length; ii++) {
      System.out.print(toHex(data[ii]) + ", ");
      if ((ii & 0x0F) == 0x0F) {
        System.out.println();
      }
    }
    System.out.println("\nresponse: " + data.length + " bytes");
  }

  private static String toHex (byte val) {
    StringBuilder buf = new StringBuilder("0x");
    int tmp = val & 0xFF;
    if (tmp < 0x10) {
      buf.append("0");
    }
    buf.append(Integer.toHexString(tmp).toUpperCase());
    return buf.toString();
  }
}
