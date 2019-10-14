<p align="center"><img src="https://github.com/wholder/CardScan62/blob/master/images/CardScan%2062%20Exterior.jpg" width="60%" height=60%"></p>

# CardScan62

**CardScan62** is an experimental, Java Language-based program I created to connect to and control a Dymo™ CardScan™ 62 Business Card Scanner.  This is a work in progress and I'm posting it here mostly to document what I've learned, so far.  As currently written, there is no GUI interface, so you need to run the code from within IntelliJ to try it out.  When you run the code in CardScan62.java, it will begin printing '.'s to the System.out to indicate it is ready to scan.  Scanning will begin wheh you insert a card to scan.  Upon completing the scan, the code will pop up a window displaying the scanned image. like this:

<p align="center"><img src="https://github.com/wholder/CardScan62/blob/master/images/Test%20Scan.png" width="60%" height=60%"></p>

The protocol used to talk to the scanner was adapted from the [SANE project](http://www.sane-project.org), but I had to tweak some of the commands in order to get them to work properly with the CardScan 62 (the code in SANE was designed for the CardScan 600C, which has a wider scan element.)

Note: I tried to make this code also work with the CardScan 60ii scanner which, externally, is similar in appearance.  However, the the internal circuitry for the 60ii is completely different and it does not seem to respond to any of the commands used to control the 62.

I created and tested CardScan62 using [**IntelliJ Community Edition 2017**](https://www.jetbrains.com/idea/download/#section=mac) on a Mac Pro and did some further testing on Windows 10 (using Parallels) and Linux Mint.  The code should work on the Mac without further configuration, but there are some additional setup and configuration steps needed before it will run on Windows or Linux (see comments in the source code for additional details.)

### Requirements

CardScan62 requires Java 8 JRE or [Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html), or later to be installed.

### Usb4Java

`CardScan62` uses `Usb4Java` to communicate with the CardScan 62.  For convenience, I have included the `Usb4Java` libraries in this project, but you should check for newer versions.
 - [Usb4Java](http://usb4java.org) - Usb4Java Project Page
 - [JavaDocs](http://usb4java.org/apidocs/index.html) - Usb4Java JavaDocs
