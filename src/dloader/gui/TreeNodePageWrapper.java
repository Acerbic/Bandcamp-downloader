package dloader.gui;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;

import dloader.page.AbstractPage;
import dloader.page.Track;

/**
 * This class objects are to serve as "user objects" to JTree model node elements.
 * 
 * It simultaneously wraps AbstractPage object, tracks download job progress by capturing PageJob's messages
 * and provides "toString()" method to represent a page in JTree view. 
 * It does not change node tree model. All modifying of the tree is located at GUI.updateTree(...) 
 * @author Acerbic
 *
 */
public class TreeNodePageWrapper extends DefaultMutableTreeNode {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -265090747493368344L;

	public final AbstractPage page; //wrapped object
	
	// TODO job progress flags and logs
	boolean readFromCache = false;
	boolean downloadPageQ = false;
	
	boolean downloading = false;
	boolean downloaded = false;
	boolean downloadPageFailed = false;
	boolean upToDate = false;
	
	int kidsInProcessing = 0;
	
	public TreeNodePageWrapper(AbstractPage page) {
		super(null);
		this.page = page;
	}

	/**
	 * Update flags and states of this node visual representation
	 * @param type
	 * @param report
	 * @return true if node must be repainted
	 */
	public boolean update(String type, long report) {
		boolean updateVisuals = false;
		switch (type) {
		//messages reported by ReadCacheJob and GetPageJob:
		case "checking cache": break;
		case "read from cache": 
			readFromCache = true; 
			updateVisuals = true; break; 
		//message reported by ReadCacheJob
		case "read cache failed": 
			readFromCache = false; break;
		//message reported by GetPageJob:
		case "cache reading failed, submitting download job": 
			readFromCache = false; downloadPageQ = true; 
			updateVisuals = true; break; 
			
		// messages reported by DownloadPageJob:
		case "download job queued": 
			downloadPageQ = true; 
			updateVisuals = true; break;
		case "download job started": 
			downloading = true; downloadPageQ = false; 
			updateVisuals = true; break;
		case "download finished": 
			downloading = false; downloaded = true; 
			updateVisuals = true; break;
		case "up to date": 
			downloading = false; downloaded = true; upToDate = true; 
			updateVisuals = true; break;
		case "download failed": 
			downloading = false; downloadPageFailed = true; 
			updateVisuals = true; break;

		
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
		return updateVisuals;
	}
	
	@Override
	public String toString() {
		if (page == null) return null;
		// TODO
		
		String header = "<html>";
		String bottom = "</html>";
		String styleCompilation = "";
		String title = page.toString();
		
		styleCompilation += "span#url {color:gray; font: 0.8em;}";
		
		// title color
		String titleColor = "black"; // page data did not differ from net
		if (readFromCache && !upToDate) titleColor = "blue"; // page data was read from cache only 
		if (downloaded && !upToDate) titleColor = "green"; // page data was updated from net
		if (downloadPageQ || downloadPageFailed) titleColor = "red"; // page is in Q to be updated or update failed
		if (downloading) titleColor = "orange"; // in a process of downloading page data
		styleCompilation += "span#title {color:" + titleColor + "}";
		styleCompilation += "span#children {color:" + (kidsInProcessing > 0? "orange" : "black") + "}";
		
		// title formatters;
		String childrenCount = (page.childPages.size() <= 0)? "":
			"<span id='children'>[" +
			(kidsInProcessing <= 0 ? "": (page.childPages.size()-kidsInProcessing) +"/") +
			page.childPages.size()+"]</span>";
		
		if (downloading) {
			title = "Scanning... " + title;
		} 
		else if (downloadPageFailed) {
			title = "Scan failed: " + title;
			styleCompilation += "span#title {font: bold}";
		} else if (downloadPageQ) {
			title = "In queue for scan... " + title;
		}
		
		// finalize title
		title = "<span id='title'>" + title + "</span>";

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

	public void kidChanged(TreeNodePageWrapper kidWrapper,
			DefaultMutableTreeNode thisNode, String message, long value) {
		
		kidsInProcessing = 0;
		for (@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> children = thisNode.children(); children.hasMoreElements();) {
			TreeNodePageWrapper kid = (TreeNodePageWrapper) children.nextElement();
			
			if (kid.downloading || kid.downloadPageQ)
				kidsInProcessing++;
		}
		
	}

}
