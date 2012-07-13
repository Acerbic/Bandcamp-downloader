package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

/**
 * Abstract class represents a single job in a job queue. 
 * 
 * This class is thread-safe. 
 * Compound operations (check-change) on status must be locked to (this)
 * @author A.Cerbic
 */
public abstract class PageJob implements Runnable, ProgressReporter {

	/**
	 *  the job object is BOUND to a page
	 */
	final public AbstractPage page;
	final protected JobMaster jobMaster;
	
	/**
	 * @param page - AbstractPage to process 
	 * @param owner - owning JobMaster, through which reports go and new jobs are submitted.
	 */
	protected
	PageJob (AbstractPage page, JobMaster owner) {
		assert (page != null);
		assert (owner != null);
		this.page = page;
		jobMaster = owner;
	}

	/**
	 * Channel to inform GUI about job results and progress. If other objects need to report too, pass "this" ref as
	 * a ProgressReporter interface implementation
	 */
	@Override
	public  
	void report (String s, int i) {
		jobMaster.report(page, s, i);
	}
}
