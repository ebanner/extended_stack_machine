package wkr;

import gui.SM;

public class UpdateTableRunnable implements Runnable {
	
	private SM sm;
	private int val;

	public UpdateTableRunnable(SM sm) {
		this.sm = sm;
	}
	
	public void setVal(int val) {
		this.val = val;
	}

	/**
	 * Updates the field of each table associated with `addr'.
	 * 
	 * @param addr  the entry that needs to be updated
	 */
	@Override
	public void run() {
		sm.table.setValueAt(new Integer(sm.mem.getContents(this.val)), this.val, 1);
		sm.stackTable.setValueAt(new Integer(sm.mem.getContents(this.val)), -this.val+16383, 1);
	}
	
}
