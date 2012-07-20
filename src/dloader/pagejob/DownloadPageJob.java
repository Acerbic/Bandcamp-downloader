package dloader.pagejob;

import dloader.JobMaster;
import dloader.page.AbstractPage;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;

/**
 * Job to download a page from network. Firstly, reads from cache and then updates with network data.
 * if no changes to current page happened, children nodes would be read from cache only (if possible, 
 * download can still happen when cache reading failed)
 * . 
 * If new page data is different, all children will be submitted to the same test.
 * forceDownload flag in constructor makes job to always download, skipping whole cache checks, 
 * for this page and all generated jobs.
 * @author Acerbic
 *
 */
public class DownloadPageJob extends PageJob {
	
	private	final 
	boolean forceDownload;

	/**
	 * Firstly, reads from cache and then updates with network data.
	 * @param forceDownload makes job to always download, skipping whole cache checks, 
	 * for this page and all generated jobs.
	 */
	public DownloadPageJob(AbstractPage page, JobMaster owner, boolean forceDownload) {
		super(page, owner);
		this.forceDownload = forceDownload;
	}

	@Override
	public void run() {
		if (!forceDownload) page.loadFromCache();
		try {
			if (page.updateFromNet(this) || forceDownload) {
				//note: this iterator does not require locking because of ConcurrentLinkedQueue implementation
				for (AbstractPage child: page.childPages) 
					jobMaster.submit(new DownloadPageJob(child, jobMaster, forceDownload));
				report("downloaded", 1);
			} else 
				for (AbstractPage child: page.childPages) 
					jobMaster.submit(new GetPageJob(child, jobMaster));
		} catch (ProblemsReadingDocumentException e) {
			report("download failed", 1);
			
			// TODO: incur more error handling, job resubmitting and such
			e.printStackTrace();
		}
			
	}

}
