package dloader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class to store data from previously downloaded pages.
 * 
 * This class is thread-safe.
 * @author A.Cerbic
 */
public class XMLCache {
	/**
	 * handles formatting for saving as a file
	 */
	private final XMLOutputter outputter;
	
	private Path xmlFile; //effectively final
	
	private final Document doc;
	
	/**
	 * Loads file and parses it into org.jdom2.Document
	 * If document cannot be read for any reason, new empty valid one is created 
	 * (the file will be created when saveCache() is called next time).
	 * @param xmlFileName - cache file name
	 * @throws IllegalArgumentException if file name is not valid
	 */
	public XMLCache(String xmlFileName) {
		Logger l = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		Document doc = null;
		
		if (xmlFileName!=null && !xmlFileName.isEmpty()) 
		try {
			xmlFile = Paths.get(xmlFileName);
			if (Files.exists(xmlFile)) {
				org.jdom2.input.SAXBuilder builder = new org.jdom2.input.SAXBuilder();
				builder.setIgnoringBoundaryWhitespace(true);
				builder.setIgnoringElementContentWhitespace(true);
				doc = builder.build(Files.newInputStream(xmlFile));
			}
		} catch (InvalidPathException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			l.log(Level.WARNING, String.format("Error reading cache file <%s>%n", xmlFileName), e);
		} catch (JDOMException e) {
			l.log(Level.WARNING, String.format("Error parsing cache file <%s>%n", xmlFileName), e);
		} 

		if (doc == null) doc = new Document(new Element("root"));
		outputter = new XMLOutputter();
		Format xmlOutputFormat = outputter.getFormat();
		xmlOutputFormat.setIndent("  ");
		xmlOutputFormat.setLineSeparator(System.getProperty("line.separator"));
		outputter.setFormat(xmlOutputFormat);
		this.doc = doc;
	}
	
	/**
	 * Scans cache for the page by its URL and class
	 * @param className - name of a class this page belongs to
	 * @param pageURL - URL of a page cached
	 * @return a CLONE of a cache element
	 */
	synchronized public
	Element getElementForPage(String pageURL) {
		String searchXPath = String.format("//root/*[@url='%s']", pageURL);
		List<Element> result = queryXPathList(searchXPath);
		return result.size()>0?(Element)result.get(0).clone():null; 
	}
	
	/**
	 * Adds new element to a cache and drops previous versions of this element, if any existed
	 * @param e - the new element to add
	 */
	synchronized public
	void addElementWithReplacement (Element e) {
		Element root = doc.getRootElement();
		
		Collection<Element> oldCachedElements = queryXPathList(
				String.format("/root/*[@url='%s']",e.getAttributeValue("url")));
		for (Element current: oldCachedElements) 
			current.detach();

		root.addContent((Element)e.clone());
	}
	
	/**
	 * Queries given JDOM document with XPath string
	 * @param query - XPath string 
	 * @param doc - JDOM Document or Element
	 * @return List of found matches, may be of zero size if nothing is found
	 */
	private
	List<Element> queryXPathList(String query) {
		if (query == null) return new ArrayList<Element>(0);
		try {
			Element root = doc.getRootElement();
			XPathBuilder<Element> xpb = new XPathBuilder<Element>(query,Filters.element()); 
			XPathExpression<Element> xpe = xpb.compileWith(XPathFactory.instance()); // default factory
			return xpe.evaluate(root);
		} catch (NullPointerException|IllegalStateException|IllegalArgumentException  e) {
			Main.logger.log(Level.SEVERE,"",e);
			return new ArrayList<Element>(0);
		} 
	}	

	/**
	 * Saves XML cache back into a file
	 * @throws IOException - if problems occur.
	 */
	public void saveCache() throws IOException {
		if (xmlFile != null)
		synchronized (doc) {
			try (OutputStream outStream = Files.newOutputStream(xmlFile)) { 
				outputter.output(doc, outStream);
			}
		}
	}
}
