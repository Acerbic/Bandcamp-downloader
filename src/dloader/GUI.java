package dloader;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.util.logging.Level;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

public class GUI extends JFrame {
	public static final class PageProcessorWorker extends
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
		public void done() {
		}

		public void subPublish(PageJob pj) {
			publish(pj);
		}
	}
	
	
	@SuppressWarnings("serial")
	public GUI() {
		
		JTree tree = new JTree();
		tree.setModel(new DefaultTreeModel(
			new DefaultMutableTreeNode("JTree") {
				{
					add(new DefaultMutableTreeNode("child one"));
				}
			}
		));
		getContentPane().add(tree, BorderLayout.CENTER);
	}

	private static final long serialVersionUID = -919422625610867342L;
	public static Thread EventDispatchThread = null;
	static GUI frame = null;
	static boolean showGUIWindow() {
		if (frame == null) {
			frame = new GUI();
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			PageProcessorWorker worker = new PageProcessorWorker();
			worker.execute();
		}
		frame.setVisible(true);
		
		return true;
	}
	@Override
	public void dispose() {
		super.dispose();
		frame = null;
		EventDispatchThread = null;
	}
	
}
