package dloader.page;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;

import dloader.Main;
import dloader.WebDownloader;
import dloader.pagejob.ProgressReporter;

import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.Tag;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.exceptions.CannotWriteException;
import entagged.audioformats.generic.TagTextField;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

//TODO: get and cache mp3-file byte length to detect if file in the net is changed and must be re-downloaded.

//TODO: calculate and cache mp3-stream (only audio part of the file) byte length of the file, so corrupt mp3 files can be detected. ???

/**
 * Class represents track web page, has no children pages 
 * @author A.Cerbic
 */
public class Track extends AbstractPage {

	private static final String SCRIPT_DESC_XPATH = "//pre:div[@id='pgBd']/pre:script";

	/**
	 * Set of custom properties read from page, saved to cache and 
	 * resulting audio file metadata tags. Before iterating through properties,
	 * they might be required to be updated with "title" field from getTitle().
	 */	
	private Properties properties; // thread-safe class
	
	/**
	 * Shortcut	
	 */
	public 
	String getProperty(String name) { return properties.getProperty(name); }
	
	/**
	 * Use null value to delete property. Empty string ("") value cannot replace non-empty value.
	 */
	public 
	String setProperty(String name, String value) {
		String oldValue = getProperty(name);
		if (value == null)
			return (String) properties.remove(name);
		else if (oldValue == null)
			return (String) properties.setProperty(name, value);
		else if (value.isEmpty() && !oldValue.isEmpty())
			return null;
		else return (String) properties.setProperty(name, value);
		
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
// XXX: track number is set by parent album or not set at all - may be this behavior should be changed  		
//		dataPatterns.put("track", Pattern.compile(".*numtracks\\s*:\\s*([\\d]*).*", Pattern.DOTALL));
		dataPatterns.put("comment", Pattern.compile(".*trackinfo:.*\"has_info\":\"([^\"]*)\".*", Pattern.DOTALL));				
	}
	
	public Track(String url, String saveTo, AbstractPage parent) throws IllegalArgumentException {
		super(url, saveTo, parent);
		properties = new Properties(); // created AFTER construction since getTitle() and setTitle() are not used in AbstractPage constructor
	}
	
	@Override
	public  
	boolean saveResult(ProgressReporter reporter) throws IOException, InterruptedException {
		Path p;
		String fileURL;
		synchronized (this) {
			p = Paths.get(getTrackFileName());
			fileURL = getProperty("mediaLink");
		}
		
		boolean wasDownloaded = 
				WebDownloader.fetchWebFile(fileURL, p.toString(), reporter) != 0;
		
		String statusReport = null; // defaults to "skipped"
		
		// tagging does not require synchronization as it is operating with Properties object which is thread-safe. 
		//XXX: this is not strictly true, but as properties are read much later than written in different PageJobs, we may assume those operations do not overlap. 
		if (tagAudioFile(Main.forceTagging))
			statusReport = "file updated";
		if (wasDownloaded)
			statusReport = "file downloaded";
		
		if (statusReport != null) {
			reporter.report(statusReport, 1);
			return true;
		} else {
			return false;
		}
		
	}
	
