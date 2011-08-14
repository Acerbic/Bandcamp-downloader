package dloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;

import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.Tag;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.exceptions.CannotWriteException;
import entagged.audioformats.generic.TagTextField;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

public class Track extends AbstractPage {

	private Map<String, String> properties = new HashMap<String,String>();
	public String getProperty(String name) {
		// convention to AbstractPage
		return name.equals("title")? title : properties.get(name) ;
	}
	public String setProperty(String name, String value) {
		if (name.equals("title")) {
			// convention to AbstractPage
			String lastValue = title;
			title = value;
			return lastValue;
		} else return properties.put(name, value);
	}
	
	// dataPatterns are to read Track info from downloaded page (XMLCacheDataKeys -> Pattern)
	private static final Map<String, Pattern> dataPatterns = new HashMap<String,Pattern>();
	
	/** XMLCacheDataKeys are keys to Track properties that are used by readXMLSelf() and getSpecificDataXML().<br/>
	 *  "title" is not included because if you don't get it with loadFromCache() call, 
	 *  you will download and parse the full page anyway
	 */
	private static final String[] XMLCacheDataKeys = {"mediaLink", "artist", "track", "album"};

	static {
		dataPatterns.put("mediaLink", Pattern.compile(".*trackinfo:.*\"file\":\"([^\"]*)\".*", Pattern.DOTALL));
		dataPatterns.put("artist", Pattern.compile(".*artist\\s*:\\s*\"([^\"]*)\".*", Pattern.DOTALL));
		dataPatterns.put("album", Pattern.compile(".*album_title\\s*:\\s*\"([^\"]*)\".*", Pattern.DOTALL));
		dataPatterns.put("title", Pattern.compile(".*title\\s*:\\s*\"([^\"]*)\".*", Pattern.DOTALL));
//		dataPatterns.put("track", Pattern.compile(".*numtracks\\s*:\\s*([\\d]*).*", Pattern.DOTALL));
		dataPatterns.put("comment", Pattern.compile(".*trackinfo:.*\"has_info\":\"([^\"]*)\".*", Pattern.DOTALL));				

	}
	
	public Track(String s) throws IllegalArgumentException {super(s);}
	public Track(URL url) throws IllegalArgumentException {super(url);}
	public Track() {super();}

	@Override
	public boolean saveResult(File saveTo) throws IOException {
		boolean wasDownloaded;
		File f = new File(saveTo, getFSSafeName(title) + ".mp3");
		if (f.isDirectory()) {
			throw new IOException( "<"+title+"> is a directory!!!\n");
		}
		wasDownloaded = WebDownloader.fetchWebFile(getProperty("mediaLink"), f) != 0;
		
		tagAudioFile(f);
		return wasDownloaded;
	}
	
	Map<String,String> getTextFieldIds(Tag fileTag) {
		Map<String,String> tagToCustomFrame = new HashMap<String,String>();
		
		// this weird casting is to get to Id strings of particular descendant of AbstractTag 
		Tag class_clone = null;
		try {
			class_clone = fileTag.getClass().newInstance();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
			return null;
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
			return null;
		}
		class_clone.addAlbum("album");
		class_clone.addTitle("title");
		class_clone.addArtist("artist");
		class_clone.addTrack("track");
		@SuppressWarnings("unchecked")
		Iterator<TagTextField> itr = class_clone.getFields();
		while (itr.hasNext()) {
			TagTextField ttf = itr.next();
			tagToCustomFrame.put(ttf.getContent(), ttf.getId());
		}
		
		return tagToCustomFrame;
		
	}
	
