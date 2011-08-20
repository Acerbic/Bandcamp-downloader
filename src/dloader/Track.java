package dloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

/**
 * Class represents track web page, has no children pages 
 * @author A.Cerbic
 */
public class Track extends AbstractPage {

	/**
	 * Set of custom properties read from page, saved to cache and 
	 * resulting audio file metadata tags
	 */	
	Properties defaultProperties;
	Properties properties;
	
	@Override
	public String getTitle() {
		return properties.getProperty("title");
	};

	@Override
	public void setTitle(String title) {
		properties.setProperty("title", title);
	};

	/**
	 * Ease of use	
	 */
	public String getProperty(String name) {
		return properties.getProperty(name);
	}
	/**
	 * Ease of use	
	 */
	public String setProperty(String name, String value) {
		return (String) properties.setProperty(name, value);
	}
	
	/**
	 * Maps property name to compiled Pattern to Track info from downloaded page
	 */ 
	private static final Map<String, Pattern> dataPatterns = new HashMap<String,Pattern>();
	
	/** XMLCacheDataKeys are names of Track properties that are used by readXMLSelf() and getSpecificDataXML().<br/>
	 *  "title" is not included because it is processed in AbstactPage separately  
	 */
	private static final String[] XMLCacheDataKeys = {"mediaLink", "artist", "track", "album"};

	static {
		dataPatterns.put("mediaLink", Pattern.compile(".*trackinfo:.*\"file\":\"([^\"]*)\".*", Pattern.DOTALL));
		dataPatterns.put("artist", Pattern.compile(".*artist\\s*:\\s*\"([^\"]*)\".*", Pattern.DOTALL));
		dataPatterns.put("album", Pattern.compile(".*album_title\\s*:\\s*\"([^\"]*)\".*", Pattern.DOTALL));
		dataPatterns.put("title", Pattern.compile(".*title\\s*:\\s*\"([^\"]*)\".*", Pattern.DOTALL));
// track number is set by parent album or not set at all - may be this behavior should be changed  		
//		dataPatterns.put("track", Pattern.compile(".*numtracks\\s*:\\s*([\\d]*).*", Pattern.DOTALL));
		dataPatterns.put("comment", Pattern.compile(".*trackinfo:.*\"has_info\":\"([^\"]*)\".*", Pattern.DOTALL));				
	}
	
	public Track(String s) throws IllegalArgumentException {super(s);}
	public Track(URL url) throws IllegalArgumentException {super(url);}
	public Track() {super();}

	{
		defaultProperties = new Properties();
		properties = new Properties(defaultProperties);
	}
	
	@Override
	public String saveResult(String saveTo) throws IOException {
		Files.createDirectories(Paths.get(saveTo));
		Path p = Paths.get(saveTo, getFSSafeName(getTitle()) + ".mp3");
		boolean wasDownloaded = 
				WebDownloader.fetchWebFile(getProperty("mediaLink"), p.toString()) != 0;
		
		String statusReport = "skipped";
		if (tagAudioFile(p.toString()))
			statusReport = "updated";
		if (wasDownloaded)
			statusReport = "downloaded";
		
		return statusReport;
	}
	
	/**
	 * Collects <b>4 'common'</b> audio tag field id codes from given tag <br>
	 * This method is needed because different audio formats declare different id codes
	 * and sometimes the same audio format can have different versions of audio tags.
	 * @param fileTag - the tag to be examined and mimicked
	 * @return mapping {Track property name -> audio tag field id}
	 */
	Map<String,String> getTextFieldIds(Tag fileTag) {
		Map<String,String> tagToCustomFrame = new HashMap<String,String>();
		
		Tag class_clone = null;
		try {
			class_clone = fileTag.getClass().newInstance();
		} catch (InstantiationException|IllegalAccessException e1) {
			logger.log(Level.SEVERE, "Can't clone file metadata tag class", e1);
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
	 * @param file - name of an audio file to tag
	 * @return true if actual write operation happened
	 * @throws IOException
	 */
	boolean tagAudioFile(String file) throws IOException {
		try {
			AudioFile theFile = AudioFileIO.read(Paths.get(file).toFile());
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
						if ((Main.forceTagging && !fieldValueAlreadyExists) || idFieldSet.size()==0) {
							// rewrite with new value
							fileTag.set(idNewField); 
							updateMP3Tag = true;
						}
					}
				} catch (NullPointerException e) {} // skip Track missing field
			
			
			if (updateMP3Tag) {
				theFile.commit();
				return true;
			}
			return false;
			
		} catch (CannotReadException|CannotWriteException e) {
			throw new IOException(e);
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
			
			// try to recover each property by its pattern
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
			// fix track number
			if (album==null || album.isEmpty())
				setProperty("album",parent.getTitle());
			if (artist==null || artist.isEmpty())
				setProperty("artist",parent.parent.getTitle());
		} catch (NullPointerException e) {
			// skip if not enough parents in a line;
		}
	}
	
	@Override
	protected String getChildNodesXPath() {
		return null;
	}
	@Override
	public String getChildrenSaveTo(String saveTo) {
		return null;
	}
}
