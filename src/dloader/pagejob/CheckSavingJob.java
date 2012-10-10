package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

public class CheckSavingJob extends PageJob {

	private boolean recurse;
	
	public CheckSavingJob(AbstractPage page, JobMaster owner) {
		this(page, owner, false);
	}	
	
	public CheckSavingJob(AbstractPage page, JobMaster owner, boolean recurse) {
		super(page, owner);
		this.recurse = recurse;
	}

	/**
	 * Summary of the messages reported by CheckSavingJob:
	 * "saving not required", 1 
	 * "saving required", 1
	 */	
	@Override
	public void run() {
		if (page.isSavingNotRequired()) 
			report("saving not required", 1); 
		else
			report("saving required", 1);
			
		if (recurse)
			//note: this iterator does not require locking because of CopyOnWriteArrayList implementation
			for (AbstractPage child: page.childPages)
				jobMaster.submit(new CheckSavingJob(child, jobMaster, true));
	}
}
