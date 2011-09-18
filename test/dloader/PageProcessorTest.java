package dloader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jdom.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dloader.PageJob.JobStatusEnum;
import dloader.page.AbstractPage;
import dloader.page.Discography;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;

public class PageProcessorTest {

	PageProcessor mockPP_useCache;
	
	@Before
	public void setUp() throws Exception {
		mockPP_useCache = new PageProcessor(null, null, true);
	}

	@After
	public void tearDown() throws Exception {
		Files.deleteIfExists(Paths.get("test/download_zone"));
		PageProcessor.getJobQ().clear();
		PageProcessor.cache = null;
		PageProcessor.logger = null;
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

	@Test (expected = IndexOutOfBoundsException.class)
	public void testProcessOnePageFailOnEmpty() throws ProblemsReadingDocumentException, IOException {
		mockPP_useCache.processOnePage(PageProcessor.getJobQ().remove(0));
	}
	
	@Test
	public void testProcessOnePageFailedReconGoesToDownload() throws ProblemsReadingDocumentException, IOException {
		PageProcessor mockPP_noCache = new PageProcessor(null, null, false);
		AbstractPage page = new Discography("file://test/homestuck.html");

		// not using cache
		PageProcessor.addJob(Paths.get("test/download_zone").toString(), page, JobStatusEnum.RECON_PAGE);
		mockPP_noCache.processOnePage(PageProcessor.getJobQ().remove(0));
		assertEquals(1, PageProcessor.getJobQ().size());
		assertEquals(JobStatusEnum.DOWNLOAD_PAGE, PageProcessor.getJobQ().remove(0).status);
		PageProcessor.getJobQ().clear();
		
		// cache is not initiated
		PageProcessor.addJob(Paths.get("test/download_zone").toString(), page, JobStatusEnum.RECON_PAGE);
		mockPP_useCache.processOnePage(PageProcessor.getJobQ().remove(0));
		assertEquals(1, PageProcessor.getJobQ().size());
		assertEquals(JobStatusEnum.DOWNLOAD_PAGE, PageProcessor.getJobQ().remove(0).status);
		PageProcessor.getJobQ().clear();
		
	}
	
	@Test
	public void testProcessOnePageReconFromCacheSuccess() throws ProblemsReadingDocumentException, IOException {
		AbstractPage page = PageProcessor.detectPage("http://homestuck.bandcamp.com");
		PageProcessor.addJob(Paths.get("test/download_zone").toString(), page, JobStatusEnum.RECON_PAGE);
		PageProcessor.initCache("test/pages_scan_cache.xml");
		mockPP_useCache.processOnePage(PageProcessor.getJobQ().remove(0));
		assertEquals(1, PageProcessor.getJobQ().size());
		PageJob job = PageProcessor.getJobQ().remove(0);
		assertEquals(JobStatusEnum.ADD_CHILDREN_JOBS, job.status);
		assertEquals("Homestuck", job.page.getTitle());
	}
	
	@Test
	public void testProcessOnePageReconFromCacheFails() throws ProblemsReadingDocumentException, IOException {
		class DummyPage extends Discography{
			public DummyPage(String string) {
				super(string);
			}

			@Override
			public
			boolean loadFromCache(Document doc) {
				setTitle("Cache failed");
				return false;
			}
		}

		AbstractPage page = new DummyPage("http://homestuck.bandcamp.com");
		PageProcessor.addJob(Paths.get("test/download_zone").toString(), page, JobStatusEnum.RECON_PAGE);
		PageProcessor.initCache("test/pages_scan_cache.xml");
		mockPP_useCache.processOnePage(PageProcessor.getJobQ().remove(0));
		assertEquals(1, PageProcessor.getJobQ().size());
		PageJob job = PageProcessor.getJobQ().remove(0);
		assertEquals(JobStatusEnum.DOWNLOAD_PAGE, job.status);
		assertEquals("Cache failed", job.page.getTitle());
	}
	
	@Test
	public void testProcessOnePageRetryDownloadPage() throws ProblemsReadingDocumentException, IOException {
		PageProcessor pp = new PageProcessor(
				Paths.get("test/download_zone").toString(), 
				"http://rberebrbere.bandcamp.com", 
				false);
		assertEquals(1, PageProcessor.getJobQ().size());
		PageJob pj = PageProcessor.getJobQ().get(0);
		assertEquals(PageJob.MAX_RETRIES, pj.retryCount);
		pp.acquireData();
		assertEquals(0, pj.retryCount);
	}
	
	@Test
	public void testProcessOnePageRetrySavePageResults() throws ProblemsReadingDocumentException, IOException {
		PageProcessor pp = new PageProcessor(
				Paths.get("test/download_zone").toString(), 
				"http://rberebrbere.bandcamp.com/track/failtrack.mp3", 
				false);
		assertEquals(1, PageProcessor.getJobQ().size());
		PageJob pj = PageProcessor.getJobQ().get(0);
		assertEquals(PageJob.MAX_RETRIES, pj.retryCount);
		pj.status = JobStatusEnum.SAVE_RESULTS;
		pj.page.setTitle("SomeName");
		pp.acquireData(); // faults because of NULL medialink property in Track object.
		assertEquals(0, pj.retryCount);
	}
}
