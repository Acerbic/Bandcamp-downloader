package dloader;

import java.net.URL;

public class StatisticGatherer {

	//	public final static int MAX_LENGTH_CHECKS = 5;
	public static int lastCheckWebLength = 0;
	public static URL lastCheckWebURL = null;
	/* statistics section */
	public static int totalLengthChecks = 0;
	public static int totalFileDownloadAttempts = 0;
	public static int totalFileDownloadFinished = 0;
	public static int totalPageDownloadFinished = 0;
	public static long totalBytesDownloaded = 0;

}
