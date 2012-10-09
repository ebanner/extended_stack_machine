package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class MyStackTableCellRenderer extends DefaultTableCellRenderer {

	private GUI gui;
	private boolean rightSideUp;

	public MyStackTableCellRenderer(GUI gui) {
		super();
		setOpaque(true);
		this.gui = gui;
		this.rightSideUp = rightSideUp;
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int col) {

		setFont(new Font("Dialog", Font.PLAIN, 12)); 
		if (col == 0 && (Integer)value == -gui.mem.getSP()+16383) {
			setBackground(Color.YELLOW);
			//setForeground(Color.BLACK);
			setValue(value);
		} else {
			setBackground(Color.WHITE);
			//setForeground(Color.BLACK);
			setValue(value);
		}

		repaint();
		return this;
	}

}
