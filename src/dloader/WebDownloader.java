package dloader;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Helps downloading resources from the net, both html pages and other files
 * @author A.Cerbic
 */
public class WebDownloader {
	
//	public final static int MAX_LENGTH_CHECKS = 5;
	public static int lastCheckWebLength = 0;
	public static URL lastCheckWebURL = null;

	/* statistics section */
	public static int totalLengthChecks = 0;
	public static int totalFileDownloadAttempts = 0;
	public static int totalFileDownloadFinished = 0;
	public static int totalPageDownloadFinished = 0;
	public static long totalBytesDownloaded = 0;
	
	/**
	 * Downloads and saves a resource by given string address (URL).
	 * @param from - resource url address
	 * @param to - File to save to.
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero-length or server has responded bad)
	 * @throws FileNotFoundException if can not open file for writing  
	 * @throws IOException on other stream problems
	 */
	public static long fetchWebFile(String from, File to) throws FileNotFoundException, IOException {
		URL u = new URL(from);
		return fetchWebFile(u,to);
	}
	
	/**
	 * Downloads and saves a resource by given URL.
	 * @param from - page address (HTTP assumed)
	 * @param to - file to save the downloaded resource
	 * @return size of the downloaded file in bytes, 
	 * 0 if download was skipped (file exists and not zero-length or server has responded bad)
	 * @throws FileNotFoundException if can not open file for writing  
	 * @throws IOException on other stream problems
	 */
	public static long fetchWebFile(URL from, File to) throws FileNotFoundException, IOException {
		totalFileDownloadAttempts++;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		
		URLConnection connection = from.openConnection();
		// check content type header to catch 404, 403... error responses
		if (connection.getContentType().contains("text/")) 
			return 0;
		 
		/* just delete zero-length files. if size is non-zero, skip it */
		if (to.isFile() && (to.length() > 0))
			return 0;
		to.delete();

		try {
			bis = new BufferedInputStream(connection.getInputStream());
			bos = new BufferedOutputStream(new FileOutputStream(to));

			byte[] buff = new byte[1024 * 100];
			int numRead;
			while ((numRead = bis.read(buff)) != -1)
				bos.write(buff, 0, numRead);
		} catch (IOException e) {
			// on actual write loop to the file;
			to.delete();
			throw e;
		} finally {
			if (bis != null) bis.close();
			if (bos != null) bos.close();
		}
		totalFileDownloadFinished++;
		totalBytesDownloaded += to.length();
		return to.length();
	}

	/**
	 * Downloads a web page by given URL.
	 * @param url - page address (HTTP assumed)
	 * @return the whole page as a String (no headers)
	 * @throws IOException
	 */
//	public static String fetchWebPage(URL url) throws IOException {
//		StringBuilder webPage = new StringBuilder(1024 * 1024);
//		BufferedReader streamBuffer = null;
//
//		try {
//			streamBuffer = new BufferedReader(new InputStreamReader(
//					url.openStream()));
//
//			char[] buf = new char[1024 * 1024]; // 1Mb buffer for a page
//			int numRead;
//			while ((numRead = streamBuffer.read(buf)) != -1)
//				webPage.append(buf, 0, numRead);
//
//		} finally {
//			if (streamBuffer!=null) streamBuffer.close();
//		}
//		totalBytesDownloaded += webPage.length()*(Character.SIZE>>3);
//		totalPageDownloadFinished++;
//		return webPage.toString();
//	}

}
