/**
 * 
 */
package dloader;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.jdom2.*;
import org.junit.*;

/**
 * @author A.Cerbic
 */
public class XMLCacheTest {

	String tmpFilename = "test_cache.xml";
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		File f = new File(tmpFilename);
		if (f.exists())
			if (!f.delete())
				throw new IOException("Can't delete cache file for testing\n");
		f = null;
	}
//
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@After
//	public void tearDown() throws Exception {
//		File f = new File(tmpFilename);
//		if (f.exists())
//			if (!f.delete())
//				throw new IOException("Can't delete cache file after testing\n");
//		f = null;
//	}
//
//	/**
//	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
//	 */
//	@Test(expected = IllegalArgumentException.class)
//	public void testXMLCacheDontAcceptNull() {
//		new XMLCache(null);
//	}
//
//	/**
//	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
//	 */
//	@Test(expected = IllegalArgumentException.class)
//	public void testXMLCacheDontAcceptEmpty() {
//		new XMLCache("");
//	}
//
//	/**
//	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
//	 */
//	@Test(expected = IllegalArgumentException.class)
//	public void testXMLCacheDontAcceptBadFilename() {
//		new XMLCache("/&^#:.@:..@@\"");
//	}
//
//	/**
//	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
//	 */
//	@Test
//	public void testXMLCacheCreatesNewStubDoc() throws IOException{
//		
//		XMLCache t = new XMLCache(tmpFilename);
//		assertNotNull(t.doc);
//		
//		Document doc = t.doc;
//		Element el = doc.getRootElement();
//		
//		assertNotNull(el);
//		assertEquals(el.getName(), "root");
//		assertEquals(1,doc.getContentSize());
//		assertEquals(0,el.getContentSize());
//	}
//	
//	/**
//	 * Test method for {@link dloader.XMLCache#saveCache()}.
//	 */
//	@Test
//	public void testSaveCacheCreatesFileAndWritesStubDocument() throws IOException {
//		XMLCache t = new XMLCache(tmpFilename);
//		t.saveCache();
//		assertTrue(new File(tmpFilename).exists());
//		XMLCache t2 = new XMLCache(tmpFilename);
//		assertEquals(t.doc.toString(), t2.doc.toString());
//	}
//
//	@Test
//	public void testSavingAndReadingMockDocument() throws IOException {
//		XMLCache t = new XMLCache(tmpFilename);
//		Document d = new Document(new Element("A"));
//		
//		Element r = d.getRootElement();
//		r.addContent(new Element("A1"));
//		r.addContent(new Element("A2"));
//		r.addContent(new Element("A3"));
//
//		t.doc.setContent(r.detach());
//		t.saveCache();
//		t = new XMLCache(tmpFilename);
//		
//		r = t.doc.getRootElement();
//		assertEquals(r.getName(), "A");
//		assertEquals(((Element)r.getChildren().get(2)).getName(), "A3");
//	}
//	
//	@Test
//	public void testReadingActualCacheSnapshot() {
//		XMLCache t = new XMLCache("test/pages_scan_cache.xml");
//		assertNotNull(t.doc);
//	}
}
