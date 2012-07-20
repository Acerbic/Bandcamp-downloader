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
		if (page.loadFromCache()) {
			//note: this iterator does not require locking because of ConcurrentLinkedQueue implementation
			for (AbstractPage child: page.childPages)
				jobMaster.submit(new ReadCacheJob(child,jobMaster));
			report("read from cache",1);
		} else
			report("read cache failed",1);
	}
}
