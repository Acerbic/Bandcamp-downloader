package dloader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;

/**
 * Class for album page, references track child pages
 * @author A.Cerbic
 */
public class Album extends AbstractPage {

	/**
	 * link to the album cover
	 */
	public URL coverUrl;
	/**
	 * arbitrary additional info
	 */
	public String moreInfo;
	/**
	 * counts parsed child track pages (include fails) 
	 * to override their "track" number property
	 */
	private int trackCounter;

	public Album(URL url) throws IllegalArgumentException {super(url);}

	public Album(String s) throws IllegalArgumentException {super(s);}

	public Album() {super();}
	
	@Override
	public boolean saveResult(String saveTo) throws IOException {
		Path p = Paths.get(saveTo, getFSSafeName(getTitle()));
		Files.createDirectories(p);
			
		if (WebDownloader.fetchWebFile(coverUrl, p.resolve("cover.jpg").toString()) != 0)
			statusReport = "cover image downloaded";
		return true;
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
	protected AbstractPage parseChild(Element element) throws ProblemsReadingDocumentException {
		try {
			trackCounter++; // that includes counting for failed parsing
			URL u = resolveLink(element.getAttributeValue("href"));
			Track t = new Track(u);
			t.setTitle(element.getText());
			// may be set default property instead?
			t.setProperty("track", String.valueOf(trackCounter));
			return t;
		} catch (IllegalArgumentException|NullPointerException|
				MalformedURLException e) {
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
				logger.log(Level.WARNING, String.format("can't get album art for <%s>", url.toString()), e);
			}
		}
		
		@SuppressWarnings("unchecked")
		List<Element> scriptList = (List<Element>) queryXPathList("//pre:script", doc);
		for (int i = 0; i<scriptList.size(); i++) {
			Pattern x = Pattern.compile(".*album_title : \"([^\"]*)\".*", Pattern.DOTALL);
			Matcher m = x.matcher(scriptList.get(i).getText());
			if (m.matches()) {
				setTitle(m.group(1));
				break;
			}
		}		
	}

	@Override
	protected String getChildNodesXPath() {
		return "//pre:table[@id='track_table']//pre:td/pre:div[@class='title']//pre:a";
	}

	@Override
	public String getChildrenSaveTo(String saveTo) throws IOException {
		return Paths.get(saveTo, getFSSafeName(getTitle())).toString();
	}

}
