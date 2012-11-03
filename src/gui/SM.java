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
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import cli.Header;
import cli.Memory;
import cli.Read;

/**
 * Creating a new instance of SM initializes the GUI.
 * 
 * @author edward
 *
 */
public class SM extends JFrame implements ActionListener {
	
	public static SM gui;

	public Memory mem;
	public boolean TRACE;  // TRACE mode not implemented
	public Scanner in;  // for READ and REAC opcodes
	public boolean oldStyle;  // support legacy opcode numbers
	public int DEBUG = 0;
	public int PC = 0;
	public long time;

	// swing stuff
	private static JTable table;
	private static JTextArea outputTextArea;
	private static JTextArea rightTextArea;
	private static JScrollPane scrollPane;
	private static JMenuBar menuBar;
	private static JMenuItem loadFileMenuItem;
	private static JMenu fileMenu;
	private static JMenu simulatorMenu;
	private static JMenu helpMenu;
	private static JPanel southPanel;
	private static JPanel northPanel;
	private static JScrollPane sPane;
	private static MessageConsole mc;
	private static String inputLine;
	private static Pattern digit;
	private static Matcher m;
	private static JTable stackTable;
	private JScrollPane stackScrollPane;
	private JScrollPane instrScrollPane;
	private StringBuilder instructions;
	private String file;
	private JMenu speedMenu;
	private JRadioButtonMenuItem fullSpeedRadioButton;
	private JRadioButtonMenuItem stepRadioButton;
	private JButton runButton;
	private JLabel PCLabel;
	private JButton clearButton;
	private JButton singleStepButton;
	private JMenuItem exitMenuItem;
	private JMenuItem aboutMenuItem;

	
	/**
	 * Initialize the Stack Machine.
	 */
	public SM() {
		mem = new Memory(16384); // SM has 16384 words of memory
		inputLine = "";  // string that ``acts" as STDIN
		instructions = new StringBuilder("");  // debugging instructions
		digit = Pattern.compile("^[-+]?\\d+");
		file = "";  // SXX file the user selects
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
		
		// button to single step through an SXX program
		singleStepButton = new JButton("Single Step");
		singleStepButton.setName("Single Step");
		toolBar.add(singleStepButton);
		singleStepButton.addActionListener(this);	
		
		// button to reset both tables
		clearButton = new JButton("Clear");
		clearButton.setName("Clear");
		clearButton.addActionListener(this);
		toolBar.add(clearButton);

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

		//Add the scroll pane to this panel.
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
		int baseAddr = new Random().nextInt(984) + 16;
		if (DEBUG == 1) {  System.out.println("Base address: " + baseAddr); }

		// insert opcodes and data into the stack machine and perform
		// relocation process
		int entryPoint = initializeMemory(baseAddr, file);
		if (DEBUG == 1) { System.out.println("Entry point: " + entryPoint); }
		
		PC = entryPoint;
		// update the PC and SP label
		PCLabel.setText("SP:"+mem.getSP()+" | PC:"+PC + " ");
	}
	
	/**
	 * Run the instruction currently being pointed to by PC.  Useful for `step-by-step'
	 * mode.
	 */
	public void runASingleInstruction() {
			try {
				// run an instruction
				PC = executeOpcode(PC);
				// update the SP & PC label
				PCLabel.setText("SP:"+mem.getSP()+" | PC:"+PC + " ");
				// update the tables and scroll them by the magic value
				table.repaint();
				stackTable.repaint();
				scrollPane.getVerticalScrollBar().setValue((int)((PC-16)/0.0625));
			} catch (Exception e) { }

			// print the SAX code for this instruction
			rightTextArea.setText(instructions.toString());
	}

