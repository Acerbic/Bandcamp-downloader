package dloader;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.util.Hashtable;
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
	
	Map<PageJob, DefaultMutableTreeNode> jobToTreenode; 
	JTree tree;
	
	public GUI() {
		//FIXME: replace with some weak references to PageJobs and deal with depleted nodes.
		jobToTreenode = new Hashtable<>();

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Bandcamp downloader");
		
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		tree = new JTree();
		tree.setRootVisible(false);
		tree.setModel(new DefaultTreeModel(
			new DefaultMutableTreeNode("JTree") 
		));
		scrollPane.setViewportView(tree);
		tree.setEditable(true);
		
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
			frame = new GUI();
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
		synchronized (pj) {
			if (pj == null) return; // must be here for PageProcessorWorker logic
			// FIXME: should've create DefaultTreeModel explicitly in the 1st place
			DefaultTreeModel tm = (DefaultTreeModel)tree.getModel();
			
			DefaultMutableTreeNode node = jobToTreenode.get(pj);
			if (node == null) {
				// no such node in a tree: ADDING
				//  check if there is a parent job in a tree;
				
				//// NOT using PageProcessor.getJobForPage because jobs are detached from that when in progress
	//			PageJob parentJob = PageProcessor.getJobForPage(pj.page.getParent());
				
				DefaultMutableTreeNode parentNode = getTreeNodeByPage(pj.page.getParent());
				
				// Strong assumption here - if page have parent, parent-job is already in a tree (was at least recon'd)
				assert ((pj.page.getParent()==null && parentNode==null)  ||
						(pj.page.getParent()!=null && parentNode!=null));
				// FIXME: code in chance child elements are added to tree before parent elements 
				// due to weird event Q shenanigans
				
				
				node = new DefaultMutableTreeNode(pj, true); 
				if (parentNode == null) {
					//add new top element
					
					parentNode = (DefaultMutableTreeNode) tm.getRoot();
					tm.insertNodeInto(node, parentNode, parentNode.getChildCount()); // to the end!
					jobToTreenode.put(pj, node); // XXX: should be a weak reference;
					
					// expand root element to show this one.
					tree.expandPath(new TreePath(parentNode));
				} else {
					// add new leaf element
					
	//				assert (parentNode != null); //duplication of above strong assumption
					tm.insertNodeInto(node, parentNode, parentNode.getChildCount());
					jobToTreenode.put(pj, node); // XXX: should be a weak reference;
				}
			} else {
				// update element visuals
	//			node.setUserObject(pj.page.getTitle() + ": " + pj.status.toString());
				tm.nodeChanged(node);
			}
			
			if (pj.status == JobStatusEnum.PAGE_DONE) {
	//			 TODO: fold parent element if every sibling is done too
			} else {
				// unfold parent element
				TreePath tp = new TreePath(tm.getPathToRoot(node.getParent()));
				tree.expandPath(tp);
			}
		}
	}
	
	private
	DefaultMutableTreeNode getTreeNodeByPage(AbstractPage page) {
		for (Map.Entry<PageJob, DefaultMutableTreeNode> entry: jobToTreenode.entrySet()) {
			if (entry.getKey().page == page)
				return entry.getValue();
		}
		return null;
	}
}
