package dloader.pagejob;

import java.util.Collection;
import java.util.LinkedList;

import dloader.PageProcessor;
import dloader.page.AbstractPage;

public class ReadCacheJob extends PageJob <Collection<AbstractPage>, ProgressReporter<Object>>{

	protected ReadCacheJob(AbstractPage page) {
		super(page);
	}

	@Override
	protected 
	Collection<AbstractPage> executeJob(ProgressReporter<Object> reporter) throws Exception {
		Collection<AbstractPage> loadedFromCache = new LinkedList<AbstractPage>();
		treeWalkReadCache(page,loadedFromCache);
		return loadedFromCache;
	}

	void treeWalkReadCache(AbstractPage p, Collection<AbstractPage> sum) {
		if (p.loadFromCache()) {
			sum.add(p);
			for (AbstractPage child: p.childPages)
				treeWalkReadCache(child, sum);
		} else 
			PageProcessor.addJob(new DownloadPageJob(p));
	}
}
