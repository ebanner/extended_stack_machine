package gui;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


public class GUI extends JFrame {
	private JTable table;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	public GUI() {
		super();
		
		setSize(480, 360);
		setTitle("Stack Machine eXtended");
		
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		/*
		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);
		
		JButton btnLoad = new JButton("Load");
		toolBar.add(btnLoad);
		
		JButton btnNewButton = new JButton("Re-Init");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		toolBar.add(btnNewButton);
		
		JButton btnRun = new JButton("Run");
		toolBar.add(btnRun);
		
		JButton btnPause = new JButton("Pause");
		toolBar.add(btnPause);
		
		JButton btnSingleStep = new JButton("Single Step");
		toolBar.add(btnSingleStep);
		*/
		
		String columnNames[] = { "Address", "Contents" };
		Object[][] data = new Object[16000][2];
		for (int row = 0; row < 16000; row++) {
			data[row][0] = "" + (16000-row-1);
			data[row][1] = "" + 5;
		}
		
		table = new JTable(data, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(150, 70));
		table.setFillsViewportHeight(true);
		//Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
 
        //Add the scroll pane to this panel.
        add(scrollPane, BorderLayout.WEST);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.EAST);
		panel.setPreferredSize(new Dimension(300,300));
		panel.setLayout(new GridLayout(2, 1));
		
		JPanel northPanel = new JPanel();
		northPanel.setLayout(null);
		panel.add(northPanel);
		
		JPanel southPanel = new JPanel();
		southPanel.setLayout(null);
		panel.add(southPanel);
		
		textField_2 = new JTextField();
		textField_2.setBounds(12, 12, 276, 123);
		southPanel.add(textField_2);
		textField_2.setColumns(10);
		
		JLabel lblSp = new JLabel("SP");
		lblSp.setBounds(26, 120, 18, 15);
		northPanel.add(lblSp);

		textField = new JTextField();
		textField.setBounds(62, 118, 72, 19);
		northPanel.add(textField);
		textField.setColumns(10);
		
		JLabel lblPc = new JLabel("PC");
		lblPc.setBounds(152, 120, 18, 15);
		northPanel.add(lblPc);
		
		textField_1 = new JTextField();
		textField_1.setBounds(188, 118, 79, 19);
		northPanel.add(textField_1);
		textField_1.setColumns(10);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenu mnSimulator = new JMenu("Simulator");
		menuBar.add(mnSimulator);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new GUI();
	}
}