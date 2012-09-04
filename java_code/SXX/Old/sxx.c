/************************************************************************/
/*									*/
/*	Extended Stack machine emulator					*/
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

#include	<stdio.h>
#include	<stdlib.h>
#include	<string.h>
#include	<ctype.h>
#include	<signal.h>
#include	<setjmp.h>

#define PROG	progname	/* program name				*/

#define	EOLN	'\n'		/* stdio value for end of line 		*/

#define MAXINT	2147483647	/* maximum integer value		*/
#define	MAXMEM	16384		/* maximum memory location of 4K memory	*/
#define MINOFFSET	16	/* minimum program offset into memory	*/
#define MAXOFFSET	128	/* maximum program offset into memory	*/
#define SP	mem[0]		/* value of stack pointer		*/
#define	TRUE	1
#define FALSE	0
#define BFSZ	256		/* interactive input buffer size	 */
#define MAXD	4		/* maximum stack print depth		 */

#define DSCAN(x)	dscan((&x))

int     mem[MAXMEM];		/* machine memory - 0..MAXMEM-1		 */
int     pc;			/* Program Counter			 */
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

FILE * obj;			/* input object file			 */
FILE * interact;		/* interactive input file		 */

struct instruction {
    int     nargs;
    char   *mnemonic;
};

struct instruction  iarray[] = {/* holds mnemonic name of op code	 */

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

    0, "return",
#define	RETURN	23		/* PC=pop() */

    1, "retn",
#define RETN	24		/* temp=pop(); SP=SP+value; PC=temp */

    0, "halt",
#define	HALT	25		/* halt program execution */

    0, "add",
#define	ADD	26		/* temp=pop(); push( pop()+temp ) */

    0, "sub",
#define	SUB	27		/* temp=pop(); push( pop()-temp ) */

    0, "mul",
#define	MUL	28		/* temp=pop(); push( pop()*temp ) */

    0, "div",
#define	DIV	29		/* temp=pop(); push( pop()/temp ) */

    0, "mod",
#define MOD	30		/* temp=pop(); push( pop()%temp ) */

    0, "or",
#define	OR	31		/* temp=pop(); push( pop() || temp ) */

    0, "and",
#define	AND	32		/* temp=pop(); push( pop() && temp ) */

    0, "xor",
#define	XOR	33		/* temp=pop(); push( pop() xor temp ) */

    0, "not",
#define	NOT	34		/* push( !pop() ) */

    0, "neg",
#define NEG	35		/* push( -pop() ) */

    1, "addx",
#define ADDX	36		/* temp=pop(); *addr=*addr+temp */

    1, "addsp",
#define ADDSP	37		/* SP=SP+value */

    0, "read",
#define	READ	38		/* read temp in %d format; push(temp) */

    0, "print",
#define	PRINT	39		/* print pop() in %d format */

    0, "readc",
#define	READC	40		/* read temp in %c format; push(temp) */

    0, "printc",
#define	PRINTC	41		/* print pop() in %c format */

    0, "tron",
#define	TRON	42		/* turn trace feature */

    0, "troff",
#define	TROFF	43		/* turn off trace feature */

    0, "dump",
#define	DUMP	44		/* temp=pop(); dump memory from pop() to temp 
				*/

#define MAXINSTR	45	/* last instruction index + 1		 */
};

#define hasaddr(i)	( iarray[i].nargs > 0 )
#define isok(i)		( (i) >= 0 && (i) < MAXINSTR && iarray[i].nargs >= 0 )
#define okmem(x)	( ((x) >= 0) && ((x) < MAXMEM) )

main (argc, argv)
int     argc;
char   **argv;
{
    do_args (argc, argv);

    initialize ();

    if (!debugon)
	em ();
    if (haltflag == FALSE)
	debug ();

    if ( banner )
       printf ("\n** Terminating... **\n");
    exit(0);

}

