package dloader;

import java.io.File;
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
	
	static class PageJob {
		enum JobStatusEnum { RECON_PAGE, DOWNLOAD_PAGE, 
			ADD_CHILDREN_JOBS, SAVE_RESULTS };
		AbstractPage page;
		JobStatusEnum status;
		File saveTo;
	}
	
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
	
	List<PageJob> getJobQ() {
		return jobQ;
	}

	/**
	 * Called for root page; this page will be added to Q and always downloaded;
	 * @param saveTo
	 * @param baseURL
	 */
	PageProcessor(File saveTo, String baseURL, boolean _isReadingCache) {
		addJob (saveTo, detectPage(baseURL), PageJob.JobStatusEnum.DOWNLOAD_PAGE);
		isReadingCache = _isReadingCache;
	}
	
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
	
	static void addJob (PageJob j) {
		jobQ.add(0,j);
	}
	static void addJob (File saveTo, AbstractPage page, PageJob.JobStatusEnum status) {
		PageJob j = new PageJob();
		j.page = page; j.saveTo = saveTo; j.status = status;
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
		if (u.getPath().isEmpty())
			return new Discography(baseURL);
		
		throw new IllegalArgumentException();
	}

	/**
	 * Gets one page from the top of a q, reads it from cache or downloads and parses web page.
	 * Then repeats process for remaining
	 * @param forceDownload - true if you want ignore cache on this one, all child nodes checks are
	 * controlled with <b>isUsingCache</b> flag
	 * @param doc - XML document storing cache on pages data.  
	 * @throws ProblemsReadingDocumentException if failed. (generally it means that web server did not respond right)
	 */
	void acquireData() throws ProblemsReadingDocumentException, IOException {
		while (!jobQ.isEmpty()) { //TODO convert this into parallel tasks
			PageJob job = jobQ.remove(0); 
			processOnePage(job);
		}
		
	}
	
	void logInfoSurvey(String method, AbstractPage p) {
		if (logger == null) return;
		String log_message = String.format("%s (%s): <%s>%s%n",
				p.getClass().getSimpleName(),
				method,
				p.url.toString(),
				(p.childPages!=null && p.childPages.length>0)?
					String.format(" [%s] children", p.childPages.length): "");
		while (p.parent != null) {
			log_message = "\t"+log_message;
			p = p.parent;
		}
		logger.info(log_message);
	}
	
	void logDataSave(boolean result, AbstractPage p) {
		if (logger == null) return;
		String log_message = String.format("%s \"%s\" %s%n",
				p.getClass().getSimpleName(),
				p.title.toString(),
				(result)? "data downloaded": "skipped"
				);
		while (p.parent != null) {
			log_message = "\t"+log_message;
			p = p.parent;
		}
		logger.info(log_message);
	}
	
	/**
	 * Gets one page from the top of a q, reads it from cache or downloads and parses web page.
	 */
	void processOnePage(PageJob job) throws ProblemsReadingDocumentException, IOException {
		AbstractPage p = job.page;
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
				p.downloadPage();
				WebDownloader.totalPageDownloadFinished++;
				
				p.saveToCache(cache.doc);
				job.status = PageJob.JobStatusEnum.ADD_CHILDREN_JOBS;
				logInfoSurvey("web", p);
				addJob (job);
				break;
			case ADD_CHILDREN_JOBS: 
				if (job.page.childPages != null)
					for (int i = 0; i < job.page.childPages.length; i++) {
						AbstractPage child = job.page.childPages[i];
						File childrenSaveTo = job.page.getChildrenSaveTo(job.saveTo);
						addJob(childrenSaveTo, child, PageJob.JobStatusEnum.RECON_PAGE);
					}
				job.status = PageJob.JobStatusEnum.SAVE_RESULTS;
				addJob(job);
				break;
			case SAVE_RESULTS:
				boolean saveNotSkipped = p.saveResult(job.saveTo);
				logDataSave(saveNotSkipped, p); 
				break;
		}
	}
}
