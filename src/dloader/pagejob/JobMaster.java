package dloader.pagejob;

import java.util.concurrent.*;

import dloader.Main;
import dloader.page.AbstractPage;

/**
 * Class to initiate and maintain jobs' queue and intermediate result reporting
 * between jobs and GUI/caller.
 * 
 * After constructing, call goGoGo() to start actual working
 * @author Acerbic
 */
public abstract class JobMaster {
	
	/**
	 * Just a vector of objects {PageJob, Future<?>}, where Future is generated for said job.
	 * This is to track Futures for jobs, to cancel specific jobs, that has not been started yet.
	 * @author Acerbic
	 */
	private
	class JobFuturePair {
		public final PageJob job;
		public final Future<?> future;
		JobFuturePair(PageJob job, Future<?> future) {
			this.job = job; this.future = future;
		}
	}
	
	/**
	 * number of threads running in parallel by default
	 */
	static final public int CORETHREADS_NUMBER = 4; 
	
	/**
	 * Associated job executor
	 */
	private ExecutorService executor;
	
	/**
	 * All submitted jobs have their Future's here. For result reporting/error checking/job resubmitting... etc
	 */
	private ConcurrentLinkedQueue<JobFuturePair> submittedPairs; 
	
	/**
	 * READCACHEPAGES - get pages tree ONLY from cache (prefetch)
	 * UPDATEPAGES - updated 1st page from the Internet and others too if needed (otherwise tries reading from cache)
	 *   	updates cache too
	 * CHECKSAVINGREQUIREMENT - compare AbstractPage objects with files on disk to establish whether saving is required,
	 * 		as saving/downloading is a time consuming operation. 
	 * SAVEDATA - download missing/corrupt files.
	 * 
	 * It is vital that different kinds of jobs are not ran on the same page concurrently.
	 * @author Acerbic
	 */
	public enum JobType { READCACHEPAGES, UPDATEPAGES, SAVEDATA, CHECKSAVINGREQUIREMENT};

	/**
	 *  Those a temporary variable to store rootPage and JobType between calls to the constructor and actual jobs execution 
	 *  (call to goGoGo()) 
	 */
	public final AbstractPage rootPage; 
	public final JobType whatToDo; 
	
	/**
	 * Initiate JobMaster with rootJob (one which can generate and submit other jobs)
	 * @param whatToDo - job type
	 * @param rootPage - page to process 
	 * @param threadsNumber - desired number of threads, if 0 then default number will be used: JobMaster.CORETHREADS_NUMBER (4)
	 */
	public
	JobMaster (JobType whatToDo, AbstractPage rootPage, int threadsNumber) {
		if (threadsNumber <= 0)
			threadsNumber = CORETHREADS_NUMBER;
		executor = Executors.newFixedThreadPool(threadsNumber);
		assert (executor instanceof ThreadPoolExecutor);
		submittedPairs = new ConcurrentLinkedQueue<>();
		
		this.whatToDo = whatToDo;
		this.rootPage = rootPage; 
	}

	/**
	 * The main execution starter. All working is started with calling this function. May be called only once per JobMaster.
	 * subsequent call would return with no results.
	 */
	public
	void goGoGo() {
		synchronized (this) {
			// can be ran only once (only in one thread);
			if (rootPage == null) return;

			try {
				switch (whatToDo) {
				case READCACHEPAGES: submit(new ReadCacheJob(rootPage, this)); break;
				case UPDATEPAGES: submit(new UpdatePageJob(rootPage, this, !Main.allowFromCache)); break;
				case SAVEDATA: submit(new SaveDataJob(rootPage, this)); break;
				case CHECKSAVINGREQUIREMENT: submit(new CheckSavingJob(rootPage, this, true)); break;
				}
			} catch (InterruptedException e) {
				// JobMaster's thread is interrupted before any tasks are submitted. just exit
				executor.shutdown(); // just in case
				return;
			}
		}
		
		// wait until all the jobs are finished before terminating this thread.
		while (!submittedPairs.isEmpty()) {
			try {
				Future<?> headFuture = submittedPairs.peek().future; 
				headFuture.get(); // can't use awaitTermination, because jobs are submitting other jobs
				submittedPairs.poll(); // no synch with peek and isEmpty is needed as all elements are added to the tail and only this thread is running task removal.
			} catch (InterruptedException | ExecutionException e) {
				// Stop all jobs
				executor.shutdown(); // no new jobs submitted
				
				// stop all jobs submitted (whether running or not)
				for (JobFuturePair pair: submittedPairs)
					pair.future.cancel(true);
				break;
			}
		}
		
		executor.shutdown();
	}
	
	/**
	 * Entry point for Jobs to add more children jobs into a queue
	 * @param job - new job to be executed some moment in the future.
	 * @throws InterruptedException 
	 */
	synchronized public
	void submit (PageJob job) throws InterruptedException {
		// if thread submitting a job was interrupted while waiting on synchronization
		//  then do not submit 
		if (Thread.interrupted())
			throw new InterruptedException();
		
		submittedPairs.add(new JobFuturePair(job, executor.submit(job)));
	}


	/**
	 * Entry point for Jobs to send reports on the progress/results.
	 * This method must be overloaded to extract results of operations, reported by individual PageJob and AbstractPage object.
	 * @param page - page is being (has been) worked on.
	 * @param type - custom string argument
	 * @param i - custom integer argument
	 */
	abstract
	public void report(AbstractPage page, String type, long i) ;

	/**
	 * Stop jobs all jobs for page. Only this page is affected, if its children must be stopped, the caller must do the recursion.
	 * @param page - the page which jobs must be stopped.
	 */
	public 
	void stopJobsForPage(AbstractPage page) {
		for (JobFuturePair pair: submittedPairs) 
			// no new tasks will be submitted because of synchronization.
			if (pair.job.page.equals(page)) 
				// we don't remove pair from queue here to not mess up with main JobMaster thread. 
				pair.future.cancel(true);
	}

}
