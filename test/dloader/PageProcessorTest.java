package dloader;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PageProcessorTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDetectPageRecognition() {
		assertEquals(PageProcessor.detectPage("http://homestuck.bandcamp.org").getClass().getSimpleName(), 
				"Discography");
		assertEquals(PageProcessor.detectPage("http://homestuck.bandcamp.org/track/Name").getClass().getSimpleName(), 
				"Track");
		assertEquals(PageProcessor.detectPage("http://homestuck.bandcamp.org/album/Name").getClass().getSimpleName(), 
				"Album");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDetectPageErrorURL() {
		PageProcessor.detectPage("sadasd132");
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testDetectPageUnknownURL() {
		PageProcessor.detectPage("http://sadasd132.com/bonk?");
	}
}
