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
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Level;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.*;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

import dloader.Main;
import dloader.PageProcessor;
import dloader.WebDownloader;
import dloader.pagejob.ProgressReporter;


/**
 * Basic class to download page, parse it and download elements it references.
 * 
 *   Objects of this class are mutable 
 *   	(effectively immutable after initialization with downloadPage() and loadFromCache(),
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
	
	public synchronized 
	AbstractPage getParent() {return parent;}

	/**
	 * List of a children items to this page (can be of size zero) 
	 */
	public final 
	Collection<AbstractPage> childPages = new ConcurrentLinkedQueue<>(); 

	public synchronized
	String getTitle() {return title;} 
	
	public synchronized 
	void setTitle(String title) {this.title = title;}

	/**
	 * Constructs from web address
	 * @param stringURL - web address
	 * @throws IllegalArgumentException if stringURL is null or bad 
	 */
	public AbstractPage(String stringURL, String saveTo, AbstractPage parent) throws IllegalArgumentException {
		try {url = resolveLink(stringURL);}
		catch (MalformedURLException e) {throw new IllegalArgumentException(e);}
		catch (NullPointerException e) {throw new IllegalArgumentException(e);}
		if (saveTo == null) throw new IllegalArgumentException();

		Path p = Paths.get(saveTo);
		if (! (Files.isDirectory(p) && Files.isWritable(p)))
			throw new IllegalArgumentException();
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
		assert (name != null);
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
	 * @param e - element to read from
	 * @throws ProblemsReadingDocumentException if reading data from XML fails
	 */
	protected abstract void readCacheSelf(Element e) throws ProblemsReadingDocumentException;

	/**
	 * Saves extracted data to disk, then saves children too. 
	 * @param saveTo - directory to save info to.
	 * @param progressIndicator - to output progress of long operations
	 * @return operation status report string, "" or null if nothing to report (operation skipped)
	 * @throws IOException if saving was terminated by error - retry might be possible.
	 */
	public abstract String saveResult(ProgressReporter progressIndicator) throws IOException;

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
			Main.log(Level.SEVERE,"",e);
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
	 * Gets data about this page from cache (JDOM tree) and creates child nodes.
	 * @param doc - JDOM Document to load from 
	 * @return true if data acquired successfully, false otherwise
	 */
	public synchronized final
	boolean loadFromCache () {
		
		Main.log(Level.FINE, String.format("Reading %s from cache...%n",url.toString()));
		try {
			Element e = PageProcessor.getCache().getElementForPage(
					this.getClass().getSimpleName(), url.toString());
			if (null == e) return false;
			String t = e.getAttributeValue("title");
			if (t == null) return false;
			setTitle(t);  
			readCacheSelf(e);
			
			childPages.clear(); // discard previous data if any.
			@SuppressWarnings("unchecked")
			Collection<Element> l = e.getContent(new ElementFilter("childref"));
			for (Element el: l) 
				childPages.add( readCacheChild(el) );
			
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
	 * @throws ProblemsReadingDocumentException if any error
	 */
	public final 
	void downloadPage(ProgressReporter reporter) throws ProblemsReadingDocumentException {
		Main.log(Level.FINE, String.format("Downloading %s from network...%n", url.toString()));
		
		org.jdom.Document doc = null;
		try {
			SAXBuilder builder = new SAXBuilder("org.ccil.cowan.tagsoup.Parser");
			URLConnection connection = url.openConnection();
			if (!WebDownloader.checkHttpResponseOK(connection))		
				throw new ProblemsReadingDocumentException("Error response from server");
			//FIXME: need to be replaced with explicit download and parsing call, to make use of progressIndicator
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
			Collection<Element> result = (Collection<Element>) queryXPathList(getChildNodesXPath(), doc);
			for (Element el: result) 
				try {
					childPages.add(parseChild(el));
				} catch (ProblemsReadingDocumentException e) {
					Main.log(Level.WARNING, "unable to parse child data", e);
				} // skip this child to next one
			
		}
		Main.log(Level.FINE, String.format("...finished %s.%n", url.toString()));
	}

	/** 
	 * Downloads the page, parses it and updates this page with new data.
	 * If there were child nodes that no longer exist - they are dropped, if there are new
	 * children, new pages are added to collection.
	 * @throws ProblemsReadingDocumentException if any error
	 * @return true if any data was changed, false if this page is unmodified.
	 */
	public final 
	boolean UpdateFromNet(ProgressReporter reporter) throws ProblemsReadingDocumentException {
		AbstractPage tempPage = PageProcessor.detectPage(url.toString(), saveTo);
		tempPage.downloadPage(reporter);
		
		synchronized (this) {
			if (isSame(tempPage)) return false;
			setTitle(tempPage.getTitle());
			readCacheSelf(tempPage.getSpecificDataXML()); // somewhat awkward way to copy object data from temp object
	
			// following loop is probably not needed, since every child will be rescanned 
			//  anyway by the enveloping algorithm
			Collection<AbstractPage> forRemoval = new LinkedList<AbstractPage>();
			for (AbstractPage child: childPages) {
				AbstractPage found=tempPage.getChildByURL(child.url);
				if (tempPage.getChildByURL(child.url) == null) 
					forRemoval.add(child);
				else tempPage.childPages.remove(found);
			}
			childPages.removeAll(forRemoval);
			childPages.addAll(tempPage.childPages);
			
			for (AbstractPage child: childPages) 
				child.parent = this;
		}
		return true;
	}

	/**
	 * Saves this page data into XML tree. 
	 * Only references to child pages are saved, not the pages data.
	 * If the title is null, nothing is saved
	 * @param doc - JDOM Document holding a cache to save to
	 */
	public synchronized final
	void saveToCache () {
		
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
		PageProcessor.getCache().addElementWithReplacement(e);
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
		if (! getTitle().equals(ref.getTitle())) return false;
		if (! url.equals(ref.url)) return false;
		String s1 = ref.getSpecificDataXML().getValue();
		String s2 = this.getSpecificDataXML().getValue();
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
	
	@Override
	public 
	String toString() {
		String className = this.getClass().getSimpleName();
		return ((getTitle() == null)? url.toString(): getTitle()) + "[" +className+ "]";
	}
}
