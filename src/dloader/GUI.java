package dloader;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JButton;
import javax.swing.JTree;
import java.awt.Font;
import javax.swing.border.BevelBorder;
import javax.swing.JCheckBox;

import dloader.page.AbstractPage;

public class GUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3871202126171183930L;
	private JTextField textFieldURL;
	private JTextField textFieldDirectory;
	private JTree tree;
	
	AbstractPage rootPage;
	private JLabel lblStatus;
	private JCheckBox chckbxUseCache;
	private JCheckBox chckbxLog;

	public GUI() throws HeadlessException {
		setTitle("Bandcamp dloader");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		
		JLabel lblNewLabel = new JLabel("Source URL:");
		
		textFieldURL = new JTextField();
		lblNewLabel.setLabelFor(textFieldURL);
		textFieldURL.setColumns(10);
		
		textFieldDirectory = new JTextField();
		textFieldDirectory.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Target directory:");
		lblNewLabel_1.setLabelFor(textFieldDirectory);
		
		JButton btnNewButton = new JButton("Prefetch");
		
		tree = new JTree();
		tree.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		
		lblStatus = new JLabel("kkkk");
		lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
		lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 18));
		
		JButton btnNewButton_1 = new JButton("Fetch");
		
		JButton btnNewButton_2 = new JButton("Fix");
		
		JButton btnNewButton_3 = new JButton("Download");
		
		JButton btnNewButton_4 = new JButton("Retag");
		
		chckbxUseCache = new JCheckBox("cache");
		
		chckbxLog = new JCheckBox("log");
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addContainerGap()
							.addComponent(tree, GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE))
						.addGroup(gl_panel.createSequentialGroup()
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
									.addComponent(btnNewButton)
									.addGap(7)
									.addComponent(btnNewButton_1, GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnNewButton_2)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnNewButton_3)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE))))
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(28)
							.addComponent(btnNewButton_4)
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
						.addComponent(btnNewButton_4)
						.addComponent(chckbxUseCache)
						.addComponent(chckbxLog))
					.addGap(44)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnNewButton)
						.addComponent(lblStatus)
						.addComponent(btnNewButton_1)
						.addComponent(btnNewButton_2)
						.addComponent(btnNewButton_3))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(tree, GroupLayout.DEFAULT_SIZE, 729, Short.MAX_VALUE)
					.addContainerGap())
		);
		panel.setLayout(gl_panel);
		// TODO Auto-generated constructor stub
	}

	public GUI(GraphicsConfiguration gc) {
		super(gc);
		// TODO Auto-generated constructor stub
	}

	public GUI(String title) throws HeadlessException {
		super(title);
		// TODO Auto-generated constructor stub
	}

	public GUI(String title, GraphicsConfiguration gc) {
		super(title, gc);
		// TODO Auto-generated constructor stub
	}
	
	public void init() {
		textFieldDirectory.setText(Main.saveTo);
		textFieldURL.setText(Main.baseURL);
		rootPage = AbstractPage.bakeAPage(null, Main.baseURL, Main.saveTo, null);
		lblStatus.setText("Preparing");
		chckbxLog.setSelected( Main.logger != null);
		chckbxUseCache.setSelected(Main.allowFromCache);
	}
}