	/**
	 * Collects <b>4 'common'</b> audio tag field id codes from given tag <br>
	 * This method is needed because different audio formats declare different id codes
	 * and sometimes the same audio format can have different versions of audio tags.
	 * @param fileTag - the tag to be examined and mimicked
	 * @return mapping {Track property name -> audio tag field id}
	 */
	private
	Map<String,String> getTextFieldIds(Tag fileTag) {
		Map<String,String> tagToCustomFrame = new HashMap<String,String>();
		
		Tag class_clone = null;
		try {
			class_clone = fileTag.getClass().newInstance();
		} catch (InstantiationException|IllegalAccessException e1) {
			Main.log(Level.SEVERE, "Can't clone file metadata tag class", e1);
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
	 * @param force - if true, existing tags will be overwritten with data from this Track object,
	 * if false - only absent tags will be filled.
	 * @return true if actual write operation happened
	 * @throws IOException - file read/write problems
	 */
	public
	boolean tagAudioFile(boolean force) throws IOException {
			AudioFile theFile = fixTag(force);
			if (null != theFile) {
				try {
					theFile.commit();
				} catch (CannotWriteException e) {
					throw new IOException(e);
				}
				return true;
			}
			return false;
	}

	/**
	 * Compares data in properties of this Track page with tags in corresponding
	 * file on disk. Prepares updates of tags in the file, depending on 'force' argument. 
	 * @param force - if true, existing tags will be overwritten with data from this Track object,
	 * if false - only absent tags will be filled.
	 * @return null if there is nothing to change in file (everything is correct, considering 'force' flag),
	 * otherwise returns AudioFile with changes applied to file tags. Consequent call to {@link AudioFile.commit} would
	 * save changes to disk.
	 * @throws IOException
	 */
	private
	AudioFile fixTag(boolean force) throws IOException {
		try {
			AudioFile theFile = AudioFileIO.read(Paths.get(getTrackFileName()).toFile());
			entagged.audioformats.Tag fileTag = theFile.getTag();
			// XXX: check API if this is needed at all
			if (fileTag == null) throw new IOException();
			
			// when ID3 tag is saved empty value for track is saved as "0" - ID3 bug 
			if (fileTag.getFirstTrack().equals("0")) 
				fileTag.setTrack("");
			
			// actual file operation flag
			boolean updateMP3Tag = false;
			// "track" -> "TRCK", etc...
			Map<String, String> propertyToFrame = getTextFieldIds(fileTag); 
			
			// copy this Track's data into fileTag

			setProperty("title", getTitle());
			for (Map.Entry<String, String> entry: propertyToFrame.entrySet()) {
				
				String newFieldValue = getProperty(entry.getKey());
				
				if (newFieldValue != null && !newFieldValue.isEmpty()) { 
					TagTextField idNewField = new TextId3Frame(entry.getValue(), newFieldValue);
					@SuppressWarnings("unchecked")
					List<TagTextField> idFieldSet = fileTag.get(entry.getValue());
					
					
					// check if EXACTLY this one value exists for this field
					boolean fieldValueAlreadyExists = false;
					for (TagTextField existingField: idFieldSet) {
						if ((existingField != null) && 
							(existingField.getContent().equals(newFieldValue))) {
							fieldValueAlreadyExists = true;
							break; // one is enough
						}
					}
					// rewrite only absent fields or 
					// existing if 'force' is true and no such value in this field
					if ((force && !fieldValueAlreadyExists) || idFieldSet.size()==0) {
						// rewrite with new value
						fileTag.set(idNewField); 
						updateMP3Tag = true;
					}
				}
			}
			
			if (updateMP3Tag) 
				return theFile;
			return null;
			
		} catch (CannotReadException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	protected void readCacheSelf(Element e) throws ProblemsReadingDocumentException {
		for (String key: Arrays.asList(XMLCacheDataKeys)) {
			String value = e.getAttributeValue(key);
			if (value==null) 
				value = ""; // keep existing value of same name if possible
			setProperty(key,value);
		}
	}

	@Override
	protected Element getSpecificDataXML() {
		if (getProperty("mediaLink") == null) return null; //no saving track data if no track present

		Element e = new Element("Track");
		setProperty("title", getTitle());
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
		List<Element> scriptList = queryXPathList(SCRIPT_DESC_XPATH, doc);
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
			setTitle(getProperty("title"));
		
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
				setProperty("album",getParent().getTitle());
			if (artist==null || artist.isEmpty())
				setProperty("artist",getParent().getParent().getTitle());
		} catch (NullPointerException e) {
			// skip if not enough parents in a line;
		}
	}
	
	@Override
	protected String getChildNodesXPath() {return null;}
	
	@Override
	public String getChildrenSaveTo() {return null;}

	@Override
	public boolean isSavingNotRequired() {
//		return true;
		try {
			Path p = Paths.get(getTrackFileName());
			
			if ( ! (Files.isRegularFile(p) && Files.size(p) > 0))
				return false;
			
			return (fixTag(Main.forceTagging)==null);
		} catch (IOException e) {
			Main.log(Level.WARNING,null,e);
		}
		return false;
	}

	@Override
	public Collection<String> getThisPageFiles() {
		Collection <String> fileset = new LinkedList<String>();
		fileset.add( getTrackFileName() );
		return fileset;
	}
	
	
	/** 
	 * Get the file name of (future) mp3 file.
	 * @return null on error, String with the full path to track's mp3 file
	 */
	public 
	String getTrackFileName() {
		String title = getTitle();
		if ((title == null) || (title.isEmpty())) return null;
		
		try {
			return Paths.get(saveTo, getFSSafeName(title) + ".mp3").toString();
		} catch (IOException e) {
			return null;
		}
	}
	
	@Override
	public boolean isOK() {
		if (! super.isOK() ) return false;
		
		for (String p: XMLCacheDataKeys) 
			if (getProperty(p) == null) 
				return false;
		return true;
	}
	
}
