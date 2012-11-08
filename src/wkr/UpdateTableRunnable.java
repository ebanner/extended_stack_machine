package wkr;

import static gui.SM.table;
import static gui.SM.stackTable;
import cli.Memory;

public class UpdateTableRunnable implements Runnable {
	
	private Memory mem;
	private int val;

	public UpdateTableRunnable(Memory mem) {
		this.mem = mem;
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
		table.setValueAt(new Integer(mem.getContents(val)), val, 1);
		stackTable.setValueAt(new Integer(mem.getContents(val)), -val+16383, 1);
	}
	
}
