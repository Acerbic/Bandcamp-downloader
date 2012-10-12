package dloader.gui;

import java.util.HashMap;
import java.util.List;

import javax.swing.SwingWorker;

import dloader.JobMaster;
import dloader.Main;
import dloader.JobMaster.JobType;
import dloader.page.AbstractPage;

/**
 * This is a SwingWorker extended to operate with JobMaster.
 * @author Acerbic
 *
 */
public class MyWorker extends SwingWorker<Object, MyWorker.ProgressReportStruct> {
	
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
	
	public final JobMaster jm;
	public HashMap<AbstractPage, Long> savingReqJobResults;
	private Thread workerThread;

	// called from ED thread
	public MyWorker(AbstractPage rootPage, JobType whatToDo) {
		
		jm = new JobMaster(whatToDo, rootPage, 0) {
			
			// bridge to SwingWorker progress reporting
			@Override
			public void report(AbstractPage page, String type, long i) {
				publish(new ProgressReportStruct(page, type, i));
			}
		};
		
		if (whatToDo.equals(JobType.CHECKSAVINGREQUIREMENT))
			savingReqJobResults = new HashMap<>(200);
	}

	// called in worker thread
	@Override
	protected Object doInBackground() throws Exception {
		workerThread = Thread.currentThread();
		if (jm != null)
			jm.goGoGo(); // -> several calls to PageJob.report() -> jm.report() -> SwingWorker.publish() -> this.process() -> gui.updateTree()
		return null; // call to done() -> gui.myWorkerDone()
	}

	// called in ED thread
	@Override
	protected void process(List<ProgressReportStruct> chunks) {
		for (ProgressReportStruct element : chunks) {
			switch (jm.whatToDo) {
			case CHECKSAVINGREQUIREMENT:
				savingReqJobResults.put(element.page, element.value); // 1st combine all, then update all after jobs are done
				break;
			default:
				// pass individual messages
				Main.gui.updateTree(element.page, element.type, element.value);
			
			}
				
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
