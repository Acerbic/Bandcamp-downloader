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

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		File f = new File(tmpFilename);
		if (f.exists())
			if (!f.delete())
				throw new IOException("Can't delete cache file after testing\n");
		f = null;
	}

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testXMLCacheDontAcceptNull() {
		new XMLCache(null);
	}

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testXMLCacheDontAcceptEmpty() {
		new XMLCache("");
	}

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testXMLCacheDontAcceptBadFilename() {
		new XMLCache("/&^#:.@:..@@\"");
	}
	

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test
	public void testXMLCacheCreatesNewStubDoc() throws IOException{
		
		XMLCache t = new XMLCache(tmpFilename);
		assertNotNull(t);
	}
	
	/**
	 * Test method for {@link dloader.XMLCache#saveCache()}.
	 */
	@Test
	public void testSaveCacheCreatesFileAndWritesStubDocument() throws IOException {
		XMLCache t1 = new XMLCache(tmpFilename);
		Element a1 = new Element("TEST");
		a1.setAttribute("url", "someurl");
		t1.addElementWithReplacement(a1);
		t1.saveCache();
		assertTrue(new File(tmpFilename).exists());
		XMLCache t2 = new XMLCache(tmpFilename);
		assertEquals(a1.getValue(), t2.getElementForPage("someurl").getValue());
	}


	@Test
	public void testReadingActualCacheSnapshot() {
		XMLCache t = new XMLCache("test/pages_scan_cache.xml");
		assertNotNull(t);
		assertEquals("Discography",t.getElementForPage("http://homestuck.bandcamp.com").getName());
	}
}
