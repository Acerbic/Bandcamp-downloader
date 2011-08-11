package dloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

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
	
	static Queue<PageJob> jobQ;
	static XMLCache cache;
	
	/**
	 *  true if this instance reads and writes to cache. 
	 *  false if only writes (when cache is not null)
	 */
	boolean isReadingCache; 

	static {
		jobQ = new LinkedList<PageJob>();
	}
	
	Queue<PageJob> getJobQ() {
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
	
	static void saveCache () throws IOException {
		cache.saveCache();
	}	
	
	static void addJob (PageJob j) {
		jobQ.add(j);
	}
	static void addJob (File saveTo, AbstractPage page, PageJob.JobStatusEnum status) {
		PageJob j = new PageJob();
		j.page = page; j.saveTo = saveTo; j.status = status;
		jobQ.add(j);
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
			processOnePage();
		}
		
	}
	
	void processOnePage() throws ProblemsReadingDocumentException, IOException {
		
		PageJob job = jobQ.remove();
		switch (job.status) {
			case RECON_PAGE: 
				boolean isLoaded = false;
				if (isReadingCache && cache != null) 
					isLoaded = job.page.loadFromCache(cache.doc);
				if (isLoaded)
					job.status = PageJob.JobStatusEnum.ADD_CHILDREN_JOBS;
				else job.status = PageJob.JobStatusEnum.DOWNLOAD_PAGE;
				addJob(job); job = null;
				break;
			case DOWNLOAD_PAGE:
				job.page.downloadPage();
				job.page.saveToCache(cache.doc);
				job.status = PageJob.JobStatusEnum.ADD_CHILDREN_JOBS;
				addJob (job);
				break;
			case ADD_CHILDREN_JOBS: 
				for (int i = 0; i < job.page.childPages.length; i++) {
					AbstractPage child = job.page.childPages[i];
					File childrenSaveTo = job.page.getChildrenSaveTo(job.saveTo);
					addJob(childrenSaveTo, child, PageJob.JobStatusEnum.RECON_PAGE);
				}
				break;
			case SAVE_RESULTS:
				job.page.saveResult(job.saveTo);
				break;
		}
	}
}
