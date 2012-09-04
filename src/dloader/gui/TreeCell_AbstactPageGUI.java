package dloader.gui;

import dloader.page.AbstractPage;

/**
 * This class objects are to serve as "user objects" to JTree model node elements.
 * 
 * It simultaneously wraps AbstractPage object, tracks download job progress by capturing PageJob's messages
 * and provides "toString()" method to represent a page in JTree view. 
 * @author Acerbic
 *
 */
public class TreeCell_AbstactPageGUI {
	
	public final AbstractPage page; //wrapped object
	
	// TODO job progress flags and logs
	
	
	
	public TreeCell_AbstactPageGUI(AbstractPage page) {
		this.page = page;
		// TODO Auto-generated constructor stub
	}

	public void update(String type, long report) {
		// TODO catch report regarding this page
	}
	
	@Override
	public String toString() {
		// TODO fix representation
		return "<html><u>" +page.toString()+ "</u></html>";
	}

}
