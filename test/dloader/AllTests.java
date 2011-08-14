package dloader;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AbstractPageTest.class, PageProcessorTest.class,
		TrackTest.class, XMLCacheTest.class })
public class AllTests {

}
