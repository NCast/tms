package com.ncast.tms;

import java.io.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * TmsLog - The Telepresenter Management System logging facility.
 * The Telepresenter Management System logger allows for unified printing of
 * all system and debug messages during run-time. The facility provides for
 * time & date stamps, source of the message and the message itself.
 */

public class TmsLog
{
	BufferedWriter log;
	Boolean logfile;
	Boolean quiet;
	
	public TmsLog(BufferedWriter out, Boolean lf, Boolean q) {
		log = out;
		logfile = lf;
		quiet = q;
	}
	
	public void log (String module, String message) {
		
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
		String local = sdf.format(cal.getTime());

		if (!quiet) {
			System.out.println(local + " (" + module + ") " + message);
			System.out.flush();
		}
		
		if (logfile) {
			try {
				log.write(local + " (" + module + ") " + message + "\n");
				log.flush();
			} catch (IOException e) {
				System.err.println("TMS logfile I/O error: " + e.getMessage());
			}
		}
	}
}
