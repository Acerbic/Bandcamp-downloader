package dloader.gui;

import java.util.Enumeration;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import dloader.gui.MyWorker.ProgressReportStruct;
import dloader.page.AbstractPage;
import dloader.page.Album;
import dloader.page.Track;

/**
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
	public final DefaultTreeModel model; //backref to model
	
	private String titleCache;
	
	boolean readFromCache = false;
	boolean downloadPageQ = false;
	
	boolean downloading = false;
	boolean downloaded = false;
	boolean downloadPageFailed = false;
	boolean upToDate = false;
	
	boolean mustSavePage = false;
	
	boolean saving = false;
	long fullSize = 0;
	long savedSoFar = 0;
	
	int kidsInScanning = 0;
	int kidsToSave = 0;
	
	public TreeNodePageWrapper(AbstractPage page, TreeModel treeModel) {
		super(null);
		this.model = (DefaultTreeModel) treeModel;
		this.page = page;
	}

	/**
	 * Update flags and states of this node visual representation
	 * @param message
	 * @param value
	 */
	public void update(String message, long value) {
		boolean updateVisuals = false;
		boolean updateParent = false;
		switch (message) {
			
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
			updateVisuals = true; updateParent = true; break;
		case "download job started": 
			downloading = true; downloadPageQ = false; 
			updateVisuals = true; break;
		case "download finished": 
			downloading = false; downloaded = true; 
			updateVisuals = true; updateParent = true; break;
		case "up to date": 
			downloading = false; downloaded = true; upToDate = true; 
			updateVisuals = true; updateParent = true; break;
		case "download failed": 
			downloading = false; downloadPageFailed = true; 
			updateVisuals = true; updateParent = true; break;

		// messages reported by CheckSavingJob:
		case "saving not required":
			mustSavePage = false; 
			saving = false; // if runs post-stop 
			updateVisuals = true; updateParent = true; break;
		case "saving required":
			mustSavePage = true;
			saving = false; // if runs post-stop 
			updateVisuals = true; updateParent = true; break;
			
		// messages reported by SaveDataJob:
		case "saving started":
			if (mustSavePage) saving = true; // only means saving job started, not actual downloading, so we check if page needs saving to lower false alarms
			updateVisuals = true; updateParent = true; break;
		case "cover image downloaded":
			saving = false;
			updateVisuals = true; updateParent = true; break;
		case "file size":
			fullSize = value; savedSoFar = 0; saving = true;
			updateVisuals = true; break;
		case "downloaded bytes":
			if (savedSoFar < value) savedSoFar = value;
			if (savedSoFar < fullSize) 
				saving = true;
			else {
				saving = false; updateParent = true;
			}
			updateVisuals = true; break;
//		case "file updated":
//			saving = false;
//			updateVisuals = true; updateParent = true; break;
//		case "file downloaded":
//			saving = false;
//			updateVisuals = true; updateParent = true; break;
		case "save skipped":
			saving = false; mustSavePage = false;
			updateVisuals = true; updateParent = true; break;
		case "saved":
			saving = false; mustSavePage = false;
			updateVisuals = true; updateParent = true; break;
		case "saving caused exception":
			saving = false;
			updateVisuals = true; updateParent = true; break;
		}
		
		if (((page instanceof Track) || (page instanceof Album)) && updateParent) {
			TreeNodePageWrapper parentNode = (TreeNodePageWrapper) getParent();
			if (parentNode != null) {
				parentNode.kidChanged(this, message, value);
				model.nodeChanged(parentNode);
			}
		}
		if (updateVisuals)
			model.nodeChanged(this);

		titleCache = page.getTitle();
	}
	
	@Override
	public String toString() {
		if (page == null) return null;
		if (titleCache == null)
			titleCache = page.getTitle();
		
		String header = "<html>";
		String bottom = "</html>";
		String styleCompilation = "";
		String title = titleCache; 
		if (title == null || title.isEmpty())
			title = "????";
		String saveDecorator = "";
		
		styleCompilation += "span#url {color:gray; font: 0.8em;}";
		
		// title color
		String titleColor = "black"; // page data did not differ from net
		if (readFromCache && !upToDate) titleColor = "blue"; // page data was read from cache only 
		if (downloaded && !upToDate) titleColor = "green"; // page data was updated from net
		if (downloadPageQ || downloadPageFailed) titleColor = "red"; // page is in Q to be updated or update failed
		if (downloading) titleColor = "orange"; // in a process of downloading page data
		styleCompilation += "span#title {color:" + titleColor + "}";
		
		String childrenCountColor = "black";
		if (kidsInScanning > 0)
			childrenCountColor = "orange";
		else if (kidsToSave > 0)
			childrenCountColor = "red";
		styleCompilation += "span#children {color:" + childrenCountColor + "}";
		
		// title formatters;
		
		int childrenCount = (kidsInScanning > 0)? kidsInScanning: kidsToSave;
		int childrenSize = page.childPages.size();
		String strChildrenCount = (childrenSize <= 0)? "":
			"<span id='children'>[" +
			(childrenCount <= 0 ? "": (childrenSize-childrenCount) +"/") +
			childrenSize+"]</span>";
		
		if (downloading) {
			title = title + " (Scanning...)";
		} 
		else if (downloadPageFailed) {
			title = "Scan failed: " + title;
			styleCompilation += "span#title {font: bold}";
		} else if (downloadPageQ) {
			title = title + " (In queue for scan...)";
		}
		
		if (mustSavePage || saving) {
			saveDecorator = "{NEW!}";
			String progress = (fullSize > savedSoFar)? String.valueOf(savedSoFar*100/fullSize) : null;
			if (saving) 
				saveDecorator = (progress != null) ? 
						"{Downloading: " + progress + "%}" : "{Downloading...}";
			styleCompilation += "span#saving {color:red}";
		}
		
		// finalize title
		title = "<span id='title'>" + title + "</span>";

		// finalize style
		header += "<style type='text/css'> " + styleCompilation + "</style>";
		
		// finalize save decorator
		saveDecorator = "<span id='saving'>" + saveDecorator + "</span>";
		
		// output layouts
		if (page instanceof Track)
			return  header +
					title + " " + saveDecorator +
					bottom;
		else 
			return header + 
					title + " " + strChildrenCount + " " + saveDecorator +
				"<br>" + "<span id='url'>" + page.url + "</span>" +
				"</u>"+ bottom;
	}

	public void kidChanged(TreeNodePageWrapper kidWrapper, String message, long value) {
		
		kidsInScanning = 0;
		kidsToSave = 0;
		for (@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> children = children(); children.hasMoreElements();) {
			TreeNodePageWrapper kid = (TreeNodePageWrapper) children.nextElement();
			
			if (kid.downloading || kid.downloadPageQ)
				kidsInScanning++;
			if (kid.mustSavePage || kid.saving)
				kidsToSave++;
		}
		
	}

	/**
	 * Updates tree nodes visuals according to the check results all at once
	 * @param bulkResults
	 */
	public void updateSavingReqBunch(
			Map<AbstractPage, ProgressReportStruct> bulkResults) {

		Long value = bulkResults.get(page).value;
		if (value != null)
			mustSavePage = value == 0;

		downloadPageQ = false;
		downloading = false;
		
		kidsToSave = 0;
		for (@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> children = children(); children.hasMoreElements();) {
			TreeNodePageWrapper kid = (TreeNodePageWrapper) children.nextElement();
			
			kid.updateSavingReqBunch(bulkResults);
			if (kid.mustSavePage || kid.saving)
				kidsToSave++;
		}
		
		model.nodeChanged(this);
	}
}
