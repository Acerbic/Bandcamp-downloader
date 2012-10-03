package dloader;

import java.awt.*;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
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
		tree.setModel(new DefaultTreeModel(new TreeNodePageWrapper(null)));
//		tree.setCellRenderer(new MyRenderer());		
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
		
		initPrefetch();
	}
	
	private void initPrefetch() {
		if (newWorker == null) {
			lblStatus.setText("Prefetching");
			newWorker = new MyWorker(rootPage, JobType.READCACHEPAGES);
			newWorker.execute();
			btnPrefetch.setEnabled(false);
		}
	}
	
	private void finishPrefetch() {
		if (lblStatus.getText().equals("Prefetching"))
			lblStatus.setText("");
		btnPrefetch.setEnabled(true);		
		newWorker = null;
		
		TreeNodePageWrapper target = (TreeNodePageWrapper)tree.getModel().getRoot();
		try {
			target = (TreeNodePageWrapper) target.getFirstChild();
			tree.expandPath(new TreePath(target.getPath())); 
		} catch (NoSuchElementException e) {
		}
		
	}
	
	private void initScan() {
		if (newWorker == null) { 
			lblStatus.setText("Scanning");
			newWorker = new MyWorker(rootPage, JobType.UPDATEPAGES);
			newWorker.execute();
			btnFetch.setEnabled(false);
		}
	}
	
	private void finishScan() {
		if (lblStatus.getText().equals("Scanning"))
			lblStatus.setText("");
		newWorker = null;
		btnFetch.setEnabled(true);		
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
				childNode = new TreeNodePageWrapper(childPage);
				
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
					TreeNodePageWrapper subChild = new TreeNodePageWrapper(subPage);
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
		
		// pass message to the user object and refresh its visual if needed
//		TreeNodePageWrapper parentWrapper = (TreeNodePageWrapper)parentNode.getUserObject();
		
		// TODO: consider using EventListener mechanic
		if (parentNode.update(message, value)) {
			model.nodeChanged(parentNode);
			if (parentNode.page instanceof Track) {
				TreeNodePageWrapper gParentNode = (TreeNodePageWrapper) parentNode.getParent();
				if (gParentNode != null) {
					gParentNode.kidChanged(parentNode, gParentNode, message, value);
					model.nodeChanged(gParentNode);
				}
			}
		}
		
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
			newWorker.stopJobsForPage(element.page); 
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
