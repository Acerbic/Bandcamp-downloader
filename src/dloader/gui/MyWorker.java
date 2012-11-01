package dloader.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingWorker;

import dloader.Main;
import dloader.page.AbstractPage;
import dloader.pagejob.JobMaster;
import dloader.pagejob.JobMaster.JobType;
import dloader.gui.MyWorker.ProgressReportStruct;

/**
 * This is a SwingWorker extended to operate with JobMaster.
 * @author Acerbic
 *
 */
public class MyWorker extends SwingWorker<Map<AbstractPage, List<ProgressReportStruct>>, MyWorker.ProgressReportStruct> {
	
	/**
	 * Simple structure to toss progress report upstream.
	 * note that it's thread-safety depends on AbstractPage thread-safety.
	 * As long is the latter is safe, the former is too.
	 * @author Acerbic
	 */
	static public 
	class ProgressReportStruct {
		public final AbstractPage page;
		public final String type;
		public final long value;
		
		ProgressReportStruct (AbstractPage page, String type, long value) {
			this.page = page;
			this.type = type;     
			this.value = value;      
		}
	}
	
	/**
	 * associated JobMaster
	 */
	public final JobMaster jm;
	
	/**
	 * Works both as storage of results and as a flag (when != null) that results must be stored, not reported
	 */
	public Map<AbstractPage, List<ProgressReportStruct>> bulkResults;
	
	private Thread workerThread; // saved for later interruption

	// called from ED thread
	public MyWorker(AbstractPage rootPage, JobType whatToDo) {
		this(rootPage, whatToDo, false);
	}

	// called from ED thread
	public MyWorker(AbstractPage rootPage, JobType whatToDo, boolean bulk) {
		if (bulk)
			bulkResults = new ConcurrentHashMap<>(200);
		
		jm = new JobMaster(whatToDo, rootPage, 0) {
			// bridge to SwingWorker progress reporting
			// this is called in worker thread
			@Override
			public void report(AbstractPage page, String type, long i) {
				ProgressReportStruct res = new ProgressReportStruct(page, type, i);
				if (bulkResults == null)
					publish(res);
				else {
					List<ProgressReportStruct> resultsForThisPage = bulkResults.get(page);
					if (resultsForThisPage == null) {
						resultsForThisPage = new LinkedList<ProgressReportStruct>();
						bulkResults.put(page, resultsForThisPage);
					}
					resultsForThisPage.add(res);
				}
			}
		};
		
	}

	// called in worker thread
	@Override
	protected Map<AbstractPage, List<ProgressReportStruct>> doInBackground() throws Exception {
		workerThread = Thread.currentThread();
		if (jm != null)
			jm.goGoGo(); // -> several calls to PageJob.report() -> jm.report() -> SwingWorker.publish() -> this.process() -> gui.updateTree()
		return bulkResults; // call to done() -> gui.myWorkerDone()
	}

	// called in ED thread
	@Override
	protected void process(List<ProgressReportStruct> pilr) {
		for (ProgressReportStruct element : pilr) {
			Main.gui.updateTree(element.page, element.type, element.value);
		}
	}
	
	// called in ED thread
	@Override
	protected void done() {
		Main.gui.myWorkerDone();
	}

	/**
	 * Stop all jobs generated for this page and all of its children (recursively)
	 * Called from ED thread.
	 * @param pageOfNode
	 */
	public void stopJobs() {
		workerThread.interrupt();
	}

	public void stopJobsForPage(AbstractPage page) {
		jm.stopJobsForPage(page); 
	}
}
