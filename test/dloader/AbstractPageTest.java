package dloader;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dloader.AbstractPage.ProblemsReadingDocumentException;

public class AbstractPageTest {
	
	class AbstractPageDummy extends AbstractPage {
		@Override
		protected String getChildNodesXPath() {return null;}

		@Override
		protected Element getSpecificDataXML() {return null;}

		@Override
		protected AbstractPage parseChild(Element element)
				throws ProblemsReadingDocumentException {return null;}

		@Override
		protected void parseSelf(Document doc)
				throws ProblemsReadingDocumentException {}

		@Override
		protected void readCacheSelf(Element e)
				throws ProblemsReadingDocumentException {}

		@Override
		public void saveResult(File saveTo) throws IOException {}

		@Override
		public File getChildrenSaveTo(File saveTo) throws IOException {return null;}
		
		AbstractPageDummy(String s) {super(s);}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test (expected = IllegalArgumentException.class)
	public void testAbstractPageStringWrongArg() {
		new AbstractPageDummy("fakeprotocol://sodope.com");
	}

	@Test
	public void testAbstractPageString() {
		AbstractPageDummy page = new AbstractPageDummy("www.google.com");
		assertEquals("http://www.google.com", page.url.toString());
	}

//	@Test
//	public void testGetFSSafeName() {
//		fail("Not yet implemented"); 
//	}

	@Test
	public void testDownloadPageDummyFromLocal() throws ProblemsReadingDocumentException {
		AbstractPageDummy p = new AbstractPageDummy("file:///D:/Gleb/JavaWorkspace_Eclipse/Dloader/test/Homestuck.htm");
		int b4 = WebDownloader.totalPageDownloadFinished;
		p.downloadPage();
		assertEquals(b4+1, WebDownloader.totalPageDownloadFinished);
		assertNotNull(p.childPages);
		assertEquals(0, p.childPages.length);
	}

	@Test
	public void testLoadFromCacheFailsOnNullArg() {
		AbstractPage p = new AbstractPageDummy("file:///D:/Gleb/JavaWorkspace_Eclipse/Dloader/test/Homestuck.htm");
		assertEquals(false, p.loadFromCache(null));
	}
	
	@Test
	public void testLoadFromCacheFailsIfCantFindElement() {
		AbstractPage p = new AbstractPageDummy("http://homestuck.bandcamp.com");
		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
		assertEquals(false, p.loadFromCache(cache.doc));
		
		AbstractPage p_discography = PageProcessor.detectPage("http://unknownband.bandcamp.com");
		assertEquals(false, p_discography.loadFromCache(cache.doc));
	}	

	@Test
	public void testLoadFromCacheSuccessOnLeafPage() {
		AbstractPage pNoChildren = PageProcessor.detectPage("http://homestuck.bandcamp.com/track/black-rose-green-sun");
		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
		assertEquals(true, pNoChildren.loadFromCache(cache.doc));
		assertEquals("Black Rose / Green Sun", pNoChildren.title);
		assertNull(pNoChildren.parent);
		assertNull(pNoChildren.childPages);
	}
	
	@Test
	public void testLoadFromCacheSuccessOnCustomPage() {
		AbstractPage p = PageProcessor.detectPage("http://noctura.bandcamp.com/album/demos");
		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
		assertEquals(true, p.loadFromCache(cache.doc));
		assertEquals("Demos", p.title);
		assertNotNull(p.childPages);
		assertEquals(7, p.childPages.length);
		assertEquals("http://noctura.bandcamp.com/track/dont-save-me-acoustic-for-x103", p.childPages[5].url.toString());
	}
	
	@Test
	public void testSaveToCache() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testScanXMLForThisElement() {
		fail("Not yet implemented"); // TODO
	}

}
