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
import javax.swing.SwingUtilities;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JButton;
import javax.swing.JTree;
import java.awt.Font;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.swing.border.BevelBorder;
import javax.swing.JCheckBox;

import dloader.JobMaster.JobType;
import dloader.page.AbstractPage;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JScrollPane;

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
	private Thread eventDispatchThread;
	private JButton btnNewButton;

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
		lblNewLabel.setLabelFor(textFieldURL);
		textFieldURL.setColumns(10);
		
		textFieldDirectory = new JTextField();
		textFieldDirectory.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Target directory:");
		lblNewLabel_1.setLabelFor(textFieldDirectory);
		
		btnNewButton = new JButton("Prefetch");
		btnNewButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				MyWorker newWorker = new MyWorker(rootPage, JobType.READCACHEPAGES);
				newWorker.execute();
				btnNewButton.setEnabled(false);
			}
		});
		
		tree = new JTree();
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setEditable(true);
		tree.setModel(new DefaultTreeModel(
			new DefaultMutableTreeNode("JTree") {
				{
//					DefaultMutableTreeNode node_1;
//					node_1 = new DefaultMutableTreeNode("colors");
//						node_1.add(new DefaultMutableTreeNode("blue"));
//						node_1.add(new DefaultMutableTreeNode("violet"));
//						node_1.add(new DefaultMutableTreeNode("red"));
//						node_1.add(new DefaultMutableTreeNode("yellow"));
//					add(node_1);
//					node_1 = new DefaultMutableTreeNode("sports");
//						node_1.add(new DefaultMutableTreeNode("basketball"));
//						node_1.add(new DefaultMutableTreeNode("soccer"));
//						node_1.add(new DefaultMutableTreeNode("football"));
//						node_1.add(new DefaultMutableTreeNode("hockey"));
//					add(node_1);
//					node_1 = new DefaultMutableTreeNode("food");
//						node_1.add(new DefaultMutableTreeNode("pizza"));
//						node_1.add(new DefaultMutableTreeNode("ravioli"));
//					add(node_1);
				}
			}
		));
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
		
		JScrollPane scrollPane = new JScrollPane();
		
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
								.addGroup(gl_panel.createSequentialGroup()
									.addGroup(gl_panel.createParallelGroup(Alignment.LEADING, false)
										.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(lblNewLabel_1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
										.addComponent(textFieldDirectory, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
										.addComponent(textFieldURL, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)))
								.addGroup(gl_panel.createSequentialGroup()
									.addComponent(btnNewButton)
									.addGap(7)
									.addComponent(btnNewButton_1, GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnNewButton_2)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnNewButton_3)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE))))
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(28)
							.addComponent(btnNewButton_4)
							.addPreferredGap(ComponentPlacement.RELATED, 548, Short.MAX_VALUE)
							.addComponent(chckbxLog)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(chckbxUseCache))
						.addGroup(gl_panel.createSequentialGroup()
							.addContainerGap()
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE)))
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
	}
	
	public void updateTree (AbstractPage p, String message, long value) {
		Deque<AbstractPage> pathToPage = new LinkedList<AbstractPage>();
		while (p != null) {
			pathToPage.push(p);
			p = p.getParent();
		}
		
		TreeModel model = tree.getModel();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) model.getRoot();
		for (AbstractPage x: pathToPage) {
			DefaultMutableTreeNode child = null; 
			boolean found = false;
			for (@SuppressWarnings("unchecked")
			Enumeration<DefaultMutableTreeNode> children = parent.children(); children.hasMoreElements();) {
				child = children.nextElement();
				if (child.getUserObject().equals(x)) {
					found = true;
					break;
				}
			}
			if (! found) {
				// add new item under this parent
				child = new DefaultMutableTreeNode(x);
				parent.add(child);
				tree.expandPath(new TreePath(parent.getPath()));
			}
			parent = child;
		}
		tree.updateUI();
	}
}
