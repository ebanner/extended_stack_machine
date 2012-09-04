/************************************************************************/
/*									*/
/*	Extended Stack machine emulator					*/
/*	Revised Sun Nov 17 14:31:32 CST 2002 - T. V. Fossum             */
/*		Added CALLS (23) opcode; others opcodes are		*/
/*		pushed up as appropriate
/*	Revised Sun Dec 31 13:57:06 CST 2000 - T. V. Fossum             */
/*		Modified definition of ADDX				*/
/*	Revised Sat Oct  7 17:41:16 CDT 2000 - T. V. Fossum		*/
/*		Converted to C++					*/
/*		Handles new style executable modules (%SXX-E)		*/
/*	Revised Wed Sep 28 16:45:47 CDT 1994 - T. V. Fossum to		*/
/*		add TSTEQ (17) and TSTNE (18) opcodes; other		*/
/*		opcodes are pushed up as appropriate			*/
/*	Revised Wed Dec 18 16:47:00 CST 1991 - T. V. Fossum to		*/
/*		upgrade to 16k						*/
/*	Revised Thu Nov  8 11:07:27 CST 1990 - T. V. Fossum to		*/
/*		skip ':0' or '::' lines					*/
/*	Revised Wed Oct 11 14:55:16 CDT 1989 - T. V. Fossum to		*/
/*		allow for ':<integer>' construct in object files	*/
/*	Revised Mon Nov 14 11:48:00 CST 1988 - T. V. Fossum to		*/
/*		handle EOFs properly during READ/READC operations	*/
/*	Revised Tue Dec 16 23:41:59 CST 1986 - T. V. Fossum to		*/
/*		enable debug upon interrupt only in debug mode		*/
/*	Revised Tue Dec  9 23:14:49 CST 1986 - T. V. Fossum to		*/
/*		add the opcodes OVER, DROP and ROT			*/
/*	Revised Thu Nov 27 00:06:16 CST 1986 - T. V. Fossum to		*/
/*		add opcodes BKPT, RETN, XOR and ADDSP			*/
/*	Fri Nov 14 11:52:30 CST 1986 - T. V. Fossum			*/
/*									*/
/************************************************************************/

/*
 *The purpose of this program is to emulate the extended
 *stack machine described by the handouts in CS 440.
 */

using namespace std;

#include	<stdio.h>
#include        <stdlib.h>
#include	<string.h>
#include	<ctype.h>
#include	<signal.h>
#include	<setjmp.h>
#include	<time.h>
// #include	<ios>
// #include	<istream>
// #include	<ostream>
#include	<iostream>
#include	<fstream>
#include	<iomanip>

#define PROG	progname	/* program name				*/

#define	EOLN	'\n'		/* stdio value for end of line 		*/

#define MAXINT	2147483647	/* maximum integer value		*/
#define	MAXMEM	16384		/* maximum memory location of 16K memory*/
#define MINOFFSET	16	/* minimum program offset into memory	*/
#define MAXOFFSET	128	/* maximum program offset into memory	*/
#define SP	mem[0]		/* value of stack pointer		*/
#define	TRUE	1
#define FALSE	0
#define BFSZ	256		/* interactive input buffer size	 */
#define MAXD	4		/* maximum stack print depth		 */


int     mem[MAXMEM];		/* machine memory - 0..MAXMEM-1		 */
int     pc;			/* internal Program Counter		 */
int     haltflag = FALSE;	/* set when the halt is executed	 */
int     traceon = FALSE;	/* set true when the trace is turned on	 */
int     debugon = FALSE;	/* set true when the debugger is on	 */
int	banner = FALSE;		/* do we print a banner?		 */
int     instr;			/* assembler instruction		 */
int     addr;			/* operand				 */
int     dumpaddr;		/* memory location to be dumped		 */
int     temp,
        temp1;			/* internal CPU registers		 */
int     offset = 0;		/* base address of the program		 */
int     pstart,
        pend;			/* save program/data start & end	 */
int     linepos = 0;		/* current print position on output	 */
int	jump = TRUE;		/* control interrupts during execution	 */
char	*progname;		/* program name				 */
int	EOFflag = FALSE;	/* TRUE if EOF has been seen on input	 */
jmp_buf	jbuf;			/* SIGINT location for debug		 */
jmp_buf read_int;		/* SIGINT location for read interrupt	 */
bool	newstyle;		/* true if a new style executable	 */
int	entry_point;		/* entry point for new style execs	 */

