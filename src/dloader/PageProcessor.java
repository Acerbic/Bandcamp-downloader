package dloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import dloader.PageJob.JobStatusEnum;
import dloader.page.AbstractPage;
import dloader.page.Album;
import dloader.page.Discography;
import dloader.page.Track;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;

/**
 * This class handles multiple pages downloads and general algorithm of the job
 * @author A.Cerbic
 */
public class PageProcessor {
	
	// shared among parallel PageProcessor instances
	static List<PageJob> jobQ;
	static List<PageJob> jobDoneList;
	static XMLCache cache;
	static Logger logger;
	
	/**
	 *  true if this instance reads and writes to cache. 
	 *  false if only writes (when cache is not null)
	 */
	boolean isReadingCache; 

	static {
		jobQ = new LinkedList<PageJob>();
		jobDoneList = new LinkedList<PageJob>(); 
	}
	
	static List<PageJob> getJobQ() {
		return jobQ;
	}

	static List<PageJob> getJobDoneList() {
		return jobDoneList;
	}	
	/**
	 * Called for root page; this page will be added to Q and always downloaded;
	 * @param saveTo - master directory in which this page results will be saved
	 * @param baseURL - the initial page
	 */
	PageProcessor(String saveTo, String baseURL, boolean _isReadingCache) {
		addJob (saveTo, detectPage(baseURL), JobStatusEnum.DOWNLOAD_PAGE);
		isReadingCache = _isReadingCache;
	}
	
	/**
	 * This constructor is to create additional PageProcessors on existing jobQ
	 * @param _isReadingCache
	 */
	PageProcessor(boolean _isReadingCache) {
		isReadingCache = _isReadingCache;		
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
		getJobQ().add(0,j);
	}
	/**
	 * creates new job and adds it to the shared queue
	 * @param saveTo - directory in which this page results will be saved
	 * @param page
	 * @param status
	 */
	static void addJob (String saveTo, AbstractPage page, JobStatusEnum status) {
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
	 * Gets one page from the top of a q, reads it from cache or downloads and parses web page.
	 * Then repeats process for remaining
	 * @param forceDownload - true if you want ignore cache on this one, all child nodes checks are
	 * controlled with <b>isReadingCache</b> flag
	 * @param doc - XML document storing cache on pages data.  
	 * @throws ProblemsReadingDocumentException if failed. (generally it means that web server did not respond right)
	 */
	void acquireData() throws ProblemsReadingDocumentException, IOException {
		while (!getJobQ().isEmpty()) { //TODO convert this into parallel tasks
			PageJob job = getJobQ().remove(0); 
			processOnePage(job);
			// TODO ???move retry facility over here by throwing exception from processOnePage
		}
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
	 * Gets one page from the top of a queue, reads it from cache or downloads and parses web page.
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
				try {
					p.downloadPage();
					job.isReadFromWeb = true;
				} catch (ProblemsReadingDocumentException e) {
					if (--job.retryCount > 0)
						addJob (job); // retry
					else 
						// log fail
						job.status = JobStatusEnum.PAGE_DONE;
						getJobDoneList().add(job);
					break; //consume this fail and go on to next item in jobQ
				}
				
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
				job.status = JobStatusEnum.SAVE_RESULTS;
				addJob(job);
				break;
			case SAVE_RESULTS:
				try {
					job.saveResultsReport = p.saveResult(job.saveTo); 
					logDataSave(job); 
					job.status = JobStatusEnum.PAGE_DONE;
					getJobDoneList().add(job);					
					break;
				} catch (IOException e) {
					if (--job.retryCount > 0)
						addJob (job); // retry
					else 
						// log fail
						job.status = JobStatusEnum.PAGE_DONE;
						getJobDoneList().add(job);
					break; //consume this fail and go on to next item in jobQ
				}
		}
	}
}
