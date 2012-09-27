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
	
	boolean downloading = false;
	boolean downloaded = false;
	boolean downloadPageFailed = false;
	boolean upToDate = false;
	
	public TreeNodePageWrapper(AbstractPage page) {
		this.page = page;
	}

	/**
	 * update flags and states of this node visual representation
	 * @param type
	 * @param report
	 * @return true if node must be repainted
	 */
	public boolean update(String type, long report) {
		switch (type) {
		//messages reported by ReadCacheJob and GetPageJob:
		case "checking cache": break;
		case "read from cache": readFromCache = true; return true; 
		//message reported by ReadCacheJob
		case "read cache failed": break;
		//message reported by GetPageJob:
		case "cache reading failed, submitting download job": downloadPageQ = true; return true; 
		// messages reported by DownloadPageJob:
		case "download job started": downloading = true; downloadPageQ = false; return true;
		case "download finished": downloading = false; downloaded = true; return true;
		case "up to date": downloading = false; downloaded = true; upToDate = true; return true;
		case "download failed": downloading = false; downloadPageFailed = true; return true;

		
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
		return false;
	}
	
	@Override
	public String toString() {
		// TODO fix representation
		
		String header = "<html>";
		String bottom = "</html>";
		String styleCompilation = "";
		String title = page.toString();
		
		styleCompilation += "span#url {color:gray; font: 0.8em;}";
		
		// title color
		String titleColor = "black";
		if (readFromCache) titleColor = "blue";
		if (downloaded && !upToDate) titleColor = "green";
		if (downloadPageQ || downloadPageFailed) titleColor = "red";
		if (downloading) titleColor = "orange";
		styleCompilation += "span#title {color:" + titleColor + "}";
		
		// title formatters;
		String childrenCount = (page.childPages.size() > 0)? "<span id='children'>["+page.childPages.size()+"]</span>":"";
		
		if (downloading) {
			title = "Scanning... " + title;
//			styleCompilation += "span#title {font: bold}";
		}
		
		if (downloadPageQ) {
			title = "In queue for scan... " + title;
//			styleCompilation += "span#title {font: bold}";
		}
		// finalize title
		title = "<span id='title'>"+title+"</span>";

		// finalize style
		header += "<style type='text/css'> " + styleCompilation + "</style>";
		
		// output layouts
		if (page instanceof Track)
			return  header +
					title +
					bottom;
		else 
			return header + 
					title + " " + childrenCount +
				"<br>" + "<span id='url'>" + page.url + "</span>" +
				"</u>"+ bottom;
	}

}
