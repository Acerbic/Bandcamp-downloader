package dloader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dloader.pagejob.PageJob;
import dloader.page.*;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;

/**
 * This class handles multiple PageJob objects and 
 * general algorithm of the program, as well as execution context
 * @author A.Cerbic
 */
public final 
class PageProcessor {
	
	/**
	 * Dynamically growing list of PageJob objects;  
	 */
	static final List<PageJob> jobQ;
	
//	/**
//	 * Holds reference to a cache, if one is present (null otherwise)
//	 */
//	private static XMLCache cache;
//	public static final XMLCache getCache() {
//		return cache;
//	}

	/**
	 * Holds reference to a logger, if one is present (null otherwise)
	 */
//	private static 
//	Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
//	public static
//	void log(Level l, String s) {
//		logger.log(l,s);
//	}
//	public static
//	void log(Level l, String s, Throwable e) {
//		logger.log(l,s,e);
//	}

	/**
	 *  true if PageProcessor reads and writes to cache. 
	 *  false if only writes (when cache is not null)
	 */
//	static boolean isReadingCache;
	
	static {
		jobQ = Collections.synchronizedList(new LinkedList<PageJob>());
	}
	
	static public
	List<PageJob> getJobQ() {
		return jobQ;
	}

	
	/**
	 * Called for root page; this page will be added to Q and always downloaded;
	 * @param saveTo - master directory in which this page results will be saved
	 * @param baseURL - the initial page
	 * @param isReadingCache - whether PageProcessor is allowed to read from cache
	 */
	static public synchronized
	void initPageProcessor(
			String saveTo, String baseURL, 
			Logger l,
			boolean isReadingCache, String cacheFilename,
			boolean isStructuredJobPriorities) {
		
//		if ((saveTo != null) && (baseURL != null))
//			addJob (saveTo, detectPage(baseURL, saveTo), JobStatusEnum.DOWNLOAD_PAGE);
		
//		logger = l;
		
//		PageProcessor.isReadingCache = isReadingCache;
//		try {
//			PageProcessor.cache = new XMLCache(cacheFilename);
//		} catch (IllegalArgumentException e) {
//			if (logger != null) logger.log(Level.WARNING, "", e);
//			PageProcessor.isReadingCache = false;
			// and follow with cache set to null
//		}
		
	}
	
//	static void saveCache () throws IOException {
//		cache.saveCache();
//	}	

	/**
	 * adds job to the shared queue
	 * @param j - the job
	 */
	public static
	void addJob (PageJob j) {
		assert (j != null);
		getJobQ().add(0,j); 
	}
	

//	/**
//	 * Detects page type by its URL address (String)
//	 * @param baseURL - String representation of URL
//	 * @return new AbstractPage descendant fitting for the page
//	 * @throws IllegalArgumentException - when baseURL is bad or null
//	 */
//	static final public
//	AbstractPage detectPage(String baseURL, String saveTo) throws IllegalArgumentException {
//		URL u;
//		try {
//			u = new URL(baseURL);
//		} catch (MalformedURLException e) {
//			throw new IllegalArgumentException(e);
//		}
//		
//		if (baseURL.contains("/track/")) 
//			return new Track(baseURL.toString(), saveTo, null);
//		if (baseURL.contains("/album/")) 
//			return new Album(baseURL.toString(), saveTo, null);
//		if (u.getPath().isEmpty() || u.getPath().equals("/"))
//			return new Discography(baseURL.toString(), saveTo, null);
//		
//		throw new IllegalArgumentException();
//	}

	/**
	 * Processes all jobs in a Q and all jobs they generate 
	 */
	static public
	void acquireData() {
		PageJob lastJob = null;
		do {
			lastJob = doSingleJob(false);
		} while ( lastJob != null );
	}
	
	/**
	 * Get one job from the Q and do it (job may re-add itself and new jobs into the Q)
	 * @return PageJob that was done (in its new status) 
	 * or null if no jobs are available;
	 */
	static public
	PageJob doSingleJob(boolean lightWeight) {
		PageJob job = null;
		synchronized (getJobQ()) {
			job = getNextJob(lightWeight);
			if (job == null) return null;
			getJobQ().remove(job);
		}
		synchronized (job) {
			try {
				processOnePage(job); // will re-add this job in a new status
			} catch (ProblemsReadingDocumentException|IOException e) {
//				synchronized (job) {
//					if (job.status == JobStatusEnum.DOWNLOAD_PAGE ||
//						job.status == JobStatusEnum.SAVE_RESULTS) {
//						if ( --job.retryCount <= 0) {
//							if (logger != null)
//								logger.log(Level.WARNING, "[Failed] " + job.toString(), e);
//							job.status = JobStatusEnum.PAGE_FAILED; 
//						}
//						addJob(job);					
//					}
//				}
			}
		}
		return job;
	}

