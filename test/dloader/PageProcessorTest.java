package dloader;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.jdom.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dloader.AbstractPage.ProblemsReadingDocumentException;
import dloader.PageProcessor.PageJob.JobStatusEnum;

public class PageProcessorTest {

	PageProcessor mockPP_useCache;
	//	Queue<AbstractPage> mockJobQ;
	@Before
	public void setUp() throws Exception {
		mockPP_useCache = new PageProcessor(true);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDetectPageRecognition() {
		assertEquals(PageProcessor.detectPage("http://homestuck.bandcamp.org").getClass().getSimpleName(), 
				"Discography");
		assertEquals(PageProcessor.detectPage("http://homestuck.bandcamp.org/track/Name").getClass().getSimpleName(), 
				"Track");
		assertEquals(PageProcessor.detectPage("http://homestuck.bandcamp.org/album/Name").getClass().getSimpleName(), 
				"Album");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDetectPageErrorURL() {
		PageProcessor.detectPage("sadasd132");
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testDetectPageUnknownURL() {
		PageProcessor.detectPage("http://sadasd132.com/bonk?");
	}

	@Test (expected = NoSuchElementException.class)
	public void testProcessOnePageFailOnEmpty() throws ProblemsReadingDocumentException, IOException {
		mockPP_useCache.processOnePage();
	}
	
	@Test
	public void testProcessOnePageFailedReconGoesToDownload() throws ProblemsReadingDocumentException, IOException {
		PageProcessor mockPP_noCache = new PageProcessor(false);
		AbstractPage page = new Discography("file://test/homestuck.html");

		// not using cache
		PageProcessor.addJob(new File("test/download_zone"), page, JobStatusEnum.RECON_PAGE);
		mockPP_noCache.processOnePage();
		assertEquals(1, PageProcessor.jobQ.size());
		assertEquals(JobStatusEnum.DOWNLOAD_PAGE, PageProcessor.jobQ.peek().status);
		PageProcessor.jobQ.clear();
		
		// cache is not initiated
		PageProcessor.addJob(new File("test/download_zone"), page, JobStatusEnum.RECON_PAGE);
		mockPP_useCache.processOnePage();
		assertEquals(1, PageProcessor.jobQ.size());
		assertEquals(JobStatusEnum.DOWNLOAD_PAGE, PageProcessor.jobQ.peek().status);
		PageProcessor.jobQ.clear();
		
	}
	
	@Test
	public void testProcessOnePageReconFromCacheSuccess() throws ProblemsReadingDocumentException, IOException {
		AbstractPage page = PageProcessor.detectPage("http://homestuck.bandcamp.com");
		PageProcessor.addJob(new File("test/download_zone"), page, JobStatusEnum.RECON_PAGE);
		PageProcessor.initCache("test/pages_scan_cache.xml");
		mockPP_useCache.processOnePage();
		assertEquals(1, PageProcessor.jobQ.size());
		PageProcessor.PageJob job = PageProcessor.jobQ.poll();
		assertEquals(JobStatusEnum.ADD_CHILDREN_JOBS, job.status);
		assertEquals("Homestuck", job.page.title);
	}
	
	@Test
	public void testProcessOnePageReconFromCacheFails() throws ProblemsReadingDocumentException, IOException {
		class DummyPage extends Discography{
			public DummyPage(String string) {
				super(string);
			}

			@Override
			protected boolean loadFromCache(Document doc) {
				title = "Cache failed";
				return false;
			}
		}

		AbstractPage page = new DummyPage("http://homestuck.bandcamp.com");
		PageProcessor.addJob(new File("test/download_zone"), page, JobStatusEnum.RECON_PAGE);
		PageProcessor.initCache("test/pages_scan_cache.xml");
		mockPP_useCache.processOnePage();
		assertEquals(1, PageProcessor.jobQ.size());
		PageProcessor.PageJob job = PageProcessor.jobQ.poll();
		assertEquals(JobStatusEnum.DOWNLOAD_PAGE, job.status);
		assertEquals("Cache failed", job.page.title);
	}
	
}
