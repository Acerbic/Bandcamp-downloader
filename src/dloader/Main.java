package dloader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.*;


public class Main {
	public static final String nl = System.getProperty ( "line.separator" );
	
	/* Default values */
	public static String baseURL = "http://homestuck.bandcamp.com";
	public static String xmlFileName = "pages_scan_cache.xml";
	public static String logFile = "download.log";
	public static boolean allowTagging = true;
	public static boolean allowFromCache = true;
	public static boolean logToCon = true;
	public static boolean logToFile = false;
	// user current directory
	public static File saveTo = new File((new File("")).getAbsolutePath()); 

	public static Logger logger;
	public static XMLCache cache;
	
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
									+ "\t\t-silent\t to run this app ninja-style. \n"
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
									+ "\t\t-noretag\t will suppress this program's tagging efforts, keeping the original ID3 tags if any \n"
									+ "\t\t\t (without it all files will be retagged, both new and 'skipped')\n"
									+ "\n"
									+ "HINT: the program tries to cache relevant data from all the pages it goes through,\n"
									+ "\tso if it finds a web page that was parsed before and cached, the page won't be downloaded.\n"
									+ "\tIf you want all pages to be re-downloaded anew - delete '"+xmlFileName+"' or use next key:\n"
									+ "\t\t-fullscan\t forces all pages to be downloaded, cache gets updated.\n"
									);
					System.exit(0);
				case 's': logToCon = false; break;
				case 'l': logToFile = true; break;
				case 'd': baseURL = s.substring(2); break;
				case 't':
					saveTo = new File(s.substring(2));
					if (saveTo.isFile()) {
						saveTo = null;
						System.out
								.println("-t must specify a directory. Default value will be used.");
					}
					break;
				case 'n': allowTagging = false; break;
				case 'f': allowFromCache = false; break;
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
//			hConsole.setLevel(Level.ALL);
			hConsole.setLevel(Level.INFO);
			logger.addHandler(hConsole);
		}
		if (logToFile) {
			try {
				Handler hFile = new StreamHandler(new FileOutputStream(logFile),fNotVerySimple) {
					public void publish(LogRecord record) {
						super.publish(record);
						flush();
					}
				};
				hFile.setLevel(Level.ALL);
				logger.addHandler(hFile);
			} catch (SecurityException e) {
				logger.log(Level.SEVERE, "", e);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "", e);
			}
			
		}
		
	}

	public static void main(String[] args) {
		parseCommandLine(args);
		initLogger(); // --> logger
		try {
			cache = new XMLCache(xmlFileName); 
			logger.info( String.format(
					"Starting to download%n from <%s>%n into <%s> with%s retagging existing files.%n",
					baseURL, saveTo, allowTagging?"":"out"));
			
			AbstractPage topElement = PageProcessor.detectPage(baseURL);
			logger.info( " -------  Survey:  -------%n");
			AbstractPage.isUsingCache = allowFromCache;
			topElement.acquireData(true, cache.doc); // always download root page.
			cache.saveCache();
			
			logger.info( " -------  Acquisition:  -------%n");
			topElement.saveResult(saveTo);
			logger.info( String.format("On total: %d files saved from net (%d bytes) + %d pages viewed%n", 
					WebDownloader.totalFileDownloadFinished, 
					WebDownloader.totalBytesDownloaded,
					WebDownloader.totalPageDownloadFinished 
					));
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "", e); 
		}
	}
	
}