ifstream objf;			/* used to open object file		 */
istream obj;			/* input object file			 */
ifstream interactf;		/* used to open interactive file	 */
istream interact;		/* interactive input file		 */

void putmem(int, int);
int getmem(int);
void do_exec();
void dexec (int);
int range (char*, int&, int&);
int val (char*, int&);
int til (char*, int&, int&, int&);
void aabort (int);
int dasm(int);
void go_debug(int);
void end_read(int);

class instruction {
public:
    int     nargs;
    const char   *mnemonic;
};

instruction  iarray[] = {/* holds mnemonic name of op code	 */

    0, "bkpt",
#define BKPT	0		/* break to debugger */

    1, "push",
#define	PUSH	1		/* push(*addr) */

    1, "pushv",
#define	PUSHV	2		/* push(value) */

    0, "pushs",
#define	PUSHS	3		/* push(*pop()) */

    1, "pushx",
#define	PUSHX	4		/* push(*(pop()+addr)) */

    1, "pop",
#define	POP	5		/* *addr=pop() */

    0, "pops",
#define	POPS	6		/* temp=pop(); *pop()=temp */

    1, "popx",
#define	POPX	7		/* temp=pop(); *(pop()+addr)=temp */

    0, "dupl",
#define	DUPL	8		/* push(*SP) */

    0, "swap",
#define	SWAP	9		/* swap *SP and *(SP+1) */

    0, "over",
#define OVER	10		/* push(*(SP+1)) */

    0, "drop",
#define DROP	11		/* SP++ */

    0, "rot",
#define ROT	12		/* temp=*SP; *SP=*(SP+2); *(SP+2)=*(SP+1);
				   *(SP+1)=temp; */
    0, "tstlt",
#define	TSTLT	13		/* temp=pop(); if (temp<0) push(1); else
				   push(0); */
    0, "tstle",
#define	TSTLE	14		/* temp=pop(); if (temp<=0) push(1); else
				   push(0); */
    0, "tstgt",
#define	TSTGT	15		/* temp=pop(); if (temp>0) push(1); else
				   push(0); */
    0, "tstge",
#define	TSTGE	16		/* temp=pop(); if (temp>=0) push(1); else
				   push(0); */
    0, "tsteq",
#define	TSTEQ	17		/* temp=pop(); if (temp==0) push(1); else
				   push(0); */
    0, "tstne",
#define	TSTNE	18		/* temp=pop(); if (temp!=0) push(1); else
				   push(0); */
    1, "bne",
#define	BNE	19		/* if (pop()!=0) PC=addr */

    1, "beq",
#define	BEQ	20		/* if (pop()==0) PC=addr */

    1, "br",
#define	BR	21		/* PC = addr */

    1, "call",
#define	CALL	22		/* push(PC); PC=addr */

    0, "calls",
#define CALLS	23		/* temp=pop(); push(PC); PC=temp */

    0, "return",
#define	RETURN	24		/* PC=pop() */

    1, "retn",
#define RETN	25		/* temp=pop(); SP=SP+value; PC=temp */

    0, "halt",
#define	HALT	26		/* halt program execution */

    0, "add",
#define	ADD	27		/* temp=pop(); push( pop()+temp ) */

    0, "sub",
#define	SUB	28		/* temp=pop(); push( pop()-temp ) */

    0, "mul",
#define	MUL	29		/* temp=pop(); push( pop()*temp ) */

    0, "div",
#define	DIV	30		/* temp=pop(); push( pop()/temp ) */

    0, "mod",
#define MOD	31		/* temp=pop(); push( pop()%temp ) */

    0, "or",
#define	OR	32		/* temp=pop(); push( pop() || temp ) */

    0, "and",
#define	AND	33		/* temp=pop(); push( pop() && temp ) */

    0, "xor",
#define	XOR	34		/* temp=pop(); push( pop() xor temp ) */

    0, "not",
#define	NOT	35		/* push( !pop() ) */

    0, "neg",
#define NEG	36		/* push( -pop() ) */

    1, "addx",
#define ADDX	37		/* push( pop() + addr ) */

    1, "addsp",
#define ADDSP	38		/* SP=SP+value */

    0, "read",
#define	READ	39		/* read temp in %d format; push(temp) */

    0, "print",
#define	PRINT	40		/* print pop() in %d format */

    0, "readc",
#define	READC	41		/* read temp in %c format; push(temp) */

    0, "printc",
#define	PRINTC	42		/* print pop() in %c format */

    0, "tron",
#define	TRON	43		/* turn trace feature */

    0, "troff",
#define	TROFF	44		/* turn off trace feature */

    0, "dump",
#define	DUMP	45		/* temp=pop(); dump memory from pop() to temp 
				*/

#define MAXINSTR	46	/* last instruction index + 1		 */
};

