/*
 *  Copyright (C) 2010-2013 JPEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.gui;

import com.jpexs.decompiler.flash.Configuration;
import com.jpexs.decompiler.flash.EventListener;
import com.jpexs.decompiler.flash.PercentListener;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.Version;
import com.jpexs.decompiler.flash.abc.avm2.AVM2Code;
import com.jpexs.decompiler.flash.gui.player.FlashPlayerPanel;
import com.jpexs.decompiler.flash.gui.proxy.ProxyFrame;
import com.jpexs.decompiler.flash.helpers.Helper;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

/**
 * Main executable class
 *
 * @author JPEXS
 */
public class Main {

    public static ProxyFrame proxyFrame;
    public static String file;
    public static String maskURL;
    public static SWF swf;
    public static String version = "";
    public static final String applicationName = "JPEXS Free Flash Decompiler";
    public static String applicationVerName;
    public static final String shortApplicationName = "FFDec";
    public static String shortApplicationVerName;
    public static final String projectPage = "http://www.free-decompiler.com/flash";
    public static String updatePageStub = "http://www.free-decompiler.com/flash/update.html?currentVersion=";
    public static String updatePage;
    public static final String vendor = "JPEXS";
    public static LoadingDialog loadingDialog;
    public static ModeFrame modeFrame;
    private static boolean working = false;
    private static TrayIcon trayIcon;
    private static MenuItem stopMenuItem;
    private static boolean commandLineMode = false;
    public static MainFrame mainFrame;
    private static final int UPDATE_SYSTEM_MAJOR = 1;
    private static final int UPDATE_SYSTEM_MINOR = 0;

    private static void loadProperties() {
        Properties prop = new Properties();
        try {
            prop.load(Main.class.getResourceAsStream("/project.properties"));
            version = prop.getProperty("version");
            applicationVerName = applicationName + " v." + version;
            updatePage = updatePageStub + version;
            shortApplicationVerName = shortApplicationName + " v." + version;
        } catch (IOException ex) {
            //ignore
            version = "unknown";
        }
    }

    public static boolean isCommandLineMode() {
        return commandLineMode;
    }

    /**
     * Dump tags to stdout
     */
    //
    public static String getFileTitle() {
        if (maskURL != null) {
            return maskURL;
        }
        return file;
    }

    public static void setSubLimiter(boolean value) {
        if (value) {
            AVM2Code.toSourceLimit = Configuration.SUBLIMITER;
        } else {
            AVM2Code.toSourceLimit = -1;
        }
    }

    public static boolean isWorking() {
        return working;
    }

    public static void showProxy() {
        if (proxyFrame == null) {
            proxyFrame = new ProxyFrame();
        }
        proxyFrame.setVisible(true);
        proxyFrame.setState(Frame.NORMAL);
    }

    public static void startWork(String name) {
        startWork(name, -1);
    }

    public static void startWork(String name, int percent) {
        working = true;
        if (mainFrame != null) {
            mainFrame.setWorkStatus(name);
            if (percent == -1) {
                mainFrame.hidePercent();
            } else {
                mainFrame.setPercent(percent);
            }
        }
        if (loadingDialog != null) {
            loadingDialog.setDetail(name);
            if (percent == -1) {
                loadingDialog.hidePercent();
            } else {
                loadingDialog.setPercent(percent);
            }
        }
        if (Main.isCommandLineMode()) {
            System.out.println(name);
        }
    }

    public static void stopWork() {
        working = false;
        if (mainFrame != null) {
            mainFrame.setWorkStatus("");
        }
        if (loadingDialog != null) {
            loadingDialog.setDetail("");
        }
    }

    public static SWF parseSWF(String file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        InputStream bis = new BufferedInputStream(fis);
        SWF locswf = new SWF(bis, new PercentListener() {
            @Override
            public void percent(int p) {
                startWork("Reading SWF", p);
            }
        }, (Boolean) Configuration.getConfig("paralelSpeedUp", Boolean.TRUE));
        locswf.addEventListener(new EventListener() {
            @Override
            public void handleEvent(String event, Object data) {
                if (event.equals("export")) {
                    startWork((String) data);
                }
                if (event.equals("getVariables")) {
                    startWork("Getting variables..." + (String) data);
                }
                if (event.equals("deobfuscate")) {
                    startWork("Deobfuscating..." + (String) data);
                }
            }
        });
        return locswf;
    }

