package dloader;

import javax.swing.JFrame;

public class GUI extends JFrame {

	private static final long serialVersionUID = -919422625610867342L;
	
	static GUI frame = null;
	static boolean showGUIWindow() {
		if (frame == null) {
			frame = new GUI();
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.init();
		}
		frame.setVisible(true);
		
		return true;
	}
	@Override
	public void dispose() {
		super.dispose();
		frame = null;
	}
	
	void init () {
		
	}
}
