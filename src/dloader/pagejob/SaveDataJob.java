package dloader.pagejob;

import java.io.IOException;

import dloader.JobMaster;
import dloader.page.AbstractPage;

/**
 * Saves relevant page's data to disk as files, repeat for children.
 * @author Acerbic
 */
public class SaveDataJob extends PageJob {

	public SaveDataJob(AbstractPage page, JobMaster owner) {
		super(page, owner);
	}

	/**
	 * summary of the messages reported by SaveDataJob:
	 * "saving started", 1
	 *     "cover image downloaded", 1 (Album)
	 *     "file size", X (Track)
	 *     "downloaded bytes", X (Track)    
	 *     "file updated", 1 (Track) -- when only tags changed
	 *     "file downloaded", 1 (Track) -- when track got actually downloaded
	 * "save skipped", 1
	 * "saved", 1
	 * "saving caused exception", 1
	 */
	@Override
	public void run() {
		report ("saving started", 1);
		
		//note: this iterator does not require locking because of CopyOnWriteArrayList implementation
		for (AbstractPage child: page.childPages)
			jobMaster.submit(new SaveDataJob(child,jobMaster));
		
		try {
			if (!page.saveResult(this))
				report("save skipped", 1);
			else report("saved", 1);
		} catch (IOException e) {
			report("saving caused exception", 1);
			//TODO: job rescheduling  and error handling
//			e.printStackTrace();
		} catch (InterruptedException e) {
			//XXX: report cancellation?
		}
		
	}

}
