package dloader.page;

import static org.junit.Assert.*;

import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import dloader.Main;
import dloader.XMLCache;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;
import dloader.pagejob.ProgressReporter;

public class DiscographyTest {
	class DummyProgressReporter implements ProgressReporter {
		String repS;
		long repI;
		
		@Override
		public void report(String type, long report) {
			repS = type;
			repI = report;
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
	public void testCaching() {
		Main.cache = new XMLCache("test/pages_scan_cache.xml");
		Discography page = new Discography("http://homestuck.bandcamp.com",null,null);
		assertFalse(page.isOK());
		assertTrue(page.loadFromCache());
		assertTrue(page.isOK());
		assertEquals("Homestuck", page.getTitle());
		assertEquals(15, page.childPages.size());
		
		Main.cache = new XMLCache("test/nullCache.xml"); // not existing file (new cache)
		page.saveToCache();
		
		Discography page2 = new Discography("http://homestuck.bandcamp.com",null,null);
		assertFalse(page2.isOK());
		assertTrue(page2.loadFromCache());
		assertTrue(page2.isOK());
		assertEquals("Homestuck", page2.getTitle());
		assertEquals(15, page2.childPages.size());
		
		assertNotNull(page.getChildByURLString("http://homestuck.bandcamp.com/album/homestuck-vol-1"));
	}
	
	@Test
	public void testUpdating() {
		Discography page = new Discography(Paths.get("test/Homestuck.htm").toUri().toString(),null,null);
		DummyProgressReporter pr = new DummyProgressReporter();
		try {
			assertTrue(page.updateFromNet(pr));
		} catch (ProblemsReadingDocumentException e) {
			fail("can't load from file");
		}
		assertNotNull(page.getChildByURLString("file:/album/homestuck-vol-9")); //the way relative url is resolved 
		assertTrue(page.isOK());
		assertEquals("Homestuck", page.getTitle());
		assertEquals(20, page.childPages.size());
		try {
			assertFalse(page.updateFromNet(pr));
		} catch (ProblemsReadingDocumentException e) {
			fail("can't load from file");
		}
		assertTrue(page.isOK());
		
		page.childPages.clear(); // now page is  different.
		try {
			assertTrue(page.updateFromNet(pr));
		} catch (ProblemsReadingDocumentException e) {
			fail("can't load from file");
		}
		assertTrue(page.isOK());
	}
	
	@Test
	public void testSavingData() {
		
	}
	
	

}