inline int hasaddr(int i)
{
	return iarray[i].nargs > 0;
}

inline int isok(int i)
{
	return i >= 0 && i < MAXINSTR && iarray[i].nargs >= 0;
}

inline int okmem(int x)
{
	return x >= 0 && x < MAXMEM;
}

void do_args(int, char**);
void initialize();
void em();
void debug();
void usage();

main (int argc, char** argv)
{
    do_args (argc, argv);

    if ( banner ) {
        /*   print banner   */
        cout << endl
             << "****************************" << endl
             << "** Stack Machine Emulator **" << endl
             << "****************************" << endl << endl;
    }

    initialize ();

    if ( banner )
	cout << "** Executing...   **" << endl << endl;

    if (!debugon)
	em ();
    if (haltflag == FALSE)
	debug ();

    if ( banner )
       cout << endl << "** Terminating... **" << endl;
    exit(0);
}

void do_args (int argc, char** argv)
{
    char *cp;

    obj = (istream)cin;		/* input file is initially stdin	 */
    interact = (istream)cin;	/* interactive input is too		 */
    progname = *argv++;
    cp = strrchr(progname, '/');
    if (cp != NULL)
	progname = cp+1;
    --argc;
    while (argc > 0 && **argv == '-') {
	switch (*(*argv + 1)) {
	    case 'd': 
		debugon = TRUE;
		banner = TRUE;
		break;
	    case 't': 
		traceon = TRUE;
		break;
	    case 'B':
		banner = TRUE;
		break;
	    case 'b': 
		if (sscanf ((*argv + 2), "%d", &offset) != 1)
		    usage ();
		if ((offset < MINOFFSET) || (offset > MAXOFFSET)) {
		    cerr << PROG
		         << ": "
			 << offset
			 << ": illegal offset"
			 << endl;
		    exit (1);
		}
		break;
	    default: 
		usage ();
	}
	--argc;
	++argv;
    }
    if (argc > 1)
	usage ();
    if (argc == 1) {
	objf.open(*argv, ios::in | ios::nocreate);
	if (!objf.good()) {
	    perror (*argv);
	    exit (1);
	}
	obj = objf;
    }
    else {
    	interactf.open("/dev/tty", ios::in | ios::nocreate);
	if (interactf.fail()) {
	    perror ("tty");
	    exit (1);
	}
	interact = interactf;
    }
}

void usage () {
    cout    << "Usage: "
	    << PROG
	    << " [-B] [-d] [-t] [-b<baseaddr>] [file]"
	    << endl;;
    exit (1);
}

enum {ERR_VAL, EOF_VAL, INT_VAL, LC_VAL, PCT_VAL};

void skip_to_eoln()
{
	char c;
	while (obj.get(c))
		if (c == '\n')
			break;
}

/* dscan -- scan for integer in input stream */
int dscan(int& i)
{
	char	c;			/* character from input obj file */
	int	sign = 0;		/* nonzero if '-' is present */
	int	ret = INT_VAL;		/* return value */

loop:
	ret = INT_VAL;
	obj.get(c);
	if (obj.eof())
		return EOF_VAL;
	if (!obj.good())
		return ERR_VAL;
	switch(c) {
	case '#':
		skip_to_eoln();
		goto loop;
	case ':':
		ret = LC_VAL;
		obj >> i;
		if (!obj.good() || i < 0)
			ret = ERR_VAL;
		skip_to_eoln();
		if (i == 0)
			goto loop;
		return ret;
	case '%':
		i = 0;
		skip_to_eoln();
		return PCT_VAL;
	default:
		obj.putback(c);
		obj >> i;
		if (!obj.good())
			ret = ERR_VAL;
		skip_to_eoln();
		return ret;
	}
}

// look for a section separator - a line beginning with %
void pscan()
{
	char c;

    	obj.get(c);
	if (!obj.good() || c != '%') {
	    cerr << "Illegal object module format" << endl;
	    exit(1);
	}
        skip_to_eoln();
}

