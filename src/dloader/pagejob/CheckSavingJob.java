package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

public class CheckSavingJob extends PageJob {

	public CheckSavingJob(AbstractPage page, JobMaster owner) {
		super(page, owner);
	}

	/**
	 * summary of the messages reported by CheckSavingJob:
	 * "saving not required", 1 
	 * "saving required", 1
	 */	
	@Override
	public void run() {
		//note: this iterator does not require locking because of CopyOnWriteArrayList implementation
		for (AbstractPage child: page.childPages)
			jobMaster.submit(new CheckSavingJob(child, jobMaster));
		
		if (page.isSavingNotRequired()) {
			report("saving not required", 1); 
		} else
			report("saving required", 1);
	}

	
}
