package dloader.page;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.jdom.Document;
//import org.jdom.Element;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import dloader.page.AbstractPage;
//import dloader.page.AbstractPage.ProblemsReadingDocumentException;
//import dloader.*;

public class AbstractPageTest {
	
//	class AbstractPageDummy extends AbstractPage {
//		@Override
//		protected String getChildNodesXPath() {return null;}
//
//		@Override
//		protected Element getSpecificDataXML() {return null;}
//
//		@Override
//		protected AbstractPage parseChild(Element element)
//				throws ProblemsReadingDocumentException {return null;}
//
//		@Override
//		protected void parseSelf(Document doc)
//				throws ProblemsReadingDocumentException {}
//
//		@Override
//		protected void readCacheSelf(Element e)
//				throws ProblemsReadingDocumentException {}
//
//		@Override
//		public String saveResult(String saveTo, AtomicInteger progressIndicator) throws IOException {return null;}
//
//		@Override
//		public String getChildrenSaveTo(String saveTo) throws IOException {return null;}
//		
//		public AbstractPageDummy(String s) {super(s, null);}
//
//		public AbstractPageDummy(URL test) {super(test, null);}
//
//		@Override
//		public boolean isSavingNotRequired(String saveTo) {return false;}
//	}
//
//	@Before
//	public void setUp() throws Exception {
//	}
//
//	@After
//	public void tearDown() throws Exception {
//	}
//
//	@Test (expected = IllegalArgumentException.class)
//	public void testAbstractPageStringWrongArg() {
//		new AbstractPageDummy("fakeprotocol://sodope.com");
//	}
//
//	@Test
//	public void testAbstractPageString() {
//		AbstractPageDummy page = new AbstractPageDummy("www.google.com");
//		assertEquals("http://www.google.com", page.url.toString());
//	}
//
//	@Test (expected = IllegalArgumentException.class)
//	public void testAbstractPageURLWrongArg() {
//		URL test = null;
//		new AbstractPageDummy(test);
//	}
//	
//	@Test 
//	public void testAbstractPageURL() throws MalformedURLException {
//		URL test = new URL("file:///some.file");
//		new AbstractPageDummy(test);
//	}
//
//	@Test (expected = NullPointerException.class)
//	public void testgetFSSafeNameNullArg() throws IOException {
//		AbstractPage.getFSSafeName(null);
//	}
//
//	@Test 
//	public void testgetFSSafeName() throws IOException {
//		String res = AbstractPage.getFSSafeName("a<>b<");
//		assertEquals("ab", res);
//	}
//
//	@Test
//	public void testDownloadPageDummyFromLocal() throws ProblemsReadingDocumentException {
//		AbstractPageDummy p = new AbstractPageDummy("file:///D:/Gleb/Eclipse Workspaces/Dloader/Dloader/test/Homestuck.htm");
//		p.downloadPage();
//		assertEquals(0, p.childPages.size());
//	}
//
//	@Test
//	public void testLoadFromCacheFailsOnNullArg() {
//		AbstractPage p = new AbstractPageDummy("file:///D:/Gleb/JavaWorkspace_Eclipse/Dloader/test/Homestuck.htm");
//		assertEquals(false, p.loadFromCache(null));
//	}
//	
//	@Test
//	public void testLoadFromCacheFailsIfCantFindElement() {
//		AbstractPage p = new AbstractPageDummy("http://homestuck.bandcamp.com");
//		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
//		assertEquals(false, p.loadFromCache(cache.doc));
//		
//		AbstractPage p_discography = PageProcessor.detectPage("http://unknownband.bandcamp.com");
//		assertEquals(false, p_discography.loadFromCache(cache.doc));
//	}	
//
//	@Test
//	public void testLoadFromCacheSuccessOnLeafPage() {
//		AbstractPage pNoChildren = PageProcessor.detectPage("http://homestuck.bandcamp.com/track/black-rose-green-sun");
//		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
//		assertEquals(true, pNoChildren.loadFromCache(cache.doc));
//		assertEquals("Black Rose / Green Sun", pNoChildren.getTitle());
//		assertNull(pNoChildren.parent);
//		assertEquals(0, pNoChildren.childPages.size());
//	}
//	
//	@Test
//	public void testLoadFromCacheSuccessOnCustomPage() {
//		AbstractPage p = PageProcessor.detectPage("http://noctura.bandcamp.com/album/demos");
//		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
//		assertEquals(true, p.loadFromCache(cache.doc));
//		assertEquals("Demos", p.getTitle());
//		assertEquals(7, p.childPages.size());
//		boolean found = false;
//		for (AbstractPage childPage: p.childPages) {
//			if (childPage.url.toString().equals(
//				"http://noctura.bandcamp.com/track/dont-save-me-acoustic-for-x103"))
//				found = true;
//		}
//		assertEquals(true, found);
//	}
//	
//	@Test
//	public void testSaveToCacheFailsOnBadPage() {
//		AbstractPage p = PageProcessor.detectPage("http://noctura.bandcamp.com/album/demos");
//		
//		XMLCache cacheNew = new XMLCache("test/new_cache.xml");
//		// p.title should be null by now.
//		assertNull (p.getTitle());
//		Document old_doc = (Document) cacheNew.doc.clone();
//		p.saveToCache(cacheNew.doc);
//		assertEquals(old_doc.getDocType(), cacheNew.doc.getDocType());
//		assertEquals(old_doc.getRootElement().getText(), cacheNew.doc.getRootElement().getText());
//		
//		p.saveToCache(cacheNew.doc); // fails because of null title
//		assertEquals(old_doc.getDocType(), cacheNew.doc.getDocType());
//		assertEquals(old_doc.getRootElement().getText(), cacheNew.doc.getRootElement().getText());
//	}
//	
//	@Test
//	public void testSaveToCacheFailsIfGetSpecificDataXMLReturnsNull() {
//		AbstractPage p = new AbstractPageDummy("http://noctura.bandcamp.com/album/demos");
//		XMLCache cacheNew = new XMLCache("test/new_cache.xml");
//		Document old_doc = (Document) cacheNew.doc.clone();
//		
//		p.saveToCache(cacheNew.doc);
//		assertEquals(old_doc.getDocType(), cacheNew.doc.getDocType());
//		assertEquals(old_doc.getRootElement().getText(), cacheNew.doc.getRootElement().getText());
//	}
//
//	@Test
//	public void testSaveToCacheSuccessAtNewFile() {
//		AbstractPage p = PageProcessor.detectPage("http://noctura.bandcamp.com/album/demos");
//		XMLCache cache = new XMLCache("test/pages_scan_cache.xml");
//		assertEquals(true, p.loadFromCache(cache.doc));
//		XMLCache cacheNew = new XMLCache("test/new_cache.xml");
//		p.saveToCache(cacheNew.doc);
//		
//		Element r = cacheNew.doc.getRootElement();
//		assertEquals(1, r.getContentSize());
//		Element e = (Element) r.getContent(0);
//		assertEquals("Album",e.getName());
//		assertEquals(7, e.getContentSize());
//		r.addContent((Element)e.clone());
//		r.addContent((Element)e.clone());
//		p.saveToCache(cacheNew.doc);
//		
//		assertEquals(1, r.getContentSize());
//		e = (Element) r.getContent(0);
//		assertEquals("Album",e.getName());
//		assertEquals(7, e.getContentSize());
//	}
//	
}