void initialize () {
    int     lc;			// used to load program
    int     i;
    int     length;		// of the program
    long    ltime;
    char	c;		// input character in object module

//  initialize memory to zeroes
    for (i = 0; i < MAXMEM; i++)
	mem[i] = 0;

//  set the stack pointer beyond the end of memory
    SP = MAXMEM;

//  determine where to relocate the program
    if (offset == 0) {
	ltime = time (NULL);
	offset = ltime % (MAXOFFSET - MINOFFSET + 1);
	offset += MINOFFSET;
    }
    pc = offset;
    pstart = offset;		/* save program/data start address	 */

    if ( banner ) {
        cout << "** Loading...     **" << endl;
        cout << "** Origin = " << offset << " **" << endl;
    }

//  read the header information
    newstyle = false;
    obj.get(c);
    if (obj.good()) {
        if (c == '%') {
    	    // is this a new style object module?
	    char buf[6];
	    obj.get(buf, 6);
	    if (strcmp(buf, "SXX-E") != 0) {
		cerr << "Not an executable module" << endl;
		exit(1);
	    }
	    newstyle = true;
	    skip_to_eoln();
        } else {
    	    obj.putback(c);
        }
    } else {
    	cerr << "Empty or corrupt input file" << endl;
	exit(1);
    }
    
    //  read the object into memory
    if (dscan(length) != INT_VAL || length < 0) {
	cerr << PROG << ": improper object module length" << endl;
	exit (1);
    }

    if (length > MAXMEM - offset) {
	cerr << PROG << ": object module too large" << endl;
	exit (1);
    }
#ifdef DEBUG
    cerr << "length=" << length << endl;
#endif

    lc = 0;
    addr = offset;
    entry_point = 0;
    if (newstyle) {
      if (dscan(entry_point) != INT_VAL) {
	cerr << "Illegal object module format" << endl;
	exit(1);
      }
      if (entry_point < 0 || entry_point >= length) {
	cerr << "Entry point exceeds module limit" << endl;
	exit(1);
      }	
      // place the program counter at the entry point
      pc += entry_point;
    }
    pscan();

    // read in text
    while (lc < length) {
	if (addr >= MAXMEM) {
	    cerr << PROG << ": object module too large" << endl;
	    exit (1);
	}
	i = dscan(temp);
	switch(i) {
	case LC_VAL:
		// unitialized storage
		if (temp == 0)
			continue;
		if (temp > MAXMEM - addr) {
		    cerr << PROG << ": object module too large" << endl;
		    exit (1);
		}
		if (temp > length - lc) {
		    cerr << PROG << ": improper unitialized storage value" << endl;
		    exit(1);
		}
		addr += temp;
		lc += temp;
#ifdef DEBUG
		cout << "lc=" << lc << " addr=" << addr << endl;
#endif
		break;

	case INT_VAL:
		// initialized word
#ifdef DEBUG
		cout << "lc=" << lc << " addr=" << addr << " value=" << temp << endl;
#endif
		putmem (addr, temp);
		lc++, addr++;
		break;
	default:
		cerr << PROG << ": improper object module text value" << endl;
		exit (1);
	}
    }
    pend = pstart + length;	/* save program/data end address	 */

    if (newstyle)
        pscan();

    // adjust relocatable addresses
    while ((i=dscan(addr)) == INT_VAL) {
#ifdef DEBUG
	cout << "i=" << i << endl;
#endif
	if (addr < 0 || addr >= length) {
	    cerr << PROG << ": " << addr << ": illegal relocation address" << endl;
	    exit (1);
	}
#ifdef DEBUG
	cout << "addr=" addr << endl;
#endif
	addr += pstart;
	/* double-check address */
	if (!okmem (addr)) {
	    cerr << PROG << ": " << addr << ": illegal relocation address" << endl;
	    exit (1);
	}
	putmem (addr, getmem (addr) + pstart);
    }

    // end of relocation dictionary
    if (newstyle) {
    	if (i != PCT_VAL) {
            cerr << PROG << ": improper object module format" << endl;
            exit (1);
        }
	if (dscan(temp) != EOF_VAL) {
            cerr << PROG << ": improper object module format" << endl;
            exit (1);
	}
    } else if (i != EOF_VAL) {
            cerr << PROG << ": improper object module format" << endl;
            exit (1);
    }
}

void trace(int, char*);

void em ()
{				/* run the program til completion */

    if ( setjmp(jbuf)==0 ) {
	if ( debugon )
	    signal (SIGINT, &go_debug);
        while (haltflag == FALSE) {
            if (traceon)
	        trace (pc, "");
	    do_exec ();
        }
    }
    if ( debugon )
	(void) signal (SIGINT, SIG_IGN);
}