	/**
	 * Checks the file and tags it if appropriate 
	 * @param f - file to tag
	 */
	void tagAudioFile(File f) {
		try {
			AudioFile theFile = AudioFileIO.read(f);
			entagged.audioformats.Tag fileTag = theFile.getTag();
			
			// when ID3 tag is saved empty value for track is saved as "0" - ID3 bug 
			if (fileTag.getFirstTrack().equals("0")) 
				fileTag.setTrack("");
			
			// actual file operation flag
			boolean updateMP3Tag = false;
			// "track" -> "TRCK", etc...
			Map<String, String> propertyToFrame = getTextFieldIds(fileTag); 
			
			// copy this Track's data into fileTag
			for (Map.Entry<String, String> entry: propertyToFrame.entrySet()) 
				try {
					String fieldValue = getProperty(entry.getKey());
					if (!fieldValue.isEmpty()) {
						TagTextField idNewField = new TextId3Frame(entry.getValue(), fieldValue);
						@SuppressWarnings("unchecked")
						List<TagTextField> idFieldSet = fileTag.get(entry.getValue());
						
						// rewrite only absent fields or 
						// existing if Main.allowTagging is set and no such value in this field
						boolean fieldValueAlreadyExists = false;
						for (TagTextField existingField: idFieldSet) {
							if ((existingField != null) && 
								(existingField.getContent() == idNewField.getContent())) {
								fieldValueAlreadyExists = true;
								break; // one is enough
							}
						}
						if ((Main.allowTagging && !fieldValueAlreadyExists) || idFieldSet.size()==0) {
							// rewrite with new value
							fileTag.set(idNewField); 
							updateMP3Tag = true;
						}
					}
				} catch (NullPointerException e) {} // skip Track missing field
			
			
			if (updateMP3Tag)
				theFile.commit();
			
		} catch (CannotReadException e) {
			logger.log(Level.SEVERE, "", e);
		} catch (CannotWriteException e) {
			logger.log(Level.SEVERE, "", e);
		}
	}

	@Override
	protected void readCacheSelf(Element e) throws ProblemsReadingDocumentException {
		for (String key: Arrays.asList(XMLCacheDataKeys)) {
			String value = e.getAttributeValue(key);
			if (value==null) throw new ProblemsReadingDocumentException();
			setProperty(key,value);
		}
	}

	@Override
	protected Element getSpecificDataXML() {
		if (getProperty("mediaLink") == null) return null; //no saving track data if no track present

		Element e = new Element("Track");
		for (String key: Arrays.asList(XMLCacheDataKeys)) {
			String value = getProperty(key);
			if (value==null) value = "";
			e.setAttribute(key, value);
		}
		return e;
	}
	
	@Override
	protected AbstractPage parseChild(Element element)  
			throws ProblemsReadingDocumentException {
		return null; // stub since no child nodes XPath and this will never be called
	}
	
	@Override
	protected void parseSelf(Document doc)  
			throws ProblemsReadingDocumentException {
		@SuppressWarnings("unchecked")
		List<Element> scriptList = (List<Element>) queryXPathList("//pre:div[@id='pgBd']/pre:script", doc);
		for (Element el: scriptList) {
			String rawData = el.getText();
			// clear JavaScript escaping: "\/" --> "/", etc.
			rawData = rawData.replaceAll("\\\\(.)", "$1");
			
			// try to recover each tag by its pattern
			for (Map.Entry<String, Pattern> entry: dataPatterns.entrySet()) {
				Matcher m = entry.getValue().matcher(rawData);
				if (m.matches()) 
					setProperty(entry.getKey(), m.group(1));
			}
		
		}
		// fix url 
		String relativePath = getProperty("mediaLink");
		try {
			setProperty("mediaLink", resolveLink(relativePath).toString());
		} catch (MalformedURLException e) {
			setProperty("mediaLink", null);
			throw new ProblemsReadingDocumentException(e);
		}
		
		try {
			// if album or artist data is missing we can try to salvage it 
			//    from parenting pages
			String album = getProperty("album");
			String artist = getProperty("artist");
			if (album==null || album.isEmpty())
				setProperty("album",parent.title);
			if (artist==null || artist.isEmpty())
				setProperty("artist",parent.parent.title);
			// fix track number
			if (Integer.parseInt(getProperty("track"))<=0)
				properties.remove("track");
		} catch (NullPointerException e) {
		} catch (NumberFormatException e) {
			properties.remove("track");
		}
		
	}
	
	@Override
	protected String getChildNodesXPath() {
		return null;
	}
	@Override
	public File getChildrenSaveTo(File saveTo) {
		return null;
	}
}
