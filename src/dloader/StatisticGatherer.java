package dloader;

/**
 * Class to hold all kind of statistical data on this program run
 * @author A.Cerbic
 */
public class StatisticGatherer {
	// FIXME: this needs MAJOR rework for proper handling of multi-threaded access

	//	public final static int MAX_LENGTH_CHECKS = 5;
	/* statistics section */
//	public static int totalLengthChecks = 0;
	public static int totalFileDownloadAttempts = 0;
	public static int totalFileDownloadFinished = 0;
	public static int totalPageDownloadFinished = 0;
	public static long totalBytesDownloaded = 0;

}
