package dloader;

import dloader.page.AbstractPage;

/**
 * Class for progress tracking of page being processed 
 * @author A.Cerbic
 */
class PageJob {
	enum JobStatusEnum { RECON_PAGE, DOWNLOAD_PAGE, 
		ADD_CHILDREN_JOBS, SAVE_RESULTS, PAGE_DONE, PAGE_FAILED };
		
	// the page is BOUND to this job object (1-to-1) and 
	//	this should not change during execution;
	final AbstractPage page;
	PageJob.JobStatusEnum status;
	String saveTo;
	
	// flags on how page was processed 
	boolean isReadFromCache = false;
	boolean isReadFromWeb = false;
	String saveResultsReport = null;

	final static int MAX_RETRIES = 3;
	int retryCount;
	
	PageJob (String _saveTo, AbstractPage _page, PageJob.JobStatusEnum _status) {
		assert (_saveTo != null);
		assert (_page != null);
		page = _page; saveTo = _saveTo; status = _status;
		retryCount = MAX_RETRIES;
	}
}