package dloader;

import java.awt.*;
import javax.swing.*;
import java.awt.Font;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.swing.border.BevelBorder;

import dloader.JobMaster.JobType;
import dloader.gui.MyWorker;
import dloader.gui.TreeNodePageWrapper;
import dloader.page.AbstractPage;

import javax.swing.tree.*;
import javax.swing.UIManager.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3871202126171183930L;
	private JTextField textFieldURL;
	private JTextField textFieldDirectory;
	private JTree tree;
	
	private AbstractPage rootPage;
	private JLabel lblStatus;
	private JCheckBox chckbxUseCache;
	private JCheckBox chckbxLog;
	private Thread eventDispatchThread;
	private JButton btnCheck;
	private JButton btnUpdate;
	private JButton btnRetag;
	private MyWorker theWorker;
	private JCheckBox chckbxForceTag;
	
	
//	@SuppressWarnings("serial")
//	class MyRenderer extends DefaultTreeCellRenderer {
//
//	    public Component getTreeCellRendererComponent(
//	                        JTree tree,
//	                        Object value,
//	                        boolean sel,
//	                        boolean expanded,
//	                        boolean leaf,
//	                        int row,
//	                        boolean hasFocus) {
//
//	        super.getTreeCellRendererComponent(
//	                        tree, value, sel,
//	                        expanded, leaf, row,
//	                        hasFocus);
//	        if (value instanceof DefaultMutableTreeNode)
//	        	return this;
//	        return this;
//	    }
//
//	}	

	public Thread getEventDispatchThread() {
		return eventDispatchThread;
	}

	public GUI() throws HeadlessException {
		assert (SwingUtilities.isEventDispatchThread());
		eventDispatchThread = Thread.currentThread();
		
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    // If Nimbus is not available, you can set the GUI to another look and feel.
		}
		
		setTitle("Bandcamp dloader");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		
		JLabel lblNewLabel = new JLabel("Source URL:");
		
		textFieldURL = new JTextField();
		textFieldURL.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reInit();
			}
		});
		lblNewLabel.setLabelFor(textFieldURL);
		textFieldURL.setColumns(10);
		
		textFieldDirectory = new JTextField();
		textFieldDirectory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reInit();
			}
		});
		textFieldDirectory.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Target directory:");
		lblNewLabel_1.setLabelFor(textFieldDirectory);
		
		tree = new JTree();
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("JTree")));
		
		lblStatus = new JLabel("Status messages");
		lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
		lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 18));
		
		btnCheck = new JButton("Check");
		btnCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reInit();
			}
		});
		btnUpdate = new JButton("Update files");
		btnUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initSaveData();
			}
		});
		
		btnRetag = new JButton("Retag");
		btnRetag.setEnabled(false);
		
		chckbxUseCache = new JCheckBox("cache");
		chckbxUseCache.setEnabled(false);
		
		chckbxLog = new JCheckBox("log");
		chckbxLog.setEnabled(false);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		
		SpringLayout sl_panel = new SpringLayout();
		sl_panel.putConstraint(SpringLayout.WEST, btnUpdate, 10, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblStatus, 6, SpringLayout.EAST, btnRetag);
		sl_panel.putConstraint(SpringLayout.NORTH, btnUpdate, 44, SpringLayout.SOUTH, lblNewLabel_1);
		sl_panel.putConstraint(SpringLayout.NORTH, btnRetag, 0, SpringLayout.NORTH, btnUpdate);
		sl_panel.putConstraint(SpringLayout.WEST, btnRetag, 6, SpringLayout.EAST, btnUpdate);
		sl_panel.putConstraint(SpringLayout.EAST, lblStatus, -10, SpringLayout.EAST, panel);
		sl_panel.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.SOUTH, btnUpdate);
		sl_panel.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.SOUTH, panel);
		sl_panel.putConstraint(SpringLayout.NORTH, lblStatus, 8, SpringLayout.SOUTH, chckbxLog);
		sl_panel.putConstraint(SpringLayout.NORTH, btnCheck, -3, SpringLayout.NORTH, lblNewLabel);
		sl_panel.putConstraint(SpringLayout.EAST, textFieldDirectory, 0, SpringLayout.EAST, textFieldURL);
		sl_panel.putConstraint(SpringLayout.WEST, textFieldURL, 35, SpringLayout.EAST, lblNewLabel);
		sl_panel.putConstraint(SpringLayout.EAST, textFieldURL, -6, SpringLayout.WEST, btnCheck);
		sl_panel.putConstraint(SpringLayout.NORTH, chckbxUseCache, 10, SpringLayout.SOUTH, btnCheck);
		sl_panel.putConstraint(SpringLayout.SOUTH, btnCheck, 57, SpringLayout.NORTH, lblNewLabel);
		sl_panel.putConstraint(SpringLayout.NORTH, chckbxLog, 0, SpringLayout.NORTH, chckbxUseCache);
		sl_panel.putConstraint(SpringLayout.EAST, chckbxUseCache, -10, SpringLayout.EAST, panel);
		sl_panel.putConstraint(SpringLayout.EAST, chckbxLog, 0, SpringLayout.WEST, chckbxUseCache);
		sl_panel.putConstraint(SpringLayout.NORTH, textFieldDirectory, -2, SpringLayout.NORTH, lblNewLabel_1);
		sl_panel.putConstraint(SpringLayout.NORTH, lblNewLabel_1, 15, SpringLayout.SOUTH, lblNewLabel);
		sl_panel.putConstraint(SpringLayout.EAST, btnCheck, 0, SpringLayout.EAST, scrollPane);
		sl_panel.putConstraint(SpringLayout.NORTH, textFieldURL, -2, SpringLayout.NORTH, lblNewLabel);
		sl_panel.putConstraint(SpringLayout.WEST, textFieldDirectory, 115, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblNewLabel_1, 10, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.NORTH, lblNewLabel, 18, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblNewLabel, 10, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.WEST, scrollPane, 10, SpringLayout.WEST, panel);
		panel.setLayout(sl_panel);
		
		scrollPane.setViewportView(tree);
		panel.add(scrollPane);
		panel.add(lblNewLabel);
		panel.add(lblNewLabel_1);
		panel.add(btnRetag);
		panel.add(btnUpdate);
		panel.add(lblStatus);
		panel.add(chckbxLog);
		panel.add(chckbxUseCache);
		panel.add(textFieldDirectory);
		panel.add(textFieldURL);
		panel.add(btnCheck);
		
		chckbxForceTag = new JCheckBox("force rescan");
		chckbxForceTag.setEnabled(false);
		sl_panel.putConstraint(SpringLayout.NORTH, chckbxForceTag, 0, SpringLayout.NORTH, chckbxLog);
		sl_panel.putConstraint(SpringLayout.EAST, chckbxForceTag, 0, SpringLayout.WEST, chckbxLog);
		panel.add(chckbxForceTag);
	}


	public GUI(GraphicsConfiguration gc) {
		super(gc);
	}

	public GUI(String title) throws HeadlessException {
		super(title);
	}

	public GUI(String title, GraphicsConfiguration gc) {
		super(title, gc);
	}
	
	public void init() {
		textFieldDirectory.setText(Main.saveTo);
		chckbxLog.setSelected( Main.logger != null);
		chckbxUseCache.setSelected(Main.allowFromCache);
		chckbxForceTag.setSelected(Main.forceTagging);
		try {
			rootPage = AbstractPage.bakeAPage(null, Main.baseURL, Main.saveTo, null);
			textFieldURL.setText(rootPage.url.toString());
		} catch (IllegalArgumentException e) {
			return;
		}
		lblStatus.setText("Press 'Check' button");
		
		setRootNodeForRootPage();
//		initPrefetch();
	}
	
	private void disableButtons() {
		btnCheck.setEnabled(false);
		btnRetag.setEnabled(false);
		btnUpdate.setEnabled(false);
		textFieldURL.setEnabled(false);
		textFieldDirectory.setEnabled(false);
	}
	
	private void enableButtons() {
		btnCheck.setEnabled(true);		
		btnRetag.setEnabled(false); //XXX: temporary as retag is not functioning yet
		btnUpdate.setEnabled(true);			
		textFieldURL.setEnabled(true);
		textFieldDirectory.setEnabled(true);
	}
	
	private void reInit() {
		// fix/normalize url if possible
		String newURL = AbstractPage.fixURLString(null, textFieldURL.getText());
		
		//TODO: add normalization to path, creation of non-existing dirs request and such.
		String newDir = textFieldDirectory.getText();
		
		if (newURL == null) {
			// fix failed, restoring current root url if one exists
			// FIXME: maybe error status message and leave wrong text in place (for continued editing?)
			if (rootPage != null) {
				textFieldURL.setText(rootPage.url.toString());
				textFieldDirectory.setText(rootPage.saveTo);
			}
			return;
		}
		
		// apply fix
		textFieldURL.setText(newURL);
		
		// check if no change happened
		if (newURL.equals(rootPage.url.toString()) &&
			newDir.equals(rootPage.saveTo)) {
			
			// url and dir did not change. 
			initPrefetch(); // still full rescan.
			return;
		}
			
		// now at least directory or URL changed --> drop old root and start anew
		AbstractPage newRootPage = null;
		try {
			newRootPage = AbstractPage.bakeAPage(null, newURL, newDir, null);
		} catch (IllegalArgumentException e) {
			// fail to make a page out of newURL.
			if (rootPage != null) {
				textFieldURL.setText(rootPage.url.toString());
				textFieldDirectory.setText(rootPage.saveTo);
			}
		}
		
		if (newRootPage != null) {
			rootPage = newRootPage;
			setRootNodeForRootPage();
			
			initPrefetch(); // prefetch -> fetch -> checkSaving chain job start
		}
	}
	
	/**
	 * recursively check tree if any file downloads are required.
	 */
	private void initCheckSavingReq() {
		if (theWorker == null) {
			lblStatus.setText("Checking files on disk");
			theWorker = new MyWorker(rootPage, JobType.CHECKSAVINGREQUIREMENT);
			theWorker.execute();
//			disableButtons();
		}
	}
	
	private void finishCheckSavingReq() {
		lblStatus.setText("");
		theWorker = null;
		enableButtons();
	}
	
	/**
	 * Starting cache-only quick lookup
	 */
	private void initPrefetch() {
		if (theWorker == null) {
			lblStatus.setText("Prefetching");
			theWorker = new MyWorker(rootPage, JobType.READCACHEPAGES);
			theWorker.execute();
			disableButtons();
		}
	}
	
	private void finishPrefetch() {
		lblStatus.setText("");
	
//		enableButtons();
		theWorker = null;
		
		unfoldFirst();
		// commented out for later. now we are in testing mode.
		initScan();
	}
	
	/**
	 * Starting cache-only quick lookup
	 */
	private void initSaveData() {
		if (theWorker == null) {
			lblStatus.setText("Downloading files");
			theWorker = new MyWorker(rootPage, JobType.SAVEDATA);
			theWorker.execute();
			disableButtons();
		}
	}
	
	private void finishSaveData() {
		lblStatus.setText("");
//		enableButtons();
		
		theWorker = null;
		initCheckSavingReq();
	}	

	/**
	 *  Shows children of the rootPage's node elements
	 */
	private void unfoldFirst() {
		TreeNodePageWrapper target = (TreeNodePageWrapper)tree.getModel().getRoot();
		try {
			target = (TreeNodePageWrapper) target.getFirstChild();
			tree.expandPath(new TreePath(target.getPath())); 
		} catch (NoSuchElementException e) {
		}
	}
	
	private void initScan() {
		if (theWorker == null) { 
			lblStatus.setText("Scanning");
			theWorker = new MyWorker(rootPage, JobType.UPDATEPAGES);
			theWorker.execute();
//			disableButtons();
		}
	}
	
	private void finishScan() {
		lblStatus.setText("");
		theWorker = null;
//		enableButtons();		
		
		initCheckSavingReq();
		unfoldFirst();
	}
	
	/**
	 * Replaces old tree with new root.
	 */
	private void setRootNodeForRootPage() {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel(); 
		TreeNodePageWrapper x = new TreeNodePageWrapper(null, model); // proxy null root for better display
		x.add(new TreeNodePageWrapper(rootPage, model)); 
		model.setRoot(x);
	}
	
	/** 
	 * Captures SwingWorker finish jobs event
	 * @param root - root job for the work in question (not used atm)
	 * @param jobType - job type
	 */
	public void myWorkerDone (AbstractPage root, JobMaster.JobType jobType) {
		switch  (jobType) {
		case READCACHEPAGES: finishPrefetch(); break;
		case SAVEDATA: finishSaveData(); break;
		case UPDATEPAGES: finishScan(); break;
		case CHECKSAVINGREQUIREMENT: finishCheckSavingReq(); break;
		default:
			break;
		}
	}

	/**
	 * Receiving message from MyWorker (SwingWorker)
	 * @param p - page node to update
	 * @param message - status info
	 * @param value - numeral info 
	 */
	public void updateTree (AbstractPage p, String message, long value) {
		// construct list of elements from root page to this page while checking pages to be actual children 
		Deque<AbstractPage> pathToPage = new LinkedList<AbstractPage>();
		while (p != null) {
			pathToPage.push(p);
			if (p.getParent() == null) {
				if (!p.equals(rootPage))
					// root node's page is not the same as root of AbstractPage tree. <- something is really wrong
					return;
			} 
			else if (!p.getParent().childPages.contains(p))
				// stray ghost report on page no longer in a tree
				return;
			p = p.getParent();
		}
		
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		TreeNodePageWrapper parentNode = (TreeNodePageWrapper) model.getRoot(); 
		for (AbstractPage childPage: pathToPage) {
			// check if this page exists in the parent
			TreeNodePageWrapper childNode = getNodeOfPage (parentNode, childPage);
			
			if (childNode == null) {
				// currentPage's node was not found in parent node 
				
				// add new item under this parent
				childNode = new TreeNodePageWrapper(childPage, model);
				
				if (childPage.getParent() == null)
					parentNode.add(childNode); // to the end
				else {
					int insertionIndex = findInsertionPoint(parentNode, childPage);
					
					if (insertionIndex == -1)
						parentNode.add(childNode); // to the end
					else 
						parentNode.insert(childNode, insertionIndex);
				}
				
				// new item's children if any (that way they maintain their order)
				for (AbstractPage subPage: childPage.childPages) {
					TreeNodePageWrapper subChild = new TreeNodePageWrapper(subPage, model);
					childNode.add(subChild);
				}
				
				int[] indices = new int[1];
				indices[0] = parentNode.getIndex(childNode);
				model.nodesWereInserted(parentNode, indices); // notification to repaint
				
			}
			
			parentNode = childNode; // advance to search next element in our pathToPage
		}
		// after search is complete, parent points to a TreeNodePageWrapper containing original p (but now p is different)
		
		// Reading cache/ downloading a page forces reset of page data (children refs), 
		//  the node branch must be trimmed accordingly
		if (message.equals("read from cache") || message.equals("read cache failed") 
			|| message.equals("cache reading failed, submitting download job")
			|| message.equals("download finished") || message.equals("up to date")
			|| message.equals("download failed")
			)
			trimBranch(parentNode, model);
		
		// usually unfolding happens only after job is finished (for performance), but in
		// case of new page downloads it is visually more pleasing to see what is going on asap 
		if (message.equals("download finished") && parentNode.page.equals(pathToPage.getFirst())) 
			unfoldFirst(); 
		
		// pass message to the user object and refresh its visual if needed
		parentNode.update(message, value);
	}

	/**
	 * Time to find proper insertion position (assume nodes are ordered same way as pages so far with "skips" probably existing)
	 * check "DoubleListMatcher.graphml" automaton diagram to understand the following
	 *	p1   p2   p3   p4        p5  p6
	 *	n1             n2   n3   n4
	 * @param parentNode - node to which new node will be inserted.
	 * @param childPage - a page to be inserted. childPage.getParent() must not be null.
	 * @return -1 if new node should be appended to the end, or a proper insertion index
	 */
	private int findInsertionPoint(TreeNodePageWrapper parentNode,
			AbstractPage childPage) {
		Iterator<AbstractPage> pageLooker = childPage.getParent().childPages.iterator();
		@SuppressWarnings("unchecked")
		Enumeration<TreeNodePageWrapper> nodeLooker = parentNode.children();
		
		// *S*
		// *A1*
		while (nodeLooker.hasMoreElements()) { // *G* exit
			TreeNodePageWrapper currentNode = nodeLooker.nextElement();
			
			// *A2*
			if (!pageLooker.hasNext())
				break; // *H* an ERROR has happened, since it is supposed to be and an impossible state.
			AbstractPage currentPage = pageLooker.next();
			
			// these are B-C-D123-B and B-C-E-B loops 
			b_loop:
			while (! currentNode.page.equals(currentPage)) { // *A2*->*B* check AND *E*->*B* check
				// *B*
				if (childPage.equals(currentPage)) {
					return parentNode.getIndex(currentNode); // *F*
				}
				// *C*
				if (parentNode.page.childPages.contains(currentNode.page)) {
					// *D2*
					while (pageLooker.hasNext()) 
						currentPage = pageLooker.next(); 
						if (currentNode.page.equals(currentPage))
							break b_loop; // -> *A*
						// *D3*
						if (childPage.equals(currentPage)) {
							return parentNode.getIndex(currentNode); // *F*
					}
					return -1; //*H* 
				}
				else // *E*
					if (nodeLooker.hasMoreElements())
						currentNode = nodeLooker.nextElement(); //skip one 
					// -> *B*
					else 
						break; // *G* exit
			}
		}
		// *G*
		return -1;
	}
	
	/**
	 * Remove branch->children nodes don't correspond to branch->page->children pages
	 * and cancel their respective jobs running and scheduled
	 * O (n*n)
	 * @param branch - node branch to clean up
	 * @param model - reference model
	 */
	private void trimBranch(TreeNodePageWrapper branch, DefaultTreeModel model) {
		AbstractPage branchPage = branch.page;
		Collection<TreeNodePageWrapper> removeList = new LinkedList<>();
		for (@SuppressWarnings("unchecked")
			Enumeration<TreeNodePageWrapper> children = branch.children(); children.hasMoreElements();) {
			TreeNodePageWrapper childNode = children.nextElement();
			AbstractPage childPage = childNode.page; 
			if (! branchPage.childPages.contains(childPage)) {
				// child's page is no more contained within its parent's page children collection
				removeList.add(childNode); // can't remove on spot, it will fuck up the iteration
			}
		}
		
		for (TreeNodePageWrapper element: removeList) {
			// ordinary, this should never happen. if this is executing, it means that cache data was in conflict
			// with updated net data.
			model.removeNodeFromParent(element);
			//since this page is no more of our concern, all jobs executing and pending are irrelevant CPU consumers
			theWorker.stopJobsForPage(element.page); 
		}
	}

	/**
	 * Search children nodes of a parentNode for a TreeNodePageWrapper containing given AbstractPage. 
	 * @param parentNode - parent of nodes to search among
	 * @param page - page to search
	 * @return found node or null if not found
	 */
	private TreeNodePageWrapper getNodeOfPage (TreeNodePageWrapper parentNode, AbstractPage page) {
		TreeNodePageWrapper childNode = null;
		for (@SuppressWarnings("unchecked")
		Enumeration<TreeNodePageWrapper> children = parentNode.children(); children.hasMoreElements();) {
			childNode = children.nextElement();
			if (page.equals(childNode.page)) 
				return childNode;
		}
		return null;
	}
}
