package dloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import dloader.PageJob.JobStatusEnum;
import dloader.page.*;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;

/**
 * This class handles multiple PageJob objects and general algorithm of the program
 * @author A.Cerbic
 */

public class PageProcessor {
	
	/**
	 * Dynamically growing list of PageJob objects;  
	 */
	static final List<PageJob> jobQ;
	/**
	 * Holds reference to a cache, if one is present (null otherwise)
	 */
	static XMLCache cache;
	/**
	 * Holds reference to a logger, if one is present (null otherwise)
	 */
	static Logger logger;
	
	/**
	 *  true if PageProcessor reads and writes to cache. 
	 *  false if only writes (when cache is not null)
	 */
	static boolean isReadingCache;
	
	/**
	 * priorities system to select next job from the Q
	 */
	static final
	Map<PageJob.JobStatusEnum, Integer> priorities;
	/**
	 * value for the top priority
	 */
	static final
	int PRIORITIES_MIN = 0;
	/**
	 * minimum priority value for job statuses that don't get executed 
	 */
	static final
	int PRIORITIES_NOWORK = 100;
	/**
	 * minimum value for "heavy"-duty priorities (involving long operations)
	 */
	static final
	int PRIORITIES_HEAVY = 4;	
	
	static {
		priorities = new Hashtable<>();
		jobQ = new LinkedList<PageJob>();
	}
	
	static public
	List<PageJob> getJobQ() {
		return jobQ;
	}

	static private
	void initPriorities(boolean structured) {
		priorities.clear();
		if (structured) {
			/**
			 * structured job order for console application 
			 * (1st finish THIS item then touch next)
			 */
			priorities.put(JobStatusEnum.RECON_PAGE, 0);
			priorities.put(JobStatusEnum.DOWNLOAD_PAGE, 1);
			priorities.put(JobStatusEnum.ADD_CHILDREN_JOBS, 2);
			priorities.put(JobStatusEnum.PRESAVE_CHECK, 3);
			priorities.put(JobStatusEnum.SAVE_RESULTS, 4);
			priorities.put(JobStatusEnum.PAGE_DONE, 100);
			priorities.put(JobStatusEnum.PAGE_FAILED, 100);		
		} else {
			/**
			 * fast-preview job order for GUI application
			 */
			priorities.put(JobStatusEnum.ADD_CHILDREN_JOBS, 0);
			priorities.put(JobStatusEnum.RECON_PAGE, 1);
			priorities.put(JobStatusEnum.PRESAVE_CHECK, 2);
			priorities.put(JobStatusEnum.DOWNLOAD_PAGE, 3);
			priorities.put(JobStatusEnum.SAVE_RESULTS, 4);
			priorities.put(JobStatusEnum.PAGE_DONE, 100);
			priorities.put(JobStatusEnum.PAGE_FAILED, 100);		
		}
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
		
		if ((saveTo != null) && (baseURL != null))
			addJob (saveTo, detectPage(baseURL), JobStatusEnum.DOWNLOAD_PAGE);
		
		logger = l;
		
		PageProcessor.isReadingCache = isReadingCache;
		try {
			PageProcessor.cache = new XMLCache(cacheFilename);
		} catch (IllegalArgumentException e) {
			if (logger != null) logger.log(Level.WARNING, "", e);
			PageProcessor.isReadingCache = false;
			// and follow with cache set to null
		}
		
		initPriorities(isStructuredJobPriorities);
	}
	
	static void saveCache () throws IOException {
		cache.saveCache();
	}	

	/**
	 * adds job to the shared queue
	 * @param j - the job
	 */
	static void addJob (PageJob j) {
		assert (j != null);
		getJobQ().add(0,j); // XXX: check for atomicity
	}
	
	/**
	 * creates new job and adds it to the shared queue
	 * @param saveTo - directory in which this page results will be saved
	 * @param page - the page bound to this job
	 * @param status - status of a job
	 */
	static void addJob (String saveTo, AbstractPage page, JobStatusEnum status) {
		assert (saveTo != null);
		assert (page != null);
		synchronized (getJobQ()) {
			PageJob job = getJobForPage(page);
			if (job == null)
				job = new PageJob(saveTo,page,status);
			else if (!job.saveTo.equals(saveTo)) {
				// this weird situation normally should not happen, but who knows...
				getJobQ().remove(job);
				job = new PageJob(saveTo,page,status); // restart job with new location
			} else {
				// Hmmm... the this EXACT page item is already in a Q.
				job.status = status;
			}
			getJobQ().add(0,job);
		}
	}
	