	/**
	 * Runs instructions loaded into the SM until a HALT opcode is reached or
	 * and error occurs.
	 */
	public void runALLTheInstructions() {
		while (true) {
			try {
				// execute a single opcode
				PC = executeOpcode(PC);
				// repaint the tables and scroll them by the magic number
				table.repaint();
				stackTable.repaint();
				scrollPane.getVerticalScrollBar().setValue((int)((PC-16)/0.0625));
				PCLabel.setText("SP:"+mem.getSP()+" | PC:"+PC + " ");
			} catch (Exception e) {
				break;
			}
			/*
			try {
				Thread.sleep(10);
			} catch(Exception e) {
			}
			*/
		}
		// print the SAX instruction
		rightTextArea.setText(instructions.toString());
	}

	/**
	 * Parses the header of the SXX executable and loads the opcodes and data
	 * into memory.
	 *
	 * @param baseAddr address where the first opcode is inserted
	 * @param file     SXX executable
	 *
	 * @return returns the entry point
	 */
	public int initializeMemory(int baseAddr, String file) {

		int nextFreeAddr = baseAddr;  // keep the base address around
		Scanner sc = null;
		try {  // open up a new scanner on the source file
			sc = new Scanner(new BufferedReader(new FileReader(file)));
		} catch (Exception e) {
			errorAndExit("ERROR: " + e.getMessage());
		}

		// ############## BEGIN PARSE HEADER ##################
		// a Header object's purpose is to parse the header of the SXX
		// program and hold useful information
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
		// ################ END PARSE HEADER #################

		// ############ BEGIN INSERTING OPCODES ##############
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
		// ############### END INSERTING OPCODES ################

		// ############# BEGIN RELOCATION PROCESS ###############
		while (sc.hasNextLine()) {  
			opcode = sc.nextLine().trim();

			if (isCommentOrBlankLine(opcode)) {
				continue;
			} else if (opcode.charAt(0) == '%') {
				break;  // we're done reading the SXX program
			} else {
				insertRelocation(baseAddr, opcode);
			}
		} // ############## END RELOCATION PROCESS ################

		try { }
		finally {  // close the scanner
			if (sc != null)
				sc.close();
		}
		return baseAddr + header.entry;  // actual entry point into memory
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

	/**
	 * Execute the opcode pointed at by PC.
	 *
	 * @param PC the program counter
	 *
	 * @return  the new PC
	 */
	public int executeOpcode(int PC) throws HaltException {
		int opcode = mem.getContents(PC);
		PC++;  // increment PC immediately
		if (oldStyle == true && opcode > 22) {
			opcode++; // support legacy opcode numbering convention
		}

		// values to be used in opcode computations
		int addr, value, num, temp; 
		addr = value = num = 0;

		if (opcodeRequiresParameter(opcode)) {
			// if it's an instruction that needs an `addr' or `value'
			// parameter, save that argument
			addr = value = mem.getContents(PC);
			PC++;  // increment PC again
		}

		switch(opcode) {  // find out the opcode and execute it
		case BKPT:   // 0
			/* unconditionally enter the sxx debugger */
			System.err.println("BKPT not implemented");
			instructions.append("BKPT not implemented\n");
			//rightTextArea.setText(instructions);
			System.exit(1);
			break;
		case PUSH:   // 1
			/* push(*addr); */
			if (DEBUG == 1) { System.out.println("PUSH " + addr); }
			instructions.append("PUSH " + addr + "\n");
			//rightTextArea.setText(instructions);
			mem.push(mem.getContents(addr));
			updateTables(mem.getSP());
			break;
		case PUSHV:  // 2
			/* push(value); */
			if (DEBUG == 1) { System.out.println("PUSHV " + value); }
			instructions.append("PUSHV " + value + "\n");
			//rightTextArea.setText(instructions);
			mem.push(value);
			updateTables(mem.getSP());
			break;
		case PUSHS:  // 3
			/* push(*pop()); */
			if (DEBUG == 1) { System.out.println("PUSHS"); }
			instructions.append("PUSHS\n");
			//rightTextArea.setText(instructions);
			num = mem.pop();
			mem.push(mem.getContents(num));
			updateTables(mem.getSP());
			break;
		case PUSHX:  // 4
			/* push(*(pop()+addr)); */
			if (DEBUG == 1) { System.out.println("PUSHX " + addr); }
			instructions.append("PUSHX\n");
			//rightTextArea.setText(instructions);
			num = mem.pop()+addr;
			mem.push(mem.getContents(num));
			updateTables(mem.getSP());
			break;
		case POP:    // 5
			/* *addr=pop(); */
			if (DEBUG == 1) { System.out.println("POP " + addr); }
			instructions.append("POP " + addr + "\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.putContents(addr, temp);
			updateTables(addr);
			break;
		case POPS:   // 6
			/* temp=pop(); *pop()=temp; */
			if (DEBUG == 1) { System.out.println("POPS"); }
			instructions.append("POPS\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			num = mem.pop();
			mem.putContents(num, temp);
			updateTables(num);
			break;
		case POPX:   // 7
			/* temp=pop(); *(pop()+addr)=temp; */
			if (DEBUG == 1) { System.out.println("POPX " + addr); }
			instructions.append("POPX " + addr + "\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			num = mem.pop()+addr;
			mem.putContents(num, temp);
			updateTables(num);
			break;
		case DUPL:   // 8
			/* push(*SP); */
			if (DEBUG == 1) { System.out.println("DUPL"); }
			instructions.append("DUPL\n");
			//rightTextArea.setText(instructions);
			mem.push(mem.getContents(mem.getSP()));
			updateTables(mem.getSP());
			break;
		case SWAP:   // 9
			/* temp=*SP; *SP=*(SP+1); *(SP+1)=temp; */
			if (DEBUG == 1) { System.out.println("SWAP"); }
			instructions.append("SWAP\n");
			//rightTextArea.setText(instructions);
			temp = mem.getContents(mem.getSP());
			mem.putContents(mem.getSP(), mem.getContents(mem.getSP()+1));
			updateTables(mem.getSP());
			mem.putContents(mem.getSP()+1, temp);
			updateTables(mem.getSP()+1);
			break;
		case OVER:   // 10
			/* push(*(SP+1)); */
			if (DEBUG == 1) { System.out.println("OVER"); }
			instructions.append("OVER\n");
			//rightTextArea.setText(instructions);
			mem.push(mem.getContents(mem.getSP()+1));
			updateTables(mem.getSP());
			break;
		case DROP:   // 11
			/* SP++; */
			if (DEBUG == 1) { System.out.println("DROP"); }
			instructions.append("DROP\n");
			//rightTextArea.setText(instructions);
			mem.setSP(mem.getSP()+1);
			table.setValueAt(new Integer(mem.getSP()), 0, 1);
			stackTable.setValueAt(new Integer(mem.getSP()), -0+16383, 1);
			break;
		case ROT:    // 12
			/* temp=*SP; *SP=*(SP+2); *(SP+2)=*(SP+1); *(SP+1)=temp; */
			if (DEBUG == 1) { System.out.println("ROT"); }
			instructions.append("ROT\n");
			//rightTextArea.setText(instructions);
			temp = mem.getContents(mem.getSP());
			mem.putContents(mem.getSP(), mem.getContents(mem.getSP()+2));
			updateTables(mem.getSP());
			mem.putContents(mem.getSP()+2, mem.getContents(mem.getSP()+1));
			updateTables(mem.getSP()+2);
			mem.putContents(mem.getSP()+1, temp);
			updateTables(mem.getSP()+1);
			break;
		case TSTLT:  // 13
			/* TSTLT       --> temp=pop(); push((temp<0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTLT"); }
			instructions.append("TSTLT\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (temp < 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case TSTLE:  // 14
			/* TSTLE       --> temp=pop(); push((temp<=0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTLE"); }
			instructions.append("TSTLE\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (temp <= 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case TSTGT:  // 15
			/* temp=pop(); push((temp>0)?1:0); */
			instructions.append("TSTGT\n");
			//rightTextArea.setText(instructions);
			if (DEBUG == 1) { System.out.println("TSTGT"); }
			temp = mem.pop();
			mem.push( (temp > 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case TSTGE:  // 16
			/* temp=pop(); push((temp>=0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTGE"); }
			instructions.append("TSTGE\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (temp >= 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case TSTEQ:  // 17
			/* temp=pop(); push((temp==0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTEQ"); }
			instructions.append("TSTEQ\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (temp == 0) ? 1 : 0);
			updateTables(mem.getSP());
			break;
		case TSTNE:  // 18
			/* temp=pop(); push((temp!=0)?1:0); */
			if (DEBUG == 1) { System.out.println("TSTNE"); }
			instructions.append("TSTNE\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (temp != 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case BNE:    // 19
			/* if (pop()!=0) PC=addr; */
			if (DEBUG == 1) { System.out.println("BNE " + addr); }
			instructions.append("BNE " + addr + "\n");
			//rightTextArea.setText(instructions);
			if (mem.pop() != 0) {
				PC = addr;
			}
			break;
		case BEQ:    // 20
			/* if (pop()==0) PC=addr; */
			if (DEBUG == 1) { System.out.println("BEQ " + addr); }
			instructions.append("BEQ " + addr + "\n");
			//rightTextArea.setText(instructions);
			if (mem.pop() == 0) {
				PC = addr;
			}
			break;
		case BR:     // 21
			/* PC=addr; */
			if (DEBUG == 1) { System.out.println("BR " + addr); }
			instructions.append("BR " + addr + "\n");
			//rightTextArea.setText(instructions);
			PC = addr;
			break;
		case CALL:   // 22
			/* push(PC); PC=addr; */
			if (DEBUG == 1) { System.out.println("CALL " + addr); }
			instructions.append("CALL " + addr + "\n");
			//rightTextArea.setText(instructions);
			mem.push(PC);
			PC = addr;
			updateTables(mem.getSP());
			break;
		case CALLS:  // 23
			/* temp=pop(); push(PC); PC=temp; */
			if (DEBUG == 1) { System.out.println("CALLS"); }
			instructions.append("CALLS\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push(PC);
			PC = temp;
			updateTables(mem.getSP());
			break;
		case RETURN: // 24
			/* PC=pop(); */
			if (DEBUG == 1) { System.out.println("RETURN"); }
			instructions.append("RETURN\n");
			//rightTextArea.setText(instructions);
			PC = mem.pop();
			break;
		case RETN:   // 25
			/* temp=pop(); SP += value; PC=temp; */
			if (DEBUG == 1) { System.out.println("RETN " + value); }
			instructions.append("RETURN " + value + "\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			//if (DEBUG == 1) { System.out.println("RETN:temp = " + temp); }
			mem.setSP(mem.getSP()+value);
			table.setValueAt(new Integer(mem.getSP()), 0, 1);
			stackTable.setValueAt(new Integer(mem.getSP()), -0+16383, 1);
			PC = temp;
			break;
		case HALT:   // 26
			/* halt program execution */
			if (DEBUG == 1) { System.out.println("HALT"); }
			instructions.append("HALT\n");
			//rightTextArea.setText(instructions);
			throw new HaltException();
			//break;
		case ADD:    // 27
			/* temp=pop(); push( pop() + temp ); */
			if (DEBUG == 1) { System.out.println("ADD"); }
			instructions.append("ADD\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push(mem.pop()+temp);
			updateTables(mem.getSP());
			break;
		case SUB:    // 28
			/* temp=pop(); push( pop() - temp ); */
			if (DEBUG == 1) { System.out.println("SUB"); }
			instructions.append("SUB\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push(mem.pop()-temp);
			updateTables(mem.getSP());
			break;
		case MUL:    // 29
			/* temp=pop(); push( pop() * temp ); */
			if (DEBUG == 1) { System.out.println("MUL"); }
			instructions.append("MUL\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push(mem.pop() * temp);
			updateTables(mem.getSP());
			break;
		case DIV:    // 30
			/* temp=pop(); push( pop() / temp ); */
			if (DEBUG == 1) { System.out.println("DIV"); }
			instructions.append("DIV\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			try {
				mem.push(mem.pop() / temp);
			} catch (ArithmeticException e) {
				errorAndExit("ERROR: Attempt to divide by zero");
			}
			updateTables(mem.getSP());
			break;
		case MOD:    // 31
			/* temp=pop(); push( pop() % temp ); */
			if (DEBUG == 1) { System.out.println("DIV"); }
			instructions.append("MOD\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			try {
				mem.push(mem.pop() % temp);
			} catch (ArithmeticException e) {
				errorAndExit("ERROR: Attempt to mod by zero");
			}
			updateTables(mem.getSP());
			break;
		case OR:     // 32
			/* temp=pop(); push( pop() || temp ); */
			if (DEBUG == 1) { System.out.println("OR"); }
			instructions.append("OR\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (mem.pop() != 0 || temp != 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case AND:    // 33
			/* temp=pop(); push( pop() && temp ); */
			if (DEBUG == 1) { System.out.println("AND"); }
			instructions.append("AND\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (mem.pop() != 0 && temp != 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case XOR:    // 34
			/* temp=pop(); push( pop() xor temp ); [see below] */
			if (DEBUG == 1) { System.out.println("XOR"); }
			instructions.append("XOR\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			mem.push( (mem.pop() != 0 ^ temp != 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case NOT:    // 35
			/* push( !pop() ); */
			if (DEBUG == 1) { System.out.println("NOT"); }
			instructions.append("NOT\n");
			//rightTextArea.setText(instructions);
			mem.push( !(mem.pop() != 0) ? 1 : 0 );
			updateTables(mem.getSP());
			break;
		case NEG:    // 36
			/* push( -pop() ); */
			if (DEBUG == 1) { System.out.println("NEG"); }
			instructions.append("NEG\n");
			//rightTextArea.setText(instructions);
			mem.push( (-1)*mem.pop());
			updateTables(mem.getSP());
			break;
		case ADDX:   // 37
			/* push( pop()+addr ); */
			if (DEBUG == 1) { System.out.println("ADDX " + addr); }
			instructions.append("ADDX " + addr + "\n");
			//rightTextArea.setText(instructions);
			mem.push(mem.pop() + addr);
			updateTables(mem.getSP());
			break;
		case ADDSP:  // 38
			/* SP += value; */
			if (DEBUG == 1) { System.out.println("ADDSP " + value); }
			instructions.append("ADDSP " + value + "\n");
			//rightTextArea.setText(instructions);
			mem.setSP(mem.getSP()+value);
			table.setValueAt(new Integer(mem.getSP()), 0, 1);
			stackTable.setValueAt(new Integer(mem.getSP()), -0+16383, 1);
			break;
		case READ:   // 39
			inputLine = inputLine.replaceAll("^\\s*", "");
			while (inputLine.equals("")) {
				inputLine = JOptionPane.showInputDialog(null, "SXX");
				// user clicks ``cancel"
				if (inputLine == null) {
					throw new InputMismatchException();
				}
				inputLine += "\n";
				// simulate the user actually typing this in
				System.out.print(inputLine);
				inputLine = inputLine.replaceAll("^\\s*", "");
			}
			m = digit.matcher(inputLine);
			if (! m.find()) {
				throw new InputMismatchException();
			}
			String digits = m.group();
			temp = Integer.parseInt(digits);
			if (DEBUG == 1) { System.out.println("READ ["+temp+"]"); }
			instructions.append("READ ["+temp+"]\n");
			//rightTextArea.setText(instructions);
			inputLine = inputLine.replaceAll("^[+-]?\\d+", "");
			mem.push(temp);
			updateTables(mem.getSP());
			break;
		case PRINT:  // 40
			/* print pop() in %d format */
			temp = mem.pop();
			if (DEBUG == 1) { System.out.println("PRINT ["+temp+"]"); }
			instructions.append("PRINT ["+temp+"]\n");
			//rightTextArea.setText(instructions);
			System.out.print(temp);
			break;
		case READC:  // 41
			/* read temp in %c format; push(temp); */
			if (inputLine.equals("")) {
				// collect line from the user
				inputLine = JOptionPane.showInputDialog(null, "SXX");			
				// user clicks ``cancel"
				if (inputLine == null) {
					temp = -1;
					break;
				}
				// stick on a newline to mimic STDIN
				inputLine += "\n";
				System.out.print(inputLine);
			}
			// get the next character
			temp = Read.READC(new Scanner(inputLine));
			if (DEBUG == 1) { System.out.println("READC ["+temp+"]"); }
			instructions.append("READC ["+temp+"]\n");
			//rightTextArea.setText(instructions);
			mem.push(temp);
			// the character is consumed
			inputLine = inputLine.substring(1);
			updateTables(mem.getSP());
			break;
		case PRINTC: // 42
			/* print pop() in %c format */
			temp = mem.pop();
			if (DEBUG == 1) { System.out.println("PRINTC ["+temp+"]"); }
			instructions.append("PRINTC ["+temp+"]\n");
			//rightTextArea.setText(instructions);
			System.out.print((char)temp);
			break;
		case TRON:   // 43
			/* turn on trace feature */
			if (DEBUG == 1) { System.out.println("TRON"); }
			instructions.append("TRON\n");
			//rightTextArea.setText(instructions);
			TRACE = true;
			break;
		case TROFF:  // 44
			/* turn off trace feature */
			if (DEBUG == 1) { System.out.println("TROFF"); }
			instructions.append("TROFF\n");
			//rightTextArea.setText(instructions);
			TRACE = false;
			break;
		case DUMP:   // 45
			/* temp=pop(); dump memory from pop() to temp; */
			if (DEBUG == 1) { System.out.println("DUMP"); }
			instructions.append("DUMP\n");
			//rightTextArea.setText(instructions);
			temp = mem.pop();
			dump(mem.pop(), temp);
			break;
		default:
			System.err.println("ERROR: Invalid opcode");
			System.err.println("  " + opcode);
			System.exit(1);
		}
		
		// update SP
		table.setValueAt(new Integer(mem.getContents(0)), 0, 1);
		stackTable.setValueAt(new Integer(mem.getContents(0)), -0+16383, 1);

		// update values on the stack
		for (int i = 16383; i >= mem.getSP(); i--) {
			table.setValueAt(new Integer(mem.getContents(i)), i, 1);
			stackTable.setValueAt(new Integer(mem.getContents(i)), -i+16383, 1);
		}

		return PC; // return the new PC
	}

	/**
	 * Dump memory from pop to temp.
	 *
	 * @param pop  beginning address
	 * @param temp ending address
	 *
	 */
	public void dump(int pop, int temp) {
		if (pop < temp || 0 > pop || pop > mem.memorySize-1
				|| 0 > temp || temp > mem.memorySize-1) {
			errorAndExit("ERROR: Illegal dump range");
		}
		for (; pop >= temp; pop--)
			System.out.println(mem.getContents(pop));
	}

	/**
	 * Returns true if opcode requires a parameter.
	 *
	 * @param opcode the opcode
	 *
	 */
	public boolean opcodeRequiresParameter(int opcode) {
		// informs the caller whether the current opcode needs to look at the
		// next location in memory for an argument to the instruction
		boolean requiresParameter;

		switch (opcode) {
		case PUSH:
		case PUSHV:
		case PUSHX:
		case POP:
		case POPX:
		case BNE:
		case BEQ:
		case BR:
		case CALL:
		case RETN:
		case ADDX:
		case ADDSP:
			requiresParameter = true;
			break;
		default:
			requiresParameter = false;
			break;
		}

		return requiresParameter;
	}

	private void printInstructions(int length, int PC) {
		for (int pointer = 16+length-1; pointer >= 16; pointer--)
			System.out.format("%2d| %2s%s\n", pointer, mem.getContents(pointer), 
					(pointer == PC) ? " <-- PC" : "");
		System.out.println();
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

	private void printDebug(int opcode, int length, int PC, int temp) {
		System.out.println("About to execute opcode: " + opcode);
		mem.reveal();
		System.out.println("\nInstructions:");
		printInstructions(length, PC-1);
		System.out.format("PC: %d  temp: %d%n%n", PC-1, temp); 
	}

	/* mneumonics and their opcodes */
	public static final int BKPT   =   0;
	public static final int PUSH   =   1;
	public static final int PUSHV  =   2;
	public static final int PUSHS  =   3;
	public static final int INDIR  =   3;
	public static final int PUSHX  =   4;
	public static final int POP    =   5;
	public static final int POPS   =   6;
	public static final int POPX   =   7;
	public static final int DUPL   =   8;
	public static final int SWAP   =   9;
	public static final int OVER   =  10;
	public static final int DROP   =  11;
	public static final int ROT    =  12;
	public static final int TSTLT  =  13;
	public static final int TSTLE  =  14;
	public static final int TSTGT  =  15;
	public static final int TSTGE  =  16;
	public static final int TSTEQ  =  17;
	public static final int TSTNE  =  18;
	public static final int BNE    =  19;
	public static final int BT     =  19;
	public static final int BEQ    =  20;
	public static final int BF     =  20;
	public static final int BR     =  21;
	public static final int CALL   =  22;
	public static final int CALLS  =  23;
	public static final int RETURN =  24;
	public static final int POPPC  =  24;
	public static final int RETN   =  25;
	public static final int HALT   =  26;
	public static final int ADD    =  27;
	public static final int SUB    =  28;
	public static final int MUL    =  29;
	public static final int DIV    =  30;
	public static final int MOD    =  31;
	public static final int OR     =  32;
	public static final int AND    =  33;
	public static final int XOR    =  34;
	public static final int NOT    =  35;
	public static final int NEG    =  36;
	public static final int ADDX   =  37;
	public static final int ADDSP  =  38;
	public static final int READ   =  39;
	public static final int PRINT  =  40;
	public static final int READC  =  41;
	public static final int PRINTC =  42;
	public static final int TRON   =  43;
	public static final int TROFF  =  44;
	public static final int DUMP   =  45;

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
						String s = pt1 + width + pt2;// + width + pt3 ;
						
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
				runASingleInstruction();
			} else if (name.equals("Run") && ! file.equals("")) {
				if (stepRadioButton.isSelected()) {
					runASingleInstruction();
				} else {  // run the whole darn program
					runALLTheInstructions();
				}
			} else if (name.equals("Clear")) {
				clearALLTheThings();
			}
		}
	}
	
	/**
	 * Massive reset for most of the variables that change along with an executing
	 * SXX program.
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
		instructions = new StringBuilder("");
		inputLine = "";
		file = "";
		PC = 0;	
		PCLabel.setText("PC: - | SP: - ");
		// redirect standard output again
		mc.redirectOut();
		// repaint the tables
		table.repaint();
		stackTable.repaint();
	}
	
	/**
	 * Updates the field of each table associated with `addr'.
	 * 
	 * @param addr  the entry that needs to be updated
	 */
	public void updateTables(int addr) {
		table.setValueAt(new Integer(mem.getContents(addr)), addr, 1);
		stackTable.setValueAt(new Integer(mem.getContents(addr)), -addr+16383, 1);
	}
	
	/**
	 * Create a new SM and go.
	 * 
	 */
	public static void main(String[] args) {
		new SM().sitAndWait();
	}

}
