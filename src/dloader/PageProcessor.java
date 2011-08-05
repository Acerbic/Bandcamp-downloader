package dloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Queue;

/**
 * This class handles multiple pages downloads and general algorithm of the job
 * @author A.Cerbic
 */
public class PageProcessor {
	
	private Queue<AbstractPage> reconQ;
	private Queue<AbstractPage> aquisitionQ;

	public Queue<AbstractPage> getReconQ() {
		return reconQ;
	}

	public Queue<AbstractPage> getAquisitionQ() {
		return aquisitionQ;
	}

	/**
	 * Detects page type by its URL address (String)
	 * @param baseURL - String representation of URL
	 * @return new PageParser descendant fitting for the page
	 * @throws IllegalArgumentException - when baseURL is bad or null
	 */
	public static final AbstractPage detectPage(String baseURL) throws IllegalArgumentException {
		URL u;
		try {
			u = new URL(baseURL);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		
		if (baseURL.contains("/track/")) 
			return new Track(baseURL);
		if (baseURL.contains("/album/")) 
			return new Album(baseURL);
		if (u.getPath().isEmpty())
			return new Discography(baseURL);
		
		throw new IllegalArgumentException();
	}

}
