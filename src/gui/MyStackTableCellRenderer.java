/**
 * This class defines a table cell renderer.  I use this custom renderer to color 
 * cells different colors when they contain the current value of SP or PC.
 */

package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class MyStackTableCellRenderer extends DefaultTableCellRenderer {

	private SM gui;

	public MyStackTableCellRenderer(SM gui) {
		super();
		setOpaque(true);
		this.gui = gui;
	}

	/**
	 * This method is called every time the table is repainted and every time
	 * a value is updated in the table.  It defines rules for when to highlight an
	 * entry in a table.
	 * 
	 * @param table  the JTable that the renderer will be used with
	 * @param value  the value of the row,col of the table
	 * @param isSelected
	 * @param hasFocus
	 * @param row  the row of the table entry
	 * @param col  the col of the table entry
	 * 
	 * @return  a table cell renderer that can be used in instantiating a JTable
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int col) {

		setFont(new Font("Dialog", Font.PLAIN, 12));
		if (col == 0 && ((Integer)value).equals(gui.mem.getSP())) {
			// set the backgound if the contents of the box is equal to the SP
			setBackground(Color.YELLOW);
			setValue(value);
		} else {
			setBackground(Color.WHITE);
			setValue(value);
		}

		repaint();
		return this;
	}

}
