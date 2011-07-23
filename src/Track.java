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
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.exceptions.CannotWriteException;
import entagged.audioformats.generic.TagField;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

public class Track extends PageParser {

	private Map<String, String> properties = new HashMap<String,String>();
	public String getProperty(String name) {
		// convention to PageParser
		return name.equals("title")? title : properties.get(name) ;
	}
	public String setProperty(String name, String value) {
		if (name.equals("title")) {
			// convention to PageParser
			String lastValue = title;
			title = value;
			return lastValue;
		} else return properties.put(name, value);
	}
	
	// dataPatterns are to read Track info from downloaded page
	private static final Map<String, Pattern> dataPatterns = new HashMap<String,Pattern>();
	// tagToID3V2Frame are to save Track info to mp3 Tag
	private static final Map<String,String> tagToID3V2Frame = new HashMap<String,String>();
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

		tagToID3V2Frame.put("title", "TIT2");
		tagToID3V2Frame.put("track", "TRCK");
		tagToID3V2Frame.put("album", "TALB");
		tagToID3V2Frame.put("artist", "TPE1");	
	}
	
	public Track(String s) throws IllegalArgumentException {super(s);}
	public Track(URL url) throws IllegalArgumentException {super(url);}
	public Track() {super();}

	@Override
	public void saveResult(File saveTo) throws IOException {
		File f = new File(saveTo, getFSSafeName(title) + ".mp3");
		if (f.isDirectory()) {
			logger.info( "<"+title+"> is a directory!!!\n");
			return; 
		}
		logger.info( title+" ... ");
		if (WebDownloader.fetchWebFile(getProperty("mediaLink"), f) == 0)
			logger.info( "skipped.\n");
		else 
			logger.info( "done.\n");
		
		tagMp3File(f);
	}
	
	/**
	 * Checks the file and tags it if appropriate 
	 * @param f - file to tag
	 */
	private void tagMp3File(File f) {
		try {
			AudioFile mp3File = AudioFileIO.read(f);
			entagged.audioformats.Tag mp3Tag = mp3File.getTag();
			
			// this works around the bug in a lib, that causes drop all fields when
			//   generic (newly created by getTag()) Tag is converted to ID3v2 on commit()
			if (!mp3Tag.getFields().hasNext()) {
				mp3Tag.setAlbum("1");
				mp3File.commit();
				mp3File = AudioFileIO.read(f);
				mp3Tag = mp3File.getTag();
				mp3Tag.setAlbum("");
			}
			
			if (mp3Tag.getFirstTrack().equals("0")) {
				@SuppressWarnings("rawtypes")
				Iterator it = mp3Tag.getTrack().iterator();
				while (it.hasNext()) {
					it.next(); 
					it.remove(); // the only way to remove field is by iterator
				}
			}

			boolean updateMP3Tag = false;
			
			// copy this Track's data into mp3Tag
			for (Map.Entry<String, String> entry: tagToID3V2Frame.entrySet()) 
				try {
					String fieldValue = getProperty(entry.getKey());
					if (!fieldValue.isEmpty()) {
						TagField x = new TextId3Frame(entry.getValue(), fieldValue);
						if (Main.allowTagging) {
							// always rewrite with new value
							mp3Tag.set(x); 
							updateMP3Tag = true;
						}
						else {
							// rewrite only absent or empty tags.
							@SuppressWarnings("unchecked")
							List<TagField> idFieldSet = mp3Tag.get(entry.getValue());
							if (idFieldSet == null || idFieldSet.size()==0 || 
								idFieldSet.get(0) == null || idFieldSet.get(0).isEmpty()) {
								
								mp3Tag.set(x);
								updateMP3Tag = true;
							}
						}
					}
				} catch (NullPointerException e) {} // skip Track missing field
			
			
			if (updateMP3Tag)
				mp3File.commit();
			
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
	protected PageParser parseChild(Element element)  
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
}
