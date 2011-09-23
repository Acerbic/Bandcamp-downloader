package dloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
	 * @param from - resource URL string
	 * @param to - file to save to.
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero length)
	 * @throws IOException on stream problems or server has responded bad
	 */
	public static long fetchWebFile(String from, String to) throws IOException {
		URL u = new URL(from);
		return fetchWebFile(u,to);
	}
	
	/**
	 * Downloads and saves a web resource by given URL.
	 * @param from - resource address (HTTP assumed)
	 * @param to - file to save the downloaded resource
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero-length or server has responded badly)
	 * @throws IOException on stream problems AND on server errors/timeouts
	 */
	public static long fetchWebFile(URL from, String to) throws IOException {
		StatisticGatherer.totalFileDownloadAttempts++;
		
		Path dstPath = Paths.get(to);
		/* if file does not exist, continue
		 * If file exists, is regular and not zero-sized, return 0 (skip)
		 * if file exists, regular and size zero, delete it and continue
		 * if file exists, not regular - error
		 */
		if (Files.exists(dstPath))
			if (Files.isRegularFile(dstPath)) 
				if (Files.size(dstPath) > 0)
					return 0;
				else Files.deleteIfExists(dstPath);
			else throw new IOException ("Destination is not a regular file");

		URLConnection connection = from.openConnection();
		if (!checkHttpResponseOK(connection))
			throw new IOException("failed to get OK response from server"); 
		try (InputStream is = connection.getInputStream();
				SeekableByteChannel boch = Files.newByteChannel(dstPath, 
						StandardOpenOption.WRITE,
						StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING))
		{
			byte[] buff = new byte[1024 * 1024];
			ByteBuffer buff2 = ByteBuffer.wrap(buff);
			int numRead;
			while ((numRead = is.read(buff)) != -1) {
				buff2.limit(numRead); // sync wrapper with the input buffer
				while (buff2.remaining() > 0) 
					boch.write(buff2); // IOException on insufficient disk space 
				buff2.rewind(); 
			}
			
			boch.close();
		} catch (IOException e) {
			// on actual write loop to the file: delete half-baked file;
			Files.deleteIfExists(dstPath);
			throw e;
		}
		
		if (Files.exists(dstPath)) { 
			// BOoooo. how to know that OS is done with the file?
			// waiting for file to be released. possible infinite loop / clinch here.
			// while (!Files.isWritable(p)) ;
			StatisticGatherer.totalFileDownloadFinished++;
			StatisticGatherer.totalBytesDownloaded += Files.size(dstPath);
			return Files.size(dstPath);
		}
		throw new IOException("Java fucked up and lost your file!");
	}
	
	public static boolean checkHttpResponseOK (URLConnection connection) {
		if (connection instanceof HttpURLConnection)
			try {
				return ((HttpURLConnection)connection).getResponseCode() == HttpURLConnection.HTTP_OK;
			} catch (IOException e) {
				return false;
			}
		
		// exceptional treatment of local files, for testing convenience
		URL u = connection.getURL();
		if (u.getProtocol().equals("file"))
			return Files.isReadable(Paths.get(u.getFile().substring(1)));
		return false; // unknown connection type
	}
}
