package dloader;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;

import dloader.PageJob.JobStatusEnum;
import dloader.page.AbstractPage;

/**
 * A class to present GUI of downloading process and controls over it
 * As of right now only ONE project at a time can be downloaded - 1 root item
 * @author A.Cerbic
 */
public class GUI extends JFrame {
	public final class PageProcessorWorker extends
			SwingWorker<PageJob, PageJob> {
		final boolean isLazyWorker;
		PageProcessorWorker(boolean isLazyWorker) {
			this.isLazyWorker = isLazyWorker;
		}
		// Worker thread
		@Override
		public PageJob doInBackground() {
			try {
				PageJob res = PageProcessor.doSingleJob(isLazyWorker);
				if (!Thread.currentThread().isInterrupted() &&
					res != null) {
					
					PageProcessorWorker worker = new PageProcessorWorker(isLazyWorker);
					worker.execute(); // next job GO
				}
				
				return res;
			} catch (Throwable e) {
				// FIXME: needs decoupling from Main
				Main.logger.log(Level.SEVERE, "", e);
			}
			return null;
		}

		// GUI Event Dispatch thread
		@Override
		protected void done() {
			try {
				updateTreeByJob(get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		// GUI Event Dispatch thread
		@Override
		public void process(List<PageJob> pjList) {
			assert (pjList != null);

			while (!pjList.isEmpty()) {
				PageJob pj = pjList.remove(0);
				assert (pj != null);
				
				// remove all consequent reports of this job from current list (duplications)
				pjList.remove(pj);
				updateTreeByJob(pj);
			}
		}
	}
	
	/**
	 * This class stores references to DefaultMutableTreeNode, 
	 * whether they are part of some TreeModel or not
	 * This class is not thread safe.
	 * @author A.Cerbic
	 */
	class TreeNodeCacher {
		private Map<PageJob, DefaultMutableTreeNode> jobToTreenode = new Hashtable<>();
		
		/**
		 * Looks up a DefaultMutableTreeNode in a jobToTreenode reference map for given AbstractPage.
		 * Searching job by page is valid since they are coupled 1-to-1 
		 * @param page - the page which job progress is displayed by tree node question.
		 * @return the tree node for given page or null if not found (null argument yields null in return)
		 */
		public
		DefaultMutableTreeNode getTreeNodeByPage(AbstractPage page) {
			if (page == null) return null;
			for (Map.Entry<PageJob, DefaultMutableTreeNode> entry: jobToTreenode.entrySet()) {
				if (entry.getKey().page == page)
					return entry.getValue();
			}
			return null;
		}		
		
		/**
		 * Puts tree node in a cache. 
		 * @param node - must have a PageJob object as user object
		 */
		public
		void putTreeNodeInCache(DefaultMutableTreeNode node) {
			if (node == null) return;
			Object pj = node.getUserObject();
			if (!(node.getUserObject() instanceof PageJob))
				throw new IllegalArgumentException("User object is not a PageJob");
			jobToTreenode.put((PageJob)pj, node); 
			
		}
		
		public
		DefaultMutableTreeNode getTreeNodeByPageJob(PageJob pj) {
			if (pj == null) return null;
			return jobToTreenode.get(pj);
		}		
	}
	List<AbstractPage> wantedParents;
	JTree treeComponent;
	DefaultTreeModel treeModel;
	TreeNodeCacher nodeList;
	
	public GUI() {
		//FIXME: replace with some weak references to AbstractPages
		wantedParents = new LinkedList<>();
		nodeList = new TreeNodeCacher();

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Bandcamp downloader");
		
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		treeComponent = new JTree();
		treeComponent.setRootVisible(false);
		treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("JTree"));
		treeComponent.setModel(treeModel);
		scrollPane.setViewportView(treeComponent);
		treeComponent.setEditable(true);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));
		
		JButton btnStart = new JButton("Start!");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PageProcessorWorker worker = new PageProcessorWorker(false);
				worker.execute(); // next job GO
			}
		});
		btnStart.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(btnStart, BorderLayout.NORTH);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.NORTH);
		
		txtBaseurl = new JTextField();
		txtBaseurl.setText("baseURL");
		panel_1.add(txtBaseurl);
		txtBaseurl.setColumns(10);
	}

	@Override
	public void dispose() {
		// TODO: insert *interrupt workers and wait on them closing* code here.
//		SwingWorker
		frame = null;
		EventDispatchThread = null;
		super.dispose();
	}

	private static final long serialVersionUID = -919422625610867342L;
	public static Thread EventDispatchThread = null;
	private static GUI frame = null;
	private JTextField txtBaseurl;
	
	/**
	 * This is the starting method to create and show GUI
	 * @return true if success
	 */
	public static boolean showGUIWindow() {
		if (frame == null) {
			frame = new GUI(); // singleton
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			// strong assumption here: only one job in a list and it is a root page job
			frame.updateTreeByJob(PageProcessor.getNextJob(false)); 
			
			// XXX: following will never be in effect since "download 1st" rule;
			// -- need to implement "re-download update" feature beforehand 
//			if (PageProcessor.hasMoreJobs(true)) {
//				PageProcessorWorker worker = frame.new PageProcessorWorker(true);
//				worker.execute(); // next job GO
//			}
			
		}
		frame.setVisible(true);
		
		return true;
	}
	
	public void updateTreeByJob(PageJob pj) {
		if (pj == null) return; // must be here for PageProcessorWorker logic
		synchronized (pj) {
			DefaultMutableTreeNode node = nodeList.getTreeNodeByPageJob(pj);
			if (node == null) {
				// no such node in a tree: ADDING
				node = new DefaultMutableTreeNode(pj, true);
				nodeList.putTreeNodeInCache(node);
				
				// if THIS node is in wanted list, pick up its lost kids.
				if (wantedParents.remove(pj.page)) {
					for (AbstractPage kidPage: pj.page.childPages) {
						DefaultMutableTreeNode kidNode = nodeList.getTreeNodeByPage(kidPage);
						if (kidNode != null)
							// XXX: Ordering?
							treeModel.insertNodeInto(kidNode, node, node.getChildCount());
					}
				}
				//  check if there is a parent node in a tree;
				DefaultMutableTreeNode parentNode = nodeList.getTreeNodeByPage(pj.page.parent);
				if (parentNode != null) {
					// add new leaf element
					// XXX: Ordering?
					treeModel.insertNodeInto(node, parentNode, parentNode.getChildCount());
				} else {
					// parentNode == null AND 
					if (pj.page.parent!=null) {
						// this page's parent is not in a tree yet. 
						// add this node as hidden, add this node's parent page to wanted list
						wantedParents.add(pj.page.parent);
					} else {
						//add new top element
						parentNode = (DefaultMutableTreeNode) treeModel.getRoot();
						// XXX: Ordering?
						treeModel.insertNodeInto(node, parentNode, parentNode.getChildCount()); // to the end!
						// expand root element to show this one.
						treeComponent.expandPath(new TreePath(parentNode));
					}
				}
			} else {
				// update element visuals
				treeModel.nodeChanged(node);
			}
			
			if (pj.status == JobStatusEnum.PAGE_DONE) {
	//			 XXX: ??? fold parent element if every sibling is done too
			} else {
				// unfold parent element IF this node actually is in a tree
				if (node.getParent() != null) {
					TreePath tp = new TreePath(treeModel.getPathToRoot(node.getParent()));
					treeComponent.expandPath(tp);
				}
			}
		}
	}
	

}
