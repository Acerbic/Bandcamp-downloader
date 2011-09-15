package dloader;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JScrollPane;

public class GUI extends JFrame {
	public final class PageProcessorWorker extends
			SwingWorker<Void, PageJob> {
		// worker thread
		@Override
		public Void doInBackground() {
			try {
				Main.sharedPageProcessor.hostWorker = this;
				Main.sharedPageProcessor.acquireData();
			} catch (Throwable e) {
				Main.logger.log(Level.SEVERE, "", e);
			}
			return null;
		}

		// GUI Event Dispatch thread
		@Override
		public void process(List<PageJob> pjList) {
			assert (pjList != null);
			// FIXME: should've create DefaultTreeModel explicitly in the 1st place
			DefaultTreeModel tm = (DefaultTreeModel)tree.getModel();

			while (!pjList.isEmpty()) {
				PageJob pj = pjList.remove(0);
				assert (pj != null);
				
				// remove all consequent reports of this job from current list (duplications)
				pjList.remove(pj);
				
				DefaultMutableTreeNode node = jobToTreenode.get(pj);
				if (node == null) {
					// no such node in a tree;
					//  check if there is a parent job in a tree;
					PageJob parentJob = PageProcessor.getJobForPage(pj.page.parent);
					DefaultMutableTreeNode parentNode = null; 
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(pj.page.getTitle(), true); 
					if (parentJob == null) {
						//add new top element
						parentNode = (DefaultMutableTreeNode) tm.getRoot();
						tm.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
						jobToTreenode.put(pj, newNode); // XXX: should be a weak reference;
						
						// expand root element to show this one.
						tree.expandPath(new TreePath(parentNode));
					} else {
						// add new leaf element
						
						// XXX: Strong assumption here - parent job is already in a tree (was at least recon'd)
						parentNode = jobToTreenode.get(parentJob);
						assert (parentNode != null);
						tm.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
						jobToTreenode.put(pj, newNode); // XXX: should be a weak reference;
					}
				} else {
					// update element visuals
				}
			}
		}

		// worker thread
		public void subPublish(PageJob pj) {
			publish(pj);
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
		tree.setShowsRootHandles(true);
		scrollPane.setViewportView(tree);
		tree.setEditable(true);
	}

	private static final long serialVersionUID = -919422625610867342L;
	public static Thread EventDispatchThread = null;
	static GUI frame = null;
	static boolean showGUIWindow() {
		if (frame == null) {
			frame = new GUI();
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			PageProcessorWorker worker = frame.new PageProcessorWorker();
			worker.execute();
		}
		frame.setVisible(true);
		
		return true;
	}
	@Override
	public void dispose() {
		// TODO: insert *interrupt workers and wait on them closing* code here.
		frame = null;
		EventDispatchThread = null;
		super.dispose();
	}
	
}