/* debugger SIGINT interrupt handler */
void go_debug (int x)
{
    if ( jump )
    	longjmp(jbuf,1);
    else
	jump = TRUE;
}

/* read interrupt SIGINT interrupt handler */
void end_read(int x)
{
    longjmp(read_int,1);
}

inline void DEBUG_ERR(const char* str)
{
	cout << "?? " << str << " - key '?' for help" << endl;
}

void debug () {
    char    com[BFSZ];
    int     mode;
    int     dotrace = FALSE;
    int     i,
            beg,
            end,
            addr,
            value;
    char    c;

#define	PC_ADDR		0
#define	STEP_CT		1
#define	MEM_VAL		2
#define MEM_NVAL	3

    (void) setjmp(jbuf);
    jump = TRUE;
    (void) signal(SIGINT, &go_debug);
    cout << endl;
    int loopcount = 0;
    while (haltflag == FALSE) {
	trace (pc, (char*)"");
	cout << "> " << flush;
	dotrace = FALSE;
	interact.getline(com, BFSZ);
	if (interact.eof()) {
		cout << endl;
		exit(1);
	}
	//cerr << "Debug input line is <" << com << ">" << endl;
	//if (strlen(com) == 0)
	//	continue;
	char* cp = com;
	while (*cp == ' ' || *cp == '\t')
		cp++;
	if (*cp == 0)
		continue;
	switch (*cp) {
            case 'm':
		if (til (cp+1, mode, addr, value) || mode != MEM_VAL ) {
		    DEBUG_ERR("Illegal specification");
		    break;
		}
		if ( !okmem(addr) ) {
		    DEBUG_ERR("Illegal address");
		    break;
		}
		mem[addr] = value;
		break;
	    case 'i':
		if ( val(cp+1, addr) || !okmem(addr) ) {
		    DEBUG_ERR("Illegal address");
		    break;
		}
		pc = addr;
		break;
	    case 't': 
		dotrace = TRUE;
	    case 'g': 
		if (til (cp+1, mode, addr, value)) {
		    DEBUG_ERR("Illegal til specification");
		    break;
		}
		switch (mode) {
		    case PC_ADDR: 
			if (!okmem (value)) {
			    DEBUG_ERR("illegal address");
			    break;
			}
			do
			    dexec (dotrace);
			while ( (pc != value) && !haltflag );
			break;
		    case STEP_CT: 
                        do
			    dexec (dotrace);
			while ( (--value > 0) && !haltflag );
			break;
		    case MEM_VAL: 
			if (!okmem (addr)) {
			    DEBUG_ERR("illegal address");
			    break;
			}
			do
			    dexec (dotrace);
			while ( (mem[addr] != value) && !haltflag );
			break;
		    case MEM_NVAL: 
			if (!okmem (addr)) {
			    DEBUG_ERR("illegal address");
			    break;
			}
			do
			    dexec (dotrace);
			while ( (mem[addr] == value) && !haltflag );
			break;
		}
		cout << endl;
		// putchar ('\n');
		break;

	    case 's': 
		dexec (FALSE);
		break;

	    case 'l': 
		for (i = pstart; i < pend; )
		    i = dasm (i);
		cout << endl;
		// putchar ('\n');
		break;

	    case 'u': 
		if (range (cp+1, beg, end) ||
			!okmem (beg) || !okmem (end)) {
		    DEBUG_ERR("illegal range specification");
		    break;
		}
		for (i = beg; i <= end; )
		    i = dasm (i);
		cout << endl;
		// putchar ('\n');
		break;

	    case 'd': 
		if (range (cp+1, beg, end) ||
			!okmem (beg) || !okmem (end)) {
		    DEBUG_ERR("illegal range specification");
		    break;
		}
		for (i = beg; i <= end; i++ )
		    cout << "   mem["
		         << setiosflags(ios::right)
		         << setw(4)
			 << i
			 << resetiosflags(ios::right)
			 << "] = "
			 << setw(15)
			 << mem[i]
			 << endl;
		cout << endl;
		break;

	    case 'p': 
		while ( !haltflag ) {
		    if (traceon)
			trace (pc, "");
		    do_exec ();
		}
		cout << endl;
		// putchar ('\n');
		break;

	    case '\n': 
		break;

	    case '?': 
		cout << endl << endl <<"Commands :" << endl << endl;
		cout << "l        - List load module" << endl;
		cout << "s        - Single step" << endl;
		cout << "m<addr>=<value>" << endl;
		cout << "         - modify memory" << endl;
		cout << "i<val>   - Set PC to val" << endl;
		cout << "p        - proceed" << endl;
		cout << "g<til>   - Go until" << endl;
		cout << "t<til>   - Trace until" << endl;
		cout << "u<range> - Unassemble memory" << endl;
		cout << "d<range> - Display memory" << endl;
		cout << "q        - quit" << endl;
		cout << endl;
		cout << "<range> ::= <start> - <end>" << endl;
		cout << "          | <start> , <count>" << endl;
		cout << endl;
		cout << "<til>   ::=        - <addr>" << endl;
		cout << "          |        , <count>" << endl;
		cout << "          | <addr> = <value>" << endl;
		cout << "          | <addr> ! <value>" << endl;
		cout << endl;
		break;

	    case 'q': 
		exit (0);

	    default: 
		DEBUG_ERR("unknown command");
		break;
	}
    }
    (void) signal(SIGINT,SIG_DFL);
}

