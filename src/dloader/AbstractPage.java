package dloader;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.*;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;


/**
 * Basic class to download page, parse it and download elements it references.  
 */
public abstract class AbstractPage {

	/**
	 * Exception generated if data cannot be read from XML cache 
	 * or downloaded document
	 * @author A.Cerbic
	 */
	@SuppressWarnings("serial")
	public class ProblemsReadingDocumentException extends Exception {
		
		public String problemDocumentURL;
		public String parentDocumentURL; /** may be null */

		public ProblemsReadingDocumentException() {
			super();
			problemDocumentURL = AbstractPage.this.url.toString();
		}
		public ProblemsReadingDocumentException(String s) {
			super(s);
			problemDocumentURL = AbstractPage.this.url.toString();
		}
		public ProblemsReadingDocumentException(Throwable e) {
			super(e);
			problemDocumentURL = AbstractPage.this.url.toString();
		}
	}
	
	/** 
	 * title of this item (as stored into cache) - SHOULD NOT be null
	 */
	public String title;
	/**
	 * url of a page referencing this item - SHOULD NOT be null 
	 */
	public URL url;
	/**
	 * array of a children items to this one (can be of size zero)
	 */
	public AbstractPage[] childPages;
	/**
	 * reference to a parent item (may be null)
	 */
	public AbstractPage parent;
	/**
	 * use caching facility?
	 */
	public static boolean isUsingCache;
	
	/**
	 * Inherited by all descendants and instances, providing unified logging.
	 */
	protected static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);;
	
	/**
	 * Is called ONLY for Class.newInstance reason.
	 * Make sure setUrl(URL url) is called right after it. 
	 */
	public AbstractPage() {}

	/**
	 * Constructs from web address
	 * @param stringURL - web address
	 * @throws IllegalArgumentException if stringURL is null or bad 
	 */
	public AbstractPage(String stringURL) throws IllegalArgumentException {
		try {url = resolveLink(stringURL);}
		catch (MalformedURLException e) {throw new IllegalArgumentException(e);}
		catch (NullPointerException e) {throw new IllegalArgumentException(e);}
	}

	/**
	 * Constructs from given URL object 
	 * @param _url - gets assigned by reference, not copied.
	 * @throws IllegalArgumentException if _url == null 
	 */
	public AbstractPage(URL _url) throws IllegalArgumentException {
		if (_url == null) throw new IllegalArgumentException();
		url = _url;
	}

//	/** 
//	 * this is called when acquiring item fails and must be dropped or restarted.
//	 */
//	public final void acquisitionFailedHook (AbstractPage failedItem) {
//		
//	}
	
