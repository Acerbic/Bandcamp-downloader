package dloader;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import dloader.page.AbstractPage;
import dloader.JobMaster;

public class JobMasterTest {
	
	class LocalReportJobMaser extends JobMaster {
		final private List<String> reports;
		public LocalReportJobMaser(List<String> reports, JobType whatToDo, AbstractPage rootPage) {
			super(whatToDo, rootPage);
			this.reports = reports;
		}
		
		@Override
		public void report(AbstractPage page, String type, int report) {
			reports.add(page.toString() + type + new Integer(report).toString());
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
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test_SimpleJob() {
		List<String> results = new ArrayList<String>();
		AbstractPage p = AbstractPage.bakeAPage("Track", "failaddress", null);
		LocalReportJobMaser jm = new LocalReportJobMaser(results, JobMaster.JobType.READCACHEPAGES, p);
		jm.goGoGo(); // starts and waits for other threads...
		assertTrue(results.contains("http://failaddress[Track]read cache failed1"));
	}

	@Test
	public void test_GeneratedJobs() {
		Main.cache = new XMLCache("test/pages_scan_cache.xml");
		List<String> results = new ArrayList<String>();
		AbstractPage p = AbstractPage.bakeAPage(null, "http://homestuck.bandcamp.com/album/alterniabound", null);
		LocalReportJobMaser jm = new LocalReportJobMaser(results, JobMaster.JobType.READCACHEPAGES, p);
		jm.goGoGo(); // starts and waits for other threads...
		assertTrue(results.contains("AlterniaBound[Album]read from cache1"));
		assertTrue(results.contains("Arisen Anew[Track]read from cache1"));
		
	}
}
