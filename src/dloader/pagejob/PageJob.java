package dloader.pagejob;

//import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import dloader.PageProcessor;
import dloader.page.AbstractPage;

/**
 * Abstract class represents a single job in a job queue. 
 * Objects of its descendants are picked by SwingWorker or PageProcessor for execution.
 * 
 * This class is thread-safe. 
 * Compound operations (check-change) on status must be locked to (this)
 * @author A.Cerbic
 */
public abstract class PageJob <R, PR extends ProgressReporter<?>>{

	/**
	 *  the job object is BOUND to this page
	 */
	final public AbstractPage page;
	/**
	 * job object store this field even if they don't use it themselves - for generated jobs.
	 */
//	final public String saveTo;
	
	/**
	 * JobStatus is a synchronization feature. Every access to 'status' field requires locking
	 * on (this). So, locking is limited in time to switching between states and checking state.
	 * PENDING jobs are awaiting execution and can be started be calling "doTheJob()" or can be
	 * SELECTED for future execution. Once job is SELECTED by some thread, other threads cannot SELECT 
	 * it or start its execution.
	 * Jobs that stay in the job queue during execution and after completion. The only time job is dropped 
	 * from the jobQ is when it is being restarted (old job is dropped and new PENDING one is added).
	 */
	public enum JobStatus {PENDING, SELECTED, EXECUTING, DONE, FAILED};
	private JobStatus status;
	/**
	 * holds a reference for a Thread that SELECTED this job by calling setSelectedStatus()
	 * or null if no thread made this call yet 
	 */
	private Thread owningThread;
	/**
	 * priority of this job, used when next job is picked for execution.
	 */
//	final public int priority; 
	
	/**
	 *  To report progress of lengthy jobs. Initial value is 0, top value is 100.
	 *  This value is for indication only and has no restriction to job status or completion.
	 */
//	protected AtomicInteger progressIndicator;
//	public 
//	AtomicInteger getProgressIndicator() {return progressIndicator;}

	public synchronized final
	JobStatus getStatus() {return status;}
	
	/**
	 * makes the job SELECTED by current Thread. No other thread will be able to 
	 * select this job or start it job after this call. 
	 * @return true if switch to SELECTED state was successful, false otherwise
	 */
	public final synchronized
	boolean select() {
		if (status == JobStatus.PENDING) {
			status = JobStatus.SELECTED;
			owningThread = Thread.currentThread();
			return true;
		}
		return false;
	}
	
	public final synchronized
	boolean release() {
		if (status == JobStatus.SELECTED &&
			owningThread == Thread.currentThread()) {
			owningThread = null;
			status = JobStatus.PENDING;
			return true;
		}
		return false;
	}
	
	protected
//	PageJob (String saveTo, AbstractPage page, int priority) {
	PageJob (AbstractPage page) {
//		assert (saveTo != null);
		assert (page != null);
		this.page = page; 
//		this.saveTo = saveTo;
//		this.priority = priority;
		status = JobStatus.PENDING;
		owningThread = null;
//		progressIndicator = new AtomicInteger(0);
	}

	/**
	 * Does one operation on a page (may fail) 
	 * and possibly puts new jobs in a queue for further processing.
	 * @param reporter TODO
	 */
	protected abstract 
	R executeJob(PR reporter) throws Exception;
	
	/**
	 * Switches status and executes the job. May fail during page operation, status 
	 * is set to DONE or FAILED to indicate outcome.
	 * @return 
	 */
	public final 
	R doTheJob(PR reporter) {
		synchronized (this) {
			// make sure one thread is not trying to start a job SELECTED or EXECUTING
			// by another thread or a job that is DONE or FAILED already
			if ((status == JobStatus.EXECUTING) ||
				(status == JobStatus.DONE) ||
				(status == JobStatus.FAILED))
				return null;
			if ((status == JobStatus.SELECTED) &&
				(owningThread != Thread.currentThread()))
				return null;
			status = JobStatus.EXECUTING;
		}
		
		R results = null;
		try {
			results = executeJob(reporter);
			synchronized (this) {
				status = JobStatus.DONE;
			}
		} catch (Exception e) {
			synchronized (this) {
				status = JobStatus.FAILED;
			}
			PageProcessor.log(
					Level.SEVERE, 
					String.format("Exception during execution of %s %n", toString()), e);
		}

		return results;
	}

	@Override
	public String toString() {
		return String.format("%s for \"%s\": %s",
				getClass().getSimpleName(),
				page,
				getStatus().toString());
	}
	
	
}
