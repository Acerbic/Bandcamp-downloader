package dloader.page;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;

import org.jdom.Document;
import org.jdom.Element;


/**
 * Class for discography page, as a number of albums
 * @author A.Cerbic
 */
public class Discography extends AbstractPage {
	
	private enum DiscographyListVariant { SIDEBAR, CENTRAL_INDEX };
	/**
	 * detected on parseSelf() call and dictates 
	 * what getChildNodesXPath() returns. 
	 */
	private DiscographyListVariant variant = DiscographyListVariant.SIDEBAR; 

	public Discography(URL url) throws IllegalArgumentException {super(url);}

	public Discography(String s) throws IllegalArgumentException {super(s);}
	
	public Discography() {super();}

	@Override
	protected void parseSelf(Document doc) throws ProblemsReadingDocumentException  {
		List<?> result = queryXPathList("//pre:title", doc);
		if ((result != null) && (result.size()>0))
			setTitle(((Element) result.get(0)).getText());
		else
			throw new ProblemsReadingDocumentException("Can't read discography title");

		// now detect type of Discography
		result = queryXPathList("//pre:ul[@title='Discography']", doc);
		if (result.size()>0) {
			variant = DiscographyListVariant.SIDEBAR;
			return;
		}
		
		result = queryXPathList("//pre:div[@id='indexpage']", doc);
		if (result.size()>0) {
			variant = DiscographyListVariant.CENTRAL_INDEX;
			return;
		}
		throw new ProblemsReadingDocumentException("Can't detect discography type");
	}

	@Override
	public String saveResult(String saveTo) throws IOException {
		Path p = Paths.get(saveTo, getFSSafeName(getTitle()));
		Files.createDirectories(p);
		return null;
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
				return "//pre:ul[@title='Discography']//pre:div[@class='trackTitle']/pre:a";
			case CENTRAL_INDEX:
				return "//pre:div[@id='indexpage']//pre:h1/pre:a";
		}
		return null;
	}

	@Override
	protected AbstractPage parseChild(Element element) throws ProblemsReadingDocumentException  {
		try {
			URL u = resolveLink(element.getAttributeValue("href"));
			Album c = new Album(u);
			c.setTitle(element.getText());
			return c;
		} catch (NullPointerException|IllegalArgumentException|
				MalformedURLException e) {
			throw new ProblemsReadingDocumentException(e);
		}
	}

	@Override
	public String getChildrenSaveTo(String saveTo) throws IOException {
		return Paths.get(saveTo, getFSSafeName(getTitle())).toString();
	}

	@Override
	public boolean isSavingNotRequired(String saveTo) {
		try {
			saveResult(saveTo);
			return true;
		} catch (IOException e) {
			logger.log(Level.WARNING,null,e);
		}
		return false; 
	}

}
