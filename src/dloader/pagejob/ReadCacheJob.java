package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

/**
 * Job to read page data from cache ONLY. Starts same jobs for child nodes. 
 * @author Acerbic
 *
 */
public class ReadCacheJob extends PageJob {

	public ReadCacheJob(AbstractPage page, JobMaster owner) {
		super(page, owner);
	}

	@Override
	public void run() {
		report ("checking cache", 1); // same as in GetPageJob before trying to read cache;
		if (page.loadFromCache()) {
			//note: this iterator does not require locking because of ConcurrentLinkedQueue implementation
			for (AbstractPage child: page.childPages)
				jobMaster.submit(new ReadCacheJob(child,jobMaster));
			report("read from cache",1); // same as in GetPageJob on successful reading cache;
		} else
			report("read cache failed",1);
	}
}
