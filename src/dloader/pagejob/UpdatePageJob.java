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
 * reports "download job started", "downloaded", "up to date", "download failed"
 */
public class UpdatePageJob extends PageJob {
	
	private	final 
	boolean forceDownload;

	/**
	 * Firstly, reads from cache and then updates with network data.
	 * @param forceDownload makes job to always download, skipping whole cache checks, 
	 * for this page and all generated jobs.
	 */
	public UpdatePageJob(AbstractPage page, JobMaster owner, boolean forceDownload) {
		super(page, owner);
		this.forceDownload = forceDownload;
	}

	/**
	 * summary of the messages reported by DownloadPageJob:
	 * "download job started", 1
	 * "download finished", 1
	 * "up to date", 1
	 * "download failed", 1
	 */
	@Override
	public void run() {
		report ("download job started", 1);
		if (!forceDownload) page.loadFromCache();
		try {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (page.updateFromNet(this) || forceDownload) {
				page.saveToCache();
				//note: this iterator does not require locking because of ConcurrentLinkedQueue implementation
				for (AbstractPage child: page.childPages) 
					jobMaster.submit(new UpdatePageJob(child, jobMaster, forceDownload));
				report("download finished", 1);
			} else {
				report("up to date", 1);
				// even if all children are "up to date" too, still need to run the jobs - for the grand-children and etc.
				// since cache checks are cheap, better err on a cautious side.
				for (AbstractPage child: page.childPages) 
					jobMaster.submit(new GetPageJob(child, jobMaster));
				
			}
		} catch (ProblemsReadingDocumentException e) {
			report("download failed", 1);
			
			// TODO: incur more error handling, job resubmitting and such
		}
			
	}

}