	/**
	 * Detects page type by its URL address (String)
	 * @param baseURL - String representation of URL
	 * @return new PageParser descendant fitting for the page
	 * @throws IllegalArgumentException - when baseURL is bad or null
	 */
	static final public
	AbstractPage detectPage(String baseURL) throws IllegalArgumentException {
		URL u;
		try {
			u = new URL(baseURL);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		
		if (baseURL.contains("/track/")) 
			return new Track(baseURL);
		if (baseURL.contains("/album/")) 
			return new Album(baseURL);
		if (u.getPath().isEmpty() || u.getPath().equals("/"))
			return new Discography(baseURL);
		
		throw new IllegalArgumentException();
	}

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
				synchronized (job) {
					if (job.status == JobStatusEnum.DOWNLOAD_PAGE ||
						job.status == JobStatusEnum.SAVE_RESULTS) {
						if ( --job.retryCount <= 0) {
							if (logger != null)
								logger.log(Level.WARNING, "[Failed] " + job.toString(), e);
							job.status = JobStatusEnum.PAGE_FAILED; 
						}
						addJob(job);					
					}
				}
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
		Integer priority = PRIORITIES_NOWORK; PageJob nextJob = null;
		synchronized (getJobQ()) {
			for (PageJob job: getJobQ()) {
				if (priorities.get(job.status) < priority) {
					priority = priorities.get(job.status);
					nextJob = job;
					if (priority == PRIORITIES_MIN) return nextJob; // cancel search for top priority found;
				}
			}
		}
		
		if (lightWeight && priority >= PRIORITIES_HEAVY)
			return null;
		else return nextJob;
	}

	/**
	 * logs results of acquiring metadata (from cache or web)
	 * @param job - the job to report about
	 */
	static void logInfoSurvey(PageJob job) {
		AbstractPage page = job.page;
		if (logger == null) return;
		String method = "failed";
		String log_message = null;
		synchronized (job) {
			if (job.isReadFromWeb) method = "web";
			else if (job.isReadFromCache) method = "cache";
			int childPagesNum = (page.getChildPages() != null)? page.getChildPages().length: 0;
			log_message = String.format("%s (%s): <%s>%s%n",
					page.getClass().getSimpleName(),
					method,
					page.url.toString(),
					(childPagesNum > 0)?
						String.format(" [%s] children", childPagesNum): "");
			while (page.getParent() != null) {
				log_message = "\t"+log_message;
				page = page.getParent();
			}
		}
		logger.info(log_message);
	}
	
	/**
	 * logs results of saving page's data to disk 
	 * @param job - the job that was saved, contains saveResultsReport field
	 */
	static void logDataSave(PageJob job) {
		if (logger == null) return;
		String result = job.saveResultsReport;
		AbstractPage page = job.page;
		if (result == null || result.isEmpty())	return;
		String log_message = String.format("%s \"%s\" %s%n",
				page.getClass().getSimpleName(),
				page.getTitle().toString(),
				result
				);
		while (page.getParent() != null) {
			log_message = "\t"+log_message;
			page = page.getParent();
		}
		logger.info(log_message);
	}
	
	/**
	 * Gets one page job, does one operation on it (may fail) 
	 * and puts job (and/or possibly new jobs) into a queue for further processing
	 */
	static
	void processOnePage(PageJob job) throws ProblemsReadingDocumentException, IOException {
		AbstractPage p = job.page;
		assert (p != null);
		switch (job.status) {
			case RECON_PAGE: 
				if ((isReadingCache && job.isReadFromCache) || job.isReadFromWeb)
					// this page (and its children is already processed - stop refresh here) 
					break;
				
				if (isReadingCache && cache != null) 
					job.isReadFromCache = p.loadFromCache(cache.doc);
				if (job.isReadFromCache) {
					job.status = JobStatusEnum.ADD_CHILDREN_JOBS;
					logInfoSurvey(job);
				}
				else job.status = JobStatusEnum.DOWNLOAD_PAGE;
				addJob(job); 
				break;
			case DOWNLOAD_PAGE:
				synchronized (p) {
					p.downloadPage();
					job.isReadFromWeb = true;
					
					StatisticGatherer.totalPageDownloadFinished++;
					if (cache != null)
						p.saveToCache(cache.doc);
				}
				job.status = JobStatusEnum.ADD_CHILDREN_JOBS;
				logInfoSurvey(job);
				addJob (job);
				break;
			case ADD_CHILDREN_JOBS: 
				synchronized (p) {
					AbstractPage[] childPages = job.page.getChildPages();
					if (childPages != null)
						for (int i = 0; i < childPages.length; i++) {
							AbstractPage child = childPages[i];
							String childrenSaveTo = job.page.getChildrenSaveTo(job.saveTo);
							addJob(childrenSaveTo, child, JobStatusEnum.RECON_PAGE);
						}
				}
				job.retryCount = PageJob.MAX_RETRIES; // reset retries for next faulty operation
				job.status = JobStatusEnum.PRESAVE_CHECK;
				addJob(job);
				break;
			case PRESAVE_CHECK:
				if (job.page.isSavingNotRequired(job.saveTo)) {
					logDataSave(job); 
					job.status = JobStatusEnum.PAGE_DONE;
					addJob(job);					
				} else {
					job.status = JobStatusEnum.SAVE_RESULTS;
					addJob(job);
				}
				break;
			case SAVE_RESULTS:
				job.saveResultsReport = p.saveResult(job.saveTo); 
				logDataSave(job); 
				job.status = JobStatusEnum.PAGE_DONE;
				addJob(job);					
				break; 
		}
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
