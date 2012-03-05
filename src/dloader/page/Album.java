package dloader.page;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;

import dloader.PageProcessor;
import dloader.WebDownloader;

/**
 * Class for album page, references track child pages
 * @author A.Cerbic
 */
public class Album extends AbstractPage {

	/**
	 * link to the album cover
	 */
	public volatile URL coverUrl; // hope volatile is enough
	/**
	 * arbitrary additional info
	 */
	public String moreInfo;
	/**
	 * counts parsed child track pages (include fails) 
	 * to override their "track" number property
	 */
	private int trackCounter;

	public Album(String url, String saveTo, AbstractPage parent) throws IllegalArgumentException 
		{super(url, saveTo, parent);}

	/**
	 * Builds full path to save cover image to disk, including filename
	 * @return path to album cover image
	 * @throws IOException
	 */
	public String getCoverSavePath() throws IOException {
		Path p = Paths.get(getChildrenSaveTo(), "cover.jpg");
		return p.toString();
	}
	
	@Override
	public synchronized
	String saveResult(AtomicInteger progressIndicator) throws IOException {
		Path p = Paths.get(saveTo, getFSSafeName(getTitle()));
		Files.createDirectories(p);
			
		if (WebDownloader.fetchWebFile(coverUrl, getCoverSavePath()) != 0) 
			return "cover image downloaded";
		else return null;
	}

	@Override
	protected 
	void readCacheSelf(Element e) throws ProblemsReadingDocumentException{
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
	protected  
	AbstractPage parseChild(Element element) throws ProblemsReadingDocumentException {
		try {
			trackCounter++; // that includes counting for failed parsing
			URL u = resolveLink(element.getAttributeValue("href"));
			Track t;
			t = new Track(u.toString(), getChildrenSaveTo(), this);
			t.setTitle(element.getText());
			// may be set default property instead?
			t.setProperty("track", String.valueOf(trackCounter));
			return t;
		} catch (IllegalArgumentException|NullPointerException|
				IOException e) {
			throw new ProblemsReadingDocumentException(e);
		}
	}

	@Override
	protected 
	void parseSelf(Document doc) throws ProblemsReadingDocumentException {
		@SuppressWarnings("unchecked")
		List<Element> imgList = (List<Element>) queryXPathList("//pre:div[@id='tralbumArt']/pre:img", doc);
		if (imgList.size() > 0) {
			try {
				coverUrl = resolveLink((imgList.get(0)).getAttributeValue("src"));
			} catch (MalformedURLException e) {
				PageProcessor.log(Level.WARNING, String.format("can't get album art for <%s>", url.toString()), e);
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
		return "//pre:tr[@class='track_row_view']//pre:td/pre:div[@class='title']//pre:a";
	}

	@Override
	public String getChildrenSaveTo() throws IOException {
		return Paths.get(saveTo, getFSSafeName(getTitle())).toString();
	}

	@Override
	public synchronized
	boolean isSavingNotRequired() {
		Path p;
		try {
			p = Paths.get(getChildrenSaveTo());
			if (! Files.isDirectory(p)) return false;
			p = Paths.get(getCoverSavePath());
			if (Files.isRegularFile(p) && Files.size(p) > 0)
				return true;
		} catch (IOException e) {
			PageProcessor.log(Level.WARNING,null,e);
		}
		return false;
	}

	@Override
	public Collection<String> getThisPageFiles() {
		Collection <String> fileset = new LinkedList<String>();
		fileset.add( Paths.get(saveTo).toString());
		try {
			fileset.add( Paths.get(getCoverSavePath()).toString());
		} catch (IOException e) {
			//TODO: add handler here
		} 
		return fileset;
	}

}
