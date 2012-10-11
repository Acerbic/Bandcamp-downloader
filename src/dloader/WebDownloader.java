package dloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import dloader.pagejob.ProgressReporter;

/**
 * Helps downloading resources from the net
 * @author A.Cerbic
 */
public class WebDownloader {
	
	/**
	 * Downloads and saves a resource by given string address (URL) .
	 * @param from - resource URL string
	 * @param to - file to save to.
	 * @param reporter - way to publish download progress.
	 * @return size of the downloaded file in bytes, 
	 * 			0 if download was skipped (file exists and not zero length)
	 * @throws IOException on stream problems or server has responded bad
	 * @throws InterruptedException 
	 */
	public static long fetchWebFile(String from, String to, ProgressReporter reporter) throws IOException, InterruptedException {
		URL u = new URL(from);
		return fetchWebFile(u,to, reporter);
	}
	
	/**
	 * Downloads and saves a web resource by given URL.
	 * @param from - resource address (HTTP assumed)
	 * @param to - file to save the downloaded resource
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero-length or server has responded badly)
	 * @throws IOException on stream problems AND on server errors/timeouts
	 * 		   InterruptedException when being interrupted
	 */
	public static long fetchWebFile(URL from, String to, ProgressReporter reporter) throws IOException, InterruptedException {
		StatisticGatherer.totalFileDownloadAttempts.incrementAndGet();
		
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

		Path tmpPathDir = dstPath.getParent(); // should be directory
		if (!Files.isDirectory(tmpPathDir))
			return 0;
		
		Path tmpFile = Files.createTempFile(tmpPathDir, "tmp_mp3_", "");
		
		URLConnection connection = from.openConnection();
		if (!checkHttpResponseOK(connection))
			throw new IOException("failed to get OK response from server");
		if (reporter != null) reporter.report("file size", connection.getContentLengthLong());
		long totalRead = 0;
		
		try (	InputStream is = connection.getInputStream();
				SeekableByteChannel boch = Files.newByteChannel(
						tmpFile, 
						StandardOpenOption.WRITE,
						StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING))
		{
		  if (Thread.interrupted())  // Clears interrupted status!
		      throw new InterruptedException();

			byte[] buff = new byte[1024 * 1024];
			ByteBuffer buff2 = ByteBuffer.wrap(buff);
			int numRead;
			while ((numRead = is.read(buff)) != -1) {
				buff2.limit(numRead); // sync wrapper with the input buffer
				while (buff2.remaining() > 0) 
					boch.write(buff2); // IOException on insufficient disk space 
				buff2.rewind(); 
				totalRead += numRead;
				if (reporter != null) reporter.report("downloaded bytes", totalRead);
			}
			
			boch.close();
			
			Files.move(tmpFile, dstPath, StandardCopyOption.REPLACE_EXISTING);
			
		} finally {
			// on actual write loop to the file: delete half-baked file;
			Files.deleteIfExists(tmpFile);
		}
		
		if (Files.exists(dstPath)) { 
			// BOoooo. how to know that OS is done with the file?
			// waiting for file to be released. possible infinite loop / clinch here.
			// while (!Files.isWritable(p)) ;
			StatisticGatherer.totalFileDownloadFinished.incrementAndGet();
			StatisticGatherer.totalBytesDownloaded.addAndGet(Files.size(dstPath));
			return Files.size(dstPath);
		}
		throw new IOException("Java fucked up and lost your file!");
	}
	
	/**
	 * Check whether resource is accessible.
	 * @param connection
	 * @return true if everything is OK. False otherwise.
	 */
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
			try {
				return Files.isReadable(Paths.get(java.net.URLDecoder.decode(u.getFile().substring(1), "UTF-8")));
			} catch (UnsupportedEncodingException e) {
				return false;
			}
		return false; // unknown connection type
	}
}
