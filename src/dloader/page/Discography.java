package dloader.page;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.jdom2.Document;
import org.jdom2.Element;

import dloader.Main;
import dloader.pagejob.ProgressReporter;


/**
 * Class for discography page, as a number of albums
 * @author A.Cerbic
 */
public class Discography extends AbstractPage {
	
	private static final String INDEX_CHILD_REF = "//pre:div[@id='indexpage']//pre:h1/pre:a";
	private static final String SIDEBAR_CHILD_LINK = "//pre:div[@id='discography']//pre:div[@class='trackTitle']/pre:a";
	private static final String INDEX_DIV_XPATH = "//pre:div[@id='indexpage']";
	private static final String SIDEBAR_DIV_XPATH = "//pre:div[@id='discography']";
	private static final String TITLE_XPATH = "//pre:title";
	
	private enum DiscographyListVariant { SIDEBAR, CENTRAL_INDEX };
	/**
	 * detected on parseSelf() call and dictates 
	 * what getChildNodesXPath() returns. 
	 */
	private DiscographyListVariant variant; 


	public Discography(String url, String saveTo, AbstractPage parent) throws IllegalArgumentException
		{super(url, saveTo, parent);}
	
	@Override
	protected void parseSelf(Document doc) throws ProblemsReadingDocumentException  {
		List<?> result = queryXPathList(TITLE_XPATH, doc);
		if ((result != null) && (result.size()>0))
			setTitle(((Element) result.get(0)).getText());
		else
			throw new ProblemsReadingDocumentException("Can't read discography title");

		// now detect type of Discography
		result = queryXPathList(SIDEBAR_DIV_XPATH, doc);
		if (result.size()>0) {
			variant = DiscographyListVariant.SIDEBAR;
			return;
		}
		
		result = queryXPathList(INDEX_DIV_XPATH, doc);
		if (result.size()>0) {
			variant = DiscographyListVariant.CENTRAL_INDEX;
			return;
		}
		throw new ProblemsReadingDocumentException("Can't detect discography type");
	}

	@Override
	public 
	boolean saveResult(ProgressReporter progressIndicator) throws IOException {
		Path p = Paths.get(saveTo, getFSSafeName(getTitle()));
		Files.createDirectories(p);
		return true;
	}

	@Override
	protected void readCacheSelf(Element e) {
	}

	@Override
	protected Element getSpecificDataXML() {
		return new Element("Discography");
	}

	// field polymorphism
	@Override
	protected String getChildNodesXPath() {
		switch (variant) {
			case SIDEBAR:
				return SIDEBAR_CHILD_LINK;
			case CENTRAL_INDEX:
				return INDEX_CHILD_REF;
		}
		return null;
	}

	@Override
	protected AbstractPage parseChild(Element element) throws ProblemsReadingDocumentException  {
		try {
			URL u = resolveLink(element.getAttributeValue("href"));
//			Album c = new Album(u.toString(), getChildrenSaveTo(), this);
			
			//sad day. bandcamp sometimes breaks hierarchy and puts "track" right into "discography"
			AbstractPage c = bakeAPage(null,u.toString(), getChildrenSaveTo(), this);
			c.setTitle(element.getText());
			return c;
		} catch (NullPointerException|IllegalArgumentException|
				IOException e) {
			throw new ProblemsReadingDocumentException(e);
		}
	}

	@Override
	public String getChildrenSaveTo() throws IOException {
		return Paths.get(saveTo, getFSSafeName(getTitle())).toString();
	}

	@Override
	public boolean isSavingNotRequired() {
		Path p;
		try {
			p = Paths.get(getChildrenSaveTo());
			if (Files.isDirectory(p))
				return true;
		} catch (IOException e) {
			Main.log(Level.WARNING,null,e);
		}
		return false;
	}

	@Override
	public Collection<String> getThisPageFiles() {
		Collection <String> fileset = new LinkedList<String>();
		try {
			fileset.add( getChildrenSaveTo() );
		} catch (IOException e) {
		}
		return fileset;
	}

}
