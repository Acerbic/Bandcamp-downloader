package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

/**
 * Abstract class represents a single job in a job queue. 
 * The job is run as a separate thread, spawning new children threads by invoking "submit(...)" on its
 * owning JobMaster, reference to which is passed during construction. <br><br>
 * 
 * Because child job's page may refer to this job's page
 * by its "parent" link, certain synchronization is due.
 * The call to create children jobs must be made AFTER all changes to this job's page are finalized, i.e.
 * this job's page lock is released. Something like this:<br><br>
 * 
 * synchronize (page) {<br>
 * ... works .... <br>
 * }<br>
 * jobMaster.submit(..);<br><br> 
 * 
 * This class is thread-safe. 
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
