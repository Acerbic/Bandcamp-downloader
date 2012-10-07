package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

public class CheckSavingJob extends PageJob {

	private boolean recursive;
	public CheckSavingJob(AbstractPage page, JobMaster owner) {
		this(page, owner, true);
	}	
	
	public CheckSavingJob(AbstractPage page, JobMaster owner, boolean recursive) {
		super(page, owner);
		this.recursive = recursive;
	}

	/**
	 * summary of the messages reported by CheckSavingJob:
	 * "saving not required", 1 
	 * "saving required", 1
	 */	
	@Override
	public void run() {
		//note: this iterator does not require locking because of CopyOnWriteArrayList implementation
		if (recursive)
			for (AbstractPage child: page.childPages)
				jobMaster.submit(new CheckSavingJob(child, jobMaster, recursive));
		
		if (page.isSavingNotRequired()) {
			report("saving not required", 1); 
		} else
			report("saving required", 1);
	}

	
}
