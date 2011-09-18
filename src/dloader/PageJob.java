package dloader;

import dloader.page.AbstractPage;

/**
 * Class for progress tracking of page being processed 
 * @author A.Cerbic
 */
class PageJob {
	enum JobStatusEnum { RECON_PAGE, DOWNLOAD_PAGE, 
		ADD_CHILDREN_JOBS, PRESAVE_CHECK, SAVE_RESULTS, PAGE_DONE, PAGE_FAILED;
		@Override
		public String toString() {
			switch (this) {
			case ADD_CHILDREN_JOBS: return "Add_childre_jobs";
			case DOWNLOAD_PAGE: return "Download_page";
			case PAGE_DONE: return "Page_done";
			case PAGE_FAILED: return "Page_failed";
			case PRESAVE_CHECK: return "Presave_check";
			case RECON_PAGE: return "Recon_page";
			case SAVE_RESULTS: return "Save_results";
			default: return "";
			}
		}
	};
		
	// the page is BOUND to this job object (1-to-1) and tracks page processing progress
	// during execution.
	final AbstractPage page;
	JobStatusEnum status;
	String saveTo;
	
	// flags on how page was processed 
	boolean isReadFromCache = false;
	boolean isReadFromWeb = false;
	String saveResultsReport = null;

	final static int MAX_RETRIES = 3;
	int retryCount;
	
	PageJob (String saveTo, AbstractPage page, PageJob.JobStatusEnum status) {
		assert (saveTo != null);
		assert (page != null);
		this.page = page; this.saveTo = saveTo; this.status = status;
		retryCount = MAX_RETRIES;
	}
	
}