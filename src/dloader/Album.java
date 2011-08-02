package dloader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;


public class Album extends PageParser {

	public URL coverUrl;
	public String moreInfo; 
	private int trackCounter;

	public Album(URL url) throws IllegalArgumentException {super(url);}

	public Album(String s) throws IllegalArgumentException {super(s);}

	public Album() {super();}
	
	@Override
	public void saveResult(File saveTo) throws IOException {
		File f = new File(saveTo, getFSSafeName(title));
		if (!f.exists())
			if (!f.mkdirs()) {
				logger.severe(String.format("Directory creation failed (%s)%n",
						f.getAbsolutePath()));
				return;
			}
		logger.info( String.format("(album) \"%s\": %s%n", title, url.toString()));
		WebDownloader.fetchWebFile(coverUrl, new File(f, "cover.jpg"));
		if (childPages != null) {
			logger.info( String.format("\t Downloading tracks (%d):%n",
					childPages.length));
			for (int i = 0; i < childPages.length; i++) 
				if (childPages[i] != null) {
					logger.info( String.format("\t\t%d. ", i + 1));
					childPages[i].saveResult(f);
				}
		}
	}

	@Override
	protected void readCacheSelf(Element e) throws ProblemsReadingDocumentException{
		try {
			coverUrl = resolveLink(e.getAttributeValue("coverUrl"));
		} catch (MalformedURLException e1) {
			throw new ProblemsReadingDocumentException(e1);
		} 
		moreInfo = e.getAttributeValue("moreInfo");
	}

	@Override
	protected Element getSpecificDataXML() {
		Element e = new Element("Album");
		if (coverUrl != null) e.setAttribute("coverUrl", coverUrl.toString());
		if (moreInfo != null) e.setAttribute("moreInfo", moreInfo);
		return e;
	}

	@Override
	protected PageParser parseChild(Element element) throws ProblemsReadingDocumentException {
		try {
			trackCounter++; // that includes counting for failed parsing
			String s = element.getAttributeValue("href");
			Track t = new Track(s);
			t.title = element.getText();
			t.setProperty("track", String.valueOf(trackCounter));
			return t;
		} catch (IllegalArgumentException e) {
			throw new ProblemsReadingDocumentException(e);
		} catch (NullPointerException e) {
			throw new ProblemsReadingDocumentException(e);
		}
	}

	@Override
	protected void parseSelf(Document doc) throws ProblemsReadingDocumentException {
		@SuppressWarnings("unchecked")
		List<Element> imgList = (List<Element>) queryXPathList("//pre:div[@id='tralbumArt']/pre:img", doc);
		if (imgList.size() > 0) {
			try {
				coverUrl = resolveLink((imgList.get(0)).getAttributeValue("src"));
			} catch (MalformedURLException e) {
				logger.log(Level.WARNING, "--no album art--", e);
			}
		}
		
		@SuppressWarnings("unchecked")
		List<Element> scriptList = (List<Element>) queryXPathList("//pre:script", doc);
		for (int i = 0; i<scriptList.size(); i++) {
			Pattern x = Pattern.compile(".*album_title : \"([^\"]*)\".*", Pattern.DOTALL);
			Matcher m = x.matcher(scriptList.get(i).getText());
			if (m.matches()) {
				title = m.group(1);
				break;
			}
		}		
	}

	@Override
	protected String getChildNodesXPath() {
		return "//pre:table[@id='track_table']//pre:td/pre:div[@class='title']//pre:a";
	}

}