//	/**
//	 * Fills this instance with data, reading it from cache or parsing web page.
//	 * Then repeats process for children pages
//	 * @param forceDownload - true if you want ignore cache on this one, all child nodes checks are
//	 * controlled with <b>isUsingCache</b> flag
//	 * @param doc - XML document storing cache on pages data.  
//	 * @throws ProblemsReadingDocumentException if failed. (generally it means that web server did not respond right)
//	 */
//	public final void acquireData(boolean forceDownload, org.jdom.Document doc) throws ProblemsReadingDocumentException {
//		assert (url != null);
//		logger.info( String.format("(%s) Starting aquisition of %s ... %n", 
//				this.getClass().getName(), url.toString()));
//		
//		boolean isLoaded = false;
//		if (!forceDownload && isUsingCache)
//			isLoaded = loadFromCache(doc);
//		if (!isLoaded) {  
//			downloadPage();
//			saveToCache(doc);
//		}
//		
//		if (childPages == null) {
//			logger.info(String.format("Reading of %s is done. %n", url.toString()));
//			return;
//		}
//
//		logger.info( String.format("Reading of %s done. Children: %d%n", childPages.length));
//		for (int i = 0; i < childPages.length; i++) 
//			try {
//				childPages[i].acquireData(false, doc);
//			} catch (ProblemsReadingDocumentException e) {
//				acquisitionFailedHook(childPages[i]);
//				childPages[i] = null;
//				logger.log(Level.WARNING, "", e);
//			} //skip to the next child page
//	}
	
	/**
	 * Convert a string to a proper file name.
	 * (checks for Windows prohibited chars only) 
	 * @param from - string to convert
	 * @return proper file name
	 * @throws IOException if file name is not valid  
	 */
	public static String getFSSafeName(String from) throws IOException   {
		assert (from!=null);
		String s = new String(from);
		for (char c : ":/\\*?\"<>|\t\n\r".toCharArray())
			s = s.replace(c, ' ');
	
		File f = new File (s);
		
		try {
			// very awkward test for file name validness
			if (f.createNewFile())
				f.delete();
			return s;
		} catch (IOException e) {
			// OK lets go try-hard on this
			int hash = 0;
			for (char c: s.toCharArray())
				hash = (hash + (int)c)*2;
			s = String.valueOf(hash);
	
			f = new File(s);
			// very awkward test for file name validness again
			if (f.createNewFile())
				f.delete();
			return s;
		}
	}

	/** 
	 * Downloads the page and creates child nodes.
	 * @throws ProblemsReadingDocumentException if any error
	 */
	void downloadPage() throws ProblemsReadingDocumentException {
		logger.log(Level.FINE, String.format("Downloading %s from network...%n", url.toString()));
		
		org.jdom.Document doc = null;
		try {
			SAXBuilder builder = new SAXBuilder("org.ccil.cowan.tagsoup.Parser");
			doc = builder.build(url.toString());
		} catch (Throwable e) {
			throw new ProblemsReadingDocumentException(e);
		}

		// discover info about this page
		parseSelf(doc);  
			
		// discover info about children pages
		@SuppressWarnings("unchecked")
		List<Element> result = (List<Element>) queryXPathList(getChildNodesXPath(), doc);
		childPages = new AbstractPage[result.size()]; // might be initialized to zero-sized array
		for (int i = 0; i<result.size(); i++) {
			try {
				childPages[i] = parseChild(result.get(i));
				childPages[i].parent = this;
			} catch (ProblemsReadingDocumentException e) {
				logger.log(Level.WARNING, "unable to parse child data", e);
			} // skip this child to next one
		}
		logger.log(Level.FINE, String.format("\t... finished downloading %s.%n", url.toString()));
	}

	/**
	 * Returns an XPath string to get links to children pages 
	 * from current page. All tags in the path are in "pre" namespace.
	 * @return the XPath string or null, if no children expected.
	 */
	protected abstract String getChildNodesXPath();

	/**
	 * Implemented by descendants to provide their own attributes for caching into XML.
	 * @return JDOM element with some custom data of this node.
	 */
	protected abstract Element getSpecificDataXML() ;	

	/**
	 * Gets data about this page from cache (JDOM tree) and creates child nodes.
	 * @param doc - JDOM Document to load from 
	 * @return true if data acquired successfully, false otherwise
	 */
	boolean loadFromCache (org.jdom.Document doc) {
		if (doc == null) return false;
		
		logger.log(Level.FINE, String.format("Reading %s from cache...%n",url.toString()));
		try {
			Element e = scanXMLForThisElement(doc);
			if (null == e) return false;
			title = e.getAttributeValue("title");  
			readCacheSelf(e);
			
			@SuppressWarnings("unchecked")
			List<Element> l = e.getContent(new ElementFilter("childref"));
			int size = l.size();
			if (size>0) {
				childPages = new AbstractPage[size];
				Iterator<Element> itr = l.listIterator();
				for (int i=0; i<size; i++) {
					childPages[i] = readCacheChild(itr.next());
					childPages[i].parent = this; 
				}
			}
			logger.log(Level.FINE, String.format("\t...Finished reading %s.%n",url.toString()));
			return true;
		} catch (ProblemsReadingDocumentException e) {
			// If ANY problem, quit with a fail code
			childPages = null; 
			return false;
		}
		
	}
	
	/**
	 * Extracts information from downloaded page about child pages
	 * @param element - fragment of a page containing data about a child page
	 * @return New child object
	 * @throws ProblemsReadingDocumentException if child cannot be created from this Element
	 */
	protected abstract AbstractPage parseChild(Element element) throws ProblemsReadingDocumentException;
	
	/**
	 * Extracts more information after a download about this page
	 * @param doc - JDOM Document of the page parsed
	 */
	protected abstract void parseSelf(Document doc) throws ProblemsReadingDocumentException;
	
	/**
	 * Queries given JDOM document with XPath string
	 * @param q - XPath string with all nodes in "pre" namespace
	 * @param doc - JDOM Document or Element
	 * @return List of found matches, may be of zero size if nothing is found
	 */
	protected final List<?> queryXPathList(String q, Document doc) {
		return queryXPathList (q, doc.getRootElement());
	}
	
	/**
	 * Queries given JDOM document with XPath string
	 * @param q - XPath string with all nodes in "pre" namespace
	 * @param doc - JDOM Document or Element
	 * @return List of found matches, may be of zero size if nothing is found
	 */
	protected final List<?> queryXPathList(String q, Element doc) {
		if (q == null) return new ArrayList<Object>(0);
		try {
			String nsURI = doc.getNamespaceURI();
			XPath xpath = new JDOMXPath(q);
			xpath.addNamespace("pre", nsURI);
			return xpath.selectNodes(doc);
		} catch (JaxenException e) {
			logger.log(Level.SEVERE,"",e);
			return new ArrayList<Object>(0);
		} 
	}
	
	/**
	 * Reads child's page title/url data from cache and creates a node.
	 * @param childRef - <childref ...> tag describing a child
	 * @return new PageParser child node.
	 * @throws ProblemsReadingDocumentException if anything went wrong
	 */
	AbstractPage readCacheChild(Element childRef) throws ProblemsReadingDocumentException {
		AbstractPage child = null;
		try {
			String u = childRef.getAttributeValue("url");
			String c = childRef.getAttributeValue("class");
			child = (AbstractPage)Class.forName("dloader."+c).newInstance();
			child.setUrl(u);
		} catch (ClassNotFoundException e1) {
			throw new ProblemsReadingDocumentException(e1);
		} catch (InstantiationException e1) {
			throw new ProblemsReadingDocumentException(e1);
		} catch (IllegalAccessException e1) {
			throw new ProblemsReadingDocumentException(e1);
		} catch (MalformedURLException e1) {
			throw new ProblemsReadingDocumentException(e1);
		} catch (NullPointerException e1) {
			throw new ProblemsReadingDocumentException(e1);
		}  
		return child;
			
	}
	
	/**
	 * Reads class-specific info from XML cache element.
	 * @param e - element to read from
	 * @throws ProblemsReadingDocumentException if reading data from XML fails
	 */
	protected abstract void readCacheSelf(Element e) throws ProblemsReadingDocumentException;

	/**
	 * Gets an URL to resource referenced from this page. 
	 * Uses this.url as a base link to resolve relative paths. 
	 * @param link - relative or absolute link
	 * @return proper URL with absolute path
	 * @throws MalformedURLException 
	 */
	protected final URL resolveLink(String link) throws MalformedURLException {
		try {
			return new URL(url, link);
		} catch (MalformedURLException e) {
			if (!link.contains("://"))
				link = "http://"+link; // default protocol 
			return new URL(url, link);
		}
	}

	/**
	 * Saves extracted data to disk, then saves children too. 
	 * @param saveTo - directory to save info to.
	 * @throws IOException
	 */
	public abstract void saveResult(File saveTo) throws IOException;

	/**
	 * Saves this page data into XML tree. 
	 * Only references to child pages are saved, not the pages data.
	 * @param doc - JDOM Document holding a cache to save to
	 */
	void saveToCache (org.jdom.Document doc) {
		assert (doc != null);
		
		// absolutely required fields
		if (title==null || url==null)
			return;
		//1. Compose this one and childrefs
		Element e = getSpecificDataXML();
		if (e==null) return; // element is corrupt and should not be cached 
		e.setAttribute("title", title);
		e.setAttribute("url", url.toString());
		if (childPages != null) 
			for (AbstractPage child: childPages) 
				if (child != null) {
					Element childElement = new Element("childref");
					childElement.setAttribute("class",child.getClass().getSimpleName());
					childElement.setAttribute("url",child.url.toString());
					e.addContent(childElement);
				}
		//2. Drop old elements with the same URL
		Element root = doc.getRootElement();
		
		@SuppressWarnings("unchecked")
		List<Element> oldCachedElements = (List<Element>) queryXPathList(
//				String.format("//pre:%s[@url='%s']",e.getName(),url.toString()), 
				String.format("//%s[@url='%s']",e.getName(),url.toString()), 
				doc);
		for (Element current: oldCachedElements) 
			current.detach();

		//3. Add this element to cache
		root.addContent(e);
	}

	/**
	 * Scans XML tree to find the 1st Element eligible to read from
	 * @return found Element or null
	 */
	Element scanXMLForThisElement(org.jdom.Document doc) {
		assert (doc != null); assert (url != null);
		String searchXPath = String.format("//%s[@url='%s']",getClass().getSimpleName(),url.toString());
		List<?> result = queryXPathList(searchXPath, doc);
		
		return result.size()>0?(Element)result.get(0):null;
	}
	/**
	 * Fixes the url after no-argument constructor was called
	 * @param s - URL String to initialize from
	 * @throws MalformedURLException if s is bad or null
	 */
	void setUrl(String s) throws MalformedURLException {
		url = resolveLink(s);
	}

	/**
	 * Generate new saving path for the children of this page from its own saveTo and page data
	 * @return saving path for the children pages or null if no children expected
	 * @throws IOException if valid filename cannot be created
	 */
	abstract public File getChildrenSaveTo(File saveTo) throws IOException;
}