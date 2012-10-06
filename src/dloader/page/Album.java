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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;

import dloader.Main;
import dloader.WebDownloader;
import dloader.pagejob.ProgressReporter;

/**
 * Class for album page, references track child pages
 * @author A.Cerbic
 */
public class Album extends AbstractPage {

	/**
	 * link to the album cover
	 */
	private URL coverUrl; 
	public synchronized final 
	URL getCoverUrl() {return coverUrl;}

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
	boolean saveResult(ProgressReporter reporter) throws IOException {
		Path p = Paths.get(saveTo, getFSSafeName(getTitle()));
		Files.createDirectories(p);
			
		if (WebDownloader.fetchWebFile(coverUrl, getCoverSavePath(), reporter) != 0) { 
			reporter.report("cover image downloaded", 1);
			return true;
		} else 
			return false; 
	}

	@Override
	protected 
	void readCacheSelf(Element e) throws ProblemsReadingDocumentException{
		try {
			coverUrl = resolveLink(e.getAttributeValue("coverUrl"));
		} catch (MalformedURLException e1) {
			throw new ProblemsReadingDocumentException(e1);
		} 
//		moreInfo = e.getAttributeValue("moreInfo");
	}

	@Override
	protected Element getSpecificDataXML() {
		Element e = new Element("Album");
		if (coverUrl != null) e.setAttribute("coverUrl", coverUrl.toString());
//		if (moreInfo != null) e.setAttribute("moreInfo", moreInfo);
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
			t.setTitle(element.getValue());
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
		trackCounter = 0;
		List<Element> imgList =  queryXPathList("//pre:div[@id='tralbumArt']//pre:img", doc);
		if (imgList.size() > 0) {
			try {
				coverUrl = resolveLink((imgList.get(0)).getAttributeValue("src"));
			} catch (MalformedURLException e) {
				Main.log(Level.WARNING, String.format("can't get album art for <%s>", url.toString()), e);
			}
		}
		
		List<Element> scriptList = queryXPathList("//pre:script", doc);
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
		return "//pre:td//pre:div[@class='title']//pre:a";
	}

	@Override
	public String getChildrenSaveTo() throws IOException {
		return Paths.get(saveTo, getFSSafeName(getTitle())).toString();
	}

	@Override
	public 
	boolean isSavingNotRequired() {
		Path p;
		try {
			p = Paths.get(getChildrenSaveTo());
			if (! Files.isDirectory(p)) return false;
			p = Paths.get(getCoverSavePath());
			if (Files.isRegularFile(p) && Files.size(p) > 0)
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
			fileset.add(getChildrenSaveTo());
			fileset.add(getCoverSavePath());
		} catch (IOException e) {
		} 
		return fileset;
	}
	
	@Override
	public boolean isOK() {
		return super.isOK() && (coverUrl != null);
	}
}
