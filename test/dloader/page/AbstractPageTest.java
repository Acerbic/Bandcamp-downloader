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
import java.util.logging.Level;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.*;

import dloader.page.AbstractPage;
import dloader.page.AbstractPage.ProblemsReadingDocumentException;
import dloader.pagejob.ProgressReporter;
import dloader.*;

public class AbstractPageTest {
	
	static
	private
	class AbstractPageDummy extends AbstractPage {

		public AbstractPageDummy(String stringURL, String saveTo,
				AbstractPage parent) throws IllegalArgumentException {
			super(stringURL, saveTo, parent);
		}
		
		@Override
		protected String getChildNodesXPath() {return null;}

		@Override 
		protected
		Element getSpecificDataXML() {return null;}

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
		public boolean saveResult(ProgressReporter progressIndicator)
				throws IOException {
			return false;
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
		String repS;
		long repI;
		
		@Override
		public void report(String type, long report) {
			repS = type;
			repI = report;
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
		boolean saveResult(ProgressReporter progressIndicator) throws IOException {
			Path p = Paths.get(saveTo, getFSSafeName(getTitle()));
			Files.createDirectories(p);
			return true;
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
		Main.cache = new XMLCache("test/pages_scan_cache.xml");
/*		//FIX ME:
		Files.deleteIfExists(testDirFilesPath.resolve("temp"));
		Files.createDirectory(testDirFilesPath.resolve("temp"));*/
	}

	@After
	public void tearDown() throws Exception {
		/*Files.deleteIfExists(testDirFilesPath.resolve("temp"));*/
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

	@Test (expected = IOException.class)
	public void testgetFSSafeName_NullArg() throws IOException {
		AbstractPage.getFSSafeName(null);
	}

	@Test 
	public void testgetFSSafeName() throws IOException {
		String res = AbstractPage.getFSSafeName("a<>b<");
		assertEquals("ab", res);
	}


	@Test
	public void testDownloadPage_DummyFromLocal() throws ProblemsReadingDocumentException, InterruptedException {
		
		AbstractPageDummy p = new AbstractPageDummy(
				testDirFilesPath.resolve("Homestuck.htm").toUri().toString(),
				testDirFilesPath.resolve("temp").toString(),null);
		p.downloadPage(new DummyProgressReporter());
		assertEquals(0, p.childPages.size());
	}
	
	@Test
	public void testDownloadPage_DiscographyFromLocal() throws ProblemsReadingDocumentException, MalformedURLException, InterruptedException {
		
		AbstractPage p = new DiscographyLocal(
				testDirFilesPath.resolve("Homestuck.htm").toUri().toString(),
				testDirFilesPath.resolve("temp").toString(),null);
		p.downloadPage(new DummyProgressReporter());
		assertEquals(20, p.childPages.size());
		assertEquals("Homestuck", p.getTitle());
		assertNotNull(p.getChildByURL(new URL("file:/album/homestuck-vol-9")));
	}
	
	@Test (expected = ProblemsReadingDocumentException.class)
	public void testDownloadPage_FailWrongAddress() throws ProblemsReadingDocumentException, InterruptedException {
		AbstractPage p = new DiscographyLocal(
				"http://www.whereismypage.comeone.nopage.qq",
				testDirFilesPath.resolve("temp").toString(),null);
		p.downloadPage(new DummyProgressReporter());
	}
	
	@Test (expected = ProblemsReadingDocumentException.class)
	public void testDownloadPage_FailWrongFile() throws ProblemsReadingDocumentException, InterruptedException {
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
				"http://homestuck-x.bandcamp.com",
				testDirFilesPath.resolve("temp").toString(),null);
		assertEquals(false, p.loadFromCache());
	}

	@Test
	public void testLoadFromCacheSuccessOnLeafPage() {
		AbstractPage pNoChildren = AbstractPage.bakeAPage(null,"http://homestuck.bandcamp.com/track/black-rose-green-sun", null, null);
		assertEquals(true, pNoChildren.loadFromCache());
		assertEquals("Black Rose / Green Sun", pNoChildren.getTitle());
		assertNull(pNoChildren.getParent());
		assertEquals(0, pNoChildren.childPages.size());
	}
	
	@Test
	public void testLoadFromCacheSuccessOnCustomPage() {
		AbstractPage p = AbstractPage.bakeAPage(null,"http://noctura.bandcamp.com/album/demos", null, null);
		assertEquals(true, p.loadFromCache());
		assertEquals("Demos", p.getTitle());
		assertEquals(7, p.childPages.size());
		boolean found = false;
		for (AbstractPage childPage: p.childPages) {
			if (childPage.url.toString().equals(
				"http://noctura.bandcamp.com/track/dont-save-me-acoustic-for-x103"))
				found = true;
		}
		assertEquals(true, found);
	}
	
	
	@Test
	public void testSaveToCacheFailsIfGetSpecificDataXMLReturnsNull() {
		AbstractPageDummy p = new AbstractPageDummy(
				"http://homestuck-x.bandcamp.com",
				testDirFilesPath.resolve("temp").toString(),null);
		assertEquals(false, p.loadFromCache());
		p.saveToCache();
		assertEquals(false, p.loadFromCache());
		
		p.setTitle("SomeTitle");
		p.saveToCache();
		assertEquals(false, p.loadFromCache());
	}
	@Test
	public void testSaveToCacheFailsIfTitleIsNull() {
		// inline class with getSpecificDataXML()  overridden
		AbstractPageDummy p = new AbstractPageDummy( 
				"http://homestuck-x.bandcamp.com",
				testDirFilesPath.resolve("temp").toString(),null) {
				
			@Override
			protected
			Element getSpecificDataXML() {return new Element("TestDummy");}
		};
		
		assertEquals(false, p.loadFromCache());
		p.saveToCache();
		assertEquals(false, p.loadFromCache());
		
		p.setTitle("SomeTitle");
		p.saveToCache();
		assertEquals(true, p.loadFromCache());
	}

	@Test
	public void testSaveToCacheSuccessAtNewFileAndOnUpdating() {
		// inline class with getSpecificDataXML()  overridden
		AbstractPageDummy p = new AbstractPageDummy( 
				"http://homestuck-x.bandcamp.com",
				testDirFilesPath.resolve("temp").toString(),null) {
				
			@Override
			protected
			Element getSpecificDataXML() {return new Element("TestDummy");}
		};
		assertFalse(p.loadFromCache());
		p.setTitle("SomeTitle");
		p.saveToCache();
		assertTrue(p.loadFromCache());
		assertEquals(0, p.childPages.size());
		p.childPages.add(new Discography("http://noctura.bandcamp.com",null,null)); 
		assertEquals(1, p.childPages.size());
		p.saveToCache();
		assertTrue(p.loadFromCache());
		assertEquals(1, p.childPages.size());
		
	}
	
	@Test
	public void testDownloadPageFromLocal() throws ProblemsReadingDocumentException, InterruptedException {
		AbstractPage p = new DiscographyLocal(
				testDirFilesPath.resolve("Homestuck.htm").toUri().toString(),
				null,null);
		DummyProgressReporter pr = new DummyProgressReporter();
		
		p.downloadPage(pr);
		assertEquals("Homestuck", p.getTitle());
		assertEquals(20, p.childPages.size());
	}
	
	@Test
	public void testDownloadPageFromNet() throws ProblemsReadingDocumentException, InterruptedException {
		AbstractPage p2 = new Album("http://homestuck.bandcamp.com/album/one-year-older/", null, null);
//		AbstractPage p = new DiscographyLocal("http://homestuck.bandcamp.com/",null,null);
		DummyProgressReporter pr = new DummyProgressReporter();
		
		p2.downloadPage(pr);
		assertEquals("One Year Older", p2.getTitle());
		assertEquals(17, p2.childPages.size());
	}
	
	@Test (expected = ProblemsReadingDocumentException.class)
	public void testDownloadPageFromNetFailsOnBadURL() throws ProblemsReadingDocumentException, InterruptedException {
		AbstractPage p = new DiscographyLocal("http://homestuck-x.bandcamp.com/",null,null);
		DummyProgressReporter pr = new DummyProgressReporter();
		
		p.downloadPage(pr);
	}	
	
	@Test
	public void testUpdateFromNet() throws ProblemsReadingDocumentException, InterruptedException {
		AbstractPage p = new DiscographyLocal("http://homestuck.bandcamp.com/",null,null);
		DummyProgressReporter pr = new DummyProgressReporter();
		p.loadFromCache();
		assertTrue(p.updateFromNet(pr));
		assertFalse(p.updateFromNet(pr));
	}
}
