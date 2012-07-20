package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;

/**
 * Job to read page data from cache and if cache is unavailable - download. 
 * Starts the same jobs for children nodes.
 * 
 * NOTE the "package-default" visibility on class, as it is used only be other class - DownloadPage.
 * @author Acerbic
 *
 */
class GetPageJob extends PageJob {

	public GetPageJob(AbstractPage page, JobMaster owner) {
		super(page, owner);
	}

	@Override
	public void run() {
		if (page.loadFromCache()) {
			//note: this iterator does not require locking because of ConcurrentLinkedQueue implementation
			for (AbstractPage child: page.childPages)
				jobMaster.submit(new GetPageJob(child,jobMaster));
			report("read from cache",1);
		} else 
			jobMaster.submit(new DownloadPageJob(page,jobMaster, false));
		
	}
}
