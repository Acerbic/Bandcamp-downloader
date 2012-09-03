package dloader.gui;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import dloader.page.AbstractPage;
import dloader.pagejob.ProgressReporter;

/**
 * This class objects are to serve as "custom objects" to JTree model node elements.
 * 
 * It simultaneously wraps AbstractPage object, tracks download job progress by capturing PageJob's messages
 * and provides appropriate TreeCellRenderer and "toString()" methods to represent a page in JTree view. 
 * @author Acerbic
 *
 */
public class TreeCell_AbstactPageGUI implements ProgressReporter, TreeCellRenderer {
	
	public final AbstractPage page; //wrapped object
	
	//TODO: job progress flags and logs
	
	
	
	public TreeCell_AbstactPageGUI(AbstractPage page) {
		this.page = page;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void report(String type, long report) {
		// TODO catch report regarding this page
	}
	
	@Override
	public String toString() {
		// TODO fix representation
		return page.toString();
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		// TODO Return customized JLabel instead
		return new DefaultTreeCellRenderer();
	}

}
