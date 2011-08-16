package dloader;

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
	 * @param from - resource URL string
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
	 * Downloads and saves a web resource by given URL.
	 * @param from - resource address (HTTP assumed)
	 * @param to - file to save the downloaded resource
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero-length or server has responded badly)
	 * @throws IOException on stream problems AND on server errors/timeouts
	 */
	public static long fetchWebFile(URL from, String to) throws IOException {
		StatisticGatherer.totalFileDownloadAttempts++;
		
		Path p = Paths.get(to);
		/* if file not exist, continue
		 * If file exists, is regular and not zero-sized, return 0 (skip)
		 * if file exists, regular and size zero, delete it and continue
		 * if file exists, not regular - error
		 */
		if (Files.exists(p))
			if (Files.isRegularFile(p)) 
				if (Files.size(p) > 0)
					return 0;
				else Files.deleteIfExists(p);
			else throw new IOException ("Destination is not a regular file");

		URLConnection connection = from.openConnection();
		
		StringBuilder httpResponseCode = new StringBuilder();
		for (String v: connection.getHeaderFields().get(null))
			httpResponseCode.append(v);
		
		if (httpResponseCode.toString().isEmpty())
			throw new IOException("server response timed out"); // retry?
		if (!httpResponseCode.toString().contains("200 OK"))  
			throw new IOException(String.format("server responded error<%s>",httpResponseCode)); // retry?
	 
		try (InputStream is = connection.getInputStream();
				SeekableByteChannel boch = Files.newByteChannel(p, 
						StandardOpenOption.WRITE,
						StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING))
		{
			byte[] buff = new byte[1024 * 1024];
			ByteBuffer buff2 = ByteBuffer.wrap(buff);
			int numRead;
			while ((numRead = is.read(buff)) != -1) {
				buff2.limit(numRead);
				while (buff2.remaining() > 0) 
					boch.write(buff2); // insufficient disk space -> IOException
				buff2.rewind();
			}
			
			boch.close();
		} catch (IOException e) {
			// on actual write loop to the file;
			Files.delete(p);
			throw e;
		}
		
		if (Files.exists(p)) { 
			// BOoooo. how to know System is done with file?
			// waiting for file to be released. possible infinite loop / clinch here.
			// while (!Files.isWritable(p)) ;
			StatisticGatherer.totalFileDownloadFinished++;
			StatisticGatherer.totalBytesDownloaded += Files.size(p);
			return Files.size(p);
		}
		throw new IOException("Java fucked up and lost your file!");
	}
}
