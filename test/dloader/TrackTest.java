package dloader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.generic.GenericTag;
import entagged.audioformats.mp3.Id3v2Tag;

public class TrackTest {

	Track t;
	String workingCopy;
	Path workingCopyPath;
	@Before
	public void setUp() throws Exception {
		t = new Track();
		t.title = "Revelations III";
		t.setProperty("album", "Sburb");
		t.setProperty("track", "12");
		t.setProperty("artist", "Tyler Dever");		
		workingCopy = "test/tagged.mp3";
		workingCopyPath = Paths.get(workingCopy);
	}

	@After
	public void tearDown() throws Exception {
	}

	void copyUntagged() throws IOException {
		Files.copy(Paths.get("test/Revelations III.mp3"), workingCopyPath, 
				StandardCopyOption.REPLACE_EXISTING);
	}
	
	@Test
	public void testTagMp3FileCreateNewTag() throws CannotReadException, IOException {
		copyUntagged();
		AudioFile mp3File = AudioFileIO.read(workingCopyPath.toFile());
		entagged.audioformats.Tag mp3Tag = mp3File.getTag();
		
		assertTrue(mp3Tag instanceof GenericTag);
		assertTrue(mp3Tag.isEmpty());

		t.title = "";
		t.setProperty("album", "");
		t.setProperty("track", "");
		t.setProperty("artist", "");				
		t.tagAudioFile(workingCopy);
		mp3File = AudioFileIO.read(workingCopyPath.toFile());
		mp3Tag = mp3File.getTag();
		
		assertTrue(mp3Tag instanceof GenericTag);
		assertTrue(mp3Tag.isEmpty());
	}
	
	@Test
	public void testTagMp3FileCreateNewID3Tag() throws CannotReadException, IOException {
		copyUntagged();

		Main.allowTagging = false;
		t.tagAudioFile(workingCopy);
		AudioFile mp3File = AudioFileIO.read(workingCopyPath.toFile());
		entagged.audioformats.Tag mp3Tag = mp3File.getTag();
		
		assertTrue(mp3Tag instanceof Id3v2Tag);
		assertTrue(!mp3Tag.isEmpty());
		@SuppressWarnings("rawtypes")
		Iterator itr = mp3Tag.getFields();
		int count = 0;
		while (itr.hasNext()) {
			count++;
			itr.next();
		}
		assertEquals(4,count);
		assertEquals("Tyler Dever", mp3Tag.getFirstArtist());
	}

	@Test
	public void testTagMp3FileNoDoublingTags() throws CannotReadException, IOException {
		copyUntagged();

		Main.allowTagging = false;
		t.tagAudioFile(workingCopy);
		t.tagAudioFile(workingCopy);
		AudioFile mp3File = AudioFileIO.read(workingCopyPath.toFile());
		entagged.audioformats.Tag mp3Tag = mp3File.getTag();
		
		assertTrue(mp3Tag instanceof Id3v2Tag);
		assertTrue(!mp3Tag.isEmpty());
		@SuppressWarnings("rawtypes")
		Iterator itr = mp3Tag.getFields();
		int count = 0;
		while (itr.hasNext()) {
			count++;
			itr.next();
		}
		assertEquals(4,count);
		assertEquals(1, mp3Tag.getArtist().size());
		assertEquals("Tyler Dever", mp3Tag.getFirstArtist());
	}

	@Test
	public void testTagMp3FileUpdatePartialTag() throws CannotReadException, IOException {
		copyUntagged();

		Main.allowTagging = false;
		t.title = "";
		t.setProperty("album", "");
		t.tagAudioFile(workingCopy);
		AudioFile mp3File = AudioFileIO.read(workingCopyPath.toFile());
		entagged.audioformats.Tag mp3Tag = mp3File.getTag();
		
		assertEquals(0, mp3Tag.getAlbum().size());
		assertEquals(0, mp3Tag.getTitle().size());
		assertEquals(1, mp3Tag.getArtist().size());
		assertEquals("",mp3Tag.getFirstAlbum());
		assertEquals("Tyler Dever", mp3Tag.getFirstArtist());
		t.title = "SomeTitle";
		t.setProperty("album", "Cool Album");
		t.setProperty("artist", "DJ Sniff Mc'Snow");		
		t.tagAudioFile(workingCopy);
		mp3File = AudioFileIO.read(workingCopyPath.toFile());
		mp3Tag = mp3File.getTag();
		assertEquals(1, mp3Tag.getAlbum().size());
		assertEquals(1, mp3Tag.getTitle().size());
		assertEquals(1, mp3Tag.getArtist().size());
		assertEquals("Cool Album",mp3Tag.getFirstAlbum());
		assertEquals("Tyler Dever", mp3Tag.getFirstArtist());
	}
	
	@Test
	public void testTagMp3FileUpdatePartialTagWithRewrite() throws CannotReadException, IOException {
		copyUntagged();

		Main.allowTagging = true;
		t.title = "";
		t.setProperty("album", "");
		t.tagAudioFile(workingCopy);
		AudioFile mp3File = AudioFileIO.read(workingCopyPath.toFile());
		entagged.audioformats.Tag mp3Tag = mp3File.getTag();
		
		assertEquals(0, mp3Tag.getAlbum().size());
		assertEquals(0, mp3Tag.getTitle().size());
		assertEquals(1, mp3Tag.getArtist().size());
		assertEquals("",mp3Tag.getFirstAlbum());
		assertEquals("Tyler Dever", mp3Tag.getFirstArtist());
		t.title = "SomeTitle";
		t.setProperty("album", "Cool Album");
		t.setProperty("artist", "DJ Sniff Mc'Snow");		
		t.tagAudioFile(workingCopy);
		mp3File = AudioFileIO.read(workingCopyPath.toFile());
		mp3Tag = mp3File.getTag();
		assertEquals(1, mp3Tag.getAlbum().size());
		assertEquals(1, mp3Tag.getTitle().size());
		assertEquals(1, mp3Tag.getArtist().size());
		assertEquals("Cool Album",mp3Tag.getFirstAlbum());
		assertEquals("DJ Sniff Mc'Snow", mp3Tag.getFirstArtist());
	}
}
