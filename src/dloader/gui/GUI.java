package dloader.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.Font;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import javax.swing.border.BevelBorder;

import dloader.Main;
import dloader.page.AbstractPage;
import dloader.pagejob.JobMaster.JobType;

import javax.swing.tree.*;
import javax.swing.UIManager.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUI extends JFrame {

	/**
	 * check "GUI_principal.graphml" automaton diagram to understand states transitions and effects
	 */
	private static boolean AUTO_PREFETCH = false;
	private enum UIState {START, READCACHEPAGES, UPDATEPAGES, CHECKSAVINGREQUIREMENT, SAVEDATA, WAIT};
	private UIState state;
	
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
	private JButton btnStop;
	
	
/*	@SuppressWarnings("serial")
	class MyRenderer extends DefaultTreeCellRenderer {

	    public Component getTreeCellRendererComponent(
	                        JTree tree,
	                        Object value,
	                        boolean sel,
	                        boolean expanded,
	                        boolean leaf,
	                        int row,
	                        boolean hasFocus) {

	        super.getTreeCellRendererComponent(
	                        tree, value, sel,
	                        expanded, leaf, row,
	                        hasFocus);
	        if (value instanceof DefaultMutableTreeNode)
	        	return this;
	        return this;
	    }

	}	*/

	
	public Thread getEventDispatchThread() {
		return eventDispatchThread;
	}

	public GUI() throws HeadlessException {
		assert (SwingUtilities.isEventDispatchThread());
		eventDispatchThread = Thread.currentThread();
		
		// install Nimbus look and feel
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
		
		textFieldURL = new JTextField();
		textFieldURL.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reInit();
			}
		});
		textFieldURL.setColumns(10);
		JLabel lblURLLabel = new JLabel("Source URL:");
		lblURLLabel.setLabelFor(textFieldURL);
		
		textFieldDirectory = new JTextField();
		textFieldDirectory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reInit();
			}
		});
		textFieldDirectory.setColumns(10);
		JLabel lblDIRLabel = new JLabel("Target directory:");
		lblDIRLabel.setLabelFor(textFieldDirectory);
		
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
				if (rootPage.isOK())
					initSaveData();
				else 
					initPrefetch();
			}
		});
		
		btnRetag = new JButton("Retag");
		btnRetag.setEnabled(false);
		
		chckbxUseCache = new JCheckBox("cache");
		chckbxUseCache.setEnabled(false);
		
		chckbxLog = new JCheckBox("log");
		chckbxLog.setEnabled(false);

		chckbxForceTag = new JCheckBox("force rescan");
		chckbxForceTag.setEnabled(false);
		
		btnStop = new JButton("Stop");
		btnStop.setEnabled(false);
		btnStop.setFont(new Font("Courier New", Font.BOLD, 14));
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (theWorker != null) {
					btnStop.setEnabled(false);
					theWorker.stopJobs();
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		
		SpringLayout sl_panel = new SpringLayout();
		sl_panel.putConstraint(SpringLayout.NORTH, lblStatus, 12, SpringLayout.SOUTH, chckbxLog);
		sl_panel.putConstraint(SpringLayout.NORTH, chckbxUseCache, 10, SpringLayout.SOUTH, textFieldDirectory);
		sl_panel.putConstraint(SpringLayout.NORTH, btnCheck, -3, SpringLayout.NORTH, textFieldURL);
		sl_panel.putConstraint(SpringLayout.SOUTH, btnCheck, 3, SpringLayout.SOUTH, textFieldDirectory);
		sl_panel.putConstraint(SpringLayout.WEST, lblStatus, 69, SpringLayout.EAST, btnRetag);
		sl_panel.putConstraint(SpringLayout.WEST, btnUpdate, 10, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.NORTH, btnUpdate, 44, SpringLayout.SOUTH, lblDIRLabel);
		sl_panel.putConstraint(SpringLayout.NORTH, btnRetag, 0, SpringLayout.NORTH, btnUpdate);
		sl_panel.putConstraint(SpringLayout.WEST, btnRetag, 6, SpringLayout.EAST, btnUpdate);
		sl_panel.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.SOUTH, btnUpdate);
		sl_panel.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.SOUTH, panel);
		sl_panel.putConstraint(SpringLayout.EAST, textFieldDirectory, 0, SpringLayout.EAST, textFieldURL);
		sl_panel.putConstraint(SpringLayout.WEST, textFieldURL, 35, SpringLayout.EAST, lblURLLabel);
		sl_panel.putConstraint(SpringLayout.EAST, textFieldURL, -6, SpringLayout.WEST, btnCheck);
		sl_panel.putConstraint(SpringLayout.NORTH, chckbxLog, 0, SpringLayout.NORTH, chckbxUseCache);
		sl_panel.putConstraint(SpringLayout.EAST, chckbxUseCache, -10, SpringLayout.EAST, panel);
		sl_panel.putConstraint(SpringLayout.EAST, chckbxLog, 0, SpringLayout.WEST, chckbxUseCache);
		sl_panel.putConstraint(SpringLayout.NORTH, textFieldDirectory, -2, SpringLayout.NORTH, lblDIRLabel);
		sl_panel.putConstraint(SpringLayout.NORTH, lblDIRLabel, 15, SpringLayout.SOUTH, lblURLLabel);
		sl_panel.putConstraint(SpringLayout.EAST, btnCheck, 0, SpringLayout.EAST, scrollPane);
		sl_panel.putConstraint(SpringLayout.NORTH, textFieldURL, -2, SpringLayout.NORTH, lblURLLabel);
		sl_panel.putConstraint(SpringLayout.WEST, textFieldDirectory, 115, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblDIRLabel, 10, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.NORTH, lblURLLabel, 18, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblURLLabel, 10, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.WEST, scrollPane, 10, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.NORTH, chckbxForceTag, 0, SpringLayout.NORTH, chckbxLog);
		sl_panel.putConstraint(SpringLayout.EAST, chckbxForceTag, 0, SpringLayout.WEST, chckbxLog);
		sl_panel.putConstraint(SpringLayout.EAST, lblStatus, -6, SpringLayout.WEST, btnStop);
		sl_panel.putConstraint(SpringLayout.WEST, btnStop, -100, SpringLayout.EAST, panel);
		sl_panel.putConstraint(SpringLayout.NORTH, btnStop, 8, SpringLayout.SOUTH, chckbxUseCache);
		sl_panel.putConstraint(SpringLayout.EAST, btnStop, -10, SpringLayout.EAST, panel);
		panel.setLayout(sl_panel);
		
		scrollPane.setViewportView(tree);
		panel.add(scrollPane);
		panel.add(lblURLLabel);
		panel.add(lblDIRLabel);
		panel.add(btnRetag);
		panel.add(btnUpdate);
		panel.add(lblStatus);
		panel.add(chckbxLog);
		panel.add(chckbxUseCache);
		panel.add(textFieldDirectory);
		panel.add(textFieldURL);
		panel.add(btnCheck);
		panel.add(chckbxForceTag);
		panel.add(btnStop);
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
	
	/**
	 * Call this after constructing GUI.
	 */
	public void init(String url, String saveTo, boolean logging, boolean allowReadCache, boolean forceTagging) {
		state = UIState.START;
		textFieldDirectory.setText(saveTo);
		chckbxLog.setSelected(logging);
		chckbxUseCache.setSelected(allowReadCache);
		chckbxForceTag.setSelected(forceTagging);
		try {
			rootPage = AbstractPage.bakeAPage(null, url, saveTo, null);
			textFieldURL.setText(rootPage.url.toString());
		} catch (IllegalArgumentException e) {
			return;
		}
		lblStatus.setText("Press 'Check' button");
		
		setRootNodeForRootPage();
		enableButtons();
		
		if (AUTO_PREFETCH) {
			initPrefetch();
			state = UIState.READCACHEPAGES;
		} else {
			// no auto-update
			btnUpdate.setEnabled(false);
		}
	}
	
	/** 
	 * Captures SwingWorker finish SwingWorkers' event
	 */
	public void myWorkerDone () {
		switch (state) {
		case READCACHEPAGES:
			assert (theWorker.jm.whatToDo.equals(JobType.READCACHEPAGES));
			finishPrefetch(); 
			initScan();
			break;
		case UPDATEPAGES:
			assert (theWorker.jm.whatToDo.equals(JobType.UPDATEPAGES));
			finishScan(); 
			initCheckSavingReq();
			break;
		case CHECKSAVINGREQUIREMENT:
			assert (theWorker.jm.whatToDo.equals(JobType.CHECKSAVINGREQUIREMENT));
			if (theWorker.bulkResults != null)
				// if in bulk mode...
				try {
					getRootNode().updateSavingReqBunch(theWorker.get());
				} catch (InterruptedException | ExecutionException e) {
					// won't happen as get() is being called from inside of SwingWorker.done() - after job execution is finished
				}
			
			finishCheckSavingReq(); 
			state = UIState.WAIT;
			break;
		case SAVEDATA:
			assert (theWorker.jm.whatToDo.equals(JobType.SAVEDATA));
			finishSaveData(); 
			initCheckSavingReq();
			break;
		default:
			// error state reached
			Main.log(Level.WARNING, "myWorkerDone () -- Error state in GUI.state: " + state.toString());
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
				
				if (parentNode.getChildCount() > 0) {
					// some children are present, but not all of them.
					// normally one would compare parenNode's children to parentPage's children, and insert the new one to proper place, but 
					//  for the sake of efficiency it is faster to just drop all existing children nodes and add new ones instead as a bunch
					parentNode.removeAllChildren();
				}
				
				
				// add new items under this parent
				childNode = addChildrenNodes(parentNode, childPage);
				
				assert (childNode != null); // can happen if ghost sneaked in - other thread compromised AbstractPage tree
				
				// new item's children if any 
				if (childPage.childPages.size() > 0)
					addChildrenNodes(childNode, childPage.childPages.get(0)); // bit awkward code reuse
				
				model.nodeStructureChanged(parentNode); // notification to repaint
			}
			
			parentNode = childNode; // advance to search next element in our pathToPage
		}
		// after search is complete, parent points to a TreeNodePageWrapper containing original p (but now p is different)
		
		// usually unfolding happens only after job is finished (for performance), but in
		// case of new page downloads it is visually more pleasing to see what is going on asap 
		if (parentNode.page.equals(pathToPage.getFirst())) 
			unfoldFirst(); 
		
		// pass message to the user object and refresh its visual if needed
		parentNode.update(message, value);
	}

	/**
	 * Disable controls for the duration of a job
	 */
	private void disableButtons() {
		btnCheck.setEnabled(false);
		btnRetag.setEnabled(false);
		btnUpdate.setEnabled(false);
		textFieldURL.setEnabled(false);
		textFieldDirectory.setEnabled(false);
		
		btnStop.setEnabled(true);
	}
	
	/**
	 * Enable controls to get user commands in-between jobs
	 */
	private void enableButtons() {
		btnCheck.setEnabled(true);		
		btnRetag.setEnabled(false); //XXX: temporary as retag is not functioning yet
		btnUpdate.setEnabled(true);			
		textFieldURL.setEnabled(true);
		textFieldDirectory.setEnabled(true);
		
		btnStop.setEnabled(false);
	}
	
	/**
	 * Reset rootPage and saving directory from updated text fields
	 */
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
			
			initPrefetch(); 
		}
	}
	
	/**
	 * recursively check tree if any file downloads are required.
	 */
	private void initCheckSavingReq() {
		if (theWorker == null) {
			state = UIState.CHECKSAVINGREQUIREMENT;
			lblStatus.setText("Checking files on disk");
			theWorker = new MyWorker(rootPage, JobType.CHECKSAVINGREQUIREMENT, true);
			theWorker.execute();
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
			state = UIState.READCACHEPAGES;
			lblStatus.setText("Prefetching");
			theWorker = new MyWorker(rootPage, JobType.READCACHEPAGES, false);
			theWorker.execute();
			disableButtons();
		}
	}
	
	private void finishPrefetch() {
		lblStatus.setText("");
		theWorker = null;
		
		unfoldFirst();
	}
	
	/**
	 * Starting cache-only quick lookup
	 */
	private void initSaveData() {
		if (theWorker == null) {
			state = UIState.SAVEDATA;
			lblStatus.setText("Downloading files");
			theWorker = new MyWorker(rootPage, JobType.SAVEDATA, false);
			theWorker.execute();
			disableButtons();
		}
	}
	
	private void finishSaveData() {
		lblStatus.setText("");
		
		theWorker = null;
	}	

	/**
	 * Download pages from Internet
	 */
	private void initScan() {
		if (theWorker == null) { 
			state = UIState.UPDATEPAGES;
			lblStatus.setText("Scanning");
			theWorker = new MyWorker(rootPage, JobType.UPDATEPAGES, false);
			theWorker.execute();
		}
	}
	
	private void finishScan() {
		lblStatus.setText("");
		theWorker = null;
		
		unfoldFirst();
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

	private TreeNodePageWrapper getRootNode() {
		TreeNodePageWrapper proxyRoot = (TreeNodePageWrapper) tree.getModel().getRoot();
		return (TreeNodePageWrapper) proxyRoot.getFirstChild();
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
	
	private TreeNodePageWrapper addChildrenNodes (TreeNodePageWrapper parentNode, AbstractPage childPage) {
		TreeNodePageWrapper returnedChildNode = null;
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		
		if (childPage.getParent() == null) {
			returnedChildNode = new TreeNodePageWrapper(childPage, model);
			parentNode.add(returnedChildNode);
			return returnedChildNode;
		} else {
			for (AbstractPage childPageSibling: childPage.getParent().childPages) {
				TreeNodePageWrapper childNode = new TreeNodePageWrapper(childPageSibling, model);
				if (childPageSibling.equals(childPage))
					returnedChildNode = childNode;
				parentNode.add(childNode);
			}
			return returnedChildNode;
		}
	}
}
