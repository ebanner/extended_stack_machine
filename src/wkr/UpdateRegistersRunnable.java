/**
 * This class defines a Runnable objects that updates values the register values 
 * on the Event Dispatch Thread.
 */

package wkr;

import gui.SM;

public class UpdateRegistersRunnable implements Runnable {

	private SM sm;
	StringBuilder s;
	String sep;

	public UpdateRegistersRunnable(SM sm) {
		this.sm = sm;
		s = new StringBuilder("");
	}

	/**
	 * Updates the field of each table associated with `addr'.
	 * 
	 * @param addr  the entry that needs to be updated
	 */
	@Override
	public void run() {
		// get rid of the old register values
		s = new StringBuilder("``Registers:\"\n");
		
		for (int i = 0; i < 17; i++) {
			sep = (i < 10) ? "  " : "";
			s.append(sep + i + "-> " + sm.mem.getContents(i) + "\n");
		}
		sm.rightTextArea.setText(s.toString());
	}
	
}