    public static void saveFile(String outfile) throws IOException {
        file = outfile;
        swf.saveTo(new FileOutputStream(outfile));
    }

    private static class OpenFileWorker extends SwingWorker {

        @Override
        protected Object doInBackground() throws Exception {
            try {
                Main.startWork("Reading SWF...");
                swf = parseSWF(Main.file);
                FileInputStream fis = new FileInputStream(file);
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, "Cannot load SWF file.");
                loadingDialog.setVisible(false);
                return false;
            }

            try {
                Main.startWork("Creating window...");
                mainFrame = new MainFrame(swf);
                loadingDialog.setVisible(false);
                mainFrame.setVisible(true);
                Main.stopWork();
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            return true;
        }
    }

    public static boolean openFile(String swfFile) {
        if (mainFrame != null) {
            mainFrame.setVisible(false);
        }
        Main.file = swfFile;
        if (Main.loadingDialog == null) {
            Main.loadingDialog = new LoadingDialog();
        }
        Main.loadingDialog.setVisible(true);
        (new OpenFileWorker()).execute();
        return true;
    }

    public static boolean saveFileDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File((String) Configuration.getConfig("lastSaveDir", ".")));
        JFrame f = new JFrame();
        View.setWindowIcon(f);
        int returnVal = fc.showSaveDialog(f);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = Helper.fixDialogFile(fc.getSelectedFile());
            try {
                Main.saveFile(file.getAbsolutePath());
                Configuration.setConfig("lastSaveDir", file.getParentFile().getAbsolutePath());
                maskURL = null;
                return true;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Cannot write to the file");
            }
        }
        return false;
    }

    public static boolean openFileDialog() {
        maskURL = null;
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File((String) Configuration.getConfig("lastOpenDir", ".")));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return (f.getName().endsWith(".swf")) || (f.isDirectory());
            }

            @Override
            public String getDescription() {
                return "SWF files (*.swf)";
            }
        });
        JFrame f = new JFrame();
        View.setWindowIcon(f);
        int returnVal = fc.showOpenDialog(f);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Configuration.setConfig("lastOpenDir", Helper.fixDialogFile(fc.getSelectedFile()).getParentFile().getAbsolutePath());
            File selfile = Helper.fixDialogFile(fc.getSelectedFile());
            Main.openFile(selfile.getAbsolutePath());
            return true;
        } else {
            return false;
        }
    }

    public static void showModeFrame() {
        if (modeFrame == null) {
            modeFrame = new ModeFrame();
        }
        modeFrame.setVisible(true);
    }

    public static void updateLicense() {
        updateLicenseInDir(new File(".\\src\\"));
    }

    /**
     * Script for updating license header in java files :-)
     *
     * @param dir Star directory (e.g. "src/")
     */
    public static void updateLicenseInDir(File dir) {
        int defaultStartYear = 2010;
        int defaultFinalYear = 2013;
        String defaultAuthor = "JPEXS";
        String defaultYearStr = "" + defaultStartYear;
        if (defaultFinalYear != defaultStartYear) {
            defaultYearStr += "-" + defaultFinalYear;
        }
        String license = "/*\r\n *  Copyright (C) {year} {author}\r\n * \r\n *  This program is free software: you can redistribute it and/or modify\r\n *  it under the terms of the GNU General Public License as published by\r\n *  the Free Software Foundation, either version 3 of the License, or\r\n *  (at your option) any later version.\r\n * \r\n *  This program is distributed in the hope that it will be useful,\r\n *  but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n *  GNU General Public License for more details.\r\n * \r\n *  You should have received a copy of the GNU General Public License\r\n *  along with this program.  If not, see <http://www.gnu.org/licenses/>.\r\n */";

        File files[] = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                updateLicenseInDir(f);
            } else {
                if (f.getName().endsWith(".java")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintWriter pw = null;
                    try {
                        pw = new PrintWriter(new OutputStreamWriter(baos, "utf8"));
                    } catch (UnsupportedEncodingException ex) {
                    }
                    try {
                        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                            String s;
                            boolean packageFound = false;
                            String author = defaultAuthor;
                            String yearStr = defaultYearStr;
                            while ((s = br.readLine()) != null) {
                                if (!packageFound) {
                                    if (s.trim().startsWith("package")) {
                                        packageFound = true;
                                        pw.println(license.replace("{year}", yearStr).replace("{author}", author));
                                    } else {
                                        Matcher mAuthor = Pattern.compile("^.*Copyright \\(C\\) ([0-9]+)(-[0-9]+)? (.*)$").matcher(s);
                                        if (mAuthor.matches()) {
                                            author = mAuthor.group(3).trim();
                                            int startYear = Integer.parseInt(mAuthor.group(1).trim());
                                            if (startYear == defaultFinalYear) {
                                                yearStr = "" + startYear;
                                            } else {
                                                yearStr = "" + startYear + "-" + defaultFinalYear;
                                            }
                                            if (!author.equals(defaultAuthor)) {
                                                System.out.println("Detected nodefault author:" + author + " in " + f.getAbsolutePath());
                                            }
                                        }
                                    }
                                }
                                if (packageFound) {
                                    pw.println(s);
                                }
                            }
                        }
                        pw.close();
                    } catch (IOException ex) {
                    }

                    FileOutputStream fos;
                    try {
                        fos = new FileOutputStream(f);
                        fos.write(baos.toByteArray());
                        fos.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }

    }

    public static void badArguments() {
        System.err.println("Error: Bad Commandline Arguments!");
        printCmdLineUsage();
        System.exit(1);
    }

    public static void printHeader() {
        System.out.println(applicationVerName);
        for (int i = 0; i < applicationVerName.length(); i++) {
            System.out.print("-");
        }
        System.out.println();
    }

    public static void printCmdLineUsage() {
        System.out.println("Commandline arguments:");
        System.out.println(" 1) -help | --help | /?");
        System.out.println(" ...shows commandline arguments (this help)");
        System.out.println(" 2) infile");
        System.out.println(" ...opens SWF file with the decompiler GUI");
        System.out.println(" 3) -proxy (-PXXX)");
        System.out.println("  ...auto start proxy in the tray. Optional parameter -P specifies port for proxy. Defaults to 55555. ");
        System.out.println(" 4) -export (as|pcode|image|shape|movie|sound|binaryData|text|textplain|all) outdirectory infile [-selectas3class class1 class2 ...]");
        System.out.println("  ...export infile sources to outdirectory as AsctionScript code (\"as\" argument) or as PCode (\"pcode\" argument), images, shapes, movies, binaryData, text with formatting, plain text or all.");
        System.out.println("     When \"as\" or \"pcode\" type specified, optional \"-selectas3class\" parameter can be passed to export only selected classes (ActionScript 3 only)");
        System.out.println(" 5) -dumpSWF infile");
        System.out.println("  ...dumps list of SWF tags to console");
        System.out.println(" 6) -compress infile outfile");
        System.out.println("  ...Compress SWF infile and save it to outfile");
        System.out.println(" 7) -decompress infile outfile");
        System.out.println("  ...Decompress infile and save it to outfile");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("java -jar ffdec.jar myfile.swf");
        System.out.println("java -jar ffdec.jar -proxy");
        System.out.println("java -jar ffdec.jar -proxy -P1234");
        System.out.println("java -jar ffdec.jar -export as \"C:\\decompiled\\\" myfile.swf");
        System.out.println("java -jar ffdec.jar -export as \"C:\\decompiled\\\" myfile.swf -selectas3class com.example.MyClass com.example.SecondClass");
        System.out.println("java -jar ffdec.jar -export pcode \"C:\\decompiled\\\" myfile.swf");
        System.out.println("java -jar ffdec.jar -dumpSWF myfile.swf");
        System.out.println("java -jar ffdec.jar -compress myfile.swf myfiledec.swf");
        System.out.println("java -jar ffdec.jar -decompress myfiledec.swf myfile.swf");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        loadProperties();
        View.setLookAndFeel();
        Configuration.loadFromFile(getConfigFile(), getReplacementsFile());

        int pos = 0;
        if (args.length > 0) {
            if (args[0].equals("-debug")) {
                Configuration.debugMode = true;
                pos++;
            }
        }
        initLogging(Configuration.debugMode);
        if (args.length < pos + 1) {
            autoCheckForUpdates();
            showModeFrame();
        } else {
            if (args[pos].equals("-proxy")) {
                int port = 55555;
                for (int i = pos; i < args.length; i++) {
                    if (args[i].startsWith("-P")) {
                        try {
                            port = Integer.parseInt(args[pos].substring(2));
                        } catch (NumberFormatException nex) {
                            System.err.println("Bad port number");
                        }
                    }
                }
                if (proxyFrame == null) {
                    proxyFrame = new ProxyFrame();
                }
                proxyFrame.setPort(port);
                addTrayIcon();
                switchProxy();
            } else if (args[pos].equals("-export")) {
                if (args.length < pos + 4) {
                    badArguments();
                }
                String exportFormat = args[pos + 1];
                if (!exportFormat.toLowerCase().equals("as")) {
                    if (!exportFormat.toLowerCase().equals("pcode")) {
                        if (!exportFormat.toLowerCase().equals("image")) {
                            if (!exportFormat.toLowerCase().equals("shape")) {
                                if (!exportFormat.toLowerCase().equals("movie")) {
                                    if (!exportFormat.toLowerCase().equals("sound")) {
                                        if (!exportFormat.toLowerCase().equals("binaryData")) {
                                            if (!exportFormat.toLowerCase().equals("text")) {
                                                if (!exportFormat.toLowerCase().equals("textplain")) {
                                                    if (!exportFormat.toLowerCase().equals("all")) {
                                                        if (!exportFormat.toLowerCase().equals("fla")) {
                                                            if (!exportFormat.toLowerCase().equals("xfl")) {
                                                                System.err.println("Invalid export format:" + exportFormat);
                                                                badArguments();
                                                            }
                                                        }

                                                    }

                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                File outDir = new File(args[pos + 2]);
                File inFile = new File(args[pos + 3]);
                if (!inFile.exists()) {
                    System.err.println("Input SWF file does not exist!");
                    badArguments();
                }
                commandLineMode = true;
                boolean exportOK;
                try {
                    printHeader();
                    SWF exfile = new SWF(new FileInputStream(inFile), (Boolean) Configuration.getConfig("paralelSpeedUp", Boolean.TRUE));
                    exfile.addEventListener(new EventListener() {
                        @Override
                        public void handleEvent(String event, Object data) {
                            if (event.equals("export")) {
                                System.out.println((String) data);
                            }
                        }
                    });
                    if (exportFormat.equals("all")) {
                        System.out.println("Exporting images...");
                        exfile.exportImages(outDir.getAbsolutePath() + File.separator + "images");
                        System.out.println("Exporting shapes...");
                        exfile.exportShapes(outDir.getAbsolutePath() + File.separator + "shapes");
                        System.out.println("Exporting scripts...");
                        exfile.exportActionScript(outDir.getAbsolutePath() + File.separator + "scripts", false, (Boolean) Configuration.getConfig("paralelSpeedUp", Boolean.TRUE));
                        System.out.println("Exporting movies...");
                        exfile.exportMovies(outDir.getAbsolutePath() + File.separator + "movies");
                        System.out.println("Exporting sounds...");
                        exfile.exportSounds(outDir.getAbsolutePath() + File.separator + "sounds", true, true);
                        System.out.println("Exporting binaryData...");
                        exfile.exportBinaryData(outDir.getAbsolutePath() + File.separator + "binaryData");
                        System.out.println("Exporting texts...");
                        exfile.exportTexts(outDir.getAbsolutePath() + File.separator + "texts", true);
                        exportOK = true;
                    } else if (exportFormat.equals("image")) {
                        exfile.exportImages(outDir.getAbsolutePath());
                        exportOK = true;
                    } else if (exportFormat.equals("shape")) {
                        exfile.exportShapes(outDir.getAbsolutePath());
                        exportOK = true;
                    } else if (exportFormat.equals("as") || exportFormat.equals("pcode")) {
                        if ((pos + 5 < args.length) && (args[pos + 4].equals("-selectas3class"))) {
                            exportOK = true;
                            for (int i = pos + 5; i < args.length; i++) {
                                exportOK = exportOK && exfile.exportAS3Class(args[i], outDir.getAbsolutePath(), exportFormat.equals("pcode"), (Boolean) Configuration.getConfig("paralelSpeedUp", Boolean.TRUE));
                            }
                        } else {
                            exportOK = exfile.exportActionScript(outDir.getAbsolutePath(), exportFormat.equals("pcode"), (Boolean) Configuration.getConfig("paralelSpeedUp", Boolean.TRUE));
                        }
                    } else if (exportFormat.equals("movie")) {
                        exfile.exportMovies(outDir.getAbsolutePath());
                        exportOK = true;
                    } else if (exportFormat.equals("sound")) {
                        exfile.exportSounds(outDir.getAbsolutePath(), true, true);
                        exportOK = true;
                    } else if (exportFormat.equals("binaryData")) {
                        exfile.exportBinaryData(outDir.getAbsolutePath());
                        exportOK = true;
                    } else if (exportFormat.equals("text")) {
                        exfile.exportTexts(outDir.getAbsolutePath(), true);
                        exportOK = true;
                    } else if (exportFormat.equals("textplain")) {
                        exfile.exportTexts(outDir.getAbsolutePath(), false);
                        exportOK = true;
                    } else if (exportFormat.equals("fla")) {
                        exfile.exportFla(outDir.getAbsolutePath(), inFile.getName(), applicationName, applicationVerName, version, (Boolean) Configuration.getConfig("paralelSpeedUp", Boolean.TRUE));
                        exportOK = true;
                    } else if (exportFormat.equals("xfl")) {
                        exfile.exportXfl(outDir.getAbsolutePath(), inFile.getName(), applicationName, applicationVerName, version, (Boolean) Configuration.getConfig("paralelSpeedUp", Boolean.TRUE));
                        exportOK = true;
                    } else {
                        exportOK = false;
                    }
                } catch (Exception ex) {
                    exportOK = false;
                    System.err.print("FAIL: Exporting Failed on Exception - ");
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    System.exit(1);
                }
                if (exportOK) {
                    System.out.println("OK");
                    System.exit(0);
                } else {
                    System.err.println("FAIL");
                    System.exit(1);
                }
            } else if (args[pos].equals("-compress")) {
                if (args.length < pos + 3) {
                    badArguments();
                }

                if (SWF.fws2cws(new FileInputStream(args[pos + 1]), new FileOutputStream(args[pos + 2]))) {
                    System.out.println("OK");
                } else {
                    System.err.println("FAIL");
                }
            } else if (args[pos].equals("-decompress")) {
                if (args.length < pos + 3) {
                    badArguments();
                }

                if (SWF.decompress(new FileInputStream(args[pos + 1]), new FileOutputStream(args[pos + 2]))) {
                    System.out.println("OK");
                    System.exit(0);
                } else {
                    System.err.println("FAIL");
                    System.exit(1);
                }
            } else if (args[pos].equals("-dumpSWF")) {
                if (args.length < pos + 2) {
                    badArguments();
                }
                try {
                    Configuration.dump_tags = true;
                    SWF swf = parseSWF(args[pos + 1]);
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    System.exit(1);
                }
                System.exit(0);
            } else if (args[pos].equals("-help") || args[pos].equals("--help") || args[pos].equals("/?")) {
                printHeader();
                printCmdLineUsage();
                System.exit(0);
            } else if (args.length == pos + 1) {
                autoCheckForUpdates();
                openFile(args[pos]);
            } else {
                badArguments();
            }
        }
    }

    public static String tempFile(String url) {
        File f = new File(getFFDecHome() + "saved" + File.separator);
        if (!f.exists()) {
            f.mkdirs();
        }
        return getFFDecHome() + "saved" + File.separator + "asdec_" + Integer.toHexString(url.hashCode()) + ".tmp";

    }

    public static void removeTrayIcon() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            if (trayIcon != null) {
                tray.remove(trayIcon);
                trayIcon = null;
            }
        }
    }

    public static void switchProxy() {
        proxyFrame.switchState();
        if (stopMenuItem != null) {
            if (proxyFrame.isRunning()) {
                stopMenuItem.setLabel("Stop proxy");
            } else {
                stopMenuItem.setLabel("Start proxy");
            }
        }
    }

    public static void addTrayIcon() {
        if (trayIcon != null) {
            return;
        }
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            trayIcon = new TrayIcon(View.loadImage("proxy16"), vendor + " " + shortApplicationName + " Proxy");
            trayIcon.setImageAutoSize(true);
            PopupMenu trayPopup = new PopupMenu();


            ActionListener trayListener = new ActionListener() {
                /**
                 * Invoked when an action occurs.
                 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("EXIT")) {
                        Main.exit();
                    }
                    if (e.getActionCommand().equals("SHOW")) {
                        Main.showProxy();
                    }
                    if (e.getActionCommand().equals("SWITCH")) {
                        Main.switchProxy();
                    }
                }
            };


            MenuItem showMenuItem = new MenuItem("Show proxy");
            showMenuItem.setActionCommand("SHOW");
            showMenuItem.addActionListener(trayListener);
            trayPopup.add(showMenuItem);
            stopMenuItem = new MenuItem("Start proxy");
            stopMenuItem.setActionCommand("SWITCH");
            stopMenuItem.addActionListener(trayListener);
            trayPopup.add(stopMenuItem);
            trayPopup.addSeparator();
            MenuItem exitMenuItem = new MenuItem("Exit");
            exitMenuItem.setActionCommand("EXIT");
            exitMenuItem.addActionListener(trayListener);
            trayPopup.add(exitMenuItem);

            trayIcon.setPopupMenu(trayPopup);
            trayIcon.addMouseListener(new MouseAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        Main.showProxy();
                    }
                }
            });
            try {
                tray.add(trayIcon);
            } catch (AWTException ex) {
            }
        }
    }

    public static void exit() {
        Configuration.saveToFile(getConfigFile(), getReplacementsFile());
        FlashPlayerPanel.unload();
        System.exit(0);
    }

    public static void about() {
        (new AboutDialog()).setVisible(true);
    }

    public static void autoCheckForUpdates() {
        Calendar lastUpdatesCheckDate = (Calendar) Configuration.getConfig("lastUpdatesCheckDate", null);
        if ((lastUpdatesCheckDate == null) || (lastUpdatesCheckDate.getTime().getTime() < Calendar.getInstance().getTime().getTime() - 1000 * 60 * 60 * 24)) {
            checkForUpdates();
        }
    }

    public static boolean checkForUpdates() {
        try {
            Socket sock = new Socket("www.free-decompiler.com", 80);
            OutputStream os = sock.getOutputStream();
            os.write(("GET /flash/update.html?action=check&currentVersion=" + version + " HTTP/1.1\r\nHost: www.free-decompiler.com\r\nUser-Agent: " + shortApplicationVerName + "\r\nConnection: close\r\n\r\n").getBytes());
            BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String s;
            boolean start = false;
            java.util.List<Version> versions = new ArrayList<>();
            String header = "";
            Pattern headerPat = Pattern.compile("\\[([a-zA-Z0-9]+)\\]");
            int updateMajor = 0;
            int updateMinor = 0;
            Version ver = null;
            while ((s = br.readLine()) != null) {
                if (start) {
                    Matcher m = headerPat.matcher(s);
                    if (m.matches()) {
                        header = m.group(1);
                        if (header.equals("version")) {
                            ver = new Version();
                            versions.add(ver);
                        }
                        if (header.equals("noversion")) {
                            break;
                        }
                    } else {
                        if (s.contains("=")) {
                            String key = s.substring(0, s.indexOf("="));
                            String val = s.substring(s.indexOf("=") + 1);
                            if ("updateSystem".equals(header)) {
                                if (key.equals("majorVersion")) {
                                    updateMajor = Integer.parseInt(val);
                                    if (updateMajor > UPDATE_SYSTEM_MAJOR) {
                                        break;
                                    }
                                }
                                if (key.equals("minorVersion")) {
                                    updateMinor = Integer.parseInt(val);
                                }
                            }
                            if ("version".equals(header) && (ver != null)) {
                                if (key.equals("versionId")) {
                                    ver.versionId = Integer.parseInt(val);
                                }
                                if (key.equals("versionName")) {
                                    ver.versionName = val;
                                }
                                if (key.equals("longVersionName")) {
                                    ver.longVersionName = val;
                                }
                                if (key.equals("releaseDate")) {
                                    ver.releaseDate = val;
                                }
                                if (key.equals("appName")) {
                                    ver.appName = val;
                                }
                                if (key.equals("appFullName")) {
                                    ver.appFullName = val;
                                }
                                if (key.equals("updateLink")) {
                                    ver.updateLink = val;
                                }
                                if (key.equals("change[]")) {
                                    String changeType = val.substring(0, val.indexOf("|"));
                                    String change = val.substring(val.indexOf("|") + 1);
                                    if (!ver.changes.containsKey(changeType)) {
                                        ver.changes.put(changeType, new ArrayList<String>());
                                    }
                                    java.util.List<String> chlist = ver.changes.get(changeType);
                                    chlist.add(change);
                                }
                            }
                        }
                    }
                }
                if (s.equals("")) {
                    start = true;
                }
            }

            if (!versions.isEmpty()) {
                NewVersionDialog newVersionDialog = new NewVersionDialog(versions);
                newVersionDialog.setVisible(true);
                Configuration.setConfig("lastUpdatesCheckDate", Calendar.getInstance());
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
        Configuration.setConfig("lastUpdatesCheckDate", Calendar.getInstance());
        return false;
    }

    public static void initLogging(boolean debug) {
        try {
            Logger logger = Logger.getLogger("");
            logger.setLevel(debug ? Level.CONFIG : Level.WARNING);
            FileHandler fileTxt = new FileHandler(getFFDecHome() + File.separator + "log.txt");

            SimpleFormatter formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);

            if (debug) {
                ConsoleHandler conHan = new ConsoleHandler();
                conHan.setFormatter(formatterTxt);
                logger.addHandler(conHan);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Problems with creating the log files");
        }
    }
    private static final String CONFIG_NAME = "config.bin";
    private static final String REPLACEMENTS_NAME = "replacements.cfg";
    private static final File unspecifiedFile = new File("unspecified");
    private static File directory = unspecifiedFile;

    private enum OSId {

        WINDOWS, OSX, UNIX
    }

    private static OSId getOSId() {
        PrivilegedAction<String> doGetOSName = new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("os.name");
            }
        };
        OSId id = OSId.UNIX;
        String osName = AccessController.doPrivileged(doGetOSName);
        if (osName != null) {
            if (osName.toLowerCase().startsWith("mac os x")) {
                id = OSId.OSX;
            } else if (osName.contains("Windows")) {
                id = OSId.WINDOWS;
            }
        }
        return id;
    }

    public static String getFFDecHome() {
        if (directory == unspecifiedFile) {
            directory = null;
            String userHome = null;
            try {
                userHome = System.getProperty("user.home");
            } catch (SecurityException ignore) {
            }
            if (userHome != null) {
                String applicationId = Main.shortApplicationName;
                OSId osId = getOSId();
                if (osId == OSId.WINDOWS) {
                    File appDataDir = null;
                    try {
                        String appDataEV = System.getenv("APPDATA");
                        if ((appDataEV != null) && (appDataEV.length() > 0)) {
                            appDataDir = new File(appDataEV);
                        }
                    } catch (SecurityException ignore) {
                    }
                    String vendorId = Main.vendor;
                    if ((appDataDir != null) && appDataDir.isDirectory()) {
                        // ${APPDATA}\{vendorId}\${applicationId}
                        String path = vendorId + "\\" + applicationId + "\\";
                        directory = new File(appDataDir, path);
                    } else {
                        // ${userHome}\Application Data\${vendorId}\${applicationId}
                        String path = "Application Data\\" + vendorId + "\\" + applicationId + "\\";
                        directory = new File(userHome, path);
                    }
                } else if (osId == OSId.OSX) {
                    // ${userHome}/Library/Application Support/${applicationId}
                    String path = "Library/Application Support/" + applicationId + "/";
                    directory = new File(userHome, path);
                } else {
                    // ${userHome}/.${applicationId}/
                    String path = "." + applicationId + "/";
                    directory = new File(userHome, path);
                }
            }
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String ret = directory.getAbsolutePath();
        if (!ret.endsWith(File.separator)) {
            ret += File.separator;
        }
        return ret;
    }

    private static String getReplacementsFile() {
        return getFFDecHome() + REPLACEMENTS_NAME;
    }

    private static String getConfigFile() {
        return getFFDecHome() + CONFIG_NAME;
    }
}