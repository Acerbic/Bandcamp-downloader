package dloader.pagejob;

import java.util.Collection;
import java.util.LinkedList;

import dloader.PageProcessor;
import dloader.page.AbstractPage;

public class DownloadPageJob extends PageJob {

	public DownloadPageJob(AbstractPage page) {
		super(page);
	}

	@Override
	protected Collection<AbstractPage> executeJob() throws Exception {
		page.loadFromCache();
		if (page.UpdateFromNet(progressIndicator)) {
			for (AbstractPage child: page.childPages) 
				PageProcessor.addJob(new DownloadPageJob(child));
			Collection<AbstractPage> result = new LinkedList<AbstractPage>();
			result.add(page);
			return result;
		} else 
			PageProcessor.addJob(new ReadCacheJob(page));
			
		return null;
	}

}
