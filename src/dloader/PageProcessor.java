package dloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import dloader.PageJob.JobStatusEnum;
import dloader.page.*;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;

/**
 * This class handles multiple pages downloads and general algorithm of the job
 * @author A.Cerbic
 */
public class PageProcessor {
	
	// shared among parallel PageProcessor instances
	static private List<PageJob> jobQ;
	static XMLCache cache;
	static Logger logger;
	
	/**
	 *  true if this instance reads and writes to cache. 
	 *  false if only writes (when cache is not null)
	 */
	boolean isReadingCache;
	
	/**
	 * priorities system to select next job from the Q
	 */
	Map<PageJob.JobStatusEnum, Integer> priorities = new Hashtable<>();
	/**
	 * value for the top priority
	 */
	int priorities_MIN;
	/**
	 * minimum priority value for job statuses that don't get executed 
	 */
	int priorities_NOWORK;
	/**
	 * minimum value for "heavy"-duty priorities (involving long operations)
	 */
	int priorities_HEAVY;	
	
	static {
		jobQ = new LinkedList<PageJob>();
	}
	
	static List<PageJob> getJobQ() {
		return jobQ;
	}

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
			priorities_MIN = 0;
			priorities_NOWORK = 100;
			priorities_HEAVY = 3;
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
			priorities_MIN = 0;
			priorities_NOWORK = 100;
			priorities_HEAVY = 3;			
		}
		
	}
	/**
	 * Called for root page; this page will be added to Q and always downloaded;
	 * @param saveTo - master directory in which this page results will be saved
	 * @param baseURL - the initial page
	 * @param isReadingCache - whether this PageProcessor is allowed to read from cache
	 */
	PageProcessor(String saveTo, String baseURL, boolean isReadingCache) {
		if ((saveTo != null) && (baseURL != null))
			addJob (saveTo, detectPage(baseURL), JobStatusEnum.DOWNLOAD_PAGE);
		this.isReadingCache = isReadingCache;
		// XXX: parameter into constructor??
		initPriorities(true);
	}
	
	static void initCache (String filename) {
		cache = new XMLCache(filename);
	}
	
	static void initLogger(Logger l) {
		logger = l;
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
		getJobQ().add(0,j);
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
		PageJob j = new PageJob(saveTo,page,status);
		getJobQ().add(0,j);
	}
	
	/**
	 * Detects page type by its URL address (String)
	 * @param baseURL - String representation of URL
	 * @return new PageParser descendant fitting for the page
	 * @throws IllegalArgumentException - when baseURL is bad or null
	 */
	static final AbstractPage detectPage(String baseURL) throws IllegalArgumentException {
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
	void acquireData() {
		do {
			doSingleJob(false);
		} while ( hasMoreJobs(false) );
	}
	
	/**
	 * Get one job from the Q and do it (job may re-add itself and new jobs into the Q)
	 * @return PageJob that was done (in its new status) 
	 * or null if no jobs are available;
	 */
	PageJob doSingleJob(boolean lightWeight) {
		PageJob job = getNextJob(lightWeight);
		if (job == null) return null;
		try {
			processOnePage(job);
		} catch (ProblemsReadingDocumentException|IOException e) {
			if (job.status == JobStatusEnum.DOWNLOAD_PAGE ||
				job.status == JobStatusEnum.SAVE_RESULTS) {
				if (--job.retryCount > 0)
					addJob (job); // retry
				else 
					// ??log fail
					job.status = JobStatusEnum.PAGE_FAILED;
					addJob(job);					
			}
		}
		return job;
	}

	/**
	 * picks next job to process with the respect to priority, task profile and synchronization
	 * @return the job for this PageProcessor or null if there are no jobs fitting profile 
	 */
	public PageJob getNextJob(boolean lightWeight) {
		// XXX: check for synchronization issues
		
		Integer priority = priorities_NOWORK; PageJob nextJob = null;
		for (PageJob job: getJobQ()) {
			if (priorities.get(job.status) < priority) {
				priority = priorities.get(job.status);
				nextJob = job;
				if (priority == priorities_MIN) return nextJob; // cancel search for top priority found;
			}
		}
		
		if (lightWeight && priority >= priorities_HEAVY)
			return null;
		else return nextJob;
	}

	/**
	 * logs results of acquiring metadata (from cache or web)
	 * @param job - the job to report about
	 */
	void logInfoSurvey(PageJob job) {
		AbstractPage page = job.page;
		if (logger == null) return;
		String method = "failed";
		if (job.isReadFromWeb) method = "web";
		else if (job.isReadFromCache) method = "cache";
		String log_message = String.format("%s (%s): <%s>%s%n",
				page.getClass().getSimpleName(),
				method,
				page.url.toString(),
				(page.childPages!=null && page.childPages.length>0)?
					String.format(" [%s] children", page.childPages.length): "");
		while (page.parent != null) {
			log_message = "\t"+log_message;
			page = page.parent;
		}
		logger.info(log_message);
	}
	
	/**
	 * logs results of saving page's data to disk 
	 * @param job - the job that was saved, contains saveResultsReport field
	 */
	void logDataSave(PageJob job) {
		if (logger == null) return;
		String result = job.saveResultsReport;
		AbstractPage page = job.page;
		if (result == null || result.isEmpty())	return;
		String log_message = String.format("%s \"%s\" %s%n",
				page.getClass().getSimpleName(),
				page.getTitle().toString(),
				result
				);
		while (page.parent != null) {
			log_message = "\t"+log_message;
			page = page.parent;
		}
		logger.info(log_message);
	}
	
	/**
	 * Gets one page job, does one operation on it (may fail) 
	 * and puts job (and/or possibly new jobs) into a queue for further processing
	 */
	void processOnePage(PageJob job) throws ProblemsReadingDocumentException, IOException {
		AbstractPage p = job.page;
		assert (p != null);
		switch (job.status) {
			case RECON_PAGE: 
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
				p.downloadPage();
				job.isReadFromWeb = true;
				
				StatisticGatherer.totalPageDownloadFinished++;
				if (cache != null)
					p.saveToCache(cache.doc);
				job.status = JobStatusEnum.ADD_CHILDREN_JOBS;
				logInfoSurvey(job);
				addJob (job);
				break;
			case ADD_CHILDREN_JOBS: 
				if (job.page.childPages != null)
					for (int i = 0; i < job.page.childPages.length; i++) {
						AbstractPage child = job.page.childPages[i];
						String childrenSaveTo = job.page.getChildrenSaveTo(job.saveTo);
						addJob(childrenSaveTo, child, JobStatusEnum.RECON_PAGE);
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
	 * @param page - the page to look for
	 * @return PageJob object for that page or null if not found or null argument
	 */
	public static PageJob getJobForPage(AbstractPage page) {
		if (page == null) return null;
		// XXX: ??? Change  == to URL compare?
		for (PageJob element: getJobQ()) 
			if (element.page == page)
				return element;
		
		return null;
	}
	
	/**
	 * Seeks uncompleted jobs in a queue, ready to be done (not finished, not failed, not paused... etc.)
	 * @param fastPreview - true if in work-preview mode (looking for easy jobs)
	 * @return true if there is more work to do, false if there are no appropriate jobs
	 */
	public static boolean hasMoreJobs(boolean fastPreview) {
		// XXX: need to revisit this when pause/start/stop functionality will be in place
		for (PageJob pj: getJobQ()) 
			if ((pj.status != PageJob.JobStatusEnum.PAGE_DONE) &&
				(pj.status != PageJob.JobStatusEnum.PAGE_FAILED)) {
				if (!fastPreview)
					return true;
				if ((pj.status != PageJob.JobStatusEnum.DOWNLOAD_PAGE) &&
					(pj.status != PageJob.JobStatusEnum.SAVE_RESULTS))
					return true;
			}
		return false;
	}
}
