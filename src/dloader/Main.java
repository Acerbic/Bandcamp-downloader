package dloader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.*;

import javax.swing.SwingUtilities;

public class Main {
//	public static final String nl = System.getProperty ( "line.separator" );
	
	/* Default values */
	public static String baseURL = "http://homestuck.bandcamp.com";
	public static String xmlFileName = "pages_scan_cache.xml";
	public static String logFile = "download.log";
	public static boolean forceTagging = false; // if true - always tag, otherwise only new/missing tag fields
	public static boolean allowFromCache = true;
	public static boolean logToCon = true;
	public static boolean logToFile = false;
	public static boolean isInConsoleMode = false;
	// user current directory
	public static String saveTo = Paths.get("").toAbsolutePath().toString(); 

	public static Logger logger;
	
	private static void parseCommandLine(String[] args) {
		for (String s : args) {
			s = s.trim();
			if (s.length() < 2)continue;
			if ((s.charAt(0) != '-') && (s.charAt(0) != '/')) continue;
			switch (s.substring(1, 2).toLowerCase().charAt(0)) {
				case '?':
					System.out
							.print("homestuck.bandcamp.com downloader by A.Cerbic, 04.2011.\n"
									+ "\tuse these command-line parameters:\n"
									+ "\t\t-?\t to call for this help, duh!\n"
									+ "\t\t-console to launch without GUI\n"
									+ "\t\t-silent\t to run this app ninja-style. (has effect only if -console is present too)\n"
									+ "\t\t\t Beware, 500+ Mb download will leave you against blinking cursor for a while.\n"
									+ "\t\t-log\t to let the program take some notes into '"+logFile+"' file\n"
									+ "\t\t-d<URL>\t to start downloading from some URL other than default (http://homestuck.bandcamp.com)\n"
									+ "\t\t\t it _might_ even work, if you have some luck (how much luck? - ALL OF IT). \n"
									+ "\t\t\t No angle brackets in actual parameter btw.\n"
									+ "\t\t-t<Directory>\t to define where to drop shitload if MP3's you gonna obtain,\n"
									+ "\t\t\t default is right where the program is.\n"
									+ "\n"
									+ "HINT: if the program discovers that a file for given track already exists, \n"
									+ "\tthat track download will be skipped, so if previous download session was interrupted you can\n"
									+ "\tjust run the program over and it will download only new files. \n"
									+ "\t\t-forcetag\t will force tagging, otherwise this program will be keeping original ID3 tags if any \n"
									+ "\t\t\t (without it all files will be retagged, both new and 'skipped')\n"
									+ "\n"
									+ "HINT: the program tries to cache relevant data from all the pages it goes through,\n"
									+ "\tso if it finds a web page that was parsed before and cached, the page won't be downloaded.\n"
									+ "\tIf you want all pages to be re-downloaded anew - delete '"+xmlFileName+"' or use next key:\n"
									+ "\t\t-rescan\t forces all pages to be downloaded, cache gets updated.\n"
									);
					System.exit(0);
				case 's': logToCon = false; break;
				case 'l': logToFile = true; break;
				case 'd': baseURL = s.substring(2); break;
				case 't':
					Path p = Paths.get(s.substring(2)).toAbsolutePath();
					saveTo = p.toString();
					if (!Files.isDirectory(p)) {
						saveTo = null;
						System.out
								.println("-t must specify a directory. Default value will be used.");
					}
					break;
				case 'f': forceTagging = true; break;
				case 'r': allowFromCache = false; break;
				case 'c': isInConsoleMode = true; break;
			} //switch
		} //for
	}

	private static void initLogger() {
		logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.setLevel(Level.ALL);
		logger.setFilter(null);
		logger.setUseParentHandlers(false);
		Formatter fNotVerySimple = new  Formatter() {
			@Override
			public String format(LogRecord record) {
				Throwable a = record.getThrown();
				if (a == null) return String.format(record.getMessage(), record.getParameters());
				StringBuilder result = new StringBuilder(500);
				result.append(record.getLevel().toString()+": "+record.getMessage());
				result.append(a.toString()+"\n");
				for (StackTraceElement trace: a.getStackTrace())
					result.append("\tat "+trace.toString()+"\n");
				return result.toString();
			}
		};
		if (logToCon) {
			Handler hConsole = new StreamHandler(System.out,fNotVerySimple) {
				public void publish(LogRecord record) {
					super.publish(record);
					flush();
				}
			};
			hConsole.setLevel(Level.INFO); // only essentials
			logger.addHandler(hConsole);
		}
		if (logToFile) {
			try {
				Handler hFile = new FileHandler(logFile);
				hFile.setFormatter(fNotVerySimple);
				hFile.setLevel(Level.ALL);
				logger.addHandler(hFile);
			} catch (SecurityException|IOException e) {
				logger.log(Level.SEVERE, "", e);
			}
		}
		
	}

	public static void main(String[] args) {
		parseCommandLine(args);
		initLogger(); // --> logger
		try {
			
			logger.info( String.format(
					"Starting to download%n from <%s>%n into <%s> %s%n",
					baseURL, saveTo, forceTagging?"with retagging existing files.":""));
			PageProcessor.initPageProcessor(
					saveTo.toString(), baseURL, 
					logger, 
					allowFromCache, xmlFileName, 
					isInConsoleMode);
			
			 if (isInConsoleMode) {
					Thread t = new Thread() {
					@Override
					public void run() {
						try {
							PageProcessor.acquireData();
						} catch (Throwable e) {
							// clean-up
							Main.logger.log(Level.SEVERE, "", e);
						}
					}
				};
				t.start();
				t.join(); // wait till thread ends
			} else {
				// GUI section startup 
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						GUI.EventDispatchThread = Thread.currentThread();
						GUI.showGUIWindow();
					}
				});
				
				assert (GUI.EventDispatchThread != null); 
				GUI.EventDispatchThread.join(); // wait till GUI closes
			}
			
			PageProcessor.saveCache();
			
			logger.info( String.format("On total: %d files saved from net (%d bytes) + %d pages viewed%n", 
					StatisticGatherer.totalFileDownloadFinished, 
					StatisticGatherer.totalBytesDownloaded,
					StatisticGatherer.totalPageDownloadFinished 
					));
		} catch (Throwable e) {
			try {
				// an attempt to salvage metadata at least
				PageProcessor.saveCache();
			} catch (IOException e1) {
				logger.log(Level.SEVERE, "", e1);
			} finally {
				logger.log(Level.SEVERE, "", e);
			}
		}
	}
	
}
