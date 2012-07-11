package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

/**
 * Job to read page data from cache. Starts same jobs for child nodes. 
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
			for (AbstractPage child: page.childPages)
				jobMaster.submit(new ReadCacheJob(child,jobMaster));
			report("read from cache",1);
		}  
//		} else 
//			jobMaster.submit(new DownloadPageJob(page,jobMaster, false));
		
	}
}
