/**
 * @author Edward M Banner
 * 
 * This SM class defines *the* Stack Machine.  The Stack Machine is an architecture
 * implemented in software.  A stack lives at high memory and grows down, while
 * data and opcodes live at low memory and grow up.  In all, the Stack Machine has
 * 16K memory.
 */

package gui;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JToolBar;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.JTextPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import wkr.ExecuteInstructionWorker;

import cli.Header;
import cli.Memory;
import cli.Read;

/**
 * Creating a new instance of SM initializes the GUI.
 * 
 * @author edward
 *
 */
public class SM extends JFrame implements ActionListener, ChangeListener {
	
	public static SM gui;

	public Memory mem;
	public boolean TRACE;  // TRACE mode not implemented
	public Scanner in;  // for READ and REAC opcodes
	public boolean oldStyle;  // support legacy opcode numbers
	public int DEBUG = 0;
	public int PC = 0;
	public Scanner sc;  // used to read the SXX file
	public boolean keepExecuting;
	public boolean singleStep;
	private ExecuteInstructionWorker eiw;
	public int speed;
	private boolean firstWorker = true;
	private boolean firstExecution = true;

	// swing stuff
	public  JTable table;
	private JTextArea outputTextArea;
	public JTextArea rightTextArea;
	public  JScrollPane scrollPane;
	private JMenuBar menuBar;
	private JMenuItem loadFileMenuItem;
	private JMenu fileMenu;
	private JMenu simulatorMenu;
	private JMenu helpMenu;
	private JPanel southPanel;
	private JPanel northPanel;
	private JScrollPane sPane;
	private MessageConsole mc;
	public  JTable stackTable;
	public  JScrollPane stackScrollPane;
	private JScrollPane instrScrollPane;
	private JSlider slider;
	private String file;
	private JMenu speedMenu;
	private JRadioButtonMenuItem fullSpeedRadioButton;
	private JRadioButtonMenuItem stepRadioButton;
	private JButton runButton;
	public  JLabel PCLabel;
	private JButton clearButton;
	private JButton singleStepButton;
	private JButton pauseButton;
	private JMenuItem exitMenuItem;
	private JMenuItem aboutMenuItem;

	
	/**
	 * Initialize the Stack Machine.
	 */
	public SM() {
		mem = new Memory(16384); // SM has 16384 words of memory
		file = "";  // SXX file the user selects
		speed = 500;
	}