do_args (argc, argv)
int     argc;
char   **argv;
{
    char *cp;

    obj = stdin;		/* input file is initially stdin	 */
    interact = stdin;		/* interactive input is too		 */
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
		    fprintf (stderr,
			    "%s: %d: illegal offset\n",
			    PROG,
			    offset);
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
	if ((obj = fopen (*argv, "r")) == NULL) {
	    perror (*argv);
	    exit (1);
	}
    }
    else {
	if ((interact = fopen ("/dev/tty", "r")) == NULL) {
	    perror ("tty");
	    exit (1);
	}
    }
}

usage () {
    printf ("Usage: %s [-B] [-d] [-t] [-b<baseaddr>] [file]\n", PROG);
    exit (1);
}

#define ERR_VAL		-2
#define EOF_VAL		-1
#define INT_VAL		0
#define LC_VAL		1

#define skip_whitespace()	while (c == ' ' || c == '\t') c = getc(obj)
#define skip_to_eol()		while (c != '\n' && c != EOF) c = getc(obj)

/* dscan -- scan for integer in input stream */
static
int
dscan(ip)
int	*ip;
{
	int	i;			/* accumulated integer value */
	int	c;			/* character from input obj file */
	int	sign = 0;		/* nonzero if '-' is present */
	int	ret = INT_VAL;		/* return value */

loop:
	c = getc(obj);
	skip_whitespace();
	if (c == '#') {
		skip_to_eol();
		if (c == EOF)
			return EOF_VAL;
		goto loop;
	}
	if (c == EOF)
		return(EOF_VAL);
	ret = 0;
	if (c == ':') {
		c = getc(obj);
		if (c == ':') {
			skip_to_eol();
			goto loop;
		}
		ret = LC_VAL;
		skip_whitespace();
	} else if (c == '-') {
		sign++;
		c = getc(obj);
	}
	if (isdigit(c) == 0)
		return(ERR_VAL);
	for (i=0 ; isdigit(c) ; c=getc(obj)) {
		c -= '0';		/* convert to internal value */
		if (i > MAXINT/10)
			return(ERR_VAL);
		i *= 10;
		if (i > MAXINT - c)
			return(ERR_VAL);
		i += c;
	}
	if (sign)
		i = -i;
	*ip = i;
	skip_to_eol();
	if (ret == LC_VAL && i == 0)
		goto loop;
	return(ret);
}

initialize () {
    int     lc;			/* used to load program			 */
    int     i;
    int     length;		/* of the program			 */
    extern  putmem ();
    extern int  getmem ();
    long    ltime;

    if ( banner ) {
        /*   print banner   */
        printf ("\n");
        printf ("****************************\n");
        printf ("** Stack Machine Emulator **\n");
        printf ("****************************\n\n");
    }

 /*  initialize memory to zeroes					 */
    for (i = 0; i < MAXMEM; i++)
	mem[i] = 0;

 /*  set the stack pointer to an impossible location		 */
    SP = MAXMEM;

 /*  determine where to relocate the program			 */
    if (offset == 0) {
	(void) time (&ltime);
	offset = ltime % (MAXOFFSET - MINOFFSET + 1);
	offset += MINOFFSET;
    }
    pc = offset;
    pstart = offset;		/* save program/data start address	 */

 /*  read the object into memory					 */
    if ( banner ) {
        printf ("** Loading...     **\n");
        printf ("** Origin = %d  **\n", offset);
    }
    if (DSCAN (length) != INT_VAL || length < 0) {
	fprintf (stderr, "%s: improper object module format\n", PROG);
	exit (1);
    }
    if (length > MAXMEM) {
	fprintf (stderr, "%s: object module too large\n", PROG);
	exit (1);
    }
#ifdef DEBUG
    printf ("length=%d\n", length);
#endif
    lc = 0;
    addr = offset;
    while (lc < length) {
	if (addr >= MAXMEM) {
	    fprintf (stderr, "%s: object module too large\n", PROG);
	    exit (1);
	}
	i = DSCAN(temp);
	if (i == LC_VAL) {
		if (temp == 0)
			continue;
		if (temp > MAXMEM - addr) {
		    fprintf (stderr, "%s: object module too large\n", PROG);
		    exit (1);
		}
		if (temp > length - lc) {
		    fprintf (stderr, "%s: improper object module format\n",
			    PROG);
		    exit(1);
		}
		addr += temp;
		lc += temp;
#ifdef DEBUG
		printf ("lc=%d addr=%d\n", lc, addr);
#endif
		continue;
	}
	if (i == INT_VAL) {
#ifdef DEBUG
		printf ("lc=%d addr=%d value=%d\n", lc, addr, temp);
#endif
		putmem (addr, temp);
		lc++, addr++;
		continue;
	}
	fprintf (stderr, "%s: improper object module format\n", PROG);
	exit (1);
    }
    pend = pstart + length;	/* save program/data end address	 */

 /*  reset relocatable addresses  				 */
    while ((i = DSCAN (addr)) != EOF_VAL) {
#ifdef DEBUG
	printf ("i=%d\n", i);
#endif
	if (i != INT_VAL) {
	    fprintf (stderr, "%s: improper object module format\n", PROG);
	    exit (1);
	}
	if (addr < 0 || addr >= length) {
	    fprintf (stderr, "%s: %d: illegal relocation address\n",
		PROG,
		addr);
	    exit (1);
	}
#ifdef DEBUG
	printf ("addr=%d\n", addr);
#endif
	addr += pstart;
	/* double-check address */
	if (!okmem (addr)) {
	    fprintf (stderr, "%s: %d: illegal relocation address\n",
		PROG,
		addr);
	    exit (1);
	}
	putmem (addr, getmem (addr) + pstart);
    }
    if ( banner )
	printf ("** Executing...   **\n\n");
}

