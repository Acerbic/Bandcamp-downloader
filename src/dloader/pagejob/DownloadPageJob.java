package dloader.pagejob;

import dloader.PageProcessor;
import dloader.page.AbstractPage;

public class DownloadPageJob extends PageJob <AbstractPage, ProgressReporter<Integer>>{

	public DownloadPageJob(AbstractPage page) {
		super(page);
	}

	@Override
	protected 
	AbstractPage executeJob(ProgressReporter<Integer> reporter) throws Exception {
		page.loadFromCache();
		if (page.UpdateFromNet(reporter)) {
			for (AbstractPage child: page.childPages) 
				PageProcessor.addJob(new DownloadPageJob(child));
			return page;
		} else 
			PageProcessor.addJob(new ReadCacheJob(page));
			
		return null;
	}

}
