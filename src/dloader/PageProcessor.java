package dloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import dloader.AbstractPage.ProblemsReadingDocumentException;

/**
 * This class handles multiple pages downloads and general algorithm of the job
 * @author A.Cerbic
 */
public class PageProcessor {
	
	/**
	 * Class for progress tracking of page being processed 
	 * @author A.Cerbic
	 */
	static class PageJob {
		enum JobStatusEnum { RECON_PAGE, DOWNLOAD_PAGE, 
			ADD_CHILDREN_JOBS, SAVE_RESULTS };
		AbstractPage page;
		JobStatusEnum status;
		String saveTo;
		final static int MAX_RETRIES = 3;
		int retryCount;
		
		PageJob (String _saveTo, AbstractPage _page, PageJob.JobStatusEnum _status) {
			assert (_saveTo != null);
			assert (_page != null);
			page = _page; saveTo = _saveTo; status = _status;
			retryCount = MAX_RETRIES;
		}
	}
	
	// shared among parallel PageProcessor instances
	static List<PageJob> jobQ;
	static XMLCache cache;
	static Logger logger;
	
	/**
	 *  true if this instance reads and writes to cache. 
	 *  false if only writes (when cache is not null)
	 */
	boolean isReadingCache; 

	static {
		jobQ = new LinkedList<PageJob>();
	}
	
	static List<PageJob> getJobQ() {
		return jobQ;
	}

	/**
	 * Called for root page; this page will be added to Q and always downloaded;
	 * @param saveTo - master directory in which this page results will be saved
	 * @param baseURL - the initial page
	 */
	PageProcessor(String saveTo, String baseURL, boolean _isReadingCache) {
		addJob (saveTo, detectPage(baseURL), PageJob.JobStatusEnum.DOWNLOAD_PAGE);
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
		jobQ.add(0,j);
	}
	/**
	 * creates new job and adds it to the shared queue
	 * @param saveTo - directory in which this page results will be saved
	 * @param page
	 * @param status
	 */
	static void addJob (String saveTo, AbstractPage page, PageJob.JobStatusEnum status) {
		PageJob j = new PageJob(saveTo,page,status);
		jobQ.add(0,j);
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
		while (!jobQ.isEmpty()) { //TODO convert this into parallel tasks
			PageJob job = jobQ.remove(0); 
			processOnePage(job);
			// TODO ???move retry facility over here by throwing exception from processOnePage
		}
	}  

	/**
	 * logs results of acquiring metadata (from cache or web)
	 * @param method - which way the data is acquired
	 * @param page - the page the data was read to
	 */
	void logInfoSurvey(String method, AbstractPage page) {
		if (logger == null) return;
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
	 * @param result - reported by AbstractPage.saveResult(...);
	 * @param page - the page that was saved, contains statusReport field
	 */
	void logDataSave(boolean result, AbstractPage page) {
		if (logger == null) return;
		if (page.statusReport == null || page.statusReport.isEmpty())
			return;
		String log_message = String.format("%s \"%s\" %s%n",
				page.getClass().getSimpleName(),
				page.title.toString(),
				page.statusReport
				);
		while (page.parent != null) {
			log_message = "\t"+log_message;
			page = page.parent;
		}
		logger.info(log_message);
	}
	
	/**
	 * Gets one page from the top of a q, reads it from cache or downloads and parses web page.
	 */
	void processOnePage(PageJob job) throws ProblemsReadingDocumentException, IOException {
		AbstractPage p = job.page;
		assert (p != null);
		switch (job.status) {
			case RECON_PAGE: 
				boolean isLoaded = false;
				if (isReadingCache && cache != null) 
					isLoaded = p.loadFromCache(cache.doc);
				if (isLoaded) {
					job.status = PageJob.JobStatusEnum.ADD_CHILDREN_JOBS;
					logInfoSurvey("cache", p);
				}
				else job.status = PageJob.JobStatusEnum.DOWNLOAD_PAGE;
				addJob(job); job = null;
				break;
			case DOWNLOAD_PAGE:
				try {
					p.downloadPage();
				} catch (ProblemsReadingDocumentException e) {
					if (--job.retryCount > 0)
						addJob (job); // retry
					else 
						// log fail
						; // TODO: add to failed list
					break; //consume this fail and go on to next item in jobQ
				}
				StatisticGatherer.totalPageDownloadFinished++;
				if (cache != null)
					p.saveToCache(cache.doc);
				job.status = PageJob.JobStatusEnum.ADD_CHILDREN_JOBS;
				logInfoSurvey("web", p);
				addJob (job);
				break;
			case ADD_CHILDREN_JOBS: 
				if (job.page.childPages != null)
					for (int i = 0; i < job.page.childPages.length; i++) {
						AbstractPage child = job.page.childPages[i];
						String childrenSaveTo = job.page.getChildrenSaveTo(job.saveTo);
						addJob(childrenSaveTo, child, PageJob.JobStatusEnum.RECON_PAGE);
					}
				job.status = PageJob.JobStatusEnum.SAVE_RESULTS;
				addJob(job);
				break;
			case SAVE_RESULTS:
				try {
					boolean saveNotSkipped = p.saveResult(job.saveTo);
					logDataSave(saveNotSkipped, p); 
					break;
				} catch (IOException e) {
					if (--job.retryCount > 0)
						addJob (job); // retry
					else 
						// log fail
						; // TODO: add to failed list
					break; //consume this fail and go on to next item in jobQ
				}
		}
	}
}
