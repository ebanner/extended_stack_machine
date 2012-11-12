package wkr;

import gui.SM;

public class UpdateSPRunnable implements Runnable {

	private SM sm;
	private static int PC;

	public UpdateSPRunnable(SM sm) {
		this.sm = sm;
	}
	
	public void setPC(int PC) {
		this.PC = PC;
	}
	
	@Override
	public void run() {		
		// update SP
		sm.table.setValueAt(new Integer(sm.mem.getContents(0)), 0, 1);
		sm.stackTable.setValueAt(new Integer(sm.mem.getContents(0)), -0+16383, 1);

		// update values on the stack
		for (int i = 16383; i >= sm.mem.getSP(); i--) {
			sm.table.setValueAt(new Integer(sm.mem.getContents(i)), i, 1);
			sm.stackTable.setValueAt(new Integer(sm.mem.getContents(i)), -i+16383, 1);
		}
		
		// update the SP & PC label
		sm.PCLabel.setText("SP:"+sm.mem.getSP()+" | PC:"+PC+ " ");
		
		// repaint the table
		sm.table.repaint();
		sm.stackTable.repaint();
		sm.scrollPane.getVerticalScrollBar().setValue((int)((PC-16)*16));
		sm.stackScrollPane.getVerticalScrollBar().setValue((int)((sm.mem.getSP()-16383-8)*8));
	}

}