em ()
{				/* run the program til completion */
    void go_debug();

    if ( setjmp(jbuf)==0 ) {
	if ( debugon )
	    signal (SIGINT, go_debug);
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
void
go_debug ()
{
    if ( jump )
    	longjmp(jbuf,1);
    else
	jump = TRUE;
}

/* read interrupt SIGINT interrupt handler */
void
end_read()
{
    longjmp(read_int,1);
}

debug () {
    char    com[BFSZ];
    int     mode;
    int     dotrace = FALSE;
    int     i,
            beg,
            end,
            addr,
            value;

#define	PC_ADDR		0
#define	STEP_CT		1
#define	MEM_VAL		2
#define MEM_NVAL	3

#define DEBUG_ERR(str)	printf("?? %s - key '?' for help\n",str)

    (void) setjmp(jbuf);
    jump = TRUE;
    (void) signal(SIGINT,go_debug);
    printf ("\n");
    while (haltflag == FALSE) {
	trace (pc, "");
	printf ("> ");
	(void) fflush (stdout);
	dotrace = FALSE;
	if (fgets (com, BFSZ, interact) == NULL) {
		printf("\n");
		exit(1);
	}
	switch (com[0]) {

            case 'm':
		if (til (com + 1, &mode, &addr, &value) || mode != MEM_VAL ) {
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
		if ( val(com+1, &addr) || !okmem(addr) ) {
		    DEBUG_ERR("Illegal address");
		    break;
		}
		pc = addr;
		break;
	    case 't': 
		dotrace = TRUE;
	    case 'g': 
		if (til (com + 1, &mode, &addr, &value)) {
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
			while ( (value-- > 0) && !haltflag );
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
		putchar ('\n');
		break;

	    case 's': 
		dexec (FALSE);
		break;

	    case 'l': 
		for (i = pstart; i < pend; )
		    i = dasm (i);
		putchar ('\n');
		break;

	    case 'u': 
		if (range (com + 1, &beg, &end) ||
			!okmem (beg) || !okmem (end)) {
		    DEBUG_ERR("illegal range specification");
		    break;
		}
		for (i = beg; i <= end; )
		    i = dasm (i);
		putchar ('\n');
		break;

	    case 'd': 
		if (range (com + 1, &beg, &end) ||
			!okmem (beg) || !okmem (end)) {
		    DEBUG_ERR("illegal range specification");
		    break;
		}
		for (i = beg; i <= end; i++ )
		    printf ("   mem[%4d] = %15d\n", i, mem[i]);
		putchar ('\n');
		break;

	    case 'p': 
		while ( !haltflag ) {
		    if (traceon)
			trace (pc, "");
		    do_exec ();
		}
		putchar ('\n');
		break;

	    case '\n': 
		break;

	    case '?': 
		printf ("\n\nCommands :\n\n");
		printf ("l        - List load module\n");
		printf ("s        - Single step\n");
		printf ("m<addr>=<value>\n");
		printf ("         - modify memory\n");
		printf ("i<val>   - Set PC to val\n");
		printf ("p        - proceed\n");
		printf ("g<til>   - Go until\n");
		printf ("t<til>   - Trace until\n");
		printf ("u<range> - Unassemble memory\n");
		printf ("d<range> - Display memory\n");
		printf ("q        - quit\n");
		putchar ('\n');
		printf ("<range> ::= <start> - <end>\n");
		printf ("          | <start> , <count>\n");
		putchar ('\n');
		printf ("<til>   ::=        - <addr>\n");
		printf ("          |        , <count>\n");
		printf ("          | <addr> = <value>\n");
		printf ("          | <addr> ! <value>\n");
		putchar ('\n');
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

dexec (t)
int     t;
{
    if (t)
	trace (pc, "        ");
    do_exec ();
}

range (str, beg, end)
char    str[];
int    *beg,
       *end;
{
    int     temp;
    char    c;

    if (sscanf (str, "%d %[-,] %d", beg, &c, &temp) != 3)
	return (-1);
    switch (c) {
	case '-': 
	    *end = temp;
	    break;
	case ',': 
	    *end = *beg + temp - 1;
	    break;
	default: 
	    return (-1);
    }
    return (0);
}

val (str, addr)
char str[];
int *addr;
{
    if ( sscanf (str, "%d",addr) != 1 )
	return (-1);
    return(0);
}

til (str, mode, addr, value)
char    str[];
int    *mode;
int    *addr,
       *value;
{
    int     i;
    char   *cp,
            c;

    for (cp = str; *cp; cp++)
	if (*cp == '-')		/* replace '-' with '<' to make scanf happy */
	    *cp = '<';
    i = sscanf (str, "%d %[<=,!] %d", addr, &c, value);
    switch (c) {
	case '<': 
	    if (i != 2)
		return (-1);
	    *mode = PC_ADDR;
	    break;
	case ',': 
	    if (i != 2)
		return (-1);
	    *mode = STEP_CT;
	    break;
	case '=': 
	    if (i != 3)
		return (-1);
	    *mode = MEM_VAL;
	    break;
	case '!': 
	    if (i != 3)
		return (-1);
	    *mode = MEM_NVAL;
	    break;
	default: 
	    return (-1);
    }
    return (0);
}

aabort (code)
int     code;
{
    char *cp;

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
    printf ("\n** ERROR: %s **\n",cp);
    go_debug();
}

putmem (addr, value)
int     addr,
        value;
{
    if (okmem (addr))
	mem[addr] = value;
    else
	aabort (2);
}

int     getmem (addr)
int     addr;
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

fetchop () {			/* fetch the next operand from memory	 */
    int     word;
 /*  check for invalid Program Counter				 */
    if (okmem (pc) == 0)
	aabort (5);
    word = mem[pc];
    pc++;
    return (word);
}

testsp ()
{
    if ( !okmem(SP) ) 
	aabort (3);
}

push (value)
int     value;
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

do_exec ()
{
    int     i;
    void    go_debug(),end_read();

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
	    push( (pop() < 0) ? 1 : 0 );
	    break;
	case TSTLE: 
	    push( (pop() <= 0) ? 1 : 0 );
	    break;
	case TSTGT: 
	    push( (pop() > 0) ? 1 : 0 );
	    break;
	case TSTGE: 
	    push( (pop() >= 0) ? 1 : 0 );
	    break;
	case TSTEQ:
	    push( (pop() == 0) ? 1 : 0 );
	    break;
	case TSTNE:
	    push( (pop() != 0) ? 1 : 0 );
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
	    push ((pop () || temp) ? 1 : 0);
	    break;
	case AND: 
	    temp = pop ();
	    push ((pop () && temp) ? 1 : 0);
	    break;
	case XOR:
	    temp = pop ();
	    temp1 = pop ();
	    push ((!(temp && temp1) && (temp || temp1)) ? 1 : 0);
	    break;
	case NOT: 
	    push (pop () ? 0 : 1);
	    break;
	case NEG: 
	    push (-pop ());
	    break;
	case ADDX: 
	    temp = pop ();
	    putmem (addr, getmem (addr) + temp);
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
		    (void) signal(SIGINT,end_read);
	        if ((i = fscanf (interact, "%d", &temp)) == EOF) {
		    EOFflag = TRUE;
	            aabort (7);
		}
	        else if (i != 1)
	            aabort (8);
	    } else
		jump = TRUE;
	    if ( debugon )
		(void) signal(SIGINT,go_debug);
	    push (temp);
	    break;
	case PRINT: 
	    printf ("%d", pop ());
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
		    (void) signal(SIGINT,end_read);
	        if ((temp = fgetc (interact)) == EOF)
		    EOFflag = TRUE;
	    } else
		jump = TRUE;
	    if ( debugon )
		(void) signal(SIGINT,go_debug);
	    push (temp);
	    break;
	case PRINTC: 
	    temp = pop ();
	    if (temp == EOLN) {
		linepos = 0;
		putchar (EOLN);
	    }
	    else
		if (temp == EOF) {
		    linepos = 0;
		    printf ("<EOF>\n");
		}
		else {
		    linepos++;
		    putchar ((char) temp);
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
		printf ("LOCATION     CONTENTS\n");
		for (dumpaddr = temp1; dumpaddr < temp; dumpaddr++)
		    printf ("%6d       %6d\n", dumpaddr, mem[dumpaddr]);
	    }
	    else
		aabort (4);
	    break;
	default: 
	    aabort (6);
	    break;
    }				/* end switch */
    go_debug();			/* enter the debugger if necessary */

}

trace (ppc, cp)
int     ppc;
char   *cp;			/* string to prefix printing of trace lines
				    */
{
    int     i,
            j;			/* vars for stack print			 */
    int     instr;

    if (linepos) {
	printf ("\n");		/* print a clean trace line		 */
	linepos = 0;
    }
    printf ("%s%4d ", cp, ppc);

    if (okmem (ppc)) {
	instr = mem[ppc];
	if (isok (instr)) {
	    printf ("%-6s", iarray[instr].mnemonic);
	    if ((ppc + 1) < MAXMEM && hasaddr (instr))
		printf (" %11d", mem[ppc + 1]);
	    else
		printf ("            ");
	}
	else
	    printf ("??????            ");
    }
    printf ("  SP = %4d", SP);
    if (SP >= 0 && SP < MAXMEM) {
	printf ("  STACK =");
	j = MAXMEM - SP;
	if (j > MAXD)
	    j = MAXD;		/* maximum stack print depth	 */
	for (i = 0; i < j; i++) {
	    if (i > 0)
		printf ("%s%43c", cp, ' ');
	    printf (" %11d\n", mem[SP + i]);
	}
    }
    else
	putchar ('\n');
}

int
dasm (addr)
int     addr;
{
    int     instr;

    if (okmem (addr))
	instr = mem[addr];
    else
	instr = -1;

 /* output addr and advance to potential op	 */
    printf ("%4d%c", addr, (addr==pc)?'<':' ');
    addr++;

    if ( isok(instr) ) {
	printf ("%-6s", iarray[instr].mnemonic);
	if (hasaddr (instr))
	    printf (" %11d", mem[addr++]);
    }
    else {
	printf ("?? %11d",instr);
	if ( (instr>=32) && (instr<127) ) /* is it printable? */
	    printf(" (%c)",instr);
    }

    putchar ('\n');

    return (addr);		/* return address of the next instruction */
}
