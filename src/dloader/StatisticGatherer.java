package dloader;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class to hold all kind of statistical data on this program run
 * @author A.Cerbic
 */
public class StatisticGatherer {

	//	public final static int MAX_LENGTH_CHECKS = 5;
	/* statistics section */
//	public static int totalLengthChecks = 0;
	public static AtomicInteger totalFileDownloadAttempts = new AtomicInteger(0);
	public static AtomicInteger totalFileDownloadFinished = new AtomicInteger(0);
	public static AtomicInteger totalPageDownloadFinished = new AtomicInteger(0);
	public static AtomicLong totalBytesDownloaded = new AtomicLong(0);

}
