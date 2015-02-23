Notes on the Telepresenter Management System - 2010-10-25 - Rev. 1.3.0

There are three main files in this package:

 TMS.java --

 	Builds the GUI interface and updates status

 TmsLog.java --

	A diagnostic logging facility with controls to turn on various logging functions

 TmsTelnet.java --

 	Classes and functions to send and receive serial commands to the Telepresenter or Presentation Recorder

Activity is initiated in TMS.java. A short description of this activity is as follows:


	 1. The main TMS class initializes startup variables from the default properties file.

	 2. The GUI is built button by button based on the state of the layout variable "layout".
	    Different settings of this variable will cause different layouts to be constructed.

	 3. Once the GUI is constructed and all buttons are wired for action, a timer is started.
	    This timer ticks every second. All changes to the state of the buttons are a result of the timer.

	 4. After initializing the timer, logging is started, a few startup messages are printed,
	    and the main routine exits.

	 5. On each timer tick a serial command is sent to the Telepresenter to retrieve status.
	    Also, a serial command is sent to retrieve the current audio meter levels.

	 6. When the status calls have completed, the TMS GUI is updated to reflect all the status variables
     	    which have been returned by the serial commands.

	 7. If any button is activated on the interface, a serial command is sent to the Telepresenter to
  	    reflect that command.

For example, consider the sequence of events if the user presses the Record Start button:

    	  1. The Record Start button is pressed and this results in an R1 serial command being sent to the unit.

	  2. Some time later, the timer ticks and a status command is sent out.

	  3. The status is received and all status variables are updated.

	  4. The state of the Record button on the interface is changed based on the state of the new status variable.

The point of this example is to explain that the action of pressing a button and then changing its status are completely
independent and decoupled. Changes in status on the GUI interface will always reflect the state of the unit (which
can be controlled from the web interface or RS-232 serial line as well as these IP Telnet commands). Pressing one
of the buttons on the interface simply sends out the corresponding command and nothing more.

If you have any questions, please contact: info@ncast.com

