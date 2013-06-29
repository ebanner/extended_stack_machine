eXtended Stack Machine
======================

The eXtended Stack Machine is a simplified CPU architecture created by Timothy
V. Fossum for educational purposes. For details about the eXtended Stack
Machine, visit Dr. Fossum's man page in the doc/ directory of this project.

Provided here is an implemention of the eXtended Stack Machine with a GUI front
end built with the Java Swing widget toolkit. Such a front end provides students
with the ability to step through and examine a running program.

Running a Program
=================

Traverse into the `jar` directory and execute the following command:

    $ java -jar SM.jar

You can then load one of the prewritten programs in the `test` directory. The
eXtended Stack Machine will recognize **.sxx** programs only.

Known Bugs
==========

When a program is finished executing and you want to run another program, you
must kill the entire XSM program and start it over. This issue does not look
like it will be fixed in the foreseeable future.

Screenshots
================================

![XSM in action](https://raw.github.com/ebanner/extended_stack_machine/master/img/xsm.png "Screenshot")

Credit
======

Credit goes to Dr. Timothy V. Fossum for giving birth to the eXtended Stack
Machine. If not for him, such a creation would not exist.

Authors
=======
Edward Banner

Dr. Fossum
