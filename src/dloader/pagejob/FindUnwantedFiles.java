package dloader.pagejob;

import dloader.page.AbstractPage;

/**
 * This job operates under presumption that any page create files in "saveTo" folder or 
 * in "getChildrenSaveTo" folder only.
 * @author Acerbic
 *
 */
public class FindUnwantedFiles extends PageJob<Void, ProgressReporter<PageToFilesMapping>> {
	 
	protected FindUnwantedFiles(AbstractPage page) {
		super(page);
	}

	@Override
	protected Void executeJob(ProgressReporter<PageToFilesMapping> reporter) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
