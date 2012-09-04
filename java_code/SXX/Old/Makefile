# makefile for the stack machine emulator sxx

DESTDIR= ../bin
CPPFLAGS = -O

sxx:	sxx.o
	g++ -o sxx sxx.o

install: sxx
	install sxx $(DESTDIR)

clean:
	rm -f sxx *.o core

sxx.o:	sxx.cc
	g++ -c sxx.cc