	/**
	 * Sticks all of the GUI pieces together and redirects STDOUT.
	 */
	public void sitAndWait() {	

		// GUI stuff
		setSize(480, 620);
		setTitle("Stack Machine eXtended");
		getContentPane().setLayout(new BorderLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		// new menu bar to hold the ``File", ``Simulation", and ``Help" menus
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		// ``file" encompasses selecting an SXX file and closing the SM
		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		// allows user to load an SXX file
		loadFileMenuItem = new JMenuItem("Load File");
		loadFileMenuItem.setName("Load File");
		fileMenu.add(loadFileMenuItem);
		loadFileMenuItem.addActionListener(this);
		
		// allows the user to exit the SM
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setName("Exit");
		fileMenu.add(exitMenuItem);
		exitMenuItem.addActionListener(this);

		// ``simulator" deals with selecting the execution method
		simulatorMenu = new JMenu("Simulator");
		menuBar.add(simulatorMenu);
		
		// how fast you want your SXX program to execute
		speedMenu = new JMenu("Speed");
		simulatorMenu.add(speedMenu);
		
		// full speed
		fullSpeedRadioButton = new JRadioButtonMenuItem("Full Speed");
		fullSpeedRadioButton.setName("Full Speed");
		speedMenu.add(fullSpeedRadioButton);
		fullSpeedRadioButton.addActionListener(this);
		
		// step-by-step
		stepRadioButton = new JRadioButtonMenuItem("Step-by-Step");
		stepRadioButton.setName("Step-by-Step");
		speedMenu.add(stepRadioButton);
		fullSpeedRadioButton.addActionListener(this);

		// ``help" deals with a little bit of documentation
		helpMenu = new JMenu("Help");
		menuBar.add(helpMenu);
		
		// a little snippet about the SM
		aboutMenuItem = new JMenuItem("About");
		aboutMenuItem.setName("About");
		helpMenu.add(aboutMenuItem);
		aboutMenuItem.addActionListener(this);
		
		// right align everything inserted into the menu bar after this line
		menuBar.add(Box.createHorizontalGlue());
		
		// set the PC and SP labels in the upper right hand corner of the SM
		PCLabel = new JLabel();
		PCLabel.setText("PC: - | SP: - ");
		menuBar.add(PCLabel);
		
		// nice removable toolbar
		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);
		
		// run the SXX program on the SM
		runButton = new JButton("Run");
		runButton.setName("Run");
		runButton.addActionListener(this);
		toolBar.add(runButton);	
		
		// button to reset both tables
		clearButton = new JButton("Clear");
		clearButton.setName("Clear");
		clearButton.addActionListener(this);
		toolBar.add(clearButton);
		
		pauseButton = new JButton("Pause");
		pauseButton.setName("Pause");
		pauseButton.addActionListener(this);
		toolBar.add(pauseButton);
		
		// button to single step through an SXX program
		singleStepButton = new JButton("Step");
		singleStepButton.setName("Single Step");
		toolBar.add(singleStepButton);
		singleStepButton.addActionListener(this);
		
		// add a slider so the user can determine how fast SXX executes
		slider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 500);
		slider.setName("Slider");
		slider.addChangeListener(this);
		toolBar.add(slider);

		// populate the PC table
		String columnNames[] = { "Address", "Contents" };
		Object[][] data = new Object[16384][2];
		for (int row = 0; row < 16384; row++) {
			data[row][0] = row;
			data[row][1] = new Integer(0);
		}

		// create the PC table
		DefaultTableModel myModel = new DefaultTableModel(data, columnNames);
		table = new JTable(myModel);
		// set the renderer to the custom rendered defined by the `MyMemoryTableCellRender' class
		table.setDefaultRenderer(Object.class, new MyMemoryTableCellRenderer(this));
		table.setPreferredScrollableViewportSize(new Dimension(160, 70));
		table.setFillsViewportHeight(true);
		//Create the scroll pane and add the table to it.
		scrollPane = new JScrollPane(table);

		//Add the scroll pane to this panel.
		add(scrollPane, BorderLayout.WEST);

		// panel for the stuff on the right side
		JPanel eastPanel = new JPanel();
		getContentPane().add(eastPanel, BorderLayout.EAST);
		eastPanel.setPreferredSize(new Dimension(300,300));
		eastPanel.setLayout(new GridLayout(2, 1));

		// northeast panel
		northPanel = new JPanel();
		northPanel.setLayout(new BorderLayout());
		eastPanel.add(northPanel);
		
		// populate the stack table
		String cNames[] = { "Address", "Contents" };
		Object[][] d = new Object[16384][2];
		for (int row = 0; row < 16384; row++) {
			d[row][0] = new Integer(16384-row-1);
			d[row][1] = new Integer(0);
		}
		
		// create the stack table
		DefaultTableModel myStackModel = new DefaultTableModel(d, cNames);
		stackTable = new JTable(myStackModel);
		// set the `MyStackTableCellRenderer' we defined as the custom renderer for this table
		stackTable.setDefaultRenderer(Object.class, new MyStackTableCellRenderer(this));
		stackTable.setPreferredScrollableViewportSize(new Dimension(150, 70));
		stackTable.setFillsViewportHeight(true);
		// Create the scroll pane and add the table to it.
		stackScrollPane = new JScrollPane(stackTable);
		// Add the scroll pane to this panel.
		northPanel.add(stackScrollPane, BorderLayout.WEST);
		
