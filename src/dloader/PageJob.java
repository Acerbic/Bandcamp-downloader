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
	}
		
	// the page is BOUND to this job object (1-to-1) and tracks page processing progress
	// during execution.
	final public AbstractPage page;  
	 
	final public String saveTo;
	
	// I rely on volatile definition and not on synchronized getters and setters because
	//   only one thread at a time writes to the fields (dedicated SwingWorker thread if GUI is on)
	//   and other threads (Event Dispatch) only read from them.
	// Another SwingWorker can not access this PageJob object since the first one 
	//   removes it from the jobQ and "doSingleJob" locks on PageJob object the whole time it works on it.
	volatile public JobStatusEnum status;  
	
	// flags on how page was processed 
	volatile public boolean isReadFromCache = false;
	volatile public boolean isReadFromWeb = false;
	volatile public String saveResultsReport = null;

	final static int MAX_RETRIES = 3;
	volatile public int retryCount;
	
	PageJob (String saveTo, AbstractPage page, PageJob.JobStatusEnum status) {
		assert (saveTo != null);
		assert (page != null);
		this.page = page; this.saveTo = saveTo; this.status = status;
		retryCount = MAX_RETRIES;
	}
	
	@Override
	public synchronized
	String toString() {
		String title_area = page.getTitle();
		title_area = (title_area == null)? page.url.toString(): title_area;
		String recon_area = isReadFromWeb? "[web] " : (isReadFromCache? "[cache] ": "");
		return title_area + ": " + recon_area + status.toString();
	}
	
	
}