package dloader;

import java.awt.*;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Font;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.swing.border.BevelBorder;

import dloader.JobMaster.JobType;
import dloader.gui.MyWorker;
import dloader.gui.TreeCell_AbstactPageGUI;
import dloader.page.AbstractPage;

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
	
	AbstractPage rootPage;
	private JLabel lblStatus;
	private JCheckBox chckbxUseCache;
	private JCheckBox chckbxLog;
	private Thread eventDispatchThread;
	private JButton btnNewButton;
	
	
	@SuppressWarnings("serial")
	class MyRenderer extends DefaultTreeCellRenderer {

	    public Component getTreeCellRendererComponent(
	                        JTree tree,
	                        Object value,
	                        boolean sel,
	                        boolean expanded,
	                        boolean leaf,
	                        int row,
	                        boolean hasFocus) {

	        super.getTreeCellRendererComponent(
	                        tree, value, sel,
	                        expanded, leaf, row,
	                        hasFocus);
	        if (value instanceof DefaultMutableTreeNode)
	        	return this;
	        return this;
	    }

	}	

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
		textFieldURL.setEditable(false);
		lblNewLabel.setLabelFor(textFieldURL);
		textFieldURL.setColumns(10);
		
		textFieldDirectory = new JTextField();
		textFieldDirectory.setEditable(false);
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
				{}
			}
		));
		tree.setCellRenderer(new MyRenderer());		
		tree.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		
		lblStatus = new JLabel("kkkkss");
		lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
		lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 18));
		
		JButton btnNewButton_1 = new JButton("Fetch");
		
		JButton btnNewButton_2 = new JButton("Fix");
		
		JButton btnNewButton_3 = new JButton("Download");
		
		JButton btnNewButton_4 = new JButton("Retag");
		
		chckbxUseCache = new JCheckBox("cache");
		chckbxUseCache.setEnabled(false);
		
		chckbxLog = new JCheckBox("log");
		chckbxLog.setEnabled(false);
		
		JScrollPane scrollPane = new JScrollPane();
		
		JLabel lblNewLabel_2 = new JLabel("sdf");
		lblNewLabel_2.setIcon(new ImageIcon("D:\\Pics & Photos\\\u0421\u0435\u043C\u044C\u044F\\\u0420\u0430\u0437\u043D\u043E\u0435\\Photo-0041.jpg"));
		
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addContainerGap()
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE))
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
							.addComponent(chckbxUseCache))
						.addGroup(gl_panel.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblNewLabel_2, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)))
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
					.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 535, GroupLayout.PREFERRED_SIZE)
					.addGap(18)
					.addComponent(lblNewLabel_2, GroupLayout.PREFERRED_SIZE, 179, Short.MAX_VALUE)
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
		
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) model.getRoot();
		for (AbstractPage x: pathToPage) {
			DefaultMutableTreeNode child = null; 
			boolean found = false;
			for (@SuppressWarnings("unchecked")
			Enumeration<DefaultMutableTreeNode> children = parent.children(); children.hasMoreElements();) {
				child = children.nextElement();
				if (((TreeCell_AbstactPageGUI)child.getUserObject()).page.equals(x)) {
					found = true;
					break;
				}
			}
			if (! found) {
				//TODO: fix elements order in tree.... 
				// add new item under this parent
				child = new DefaultMutableTreeNode(new TreeCell_AbstactPageGUI(x));
				parent.add(child);
				int[] indices = new int[1];
				indices[0] = parent.getIndex(child);
				model.nodesWereInserted(parent, indices);
				tree.expandPath(new TreePath(parent.getPath()));
			}
			parent = child;
		}
	}
}
