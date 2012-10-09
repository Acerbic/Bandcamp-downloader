package dloader.pagejob;

import java.util.HashMap;

import dloader.JobMaster;
import dloader.page.AbstractPage;

public class CheckSavingJob extends PageJob {

	private HashMap<AbstractPage, Boolean> results;
	
	public CheckSavingJob(AbstractPage page, JobMaster owner) {
		this(page, owner, null);
	}	
	
	public CheckSavingJob(AbstractPage page, JobMaster owner, HashMap<AbstractPage, Boolean> results) {
		super(page, owner);
		this.results = results;
	}

	/**
	 * Summary of the messages reported by CheckSavingJob:
	 * "saving not required", 1 
	 * "saving required", 1
	 */	
	@Override
	public void run() {
		if (results == null) {		
			if (page.isSavingNotRequired()) 
				report("saving not required", 1); 
			else
				report("saving required", 1);
		} else {
			report ("",  page.isSavingNotRequired()? 1: 0); // special kind of report that will not propagate to updateTree() directly
			
			//note: this iterator does not require locking because of CopyOnWriteArrayList implementation
			if (results != null)
				for (AbstractPage child: page.childPages)
					jobMaster.submit(new CheckSavingJob(child, jobMaster, results));
		}
	}
}
