package dloader.page;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.*;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

import dloader.WebDownloader;


/**
 * Basic class to download page, parse it and download elements it references.
 * 
 *   Objects of this class are mutable 
 *   (effectively immutable after initialization with downloadPage() and loadFromCache(),
 *    but that can be delayed)
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
	 * title of this item 
	 */
	private volatile String title; // guarded by (this) on write. volatile for reading
	
	/**
	 * url of a page referencing this item 
	 */
	public final URL url;
	/**
	 * reference to a parent item (may be null)
	 */
	public final AbstractPage parent;
	
	/**
	 * List of a children items to this page (can be of size zero)
	 */
	public final List<AbstractPage> childPages = Collections.synchronizedList(new ArrayList<AbstractPage>()); 

	/**
	 * Inherited by all descendants and instances, providing unified logging.
	 */
	protected static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);;
	
	public String getTitle() {return title;} 
	
	public synchronized 
	void setTitle(String title) {this.title = title;}
//	public final 
//	int getChildPagesNum() {return childPages.size();}
//	public final 
//	AbstractPage getChild(int n) {return childPages.get(n);}

	/**
	 * Constructs from web address
	 * @param stringURL - web address
	 * @throws IllegalArgumentException if stringURL is null or bad 
	 */
	public AbstractPage(String stringURL, AbstractPage parent) throws IllegalArgumentException {
		try {url = resolveLink(stringURL);}
		catch (MalformedURLException e) {throw new IllegalArgumentException(e);}
		catch (NullPointerException e) {throw new IllegalArgumentException(e);}
		this.parent = parent;
	}

	/**
	 * Constructs from given URL object 
	 * @param url - gets assigned by reference, not copied.
	 * @throws IllegalArgumentException if url == null 
	 */
	public AbstractPage(URL url, AbstractPage parent) throws IllegalArgumentException {
		if (url == null) throw new IllegalArgumentException();
		this.url = url;
		this.parent = parent; 
	}

	/**
	 * Convert a string to a proper file name (NOT path, only filename).
	 * (don't check for existing file collisions, only validness of a name) 
	 * @param name - string to convert
	 * @return proper file name
	 * @throws IOException if file name is not valid  
	 */
	public static final
	String getFSSafeName(String name) throws IOException   {
		for (char c : ":/\\*?\"<>|\t\n\r".toCharArray())
			name = name.replace(String.valueOf(c), "");
		name = name.trim(); // only trailing spaces are forbidden
	
		try {
			Paths.get(name);
		} catch (InvalidPathException e) {
			// OK lets go try-hard on this
			int hash = 0;
			for (char c: name.toCharArray())
				hash = (hash + (int)c)*2;
			name = String.valueOf(hash);

			try {
				Paths.get(name); 
			} catch (InvalidPathException e1) {
				throw new IOException(e1);
			}
		}
		
		return name;
	}

	/** 
	 * Downloads the page, parses it and creates child nodes.
	 * @throws ProblemsReadingDocumentException if any error
	 */
	public void downloadPage() throws ProblemsReadingDocumentException {
		logger.log(Level.FINE, String.format("Downloading %s from network...%n", url.toString()));
		
		org.jdom.Document doc = null;
		try {
			SAXBuilder builder = new SAXBuilder("org.ccil.cowan.tagsoup.Parser");
			URLConnection connection = url.openConnection();
			if (!WebDownloader.checkHttpResponseOK(connection))		
				throw new ProblemsReadingDocumentException("Error response from server");
			doc = builder.build(connection.getInputStream());
		} catch (IOException|JDOMException e) {
			throw new ProblemsReadingDocumentException(e);
		}

		synchronized (this) {
			// discover info about this page
			parseSelf(doc);  
				
			// discover info about children pages
			childPages.clear();
			@SuppressWarnings("unchecked")
			List<Element> result = (List<Element>) queryXPathList(getChildNodesXPath(), doc);
			for (Element el: result) 
				try {
					childPages.add(parseChild(el));
				} catch (ProblemsReadingDocumentException e) {
					logger.log(Level.WARNING, "unable to parse child data", e);
				} // skip this child to next one
			
		}
		logger.log(Level.FINE, String.format("...finished %s.%n", url.toString()));
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
	public synchronized 
	boolean loadFromCache (org.jdom.Document doc) {
		if (doc == null) return false;
		
		logger.log(Level.FINE, String.format("Reading %s from cache...%n",url.toString()));
		try {
			Element e = scanXMLForThisElement(doc);
			if (null == e) return false;
			String t = e.getAttributeValue("title");
			if (t == null) return false;
			setTitle(t);  
			readCacheSelf(e);
			
			childPages.clear(); // discard previous data if any.
			@SuppressWarnings("unchecked")
			List<Element> l = e.getContent(new ElementFilter("childref"));
			for (Element el: l) 
				childPages.add( readCacheChild(el) );
			
			logger.log(Level.FINE, String.format("\t...Finished reading %s.%n",url.toString()));
			return true;
		} catch (ProblemsReadingDocumentException e) {
			// If ANY problem, quit with a fail code
			childPages.clear(); 
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
	 * @param q - XPath string with all nodes in "pre" namespace for parsed HTML files 
	 * (no prefix for XML cache files with no namespace definition)
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
	@SuppressWarnings("unchecked")
	private
	AbstractPage readCacheChild(Element childRef) throws ProblemsReadingDocumentException {
		AbstractPage child = null;
		String u = childRef.getAttributeValue("url");
		String className = childRef.getAttributeValue("class");
		String packageName = AbstractPage.class.getPackage().getName();
		Class<? extends AbstractPage> cl;
		Constructor<? extends AbstractPage> cons;
		try {
			cl = (Class<? extends AbstractPage>) Class.forName(packageName+ "." +className);
			cons = cl.getConstructor(String.class, AbstractPage.class);
			child = cons.newInstance(u, this);
		} catch (NoSuchMethodException | SecurityException | 
				IllegalArgumentException | InvocationTargetException | 
				ClassNotFoundException | InstantiationException | 
				IllegalAccessException| NullPointerException e) {
			throw new ProblemsReadingDocumentException(e);
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
	 * @return operation status report string, "" or null if nothing to report (operation skipped)
	 * @throws IOException if saving was terminated by error - retry might be possible.
	 */
	public abstract String saveResult(String saveTo) throws IOException;

	/**
	 * Saves this page data into XML tree. 
	 * Only references to child pages are saved, not the pages data.
	 * @param doc - JDOM Document holding a cache to save to
	 */
	public synchronized
	void saveToCache (org.jdom.Document doc) {
		assert (doc != null);
		
		// absolutely required field
		if (getTitle()==null) return;
		//1. Compose this one and childrefs
		Element e = getSpecificDataXML();
		if (e==null) return; // element is corrupt and should not be cached 
		e.setAttribute("title", getTitle());
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
	private
	Element scanXMLForThisElement(org.jdom.Document doc) {
		assert (doc != null); 
		String searchXPath = String.format("//%s[@url='%s']",getClass().getSimpleName(),url.toString());
		List<?> result = queryXPathList(searchXPath, doc);
		
		return result.size()>0?(Element)result.get(0):null;
	}

	/**
	 * Generate new saving path for the children of this page from its own saveTo and page data
	 * @return saving path for the children pages or null if no children expected
	 * @throws IOException if valid filename cannot be created
	 */
	abstract public String getChildrenSaveTo(String saveTo) throws IOException;
	
	/**
	 * Checks if call to saveResult can be skipped (especially if it is a long operation)
	 * If the effect of saveResult is a minor thing (like simple directory creation) this function is 
	 * allowed to make call to saveResult and return true;
	 * @param saveTo - path to where this item will be saved in consequent call to saveResult 
	 * @return true if call to saveResult can be skipped (will yield no effect), \t false if it must be performed
	 */
	abstract public boolean isSavingNotRequired(String saveTo);
	
	@Override
	public 
	String toString() {
		return (getTitle() == null)? url.toString(): getTitle();
	}
}