void dexec (int t)
{
    if (t)
	trace (pc, "        ");
    do_exec ();
}

int range (char* str, int& beg, int& end)
{
    int     temp;
    char    c;

    if (sscanf (str, " %d %[-,] %d", &beg, &c, &temp) != 3)
	return (-1);
    switch (c) {
	case '-': 
	    end = temp;
	    break;
	case ',': 
	    end = beg + temp - 1;
	    break;
	default: 
	    return (-1);
    }
    return (0);
}

int val (char* str, int& addr)
{
    if ( sscanf (str, "%d", &addr) != 1 )
	return (-1);
    return(0);
}

int til (char* str, int& mode, int& addr, int& value)
{
    int     i;
    char   *cp,
            c;

    // cerr << "til: str=<" << str << ">" << endl;
    for (cp = str; *cp; cp++) {
    	c = *cp;
	if (isspace(c))
	    continue;
    	if (isdigit(c)) {
	    i = sscanf (str, "%d %[=!] %d", &addr, &c, &value);
	    if (i != 3)
	    	return -1;
	    switch(c) {
	    case '=':
	    	mode = MEM_VAL;
		return 0;
	    case '!':
	    	mode = MEM_NVAL;
		return 0;
	    default:
	        return -1;
	    }
	} else if (c == '-' || c == ',') {
	    i = sscanf (cp+1, " %d", &value);
	    if (i != 1)
	    	return -1;
	    switch(c) {
	    case '-':
	        mode = PC_ADDR;
	        return 0;
	    case ',':
	        mode = STEP_CT;
	        return 0;
	    }
	}
    }
    return -1;
}

void aabort (int code)
{
    const char *cp;

    switch (code) {
	case 1: 
	    cp = "attempt to divide by zero";
	    break;
	case 2: 
	    cp = "address out of range";
	    break;
	case 3: 
	    cp = "SP out of range";
	    break;
	case 4: 
	    cp = "illegal dump range";
	    break;
	case 5: 
	    cp = "invalid PC";
	    break;
	case 6: 
	    cp = "invalid op code";
	    break;
	case 7: 
	    cp = "attempt to read past end of file";
	    break;
	case 8: 
	    cp = "illegal integer on read";
	    break;
    }
    cout << endl << "** ERROR: " << cp << " **" << endl;
    interact.clear();
    go_debug(0);
}

void putmem (int addr, int value)
{
    if (okmem (addr))
	mem[addr] = value;
    else
	aabort (2);
}

int     getmem (int addr)
{
    if (okmem (addr))
	return (mem[addr]);
    else
	aabort (2);
    return(0);
}

int     fetch () {		/* fetch the next opcode from memory	 */
    int     word;
 /*  check for invalid Program Counter				 */
    if (okmem (pc) == 0)
	aabort (5);
    word = mem[pc];
    if (isok (word) == 0)
	aabort (6);
    pc++;
    return (word);
}

int fetchop () {		/* fetch the next operand from memory	 */
    int     word;
 /*  check for invalid Program Counter				 */
    if (okmem (pc) == 0)
	aabort (5);
    word = mem[pc];
    pc++;
    return (word);
}

void testsp ()
{
    if ( !okmem(SP) ) 
	aabort (3);
}

void push (int value)
{
    SP--;
    testsp ();
    mem[SP] = value;
}

int pop ()
{
    int     value;

    testsp ();
    value = mem[SP];
    SP++;
    return (value);
}

