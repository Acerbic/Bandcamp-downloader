package dloader.page;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dloader.page.AbstractPage;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;
import dloader.pagejob.ProgressReporter;
import dloader.*;

public class AbstractPageTest {
	
	static
	private
	class AbstractPageDummy extends AbstractPage {
		
		private
		XMLCache cache;

		public AbstractPageDummy(String stringURL, String saveTo,
				AbstractPage parent) throws IllegalArgumentException {
			super(stringURL, saveTo, parent);
			cache = new XMLCache("test/pages_scan_cache.xml");
		}

		@Override 
		public XMLCache getCache() {
			return cache;
		}
		
		
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
		public String saveResult(ProgressReporter progressIndicator)
				throws IOException {
			return null;
		}

		@Override
		public String getChildrenSaveTo() throws IOException {
			return null;
		}

		@Override
		public boolean isSavingNotRequired() {
			return false;
		}

		@Override
		public Collection<String> getThisPageFiles() {
			return null;
		}
	}

	class DummyProgressReporter implements ProgressReporter {

		@Override
		public void report(String type, int report) {
			System.out.printf("%s : %d", type, report);
		}
		
	}
	Path testDirFilesPath = Paths.get(System.getProperty("user.dir"),"test");
	
	
	static
	class DiscographyLocal extends AbstractPageDummy {

		private enum DiscographyListVariant { SIDEBAR, CENTRAL_INDEX };
		/**
		 * detected on parseSelf() call and dictates 
		 * what getChildNodesXPath() returns. 
		 */
		private DiscographyListVariant variant = DiscographyListVariant.SIDEBAR; 


		public DiscographyLocal(String url, String saveTo, AbstractPage parent) throws IllegalArgumentException
			{super(url, saveTo, parent);}
		
		@Override
		protected void parseSelf(Document doc) throws ProblemsReadingDocumentException  {
			List<?> result = queryXPathList("//pre:title", doc);
			if ((result != null) && (result.size()>0))
				setTitle(((Element) result.get(0)).getText());
			else
				throw new ProblemsReadingDocumentException("Can't read discography title");

			// now detect type of Discography
			result = queryXPathList("//pre:div[@id='discography']", doc);
			if (result.size()>0) {
				variant = DiscographyListVariant.SIDEBAR;
				return;
			}
			
			result = queryXPathList("//pre:div[@id='indexpage']", doc);
			if (result.size()>0) {
				variant = DiscographyListVariant.CENTRAL_INDEX;
				return;
			}
			throw new ProblemsReadingDocumentException("Can't detect discography type");
		}

		@Override
		public 
		String saveResult(ProgressReporter progressIndicator) throws IOException {
			Path p = Paths.get(saveTo, getFSSafeName(getTitle()));
			Files.createDirectories(p);
			return null;
		}

		@Override
		protected void readCacheSelf(Element e) {
		}

		@Override
		protected Element getSpecificDataXML() {
			return new Element("Discography");
		}

		// field polymorphism
		@Override
		protected String getChildNodesXPath() {
			switch (variant) {
				case SIDEBAR:
					return "//pre:div[@id='discography']//pre:div[@class='trackTitle']/pre:a";
				case CENTRAL_INDEX:
					return "//pre:div[@id='indexpage']//pre:h1/pre:a";
			}
			return null;
		}

		@Override
		protected AbstractPage parseChild(Element element) throws ProblemsReadingDocumentException  {
			try {
				URL u = resolveLink(element.getAttributeValue("href"));
				Album c = new Album(u.toString(), getChildrenSaveTo(), this);
				c.setTitle(element.getText());
				return c;
			} catch (NullPointerException|IllegalArgumentException|
					IOException e) {
				throw new ProblemsReadingDocumentException(e);
			}
		}

		@Override
		public String getChildrenSaveTo() throws IOException {
			return Paths.get(saveTo, getFSSafeName(getTitle())).toString();
		}

		@Override
		public boolean isSavingNotRequired() {
			Path p;
			try {
				p = Paths.get(getChildrenSaveTo());
				if (Files.isDirectory(p))
					return true;
			} catch (IOException e) {
				Main.log(Level.WARNING,null,e);
			}
			return false;
		}

