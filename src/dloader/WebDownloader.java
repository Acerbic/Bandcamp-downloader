package dloader;
//import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Helps downloading resources from the net
 * @author A.Cerbic
 */
public class WebDownloader {
	
/**
	 * Downloads and saves a resource by given string address (URL) .
	 * @param from - resource url address
	 * @param to - file to save to.
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero-length or server has responded bad)
	 * @throws IOException on other stream problems
	 */
	public static long fetchWebFile(String from, String to) throws IOException {
		URL u = new URL(from);
		return fetchWebFile(u,to);
	}
	
	/**
	 * Downloads and saves a binary resource (not "text/*" MIME) by given URL.
	 * @param from - resource address (HTTP assumed)
	 * @param to - file to save the downloaded resource
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero-length or server has responded badly)
	 * @throws IOException on other stream problems
	 */
	public static long fetchWebFile(URL from, String to) throws IOException {
		StatisticGatherer.totalFileDownloadAttempts++;
		InputStream is = null;
		SeekableByteChannel boch = null;
		
		Path p = Paths.get(to);
		/* just delete zero-length files. if size is non-zero, skip it */
		if (Files.exists(p) && Files.size(p) > 0)
			return 0;
		Files.deleteIfExists(p);

		URLConnection connection = from.openConnection();
		// check content type header to catch 404, 403... error responses
		if (connection.getContentType().contains("text/")) 
			return 0;
		 
		try {
			is = connection.getInputStream();
			boch = Files.newByteChannel(p, 
					StandardOpenOption.WRITE,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);

			byte[] buff = new byte[1024 * 100];
			ByteBuffer buff2 = ByteBuffer.wrap(buff);
			int numRead;
			while ((numRead = is.read(buff)) != -1) {
				buff2.limit(numRead);
				boch.write(buff2);
				buff2.rewind();
			}
		} catch (IOException e) {
			// on actual write loop to the file;
			Files.delete(p);
			throw e;
		} finally {
			if (is != null) is.close();
			if (boch != null) boch.close();
		}
		StatisticGatherer.totalFileDownloadFinished++;
		StatisticGatherer.totalBytesDownloaded += Files.size(p);
		return Files.size(p);
	}
}