void do_exec ()
{
    int     i;

    instr = fetch ();

    /*  does this instruction require an address?  */
    if (hasaddr (instr))
	addr = fetchop ();
    jump = FALSE;	/* disable SIGINT trap during instruction exec */
    switch (instr) {

        case BKPT:
	    jump = TRUE;
	    break;
	case PUSH: 
	    push (getmem (addr));
	    break;
	case PUSHV: 
	    push (addr);
	    break;
	case PUSHS: 
	    push (getmem (pop ()));
	    break;
	case PUSHX: 
	    push (getmem (pop () + addr));
	    break;
	case POP: 
	    putmem (addr, pop ());
	    break;
	case POPS: 
	    temp = pop ();
	    putmem (pop (), temp);
	    break;
	case POPX: 
	    temp = pop ();
	    putmem (pop () + addr, temp);
	    break;
	case DUPL: 
	    push (getmem (SP));
	    break;
	case SWAP: 
	    addr = SP;
	    temp = getmem (addr);
	    putmem (addr, getmem (addr + 1));
	    putmem (addr + 1, temp);
	    break;
	case OVER:
	    push(getmem(SP+1));
	    break;
	case DROP:
	    testsp();
	    SP++;
	    break;
	case ROT:
	    temp=getmem(SP);
	    putmem(SP,getmem(SP+2));
	    putmem(SP+2,getmem(SP+1));
	    putmem(SP+1,temp);
	    break;
	case TSTLT: 
	    push(pop() < 0);
	    break;
	case TSTLE: 
	    push(pop() <= 0);
	    break;
	case TSTGT: 
	    push(pop() > 0);
	    break;
	case TSTGE: 
	    push(pop() >= 0);
	    break;
	case TSTEQ:
	    push(pop() == 0);
	    break;
	case TSTNE:
	    push(pop() != 0);
	    break;
	case BNE: 
	    if (pop () != 0)
		pc = addr;
	    break;
	case BEQ: 
	    if (pop () == 0)
		pc = addr;
	    break;
	case BR: 
	    pc = addr;
	    break;
	case CALL: 
	    push (pc);
	    pc = addr;
	    break;
	case RETURN: 
	    pc = pop ();
	    break;
	case RETN:
	    pc = pop ();
	    SP += addr;
	    break;
	case HALT: 
	    haltflag = TRUE;
	    break;
	case ADD: 
	    temp = pop ();
	    push (pop () + temp);
	    break;
	case SUB: 
	    temp = pop ();
	    push (pop () - temp);
	    break;
	case MUL: 
	    temp = pop ();
	    push (pop () * temp);
	    break;
	case DIV: 
	    temp = pop ();
	    if (temp == 0) {
		temp = 1;
		aabort (1);
	    }
	    push (pop () / temp);
	    break;
	case MOD: 
	    temp = pop ();
	    if (temp == 0) {
		temp = 1;
		aabort (1);
	    }
	    push (pop () % temp);
	    break;
	case OR: 
	    temp = pop ();
	    push (pop () || temp);
	    break;
	case AND: 
	    temp = pop ();
	    push (pop () && temp);
	    break;
	case XOR:
	    temp = pop ();
	    temp1 = pop ();
	    push ((!temp && temp1) || (temp && !temp1));
	    break;
	case NOT: 
	    push (pop () ? 0 : 1);
	    break;
	case NEG: 
	    push (-pop ());
	    break;
	case ADDX: 
	    push(pop() + addr);
	    // temp = pop ();
	    // putmem (addr, getmem (addr) + temp);
	    break;
	case ADDSP:
	    SP += addr;
	    break;
	case READ: 
	    if (EOFflag == TRUE)
	        aabort (7);
	    temp = 0;
	    (void) fflush (stdout);
	    if ( setjmp(read_int) == 0 ) {
		if ( debugon )
		    (void) signal(SIGINT, &end_read);
		interact >> temp;
		if (interact.eof()) {
		    EOFflag = TRUE;
	            aabort (7);
		}
	        else if (interact.fail())
	            aabort (8);
		interact.clear();
	    } else
		jump = TRUE;
	    if ( debugon )
		(void) signal(SIGINT, &go_debug);
	    push (temp);
	    break;
	case PRINT: 
	    cout << pop();
	    linepos++;
	    break;
	case READC: 
	    if (EOFflag == TRUE) {
		push(EOF);
		break;
	    }
	    temp = 0;
	    (void) fflush (stdout);
	    if ( setjmp(read_int) == 0 ) {
		if ( debugon )
		    (void) signal(SIGINT, &end_read);
		char c;
		if (EOFflag)
		    temp = -1;
		else {
		    interact.get(c);
		    if (interact.eof()) {
			EOFflag = TRUE;
			temp = -1;
		    }
		    else
			temp = c & 0xff;
		}
	    } else
		jump = TRUE;
	    if ( debugon )
		(void) signal(SIGINT, &go_debug);
	    push (temp);
	    break;
	case PRINTC: 
	    temp = pop ();
	    if (temp == EOLN) {
		linepos = 0;
		cout << (char)EOLN;
		// putchar (EOLN);
	    }
	    else
		if (temp == EOF) {
		    linepos = 0;
		    cout << "<EOF>" << endl;
		}
		else {
		    linepos++;
		    cout << (char)temp;
		    // putchar ((char) temp);
		}
	    break;
	case TRON: 
	    traceon = TRUE;
	    break;
	case TROFF: 
	    traceon = FALSE;
	    break;
	case DUMP: 
	    temp = pop ();
	    temp1 = pop ();
	    if (okmem (temp) && okmem (temp1) && (temp1 <= temp)) {
		cout << "LOCATION     CONTENTS" << endl;
		for (dumpaddr = temp1; dumpaddr < temp; dumpaddr++)
		    cout << setw(6) << dumpaddr
		         << "       "
			 << setw(6) << mem[dumpaddr]
			 << endl;
	    }
	    else
		aabort (4);
	    break;
	default: 
	    aabort (6);
	    break;
    }				/* end switch */
    go_debug(0);		/* enter the debugger if necessary */

}

