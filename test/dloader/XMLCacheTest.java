/**
 * 
 */
package dloader;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author A.Cerbic
 */
public class XMLCacheTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test(expected = NullPointerException.class)
	public void testXMLCacheConstructorDontAcceptNull() {
		new XMLCache(null);
	}

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testXMLCacheConstructorDontAcceptEmpty() {
		new XMLCache("");
	}

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testXMLCacheConstructorDontAcceptBadFilename() {
		new XMLCache("/&^#:.@:..@@\"");
	}

	/**
	 * Test method for {@link dloader.XMLCache#XMLCache(java.lang.String)}.
	 */
	@Test
	public void testXMLCacheConstructorCreatesNewStubDoc() throws IOException{
		String filename = "test_cache.xml";
		File f = new File(filename);
		if (f.exists())
			if (!f.delete())
				throw new IOException("Can't delete cache file for testing\n");
		XMLCache t = new XMLCache(filename);
		assertNotNull(t.doc);
		
		Document doc = t.doc;
		Element el = doc.getRootElement();
		assertNotNull(el);
		assertEquals(el.getName(), "root");
		assertEquals(1,doc.getContentSize());
		assertEquals(0,el.getContentSize());
	}
	
	/**
	 * Test method for {@link dloader.XMLCache#saveCache()}.
	 */
	@Test
	public void testSaveCache() {
		fail("Not yet implemented"); // TODO
	}

}
