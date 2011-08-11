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

	@Test
	public void testGetFSSafeName() {
		fail("Not yet implemented"); // TODO
	}

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
	public void testLoadFromCache() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testQueryXPathListStringDocument() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testReadCacheChild() {
		fail("Not yet implemented"); // TODO
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