void trace (int ppc, char* cp)
{
    int     i,
            j;			/* vars for stack print			 */
    int     instr;

    if (linepos) {
    	cout << endl;		/* print a clean trace line		 */
	linepos = 0;
    }
    cout << cp
         << setiosflags(ios::left)
         << setw(5)
	 << ppc
	 << resetiosflags(ios::left);

    if (okmem (ppc)) {
	instr = mem[ppc];
	if (isok (instr)) {
	    cout << setiosflags(ios::left)
	         << setw(6)
		 << iarray[instr].mnemonic
		 << resetiosflags(ios::left);
	    // printf ("%-6s", iarray[instr].mnemonic);
	    if ((ppc + 1) < MAXMEM && hasaddr (instr))
	        cout << setw(12) << mem[ppc+1];
		// printf (" %11d", mem[ppc + 1]);
	    else
	        cout << setw(12) << " ";
		// printf ("            ");
	}
	else
	    cout << "??????            ";
	    // printf ("??????            ");
    }
    cout << "   SP = " << setw(4) << SP;
    // printf ("  SP = %4d", SP);
    if (SP >= 0 && SP < MAXMEM) {
        cout << "  STACK =";
	// printf ("  STACK =");
	j = MAXMEM - SP;
	if (j > MAXD)
	    j = MAXD;		/* maximum stack print depth	 */
	for (i = 0; i < j; i++) {
	    if (i > 0)
	    	cout << cp
		     << setw(45)
		     << " ";
	    // printf ("%s%43c", cp, ' ');
	    cout << setw(12)
	         << mem[SP + i]
		 << endl;
	    // printf (" %11d\n", mem[SP + i]);
	}
    }
    else
    	cout << endl;
	// putchar ('\n');
}

int dasm (int addr)
{
    int     instr;

    if (okmem (addr))
	instr = mem[addr];
    else
	instr = -1;

 /* output addr and advance to potential op	 */
    cout << setw(4)
         << setiosflags(ios::left)
         << addr
	 << resetiosflags(ios::left)
	 << ((addr==pc)?'<':' ');
    // printf ("%4d%c", addr, (addr==pc)?'<':' ');
    addr++;

    if ( isok(instr) ) {
    	cout << setiosflags(ios::left)
 	     << setw(6)
	     << iarray[instr].mnemonic
	     << resetiosflags(ios::left);
	// printf ("%-6s", iarray[instr].mnemonic);
	if (hasaddr (instr))
	    cout << setw(12) << mem[addr++];
	    // printf (" %11d", mem[addr++]);
    }
    else {
    	cout << "?? " << setw(11) << instr;
	// printf ("?? %11d",instr);
	if ( (instr>=32) && (instr<127) ) /* is it printable? */
	    cout << (char)instr;
	    // printf(" (%c)",instr);
    }

    cout << endl;
    // putchar ('\n');

    return (addr);		/* return address of the next instruction */
}
