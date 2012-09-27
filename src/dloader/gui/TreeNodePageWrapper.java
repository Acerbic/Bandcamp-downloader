package dloader.gui;

import dloader.page.AbstractPage;
import dloader.page.Track;

/**
 * This class objects are to serve as "user objects" to JTree model node elements.
 * 
 * It simultaneously wraps AbstractPage object, tracks download job progress by capturing PageJob's messages
 * and provides "toString()" method to represent a page in JTree view. 
 * @author Acerbic
 *
 */
public class TreeNodePageWrapper {
	
	public final AbstractPage page; //wrapped object
	
	// TODO job progress flags and logs
	boolean readFromCache = false;
	boolean downloadPageQ = false;
	boolean downloaded = false;
	
	public TreeNodePageWrapper(AbstractPage page) {
		this.page = page;
	}

	public void update(String type, long report) {
		switch (type) {
		//messages reported by ReadCacheJob and GetPageJob:
		case "checking cache": break;
		case "read from cache": readFromCache = true; break;
		//message reported by ReadCacheJob
		case "read cache failed": break;
		//message reported by GetPageJob:
		case "cache reading failed, submitting download job": downloadPageQ = true; break;
		
		/**
		 * summary of the messages reported by DownloadPageJob:
		 * "download job started", 1
		 * "download finished", 1
		 * "up to date", 1
		 * "download failed", 1
		 */
		
		/**
		 * summary of the messages reported by SaveDataJob:
		 * "saving started", 1
		 *     "cover image downloaded", 1 (Album)
		 *     "file size", X (Track)
		 *     "downloaded bytes", X (Track)    
		 *     "file updated", 1 (Track)
		 *     "file downloaded", 1 (Track)
		 * "save skipped", 1
		 * "saved", 1
		 * "saving caused exception", 1
		 */
		
		}
	}
	
	@Override
	public String toString() {
		
		String header = "<html>";
		String bottom = "</html>";
		// TODO fix representation
		if (page instanceof Track)
			return  header +page.toString()+
					bottom;
		else 
			return header+"<b>" +page.toString()+
				"<br></b><u>" +page.url+
				"</u>"+ bottom;
	}

}