		// create the area to write the SAX instructions to
		rightTextArea = new JTextArea();
		instrScrollPane = new JScrollPane(rightTextArea);
		northPanel.add(instrScrollPane, BorderLayout.CENTER);

		// southeast panel
		southPanel = new JPanel();
		southPanel.setLayout(new BorderLayout());
		eastPanel.add(southPanel);

		outputTextArea = new JTextArea();
		sPane = new JScrollPane(outputTextArea);

		// Add the scroll pane to this panel.
		southPanel.add(sPane, BorderLayout.CENTER);
		
		// redirect STDOUT to the `outputTextArea' area
		mc = new MessageConsole(outputTextArea);
		mc.redirectOut();
		
		// let's make it so we can see the SM!
		setVisible(true);
	}

	/**
	 * Initializes the data and opcode segment of the SM.
	 * 
	 * @param file  the SXX input file
	 */
	public void run(String file) {
		// start the PC at a random spot between 16 and 999
	    //int baseAddr = new Random().nextInt(984) + 16;
		int baseAddr = 16;
		if (DEBUG == 1) {  System.out.println("Base address: " + baseAddr); }

		// insert opcodes and data into the stack machine and perform
		// relocation process
		int entryPoint = initializeMemory(baseAddr, file);
		if (DEBUG == 1) { System.out.println("Entry point: " + entryPoint); }
		
		PC = entryPoint;
		// update the PC and SP label
		PCLabel.setText("SP:"+mem.getSP()+" | PC:"+PC + " ");
		
		if (firstWorker) {  // only create the thread the first time through
			// create a new worker to do all of the heavy lifting
			eiw = new ExecuteInstructionWorker(this);
			eiw.execute();
			
			// never create this swing worker again
			firstWorker = false;
		} else  // don't create a new swing worker.  just reset its fields
			eiw.reset(this);
	}

	/**
	 * Parses the header of the SXX executable and loads the opcodes and data
	 * into memory.
	 *
	 * @param baseAddr address where the first opcode is inserted
	 * @param file     SXX executable
	 *
	 * @return returns  the entry point into the SXX program
	 */
	public int initializeMemory(int baseAddr, String file) {

		int nextFreeAddr = baseAddr;  // keep the base address around
		sc = null;
		try {  // open up a new scanner on the source file
			sc = new Scanner(new BufferedReader(new FileReader(file)));
		} catch (Exception e) {
			errorAndExit("ERROR: " + e.getMessage());
		}

		/* Determine if the SXX program has a syntactically correct header, 
		 * determine the length of the program, and will advance `sc' will point
		 * to the opcode/data section. */
		Header header = parseHeaderSection(baseAddr);

		/* Go through all of the opcodes in the SXX program and insert them
		 * into low memory.  When we return from this method, `sc' will point
		 * to the relocation section */
		processOpcodeSection(header, nextFreeAddr);
		
		/* Parse the relocatable section and perform the relocation process. */
		processRelocatableSection(baseAddr);

		if (sc != null)  // close the scanner
			sc.close();
		
		return baseAddr + header.entry;  // actual entry point into memory
	}

	/**
	 * Parse the relocateable section and perform the relocation process.
	 * 
	 * @param baseAddr  the memory location in which to start loading opcodes
	 */
	private void processRelocatableSection(int baseAddr) {
		String relocation = null;
		
		while (sc.hasNextLine()) {
			// get the next opcode
			relocation = sc.nextLine().trim();

			if (isCommentOrBlankLine(relocation)) {
				continue;
			} else if (relocation.charAt(0) == '%') {
				break;  // we're done reading the SXX program
			} else {
				insertRelocation(baseAddr, relocation);
			}
		}
	}

	/**
	 * Parses the SXX program for opcodes and inserts them starting at low
	 * memory.
	 * 
	 * @param header  the header of the SXX program
	 * @param nextFreeAddr  the base address of the SXX program
	 */
	private void processOpcodeSection(Header header, int nextFreeAddr) {
		String opcode = null;
		
		// keep track of how many words have been inserted into memory
		int words = 0; 
		int currFreeAddr;
		// insert opcodes until you reach the relocation section
		while (sc.hasNextLine() && words <= header.length) {
			// read the next opcode and trim off whitespace
			opcode = sc.nextLine().trim();

			if (isCommentOrBlankLine(opcode)) {
				continue;
			} else if (opcode.charAt(0) == '%') {
				break;  // move onto the relocation process
			} else {
				// keep current address around so we can tell after how many
				// memory cells we consumed
				currFreeAddr = nextFreeAddr;
				// insert the opcode/data into the stack machine
				// or skip over memory cells if the opcode begins with `:'
				nextFreeAddr = insertOpcode(nextFreeAddr, opcode);
				// update the number of words that have been inserted into
				// memory
				words += nextFreeAddr - currFreeAddr;
			}
		} 
	}

	/**
	 * Parses the header of a SXX program.  A result of this parse will be that
	 * we will know if the SXX program is `oldstyle' or not.  We will also
	 * determine the length of the program.
	 * 
	 * @param sc  a scanner that points to the first line of the SXX program
	 * 
	 * @return  a scanner that points to the next location after the header
	 */
	private Header parseHeaderSection(int baseAddr) {
		
		/* A Header object's purpose is to parse the header of the SXX 
		 * program and hold useful information. */
		Header header = new Header(sc); 
		// parse the header and give us back the scanner where the header ends
		sc = header.parseHeader();
		// the header will tell us if we're using old style opcode numbering
		oldStyle = header.oldStyle;

		if (0 > header.length || // make sure the length is in range
				header.length > mem.memorySize-baseAddr) {
			System.err.println("Illegal length: Out of range");
			System.err.println("  " + header.length);
			System.exit(1);
		}
		if (DEBUG == 1) { System.out.println("Length: " + header.length); }

		// return the scanner so we can parse the rest of the SXX program
		return header;
	}

	public int insertOpcode(int nextFreeAddr, String instr) {
		// inserts the opcode of the next instruction or skips over a number of
		// memory cells if a line staring with `:' is encountered

		String number = "^-?(\\d)+$";
		String colonInstruction = "^:(\\d)+$";

		if (Pattern.matches(number, instr)) {
			// we now know that we have a digit
			if (DEBUG == 1) { System.out.println("mem["+nextFreeAddr+"]="+instr); }
			mem.putContents(nextFreeAddr, Integer.parseInt(instr));
			table.setValueAt(new Integer(mem.getContents(nextFreeAddr)), nextFreeAddr, 1);
			stackTable.setValueAt(new Integer(mem.getContents(nextFreeAddr)), -nextFreeAddr+16383, 1);
			nextFreeAddr++;
		} else if (Pattern.matches(colonInstruction, instr)) {
			if (DEBUG == 1) { System.out.println("BSS "+instr.split(":")[1]); }
			nextFreeAddr += Integer.parseInt(instr.split(":")[1]);
		} else {
			System.err.println("ERROR: Not a valid line:");
			System.err.println("  " + instr);
			System.exit(1);
		}

		return nextFreeAddr;
	}

	public void insertRelocation(int baseAddr, String address) {
		// insert into the current memory address the following:
		//     *(baseAddr+addresss) + baseAddr

		String number = "^-?(\\d)+$";
		if (! Pattern.matches(number, address)) {
			System.err.println("ERROR: Not a relocation address:");
			System.err.println("  " + address);
			System.exit(1);
		} else {
			int addr = Integer.parseInt(address) + baseAddr;
			if (DEBUG == 1) { System.out.println("relocating memory at " + addr); }
			mem.putContents(addr, mem.getContents(addr)+baseAddr);
			table.setValueAt(new Integer(mem.getContents(addr)), addr, 1);
			stackTable.setValueAt(new Integer(mem.getContents(addr)), -addr+16383, 1);
		}
	}

	private boolean isCommentOrBlankLine(String line) {
		if (line.isEmpty() || line.charAt(0) == '#')
			return true;
		else
			return false;
	}

	private void errorAndExit(String error) {
		System.err.println(error);
		System.exit(1);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem item = (JMenuItem)e.getSource();
			String name = item.getName();
			if (name.equals("Load File")) {
				// fire up a JFileChooser and let the user pick a SXX file
				JFileChooser fc = new JFileChooser();
				// spawn the JFileChooser
				int returnVal = fc.showOpenDialog(null);
				
				// make sure it was a valid file
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					// do a massive reset of everything
					clearALLTheThings();
					file = fc.getSelectedFile().getAbsolutePath();
					// set up the SM
					run(file);
				}
			} else if (name.equals("Exit")) {
				// we're done
				System.exit(0);
			} else if (name.equals("About")) {
				// create a runnable so we can spawn a dialog box representing the
				// rules in HTML
				Runnable r = new Runnable() {
					public void run() {
						String pt1 = "<html><body width='";
						String pt2 =
							"'><center><h1>Stack Machine eXtended</h1></center>" +
						"A sick and twisted creation dreamt up by one Dr. Timothy V. Fossum.";

						int width = 250;
						String s = pt1 + width + pt2;
						
						// pop up with the rules
						JOptionPane.showMessageDialog(null, s);
					}
				};
				SwingUtilities.invokeLater(r);
			}
		} else if (e.getSource() instanceof JButton) {
			JButton button = (JButton)e.getSource();
			String name = button.getName();
			
			if (name.equals("Single Step") && ! file.equals("")) {
				// just run one instruction
				singleStep = true;
			} else if (name.equals("Run") && ! file.equals("")) {
				keepExecuting = true;
			} else if (name.equals("Clear")) {
				clearALLTheThings();
			} else if (name.equals("Pause")) {
				/* Decide whether to flip to ``Pause" button to ``Resume" or
				 * vice-versa. */
				if (button.getText().equals("Pause") && keepExecuting) {
					// button says ``Pause" and we are executing
					button.setText("Resume");
					/* If we're executing, stop executing.
					 * If we're not executing, continue executing. */
					keepExecuting = ! keepExecuting;
				} else if (button.getText().equals("Resume") 
						&& ! keepExecuting) {
					// button says ``Resume" and we are not executing
					button.setText("Pause");
					/* If we're executing, stop executing.
					 * If we're not executing, continue executing. */
					keepExecuting = ! keepExecuting;
				}
			}
		}
	}
	
	/**
	 * Massive reset for most of the variables that change along with an
	 * executing SXX program.
	 */
	public void clearALLTheThings() {
		for (int row = 0; row < 16384; row++) {
			// reset the tables
			table.setValueAt(new Integer(0), row, 1);
			stackTable.setValueAt(new Integer(0), row, 1);
		}
		// clear out the SM's memory
		mem = new Memory(16384);
		// clear out the SAX instructions
		rightTextArea.setText("");
		// clear out STDOUT
		outputTextArea.setText("");
		file = "";
		PC = 0;
		PCLabel.setText("PC: - | SP: - ");
		// redirect standard output again
		mc.redirectOut();
		// scroll the table back up to the top
		scrollPane.getVerticalScrollBar().setValue(0);
		stackScrollPane.getVerticalScrollBar().setValue(0);
		// repaint the tables
		table.repaint();
		stackTable.repaint();
		// stop execution
		keepExecuting = false;
	}
	
	/**
	 * Create a new SM and go.
	 * 
	 */
	public static void main(String[] args) {
		new SM().sitAndWait();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof JSlider) {
			JSlider s = (JSlider)e.getSource();
			// change the speed of execution
			speed = -s.getValue() + 1000;
		}
	}

}