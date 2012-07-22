package dloader;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import dloader.page.*;

@RunWith(Suite.class)
@SuiteClasses({ AbstractPageTest.class, TrackTest.class, DiscographyTest.class, 
	JobMasterTest.class, XMLCacheTest.class, WebDownloaderTest.class })
public class AllTests {

}
