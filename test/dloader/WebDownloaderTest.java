package dloader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WebDownloaderTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFetchWebFileURLString() throws IOException {
		Path p = Paths.get("test/classicplus.png");
		Files.deleteIfExists(p);
		WebDownloader.fetchWebFile("http://www.google.com/logos/classicplus.png", 
				p.toString(), null);
		assertEquals(true, Files.exists(p));
		assertEquals(11508,Files.size(p)); 
		Files.deleteIfExists(p);
	}
	
}