	/**
	 * picks next job to process with the respect to priority, task profile and synchronization
	 * @return the job for this PageProcessor or null if there are no jobs fitting profile 
	 */
	static
	public PageJob getNextJob(boolean lightWeight) {
//		Integer priority = PRIORITIES_NOWORK; PageJob nextJob = null;
//		synchronized (getJobQ()) {
//			for (PageJob job: getJobQ()) {
//				if (priorities.get(job.status) < priority) {
//					priority = priorities.get(job.status);
//					nextJob = job;
//					if (priority == PRIORITIES_MIN) return nextJob; // cancel search for top priority found;
//				}
//			}
//		}
		
//		if (lightWeight && priority >= PRIORITIES_HEAVY)
			return null;
//		else return nextJob;
	}

	/**
	 * logs results of acquiring metadata (from cache or web)
	 * @param job - the job to report about
	 */
	static void logInfoSurvey(PageJob job) {
//		AbstractPage page = job.page;
//		if (logger == null) return;
//		String method = "failed";
//		String log_message = null;
//		synchronized (job) {
//			if (job.isReadFromWeb) method = "web";
//			else if (job.isReadFromCache) method = "cache";
//			int childPagesNum = page.childPages.size();
//			log_message = String.format("%s (%s): <%s>%s%n",
//					page.getClass().getSimpleName(),
//					method,
//					page.url.toString(),
//					(childPagesNum > 0)?
//						String.format(" [%s] children", childPagesNum): "");
//			while (page.getParent() != null) {
//				log_message = "\t"+log_message;
//				page = page.getParent();
//			}
//		}
//		logger.info(log_message);
	}
	
	/**
	 * logs results of saving page's data to disk 
	 * @param job - the job that was saved, contains saveResultsReport field
	 */
	static void logDataSave(PageJob job) {
//		if (logger == null) return;
//		String result = job.saveResultsReport;
//		AbstractPage page = job.page;
//		if (result == null || result.isEmpty())	return;
//		String log_message = String.format("%s \"%s\" %s%n",
//				page.getClass().getSimpleName(),
//				page.getTitle().toString(),
//				result
//				);
//		while (page.getParent() != null) {
//			log_message = "\t"+log_message;
//			page = page.getParent();
//		}
//		logger.info(log_message);
	}
	
	/**
	 * Gets one page job, does one operation on it (may fail) 
	 * and puts job (and/or possibly new jobs) into a queue for further processing
	 */
	static
	void processOnePage(PageJob job) throws ProblemsReadingDocumentException, IOException {
//		AbstractPage page = job.page;
//		switch (job.status) {
//			case RECON_PAGE: 
//				if ((isReadingCache && job.isReadFromCache) || job.isReadFromWeb)
//					// this page (and its children is already processed - stop refresh here) 
//					break;
//				
//				if (isReadingCache && cache != null) 
////					job.isReadFromCache = page.loadFromCache(cache.doc);
//				if (job.isReadFromCache) {
//					job.status = JobStatusEnum.ADD_CHILDREN_JOBS;
//					logInfoSurvey(job);
//				}
//				else job.status = JobStatusEnum.DOWNLOAD_PAGE;
//				addJob(job); 
//				break;
//			case DOWNLOAD_PAGE:
//				synchronized (page) {
//					page.downloadPage(new AtomicInteger(0));
//					job.isReadFromWeb = true;
//					
//					StatisticGatherer.totalPageDownloadFinished.incrementAndGet();
//					if (cache != null)
//						page.saveToCache();
//				}
//				job.status = JobStatusEnum.ADD_CHILDREN_JOBS;
//				logInfoSurvey(job);
//				addJob (job);
//				break;
//			case ADD_CHILDREN_JOBS: 
//				for (AbstractPage child: page.childPages) {
//					String childrenSaveTo = page.getChildrenSaveTo();
//					addJob(childrenSaveTo, child, JobStatusEnum.RECON_PAGE);
//				}
//				job.retryCount = PageJob.MAX_RETRIES; // reset retries for next faulty operation
//				job.status = JobStatusEnum.PRESAVE_CHECK;
//				addJob(job);
//				break;
//			case PRESAVE_CHECK:
//				if (job.page.isSavingNotRequired()) {
//					logDataSave(job); 
//					job.status = JobStatusEnum.PAGE_DONE;
//					addJob(job);					
//				} else {
//					job.status = JobStatusEnum.SAVE_RESULTS;
//					addJob(job);
//				}
//				break;
//			case SAVE_RESULTS:
//				job.saveResultsReport = page.saveResult(new AtomicInteger(0)); 
//				logDataSave(job); 
//				job.status = JobStatusEnum.PAGE_DONE;
//				addJob(job);					
//				break; 
//		}
	}

	/**
	 * Return the PageJob object for specific page (1-to-1 relationship)
	 * only this snapshot of job q is scanned! So, jobs that are temporary removed from q 
	 * (or not added yet) can fall off the grid. 
	 * @param page - the page to look for
	 * @return PageJob object for that page or null if not found or null argument
	 */
	public static  
	PageJob getJobForPage(AbstractPage page) {
		if (page == null) return null;
		
		synchronized (getJobQ()) {
			// XXX: ??? Change  == to URL equals compare?
			for (PageJob element: getJobQ()) 
				if (element.page == page)
					return element;
		}
		
		return null;
	}
}
