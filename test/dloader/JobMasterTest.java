package dloader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import dloader.page.AbstractPage;
import dloader.page.Album;
import dloader.page.Discography;
import dloader.page.Track;
import dloader.JobMaster;

public class JobMasterTest {
	List<String> results;
	Path testDirFilesPath = Paths.get(System.getProperty("user.dir"),"test");
	
	class LocalReportJobMaser extends JobMaster {
		final private List<String> reports;
		public LocalReportJobMaser(List<String> reports, JobType whatToDo, AbstractPage rootPage) {
			super(whatToDo, rootPage, 0);
			this.reports = reports;
		}
		
		@Override
		public void report(AbstractPage page, String type, long report) {
			reports.add(page.toString() + type + new Long(report).toString());
		}
	}

	public static
	class DiscographyForDownloadPageTest extends Discography{
		public DiscographyForDownloadPageTest(String url, String saveTo,
				AbstractPage parent) throws IllegalArgumentException {
			super(url, saveTo, parent);
		}

		// very special case, overrides parsing because paths no longer reflect object essence (no "/album/" parts) and instead 
		@Override
		protected AbstractPage parseChild(Element element) throws ProblemsReadingDocumentException  {
			try {
				String href = element.getAttributeValue("href");
				
				AbstractPage c;
				String newURL = Paths.get(url.toURI()).resolveSibling(href.substring(1)).toUri().toString();
				if (href.equals("/emancipator-children/elephant-survival.htm"))
					c = new Track(newURL, getChildrenSaveTo(), this);
				else 
					c = new Album(newURL, getChildrenSaveTo(), this);
				c.setTitle(element.getText());
				return c;
			} catch (NullPointerException|IllegalArgumentException|
					IOException | URISyntaxException e) {
				throw new ProblemsReadingDocumentException(e);
			}
		}
		
	}
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		Main.cache = new XMLCache("test/pages_scan_cache.xml");
		results = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test_SimpleJob() {
		AbstractPage p = AbstractPage.bakeAPage("Track", "failaddress", null, null);
		LocalReportJobMaser jm = new LocalReportJobMaser(results, JobMaster.JobType.READCACHEPAGES, p);
		jm.goGoGo(); // starts and waits for other threads...
		assertTrue(results.contains("http://failaddress[Track]read cache failed1"));
	}

	@Test
	public void test_GeneratedJobs() {
		AbstractPage p = AbstractPage.bakeAPage(null, "http://homestuck.bandcamp.com/album/alterniabound", null, null);
		LocalReportJobMaser jm = new LocalReportJobMaser(results, JobMaster.JobType.READCACHEPAGES, p);
		jm.goGoGo(); // starts and waits for other threads...
		assertTrue(results.contains("AlterniaBound[Album]read from cache1"));
		assertTrue(results.contains("Arisen Anew[Track]read from cache1"));
		
	}
	
	@Test
	public void test_ReadCacheJob() throws MalformedURLException {
		AbstractPage p = AbstractPage.bakeAPage(null, "http://emancipator.bandcamp.com", null, null);
		LocalReportJobMaser jm = new LocalReportJobMaser(results, JobMaster.JobType.READCACHEPAGES, p);
		jm.goGoGo(); // starts and waits for other threads...
		assertTrue(results.contains("emancipator[Discography]read from cache1"));
		assertEquals("emancipator", p.getTitle());
		assertTrue(results.contains("Remixes[Album]read from cache1"));
		assertTrue(results.contains("soon it will be cold enough[Album]read from cache1"));
		assertTrue(results.contains("safe in the steep cliffs[Album]read from cache1"));
		assertTrue(results.contains("http://emancipator.bandcamp.com/album/free-downloads[Album]read cache failed1"));
		
		assertEquals(5, p.childPages.size());
		assertEquals("http://f0.bcbits.com/z/17/92/1792496746-1.jpg", ((Album) p.getChildByURL(new URL("http://emancipator.bandcamp.com/album/remixes-2"))).getCoverUrl().toString());
	}
	
	@Test
	public void test_DownloadPageJob() throws IOException {
		AbstractPage px = new DiscographyForDownloadPageTest (testDirFilesPath.resolve("emancipator.htm").toUri().toString(), 
				null, null) ;
		LocalReportJobMaser jm = new LocalReportJobMaser(results, JobMaster.JobType.UPDATEPAGES, px);
		jm.goGoGo(); // starts and waits for other threads...
		assertTrue(results.contains("emancipator[DiscographyForDownloadPageTest]download finished1"));
		assertTrue(results.contains("Remixes[Album]up to date1"));
		assertTrue(results.contains("Elephant Survival[Track]download finished1"));
		assertTrue(results.contains("soon it will be cold enough[Album]download finished1"));
		assertTrue(results.contains("safe in the steep cliffs[Album]download failed1"));
		assertTrue(results.contains("Shook (Sigur Ros X Mobb Deep)[Track]download failed1"));
		
		assertEquals(5, px.childPages.size());
	}
	
}
