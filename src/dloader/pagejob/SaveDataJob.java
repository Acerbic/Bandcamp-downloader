package dloader.pagejob;

import java.io.IOException;

import dloader.JobMaster;
import dloader.page.AbstractPage;

/**
 * Saves relevant page's data to disk as files, repeat for children.
 * @author Acerbic
 *
 */
public class SaveDataJob extends PageJob {

	public SaveDataJob(AbstractPage page, JobMaster owner) {
		super(page, owner);
	}

	@Override
	public void run() {
		String operationResult;
		try {
			operationResult = page.saveResult(this);
			if (operationResult == null)
				report("saving skipped",1);
			else 
				report(operationResult,1);
		} catch (IOException e) {
			report("saving caused exception",1);
			e.printStackTrace();
		}
		
		for (AbstractPage child: page.childPages)
			jobMaster.submit(new SaveDataJob(child,jobMaster));
	}

}
