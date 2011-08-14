package dloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

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
	public boolean saveResult(File saveTo) throws IOException {
		File f = new File(saveTo, getFSSafeName(title));
		if (!f.exists())
			if (!f.mkdirs()) {
				throw new IOException(String.format("Directory creation failed (%s)%n",
						f.getAbsolutePath()));
			}
		statusReport = "";
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
			c.title = element.getText();
			return c;
		} catch (NullPointerException|IllegalArgumentException|
				MalformedURLException e) {
			throw new ProblemsReadingDocumentException(e);
		}
	}

	@Override
	public File getChildrenSaveTo(File saveTo) throws IOException {
		return new File(saveTo, getFSSafeName(title));
	}

}
