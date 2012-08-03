package dloader;

import java.util.List;

import javax.swing.SwingWorker;

import dloader.page.AbstractPage;

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

	public MyWorker(AbstractPage rootPage, JobMaster.JobType whatToDo) {
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
			jm.goGoGo();
		return null;
	}

	// called in ED thread
	@Override
	protected void process(List<ProgressReportStruct> chunks) {
		for (ProgressReportStruct element : chunks) {
			// kinda stupid.
			Main.gui.updateTree(element.page, element.type, element.value);
		}
	}
}
