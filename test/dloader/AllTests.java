package dloader;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import dloader.page.*;

@RunWith(Suite.class)
@SuiteClasses({ AbstractPageTest.class, PageProcessorTest.class,
	dloader.page.TrackTest.class, XMLCacheTest.class })
public class AllTests {

}
