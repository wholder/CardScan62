
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
/*
 *  Test Program to communicate with and control a Dymo CardScan 62
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

//  Windows driver: https://gist.github.com/mbrownnycnyc/8939716

//  08F0:0006 - Bus: 2288 Speed: Full, Class: Vendor-specific, Manf: Corex Technologies, Device: unknown (Model 62)
//  interface: 0
//      BLK add: 0x01 (OUT) pkt: 64
//      BLK add: 0x82 (IN)  pkt: 64
//
//  settings command:     0x48,       0x00, 0x00
//                                    ====  ==== = 0x00 = data length
//  settings response:    0xC8, 0x00, 0x0C, 0x00, 0xB0, 0x02, 0x01, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x58, 0xCA, 0x7D
//                              ==== 1 = has paper, else 0
//                                    ====  ==== = 0x0C (12) = data length
//                                                ====  ==== 0x2B0 = scan width (688 pixels)
//                                                ====  ====  ====  ====  ====  ====  ====  ====  ====  ====  ====  ==== data
//
//  calibration command:  0x45,       0x00, 0x00
//                                    ====  ==== = 0x00 = data length
//  calibration response: 0xC5, 0x00, 0x87, 0x15, 0x22, 0x3A, 0x00, 0x00, etc. (total of 5568 bytes returned)
//                              ==== 1 = has paper, else 0
//                                    ====  ==== = 0x1587 (5511) = data length
//                                                ====  ====  ====  ==== data...
//
//  status1 command:      0x01,       0x01, 0x00, 0x00
//                                    ====  ==== = 0x01 = data length
//                                          ====  data
//  status1 response:     0x81, 0x00, 0x07, 0x00, 0x00, 0x48, 0x0F, 0x16, 0x09, 0xB8, 0x3E
//                              ==== 1 = has paper, else 0
//                                    ====  ==== = 0x07 = data length (1 + 6)
//                                                ==== echoed
//                                                      ====  ====  ====  ====  ====  ==== response data (meaning?)
//
//  status2 command:      0x34,       0x00, 0x00
//                                    ====  ==== = 0x00 = data length
//  status2 response:     0xB4, 0x00, 0x00, 0x00
//                                    ====  ==== = 0x00 = data length
//                              ==== 1 = has paper, else 0
//
//  = = = = = = = = = = Typical sequence for gray scale scanning = = = = = = = = = = =
//
//  paperCheck command:   0x35,       0x01, 0x00, 0x00
//                                    ====  ==== = 0x01 = data length
//                                                ====  data
//  paperCheck response:  0xB5, 0x00, 0x01, 0x00, 0x00  (no paper)
//  paperCheck response:  0xB5, 0x01, 0x01, 0x00, 0x00  (with paper)
//                              ==== 1 = has paper, else 0
//                                    ====  ==== = 0x01 = data length
//                                                ====  echoed data
//
//  cmd2 command:         0x14,       0x05, 0x00, 0x80, 0x1B, 0x28, 0x00, 0x0F
//                                    ====  ==== = 0x05 = data length
//  cmd2 response:        0x94, 0x01, 0x05, 0x00, 0x80, 0x1B, 0x28, 0x00, 0x0F
//                              ==== 1 = has paper, else 0
//
//  cmd3 command:         0x22,       0x01, 0x00, 0x00
//                                    ====  ==== = 0x01 = data length
//                                                ====  data
//  cmd3 response:        0xA2, 0x01, 0x01, 0x00, 0x00
//                              ==== 1 = has paper, else 0
///                                               ==== echoed data
//
//  cmd4 command:         0x1A,       0x01, 0x00, 0x66
//                                    ====  ==== = 0x01 = data length
//                                                ====  data
//  cmd4 response:        0x9A, 0x01, 0x01, 0x00, 0x66
//                              ==== 1 = has paper, else 0
///                                               ==== echoed data
//
//  cmd5 command:         0x19,       0x03, 0x00, 0x51, 0x62, 0x49
//                                    ====  ==== = 0x03 = data length
//                                                ====  ====  ====  data
//  cmd5 response:        0x99, 0x01, 0x03, 0x00, 0x51, 0x62, 0x49
//                              ==== 1 = has paper, else 0
//                                    ====  ==== = 0x03 = data length
///                                               ====  ====  ==== echoed data
//
//  grayLamp command:     0x12,       0x06, 0x00,       0x00, 0x01, 0x60, 0x00, 0x61, 0x00
//                                    ====  ==== = 0x01 = data length
//                                                      ====  ====  ====  ====  ====  ==== data (meaning?)
//  grayLamp response:    0x92, 0x01, 0x3D, 0x00, 0x00, 0x01, 0x60, 0x00, 0x61, 0x00, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00
//                              ==== 1 = has paper, else 0
//                                    ====  ==== 0x03D (61) = data length (6 + 55)
//                                                ====  ====  ====  ====  ====  ==== echoed
//                                                                                    ====  ====  ====  ====  ====  ====...
//                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
//                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
//                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
//                        0x0C
//
//  grayScan command:     0x12,       0x06, 0x00, 0x00, 0x01, 0x60, 0x00, 0x61, 0x00
//                                    ====  ==== data length = 6
//                                                ====  ====  ====  ====  ====  ==== data
//                                                      ==== lines/scan
//  grayScan response:    0x92, 0x01, 0xF4, 0x04, 0x01, 0x01, 0x60, 0x00, 0x18, 0x05, 0x02, 0x02 ...
//                              ==== 1 = has paper, else 0
//                                    ====  ==== = 0x4F4 (1268) = data length (6 echoed bytes + 1262 pixels)
//                                                ====  ====  ====  ====  ==== ==== echoed (sort of)
//                                                                                   ====  ====. .. pixel data
//  Active pixels (gray) 60 - 612 (inclusive) or 549 pixels, 0 = black, 127 = white, right to left in array
//
//  cmd1 command:         0x13,       0x01, 0x00, 0x28
//                                    ====  ==== = 0x01 = data length
//                                                ==== data
//  cmd1 response:        0x93, 0x01, 0x01, 0x00, 0x28
//                                    ====  ==== = 0x01 = data length
//                                                ==== echoed data
//
//  powerDown command:    0x21,       0x02, 0x00, 0x0A, 0x00
//                                    ====  ==== data length = 2
//                                                ====  ====  ==== data
//  powerDown response:   0xA1, 0x00, 0x02, 0x00, 0x0A, 0x00
//                              ==== 1 = has paper, else 0
//                                    ====  ==== = 0x02 = data length
//                                                ====  ==== echoed data
//
//  = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
//
//  colorScan command:    0x12, 0x06, 0x00,       0x01, 0x01, 0x60, 0x00, 0x18, 0x05
//  colorScan response:   0x92, 0x01, 0xF4, 0x04, 0x01, 0x01, 0x60, 0x00, 0x18, 0x05, 0x02, 0x02...
//  Note: with colorScan, pixels returned are all zero value
//

public class CardScan62 extends JFrame {
  private static final boolean  DEBUG = false;
  private static final short    VENDOR_ID     = 0x08F0;
  private static final short    PRODUCT_ID    = 0x0006;
  private static final byte     INTERFACE     = 0;
  private static final byte     OUT_ENDPOINT  = (byte) 0x01;
  private static final byte     IN_ENDPOINT   = (byte) 0x82;
  private static final double   angle         = 90;                         // rotate image by this amount
  //                                             Cmd   lenL  lenH
  private static final byte[]   settings      = {0x48, 0x00, 0x00};
  private static final byte[]   calibration   = {0x45, 0x00, 0x00};
  private static final byte[]   status2       = {0x34, 0x00, 0x00};
  private static final byte[]   colorScan     = {0x12, 0x06, 0x00, 0x01, 0x01, 0x60, 0x00, 0x18, 0x05};
  private static final byte[]   colorLamp     = {0x18, 0x07, 0x00, 0x00, 0x01, 0x60, 0x00, 0x61, 0x00, 0x07};



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

  public static void main (String[] args) {
    USBIO usb = null;
    try {
      usb = new USBIO(VENDOR_ID, PRODUCT_ID, INTERFACE, OUT_ENDPOINT, IN_ENDPOINT);
      // Gobble up any leftovers from prior session
      byte[] dump;
      do {
        dump = usb.receive(100);
      } while (dump.length > 0);
      if (false) {
        // Test code
        System.out.println("calibration");
        usb.send(calibration);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        getResponse(usb, bout);
        printRespnse(bout.toByteArray());
      }

      while (!sendCmd(usb, "paperCheck", paperCheck)) {
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


      EventQueue.invokeLater(() -> new ImageViewer(
          RotateImage.rotate(scan, angle))
      );

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
      // usable pixel data is at 60 - 704 (inclusive)
      byte[] line = new byte[705 - 60];
      byte[] data = bout.toByteArray();
      for (int ii = 0; ii < linesPerScan; ii++) {
        System.arraycopy(data, 60 + ii * scanBytes, line, 0, line.length);
        lines.add(new ScanLine(line));
      }
      if (!hasPaper) {
        extra--;
      }
    } while (hasPaper || extra > 0);
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
