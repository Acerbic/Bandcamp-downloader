package dloader;

import java.util.concurrent.*;


import dloader.page.AbstractPage;
import dloader.pagejob.*;

public class JobMaster {
	
	static int CORETHREADS_NUMBER = 4; // number of threads running in parallel
	
	/**
	 * Associated job executor
	 */
	private ExecutorService executor;
	
	/**
	 * All submitted jobs have their Future's here. For result reporting/error checking/job resubmitting... etc
	 */
	private ConcurrentLinkedQueue<Future<?>> results; 
	
	/**
	 * READCACHEPAGES - get pages tree only from cache (prefetch)
	 * UPDATEPAGES - updated 1st page from the Internet and others too if needed, updates cache too
	 * UPDATEDATA - download missing/corrupt files.
	 * @author Acerbic
	 *
	 */
	public enum JobType { READCACHEPAGES, UPDATEPAGES, UPDATEDATA};

	/**
	 *  i'm not sure about this. It is a temporary variable to the rootPage. 
	 *  i just did not want to start the jobs running within a constructor 
	 *  (also since creating jobs means sharing a reference to this JobMaster 
	 *    it is not wise to leak "this" reference before construction is finished) 
	 */
	private AbstractPage rootPage; 
	private JobType whatToDo; 
	
	/**
	 * Initiate JobMaster with rootJob (one which can generate and submit other jobs)
	 * @param whatToDo - job type
	 * @param rootPage - page to process 
	 */
	public
	JobMaster (JobType whatToDo, AbstractPage rootPage) {
		executor = Executors.newFixedThreadPool(CORETHREADS_NUMBER);
		assert (executor instanceof ThreadPoolExecutor);
		results = new ConcurrentLinkedQueue<Future<?>>();
		
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
			// can be ran only once;
			if (rootPage == null) return;
			//TODO GENERATE A JOB HERE
			submit( null );
			rootPage = null;
		}
		
		while (!results.isEmpty())
			try {
				results.poll().get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		executor.shutdown();
	}
	
	synchronized public
	void submit (Runnable job) {
		results.add(executor.submit(job));
	}


	public void report(AbstractPage page, String type, int report) {
		// TODO SEND message to GUI thread
		
	}
	
	

}
