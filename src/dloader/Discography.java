package dloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;


public class Discography extends PageParser {
	
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
			title = ((Element) result.get(0)).getText();
		else
			title = "Unknownband";

		// now detect type of Discography
		result = queryXPathList("//pre:ul[@title='Discography']", doc);
		if (result.size()>0) {
			variant = DiscographyListVariant.SIDEBAR;
			return;
		}
		
		result = queryXPathList("//pre:div[@id='indexpage']", doc);
		if (result.size()>0) {
			variant = DiscographyListVariant.CENTRAL_INDEX;
		}
	}

	@Override
	public void saveResult(File saveTo) throws IOException {
		File f = new File(saveTo, getFSSafeName(title));
		if (!f.exists())
			if (!f.mkdirs()) {
				logger.severe(String.format("Directory creation failed (%s)%n",
						f.getAbsolutePath()));
				return;
			}
		logger.info( String.format("Discography: %s%n", title));
		if (childPages != null) {
			logger.info( String.format("Saving albums (%d):%n",
					childPages.length));
			for (int i = 0; i < childPages.length; i++) {
				logger.info( String.format("\t%d. ", i + 1));
				if (childPages[i] != null)
					childPages[i].saveResult(f);
				else
					logger.info( "--- don't exist! --- \n");
			}
		}

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
	protected PageParser parseChild(Element element) throws ProblemsReadingDocumentException  {
		try {
			String s = element.getAttributeValue("href");
			Album c = new Album(s);
			c.title = element.getText();
			return c;
		} catch (NullPointerException e) {
			throw new ProblemsReadingDocumentException (e);
		} catch (IllegalArgumentException e) {
			throw new ProblemsReadingDocumentException (e);
		}
	}

}
