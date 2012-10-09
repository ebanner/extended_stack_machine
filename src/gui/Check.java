package gui;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import javax.swing.JLabel;

public class Check extends JFrame {
	public Check() {
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenu mnSpeed = new JMenu("Speed");
		mnFile.add(mnSpeed);
		
		JRadioButtonMenuItem rdbtnmntmFullSpeed = new JRadioButtonMenuItem("Full Speed");
		rdbtnmntmFullSpeed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		mnSpeed.add(rdbtnmntmFullSpeed);
		
		JRadioButtonMenuItem rdbtnmntmStepByStep = new JRadioButtonMenuItem("Step by step");
		mnSpeed.add(rdbtnmntmStepByStep);
		
		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);
		
		JLabel lblPc = new JLabel("PC:");
		toolBar.add(lblPc);
		
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new Check();
	}

}
