package dloader;

import java.awt.*;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Font;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.swing.border.BevelBorder;

import dloader.JobMaster.JobType;
import dloader.gui.MyWorker;
import dloader.gui.TreeNodePageWrapper;
import dloader.page.AbstractPage;
import dloader.page.Track;

import javax.swing.tree.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
	private JButton btnPrefetch;
	private JButton btnFetch;
	private JButton btnFix;
	private JButton btnUpdate;
	private JButton btnRetag;
	private MyWorker newWorker;
	
	
	@SuppressWarnings("serial")
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

	}	

	public Thread getEventDispatchThread() {
		return eventDispatchThread;
	}

	@SuppressWarnings("serial")
	public GUI() throws HeadlessException {
		assert (SwingUtilities.isEventDispatchThread());
		eventDispatchThread = Thread.currentThread();
		
		
		setTitle("Bandcamp dloader");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		
		JLabel lblNewLabel = new JLabel("Source URL:");
		
		textFieldURL = new JTextField();
		textFieldURL.setEditable(false);
		lblNewLabel.setLabelFor(textFieldURL);
		textFieldURL.setColumns(10);
		
		textFieldDirectory = new JTextField();
		textFieldDirectory.setEditable(false);
		textFieldDirectory.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Target directory:");
		lblNewLabel_1.setLabelFor(textFieldDirectory);
		
		tree = new JTree();
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setEditable(true);
		tree.setModel(new DefaultTreeModel(
			new DefaultMutableTreeNode("JTree") {
				{}
			}
		));
		tree.setCellRenderer(new MyRenderer());		
		tree.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		
		lblStatus = new JLabel("kkkkss");
		lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
		lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 18));
		
		btnPrefetch = new JButton("Prefetch");
		btnFetch = new JButton("Fetch");
		btnFix = new JButton("Fix");
		btnFix.setEnabled(false);
		btnUpdate = new JButton("Update");
		btnRetag = new JButton("Retag");
		
		
		chckbxUseCache = new JCheckBox("cache");
		chckbxUseCache.setEnabled(false);
		
		chckbxLog = new JCheckBox("log");
		chckbxLog.setEnabled(false);
		
		JScrollPane scrollPane = new JScrollPane();
		
		btnPrefetch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				initPrefetch();
			}
		});
		
		btnFetch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				initScan();
			}
		});
		
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
						.addGroup(Alignment.LEADING, gl_panel.createSequentialGroup()
							.addContainerGap()
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE))
						.addGroup(Alignment.LEADING, gl_panel.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
								.addGroup(gl_panel.createSequentialGroup()
									.addGroup(gl_panel.createParallelGroup(Alignment.LEADING, false)
										.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(lblNewLabel_1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
										.addComponent(textFieldDirectory, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 617, Short.MAX_VALUE)
										.addComponent(textFieldURL, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 617, Short.MAX_VALUE)))
								.addGroup(gl_panel.createSequentialGroup()
									.addComponent(btnPrefetch)
									.addGap(7)
									.addComponent(btnFetch, GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnFix)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnUpdate)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE))))
						.addGroup(Alignment.LEADING, gl_panel.createSequentialGroup()
							.addGap(28)
							.addComponent(btnRetag)
							.addPreferredGap(ComponentPlacement.RELATED, 538, Short.MAX_VALUE)
							.addComponent(chckbxLog)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(chckbxUseCache)))
					.addContainerGap())
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNewLabel)
						.addComponent(textFieldURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(textFieldDirectory, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblNewLabel_1))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnRetag)
						.addComponent(chckbxUseCache)
						.addComponent(chckbxLog))
					.addGap(44)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnPrefetch)
						.addComponent(lblStatus)
						.addComponent(btnFetch)
						.addComponent(btnFix)
						.addComponent(btnUpdate))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
					.addContainerGap())
		);
		
		scrollPane.setViewportView(tree);
		panel.setLayout(gl_panel);
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
		textFieldURL.setText(Main.baseURL);
		rootPage = AbstractPage.bakeAPage(null, Main.baseURL, Main.saveTo, null);
		lblStatus.setText("Preparing");
		chckbxLog.setSelected( Main.logger != null);
		chckbxUseCache.setSelected(Main.allowFromCache);
		
		updateTree (rootPage, "", 1); //empty message to put root page on display;
	}
	
	private void initPrefetch() {
		lblStatus.setText("Prefetching");
		if (newWorker != null) 
			newWorker = new MyWorker(rootPage, JobType.READCACHEPAGES);
		newWorker.execute();
		btnPrefetch.setEnabled(false);		
	}
	
	private void finishPrefetch() {
		if (lblStatus.getText().equals("Prefetching"))
			lblStatus.setText("");
		btnPrefetch.setEnabled(true);		
		newWorker = null;
	}
	
	private void initScan() {
		lblStatus.setText("Scanning");
		if (newWorker != null) 
			newWorker = new MyWorker(rootPage, JobType.UPDATEPAGES);
		newWorker.execute();
		btnFetch.setEnabled(false);		
	}
	
	private void finishScan() {
		if (lblStatus.getText().equals("Scanning"))
			lblStatus.setText("");
		newWorker = null;
		btnFetch.setEnabled(true);		
	}
	
	/**
	 * Receiving message from SwingWorker
	 * @param p - page node to update
	 * @param message - status info
	 * @param value - numeral info 
	 */
	public void updateTree (AbstractPage p, String message, long value) {
		// construct list of elements from root page to this page
		Deque<AbstractPage> pathToPage = new LinkedList<AbstractPage>();
		while (p != null) {
			pathToPage.push(p);
			p = p.getParent();
		}
		
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) model.getRoot(); 
		for (AbstractPage currentPage: pathToPage) {
			// check if this page exists in the tree
			DefaultMutableTreeNode child = null;
			TreeNodePageWrapper childsUserObject = null;
			boolean found = false;
			for (@SuppressWarnings("unchecked")
			Enumeration<DefaultMutableTreeNode> children = parent.children(); children.hasMoreElements();) {
				child = children.nextElement();
				if (child.getUserObject() instanceof TreeNodePageWrapper)
					childsUserObject = (TreeNodePageWrapper) child.getUserObject();
				if (currentPage.equals(childsUserObject.page)) {
					found = true;
					break;
				}
			}
			if (! found) {
				//TODO: fix elements order in tree.... 
				
				// add new item under this parent
				child = new DefaultMutableTreeNode();
				childsUserObject = new TreeNodePageWrapper(currentPage, child);
				child.setUserObject(childsUserObject);
				
				parent.add(child);
				
				int[] indices = new int[1];
				indices[0] = parent.getIndex(child);
				
				// new item's children if any (that way they maintain their order)
				for (AbstractPage subPage: currentPage.childPages) {
					DefaultMutableTreeNode subChild = new DefaultMutableTreeNode();
					subChild.setUserObject(new TreeNodePageWrapper(subPage, subChild));					
				}
				model.nodesWereInserted(parent, indices); //notification to repaint
				
				//expand only if not a Track
				if (!(childsUserObject.page instanceof Track))
					tree.expandPath(new TreePath(parent.getPath())); 
			}
			
			parent = child; // advance to search next element in our pathToPage
		}
		// after search is complete, parent points to a DefaultMutableTreeNode 
		//  containing TreeNodePageWrapper containing original p (but now p is different)
		
		// Reading cache forces reset of page data (children refs), the node branch must be trimmed accordingly
		if (message.equals("read from cache") || message.equals("read cache failed") 
			|| message.equals("cache reading failed, submitting download job")
			|| message.equals("download finished") || message.equals("up to date")
			|| message.equals("download failed")
			)
			trimBranch(parent, model);
		
		// pass message to the user object and refresh its visual if needed
		if (((TreeNodePageWrapper)parent.getUserObject()).update(message, value))
			model.nodeChanged(parent);
		
	}
	
	/**
	 * Remove branch->children nodes don't correspond to branch->page->children pages
	 * @param branch - node branch to clean up
	 * @param model - reference model
	 */
	private void trimBranch(DefaultMutableTreeNode branch, DefaultTreeModel model) {
		AbstractPage branchPage = pageOfNode(branch);
		Collection<DefaultMutableTreeNode> removeList = new LinkedList<>();
		for (@SuppressWarnings("unchecked")
			Enumeration<DefaultMutableTreeNode> children = branch.children(); children.hasMoreElements();) {
			DefaultMutableTreeNode childNode = children.nextElement();
			AbstractPage childPage = pageOfNode(childNode); 
			if (! branchPage.childPages.contains(childPage)) {
				// child's page is no more contained within its parent's page children collection
				removeList.add(childNode); // can't remove on spot, it will fuck up the iteration
			}
		}
		for (DefaultMutableTreeNode element: removeList) { 
			model.removeNodeFromParent(element);
			//since this page is no more of our concern, all jobs executing and pending are irrelevant CPU consumers
			newWorker.stopJobsForPage(pageOfNode(element)); 
		}
	}

	// captures SwingWorker finish jobs event
	public void myWorkerDone (AbstractPage root, JobMaster.JobType jobType) {
		switch  (jobType) {
		case READCACHEPAGES: finishPrefetch(); break;
		case SAVEDATA:
			break;
		case UPDATEPAGES: finishScan(); break;
		default:
			break;
		}
	}
	
	private AbstractPage pageOfNode(DefaultMutableTreeNode node) {
		if (node.getUserObject() instanceof TreeNodePageWrapper)
			return ((TreeNodePageWrapper)node.getUserObject()).page;
		else return null;
	}
}
