package dloader.page;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.logging.Level;

import org.jdom2.*;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import dloader.Main;
import dloader.WebDownloader;
import dloader.XMLCache;
import dloader.pagejob.ProgressReporter;


//XXX: 1st download a page, THEN guess its class by content?

//XXX: sad day. bandcamp allows to break hierarchy and put "track" right into "discography"

//XXX: switch url from URL to URI and support both "http" and "file" protocols natively (without nasty tricks for JUnit)

/**
 * Basic class to maintain page-relevant data and procedures, embodies one "download session", i.e. URL-to-files
 * transfer notion.
 * 
 *   Objects of this class are mutable 
 *   	(effectively immutable after initialization with updateFromNet()/downloadPage() and loadFromCache(),
 *     	but that can be delayed).
 *   Objects of this class are thread-safe 
 */
public abstract class AbstractPage {
	/**
	 * Exception generated if data cannot be read from XML cache 
	 * or downloaded document
	 * @author A.Cerbic
	 */
	@SuppressWarnings("serial")
	public class ProblemsReadingDocumentException extends Exception {
		public ProblemsReadingDocumentException() {super();}
		public ProblemsReadingDocumentException(String s) {super(s);}
		public ProblemsReadingDocumentException(Throwable e) {super(e);}
	}
	
	/** 
	 * title of this item 
	 */
	private String title;  
	
	/**
	 * reference to a parent item (may be null)
	 */
	private AbstractPage parent;
	
	/**
	 * URL of a page referencing this item 
	 */
	public final URL url;
	
	/**
	 * Directory where to save this page's data
	 */
	public final String saveTo;

	/**
	 * List of a children items to this page (can be of size zero, can't be null) 
	 * It is important this variable is concurrent, since many different threads may iterate
	 * through children at any given moment.
	 * This list is empty on construction and filled with elements by loadFromCache() and updateFromNet() calls.
	 * Due to efficiency issues, elements should be added as .addAll(...) when possible
	 */
	public final 
//	Queue<AbstractPage> childPages = new ConcurrentLinkedQueue<>(); 
	List<AbstractPage> childPages = new CopyOnWriteArrayList<>(); 
	

	/**
	 * Cache to operate cache-related functions
	 * @return
	 */
	static
	private XMLCache getCache() {
		return Main.cache; //default cache location. 
	}
	
	public synchronized final
	AbstractPage getParent() {return parent;}

	public synchronized final
	String getTitle() {return title;} 
	
	public synchronized final
	void setTitle(String title) {this.title = title;}

