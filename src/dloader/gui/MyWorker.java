package dloader.gui;

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
	
	private JobMaster jm;

	public MyWorker(AbstractPage rootPage, JobType whatToDo) {
		jm = new JobMaster(whatToDo, rootPage, 0) {
			
			// bridge to SwingWorker progress reporting
			@Override
			public void report(AbstractPage page, String type, long i) {
				publish(new ProgressReportStruct(page, type, i));
//				try {
//					Thread.sleep(300);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		};
	}

	// called in worker thread
	@Override
	protected Object doInBackground() throws Exception {
		if (jm != null)
			jm.goGoGo(); // -> several calls to jm.report() -> publish() -> process() -> gui.updateTree()
		return null; // call to done() -> gui.myWorkerDone()
	}

	// called in ED thread
	@Override
	protected void process(List<ProgressReportStruct> chunks) {
		for (ProgressReportStruct element : chunks) {
			// kind of stupid.
			Main.gui.updateTree(element.page, element.type, element.value);
		}
	}
	
	// called in ED thread
	@Override
	protected void done() {
		Main.gui.myWorkerDone(jm.rootPage, jm.whatToDo);
	}

	/**
	 * Stop all jobs generated for this page and all of its children (recursively)
	 * @param pageOfNode
	 */
	public void stopJobsForPage(AbstractPage pageOfNode) {
		// TODO Auto-generated method stub
	}
}
