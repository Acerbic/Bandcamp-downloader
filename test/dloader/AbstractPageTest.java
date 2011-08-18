package dloader;

import static org.junit.Assert.*;

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
		public boolean saveResult(String saveTo) throws IOException {return false;}

		@Override
		public String getChildrenSaveTo(String saveTo) throws IOException {return null;}
		
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
	public void testDownloadPageDummyFromLocal() throws ProblemsReadingDocumentException {
		AbstractPageDummy p = new AbstractPageDummy("file:///D:/Gleb/JavaWorkspace_Eclipse/Dloader/test/Homestuck.htm");
		p.downloadPage();
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
	public void testSaveToCacheFailsOnBadPage() {
		AbstractPage p = PageProcessor.detectPage("http://noctura.bandcamp.com/album/demos");
		
		XMLCache cacheNew = new XMLCache("test/new_cache.xml");
		// p.title should be null by now.
		assertNull (p.title);
		Document old_doc = (Document) cacheNew.doc.clone();
		p.saveToCache(cacheNew.doc);
		assertEquals(old_doc.getDocType(), cacheNew.doc.getDocType());
		assertEquals(old_doc.getRootElement().getText(), cacheNew.doc.getRootElement().getText());
		
		p.title = "Title";
		p.url = null;
		p.saveToCache(cacheNew.doc);
		assertEquals(old_doc.getDocType(), cacheNew.doc.getDocType());
		assertEquals(old_doc.getRootElement().getText(), cacheNew.doc.getRootElement().getText());
	}
	
	@Test
	public void testSaveToCacheFailsIfGetSpecificDataXMLReturnsNull() {
		AbstractPage p = new AbstractPageDummy("http://noctura.bandcamp.com/album/demos");
		XMLCache cacheNew = new XMLCache("test/new_cache.xml");
		Document old_doc = (Document) cacheNew.doc.clone();
		
		p.saveToCache(cacheNew.doc);
		assertEquals(old_doc.getDocType(), cacheNew.doc.getDocType());
		assertEquals(old_doc.getRootElement().getText(), cacheNew.doc.getRootElement().getText());
	}

	@Test
	public void testSaveToCacheSuccessAtNewFile() {
		AbstractPage p = PageProcessor.detectPage("http://noctura.bandcamp.com/album/demos");
		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
		assertEquals(true, p.loadFromCache(cache.doc));
		XMLCache cacheNew = new XMLCache("test/new_cache.xml");
		p.saveToCache(cacheNew.doc);
		
		Element r = cacheNew.doc.getRootElement();
		assertEquals(1, r.getContentSize());
		Element e = (Element) r.getContent(0);
		assertEquals("Album",e.getName());
		assertEquals(7, e.getContentSize());
		r.addContent((Element)e.clone());
		r.addContent((Element)e.clone());
		p.saveToCache(cacheNew.doc);
		
		assertEquals(1, r.getContentSize());
		e = (Element) r.getContent(0);
		assertEquals("Album",e.getName());
		assertEquals(7, e.getContentSize());
	}
	
}