		@Override
		public Collection<String> getThisPageFiles() {
			Collection <String> fileset = new LinkedList<String>();
			try {
				fileset.add( getChildrenSaveTo() );
			} catch (IOException e) {
			}
			return fileset;
		}
	}
	@Before
	public void setUp() throws Exception {
		//FIXME:
		Files.deleteIfExists(testDirFilesPath.resolve("temp"));
		Files.createDirectory(testDirFilesPath.resolve("temp"));
	}

	@After
	public void tearDown() throws Exception {
		Files.deleteIfExists(testDirFilesPath.resolve("temp"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testAbstractPageStringWrongArg() {
		new AbstractPageDummy("fakeprotocol://sodope.com",null,null);
	}

	
	@Test (expected = IllegalArgumentException.class)
	public void testAbstractPageStringWrongArg2() {
		new AbstractPageDummy(null,"c:\temp",null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testAbstractPageStringWrongArg3() {
		new AbstractPageDummy(null,null,null);
	}

	@Test
	public void testAbstractPage_constructor() {
		AbstractPageDummy page = new AbstractPageDummy("www.google.com","c:\\temp",null);
		assertEquals("http://www.google.com", page.url.toString());
		assertEquals("c:\\temp", page.saveTo);
		AbstractPageDummy page2 = new AbstractPageDummy("www.google.com","c:\\temp",page);
		assertEquals(page, page2.getParent());
	}

	@Test (expected = NullPointerException.class)
	public void testgetFSSafeName_NullArg() throws IOException {
		AbstractPage.getFSSafeName(null);
	}

	@Test 
	public void testgetFSSafeName() throws IOException {
		String res = AbstractPage.getFSSafeName("a<>b<");
		assertEquals("ab", res);
	}


	@Test
	public void testDownloadPage_DummyFromLocal() throws ProblemsReadingDocumentException {
		
		AbstractPageDummy p = new AbstractPageDummy(
				testDirFilesPath.resolve("Homestuck.htm").toUri().toString(),
				testDirFilesPath.resolve("temp").toString(),null);
		p.downloadPage(new DummyProgressReporter());
		assertEquals(0, p.childPages.size());
	}
	
	@Test
	public void testDownloadPage_DiscographyFromLocal() throws ProblemsReadingDocumentException, MalformedURLException {
		
		AbstractPage p = new DiscographyLocal(
				testDirFilesPath.resolve("Homestuck.htm").toUri().toString(),
				testDirFilesPath.resolve("temp").toString(),null);
		p.downloadPage(new DummyProgressReporter());
		assertEquals(20, p.childPages.size());
		assertEquals("Homestuck", p.getTitle());
		assertNotNull(p.getChildByURL(new URL("file:/album/homestuck-vol-9")));
	}
	
	@Test (expected = ProblemsReadingDocumentException.class)
	public void testDownloadPage_FailWrongAddress() throws ProblemsReadingDocumentException {
		AbstractPage p = new DiscographyLocal(
				"http://www.whereismypage.comeone.nopage.qq",
				testDirFilesPath.resolve("temp").toString(),null);
		p.downloadPage(new DummyProgressReporter());
	}
	
	@Test (expected = ProblemsReadingDocumentException.class)
	public void testDownloadPage_FailWrongFile() throws ProblemsReadingDocumentException {
		AbstractPage p = new DiscographyLocal(
				testDirFilesPath.resolve("Revelations III.mp3").toUri().toString(),
				testDirFilesPath.resolve("temp").toString(),null);
		p.downloadPage(new DummyProgressReporter());
	}	

	@Test
	public void testLoadFromCacheFailsOnNullArg() {
		AbstractPageDummy p = new AbstractPageDummy(
				testDirFilesPath.resolve("Homestuck.htm").toUri().toString(),
				testDirFilesPath.resolve("temp").toString(),null);
		assertEquals(false, p.loadFromCache());
	}
	
	@Test
	public void testLoadFromCacheFailsIfCantFindElement() {
		AbstractPageDummy p = new AbstractPageDummy(
				"http://homestuck.bandcamp.com",
				testDirFilesPath.resolve("temp").toString(),null);
		assertEquals(false, p.loadFromCache());
		
	}	
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
	
}