	/**
	 * Constructs from web address
	 * @param stringURL - web address (required)
	 * @param saveTo - where data of this page will be saved to (null if no children expected)
	 * @param parent - parent page to this page, may be null 
	 * @throws IllegalArgumentException  
	 */
	public AbstractPage(String stringURL, String saveTo, AbstractPage parent) throws IllegalArgumentException {
		try {url = resolveLink(stringURL);}
		catch (MalformedURLException e) {throw new IllegalArgumentException(e);}
		catch (NullPointerException e) {throw new IllegalArgumentException(e);}

		//path in question may not exist at this point and will be created by parent page later. 
//		Path p = Paths.get(saveTo);
//		if (! (Files.isDirectory(p) && Files.isWritable(p)))
//			throw new IllegalArgumentException();
		this.saveTo = saveTo;
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
	 * Reads class-specific info from XML cache element.
	 * Note: this is used two-fold. First, as intended it is reading data cached to XML; 
	 * second, it is involved in a trick to copy page's data in "updateFromNet()"
	 * @param e - element to read from
	 * @throws ProblemsReadingDocumentException if reading data from XML fails
	 */
	protected abstract void readCacheSelf(Element e) throws ProblemsReadingDocumentException;

	/**
	 * Saves extracted data to disk. 
	 * @param saveTo - directory to save info to.
	 * @param reporter - to output progress of long operations
	 * @return operation status report. True on completion, false if skipped (results already saved) 
	 * @throws IOException if saving was terminated by error - retry might be possible.
	 */
	public abstract boolean saveResult(ProgressReporter reporter) throws IOException;

	/**
	 * Generate new saving path for the children of this page from its own saveTo and page data
	 * @return saving path for the children pages or null if no children expected
	 * @throws IOException if valid filename cannot be created
	 */
	abstract public String getChildrenSaveTo() throws IOException;
	
	/**
	 * Checks if call to saveResult can be skipped (especially if it is a long operation)
	 * @return true if call to saveResult can be skipped (will yield no effect), \t false if it must be performed
	 */
	abstract public boolean isSavingNotRequired();
	
	/**
	 * All the files (including directories) this page would create when 'saveResult' 
	 * is called
	 * @return Collection of full filenames
	 */
	public abstract 
	Collection<String> getThisPageFiles();
	
	/**
	 * Queries given JDOM (html) document with XPath string
	 * @param q - XPath string with all nodes with "pre" prefixes
	 * @param doc - JDOM Document or Element
	 * @return List of found matches, may be of zero size if nothing is found
	 */
	protected final List<Element> queryXPathList(String q, Document doc) {
		return queryXPathList (q, doc.getRootElement());
	}
	
	//this is similar to one in XMLCache class. duplication problem?
	/**
	 * Queries given JDOM document with XPath string
	 * @param query - XPath string with all nodes with "pre" prefixes for parsed HTML files 
	 * @param doc - JDOM Document or Element
	 * @return List of found matches, may be of zero size if nothing is found
	 */
	protected final List<Element> queryXPathList(String query, Element doc) {
		if (query == null) return new ArrayList<Element>(0);
		try {
			String nsURI = doc.getNamespaceURI();
			XPathBuilder<Element> xpb = new XPathBuilder<Element>(query,Filters.element()); // null filter
			// binding prefix to existing namespace as per XML standard requirement
			xpb.setNamespace("pre", nsURI);
			XPathExpression<Element> xpe = xpb.compileWith(XPathFactory.instance()); // default factory
			return xpe.evaluate(doc);
		} catch (NullPointerException|IllegalStateException|IllegalArgumentException  e) {
			Main.logger.log(Level.SEVERE,"",e);
			return new ArrayList<Element>(0);
		} 		
	}
	
	/**
	 * Reads child's page url data from cache and creates a node.
	 * @param childRef - <childref ...> tag describing a child
	 * @return new PageParser child node.
	 * @throws ProblemsReadingDocumentException if anything went wrong
	 */
	private
	AbstractPage readCacheChild(Element childRef) throws ProblemsReadingDocumentException {
		AbstractPage child = null;
		String u = childRef.getAttributeValue("url");
		String className = childRef.getAttributeValue("class");
		
		try {
			child = bakeAPage(className, u, getChildrenSaveTo(),this);
		} catch (IllegalArgumentException | IOException e) {
			throw new ProblemsReadingDocumentException(e);
		}
		return child;
	}

	/**
	 * Gets an URL to resource referenced from this page. 
	 * Uses this.url as a base link to resolve relative paths. 
	 * @param link - relative or absolute link
	 * @return proper URL with absolute path
	 * @throws MalformedURLException 
	 */
	protected URL resolveLink(String link) throws MalformedURLException {
		return new URL(url, fixURLString(url, link));
	}

	private final static 
	String fixURLString(URL base, String u) {
		if (u == null) return null;
		if (u.endsWith("/"))
			u = u.substring(0, u.length()-1); // uniform "...com/" to "...com" address
		if (base == null && !u.contains(":/"))
			u = "http://"+u; // default protocol
		return u;
	}
	
	/**
	 * Gets data about this page from cache (JDOM tree) and creates child nodes.
	 * Old data on children pages may be discarded (references to child pages are unreliable from now on
	 * and must be re-checked to ensure they are still included as children).
	 * @return true if data acquired successfully, false otherwise. 
	 */
	public synchronized final
	boolean loadFromCache () {
		
		Main.log(Level.FINE, String.format("Reading %s from cache...%n",url.toString()));
		try {
			Element e = getCache().getElementForPage(url.toString());
			if (null == e) return false;
			String t = e.getAttributeValue("title");
			if (t == null) return false;
			setTitle(t);  
			readCacheSelf(e);
			
			Collection<AbstractPage> newChildren = new LinkedList<>();
			Collection<Element> l = e.getContent(new ElementFilter("childref"));
			for (Element el: l) 
				newChildren.add(readCacheChild(el));
			
			mergeNewChildren(new LinkedList<>(childPages), newChildren);
			
			Main.log(Level.FINE, String.format("\t...Finished reading %s.%n",url.toString()));
			return true;
		} catch (ProblemsReadingDocumentException|NullPointerException e) {
			// If ANY problem, quit with a fail code
			childPages.clear();
			setTitle(null);
			return false;
		}
	}

	/** 
	 * Downloads the page, parses it and creates child nodes.
	 * Note: children pages are dropped always. Their references are no longer part of the page tree.
	 * Note2: it is better to invoke updateFromNet(), as it preserves children pages if they are identical to the new data
	 * @throws ProblemsReadingDocumentException if any error
	 */
	public final 
	void downloadPage(ProgressReporter reporter) throws ProblemsReadingDocumentException {
		Main.log(Level.FINE, String.format("Downloading %s from network...%n", url.toString()));
		
		org.jdom2.Document doc = null;
		try {
			XMLReaderSAX2Factory saxConverter = new XMLReaderSAX2Factory(false, "org.ccil.cowan.tagsoup.Parser");
			SAXBuilder builder = new SAXBuilder(saxConverter);
			URLConnection connection = url.openConnection();
			if (!WebDownloader.checkHttpResponseOK(connection))		
				throw new ProblemsReadingDocumentException("Error response from server");
			//FIXME: need to be replaced with explicit download and parsing call, to make use of ProgressReporter. Actually, probably not.
			doc = builder.build(connection.getInputStream());
//		} catch (IOException|JDOMException e) {
		} catch (Exception e) {//XXX: some glitch in JDOM2 lets other random exceptions bubble up. So need to catch 'em all.
			throw new ProblemsReadingDocumentException(e);
		}
	
		synchronized (this) {
			// discover info about this page
			parseSelf(doc);  
				
			// discover info about children pages
			childPages.clear();
			Collection<Element> result = queryXPathList(getChildNodesXPath(), doc);
			List<AbstractPage> newChildren = new LinkedList<>();
			for (Element el: result) 
				try {
					newChildren.add(parseChild(el));
				} catch (ProblemsReadingDocumentException e) {
					Main.log(Level.WARNING, "unable to parse child data", e);
				} // skip this child to next one

			childPages.addAll(newChildren);
		}
		Main.log(Level.FINE, String.format("...finished %s.%n", url.toString()));
	}

	/**
	 * Create another object of the same class,
	 * initiated with the same URL, saveTo and parent.
	 * @return new object.
	 * @throws Exception if something went wrong.
	 */
	private
	AbstractPage createSameClassPage() throws Exception {
		AbstractPage result = null;
		result = bakeAPage(this.getClass().getName(), url.toString(), saveTo, getParent());
		return result;

	}
	
	/** 
	 * Downloads the page, parses it and updates this page with new data.
	 * References to children pages are unreliable. Old pages may be discarded and replaced with
	 * empty ones.
	 * 
	 * @throws ProblemsReadingDocumentException if any error
	 * @return true if any data was changed, false if this page is unmodified.
	 */
	public final 
	boolean updateFromNet(ProgressReporter reporter) throws ProblemsReadingDocumentException {
		AbstractPage tempPage;
		try {
			tempPage = createSameClassPage();
		} catch (Exception e) {
			throw new ProblemsReadingDocumentException(e);
		}
		tempPage.downloadPage(reporter);
		
		synchronized (this) {
			if (isSame(tempPage)) return false;
			setTitle(tempPage.getTitle());
			// somewhat awkward way to copy custom object data from temp object
			readCacheSelf(tempPage.getSpecificDataXML()); 
	
			// lets try to save some existing children for performance 
			mergeNewChildren(new LinkedList<>(childPages), tempPage.childPages);
			
			for (AbstractPage child: childPages)
				child.parent = this;
		}
		return true;
	}

	/**
	 * Replaces childPages list with new children, while saving some of the old children for performance
	 * @param oldChildren
	 * @param newChildren
	 */
	private void mergeNewChildren (Collection<AbstractPage> oldChildren, Collection<AbstractPage> newChildren) {
		childPages.clear(); // discard previous data if any.
		for (AbstractPage newChild: newChildren) {
			// search if exactly that child existed.
			AbstractPage oldChild = null;
			for (AbstractPage current: oldChildren)
				if (newChild.url.equals(current.url) &&
					newChild.getClass().equals(current.getClass()) &&
					((newChild.saveTo == null && current.saveTo==null) || newChild.saveTo.equals(current.saveTo))) { // last check is probably excessive
					oldChild = current; break;
				}
			
			List<AbstractPage> resultChildren = new LinkedList<>();
			if (oldChild != null) {
				resultChildren.add(oldChild);
			} else
				resultChildren.add(newChild);
			childPages.addAll(resultChildren);
		}
	}
	/**
	 * Saves this page data into XML tree. 
	 * Only references to child pages are saved, not the pages data.
	 * Uses (overridden) functions getSpecificDataXML(), getTitle().
	 * If the title is null or getSpecificDataXML() returns null, nothing is saved.
	 * 
	 * @param doc - JDOM Document holding a cache to save to
	 */
	public synchronized final
	void saveToCache () {
		if (getCache() == null)
			return;
		// absolutely required field
		if (getTitle()==null) return;
		//1. Compose this one and childrefs
		Element e = getSpecificDataXML();
		if (e==null) return; // element is corrupt and should not be cached 
		e.setAttribute("title", getTitle());
		e.setAttribute("url", url.toString());
		for (AbstractPage child: childPages) 
			if (child != null) {
				Element childElement = new Element("childref");
				childElement.setAttribute("class",child.getClass().getSimpleName());
				childElement.setAttribute("url",child.url.toString());
				e.addContent(childElement);
			}
		getCache().addElementWithReplacement(e);
	}

	/**
	 * Shallow comparison: title, url, children urls, 
	 *    additional cacheable data
	 * Used currently to check downloaded page against cached 
	 *   (same thread - no sync needed)
	 * @return true if argument is not different from this page
	 */
	private 
	boolean isSame(AbstractPage ref) {
		if (getTitle()==null && ref.getTitle()!=null) return false;
		
		if (getTitle()!=null && !getTitle().equals(ref.getTitle())) return false;
		if (! url.equals(ref.url)) return false;
		Element x = ref.getSpecificDataXML();
		String s1 = (x == null ? "": new XMLOutputter().outputString(x));
		x = this.getSpecificDataXML();
		String s2 = (x == null ? "": new XMLOutputter().outputString(x));
		if (!s1.equals(s2))
			return false;
		
		if (childPages.size()!=ref.childPages.size()) return false;
		
		for (AbstractPage child: childPages) 
			if (ref.getChildByURL(child.url) == null) 
				return false;
		return true;
	}

	/**
	 * Lookup a child page by its URL
	 * @param urlRequested
	 * @return found page or null if not found 
	 */
	public final
	AbstractPage getChildByURL (URL urlRequested) {
		for (AbstractPage child: childPages) 
			if (child.url.equals(urlRequested)) return child;
		return null;
	}
	
	/**
	 * A page factory method. Tries 2 different approaches to create an AbstractPage object according to specifications.
	 * First tries to create it by class name if provided. If fails, tries to guess appropriate class by parsing the url.
	 * @param className - a desired class in "dloader.page" package. (can be null)
	 * @param baseURL - url of the new page. required
	 * @param saveTo - saveTo path of the new page. can be null.
	 * @return created page.
	 * @throws IllegalArgumentException - is thrown when both approaches to create a page failed.
	 */
	@SuppressWarnings("unchecked")
	public static
	AbstractPage bakeAPage(String className, String baseURL, String saveTo, AbstractPage parent) throws IllegalArgumentException {
		AbstractPage result = null;
		String packageName = AbstractPage.class.getPackage().getName();
		Class<? extends AbstractPage> cl = null;
		Constructor<? extends AbstractPage> cons;
		
		baseURL = fixURLString(null, baseURL);
		
		try {
			try {
				cl = (Class<? extends AbstractPage>) Class.forName(className);
			} catch (ClassNotFoundException e) {
				className = packageName+ "." +className; // default package in case being omitted
				cl = (Class<? extends AbstractPage>) Class.forName(className);
			}
			cons = cl.getConstructor(String.class, String.class, AbstractPage.class);
			result = cons.newInstance(baseURL, saveTo, parent);
		} catch (NoSuchMethodException | SecurityException | 
					IllegalArgumentException | InvocationTargetException | 
					ClassNotFoundException | InstantiationException | 
					IllegalAccessException| NullPointerException e1) {
				// generic creation failed. Going to try and parse URL.
				URL u;
				try {
					u = new URL(baseURL);
				} catch (MalformedURLException e2) {
					throw new IllegalArgumentException(e2);
				}
				
				if (baseURL.contains("/track/")) 
					return new Track(baseURL.toString(), saveTo, null);
				if (baseURL.contains("/album/")) 
					return new Album(baseURL.toString(), saveTo, null);
				if (u.getPath().isEmpty())
					return new Discography(baseURL.toString(), saveTo, null);
				
				throw new IllegalArgumentException(e1);
			}
		
		return result;
	}

	public AbstractPage getChildByURLString(String string) {
		try {
			return getChildByURL(resolveLink(string));
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	/**
	 * Integrity check.
	 * Check if page data is complete - otherwise page must be read from cache/net
	 *
	 * isPageOK() can return false even after being updated from net - if downloaded page is missing
	 * some elements. Sometimes this is crucial, sometimes it is not.
	 * @return true if check passed, false otherwise.
	 */
	//XXX: may be should be complemented with "lastUpdated" time stamp.
	public 
	boolean isPageOK() {
		if (getTitle()==null || getTitle().isEmpty())
			return false;
		return true;
	}
	
	@Override
	public 
	String toString() {
		String className = this.getClass().getSimpleName();
		return ((getTitle() == null || getTitle().isEmpty())? url.toString() : "[" +className+ "] " + getTitle());
	}
}
