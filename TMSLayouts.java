package com.ncast.tms;

import java.io.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.awt.*;
import java.awt.event.*;
import java.util.Properties;    

/**
 * TMSLayouts - The Telepresenter Management System Layouts Controller.
 * The Layouts Controller allows you to select different window layouts.
 */

public class TMSLayouts extends WindowAdapter implements ActionListener {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    
    private Properties tmsLayoutsProps;
    private String system;
    private TmsLog l;
    private TmsTelnet t;
    private String site;
    private int port;
    private String password;
    private Boolean debug;
    private Boolean quiet;
    private Boolean log;
    private int columns;
    private int rows;
    private Boolean PRHD = false;
    private double swRevision = 0.0;
    private static String arg0 = "";
    private Boolean firstStatus = true;
    
    private JFrame frame;
    private JPanel boxPane;
    private JPanel tmsPane;
    private JPanel entryPane;
    private JButton entryButton;
    private String entryString = "Command";
    private JTextField entryText;
    
    private void createTMSLayouts() {
    
    	system = System.getProperty("os.name");
        
		try {
		  // create and load default properties
			Properties defaultTMSLayoutsProps = new Properties();
			FileInputStream in = new FileInputStream("tmsLayouts_default.properties");
			defaultTMSLayoutsProps.load(in);
			in.close();
			
			// create application properties with default
			tmsLayoutsProps = new Properties(defaultTMSLayoutsProps);

			// now load properties from last invocation
			in = new FileInputStream("tmsLayouts.properties");
			tmsLayoutsProps.load(in);
			in.close();
		} catch (FileNotFoundException fnf) {
		  System.err.println("TMSLayouts Properties file not found error: " + fnf.getMessage());
		  //return;
		} catch (IOException ioe) {
		  System.err.println("TMSLayouts Properties system error: " + ioe.getMessage());
		  return;
		}
		
		site = tmsLayoutsProps.getProperty("site", "");
		if (arg0.length() > 0) {
		  site = arg0;
		  tmsLayoutsProps.setProperty("site", site);
		} else {
		  if (site.length() == 0) {
		    JFrame ipframe = new JFrame("Recorder IP Address Information Missing");
		    String s = (String)JOptionPane.showInputDialog(
		            ipframe,
		            "Enter the IP address or DNS name of the Unit",
		            "Request for IP Address or DNS Name",
		            JOptionPane.QUESTION_MESSAGE,
		            null,
		            null,
		    "address");
		    //If a string was returned, setup site value.
		    if ((s != null) && (s.length() > 0) && (!s.equals("address"))) {
		      site = s;
		      tmsLayoutsProps.setProperty("site",site);
		    } else {
		      //If here, the return value was null/empty.
		      JOptionPane.showMessageDialog(
		              frame,
		              "There was an error in the IP address",
		              "Unit IP Address Error",
		              JOptionPane.ERROR_MESSAGE
		      );
		      System.err.println("Usage: java TMSLayouts site");
		      System.exit(-1);
		    }
		  }
		}


		String port_str = tmsLayoutsProps.getProperty("port");
		password = tmsLayoutsProps.getProperty("password");
		String debug_str = tmsLayoutsProps.getProperty("debug");
		String quiet_str = tmsLayoutsProps.getProperty("quiet");
		String log_str = tmsLayoutsProps.getProperty("log");
		String columns_str = tmsLayoutsProps.getProperty("columns");
		String rows_str = tmsLayoutsProps.getProperty("rows");
        
		port = Integer.parseInt(port_str);
		debug = debug_str.equals("true");
		quiet = quiet_str.equals("true");
		log = log_str.equals("true");
		columns = Integer.parseInt(columns_str);
		rows = Integer.parseInt(rows_str);

		// Setup a timer
		int delay = 1000; //milliseconds
		ActionListener taskPerformer = new ActionListener() {
		  public void actionPerformed(ActionEvent evt) {
		    //...Perform a task...
		    t.getStatus();
		    updateTMSLayouts();
		  }
		};
  		
		new Timer(delay, taskPerformer).start();
 
		BufferedWriter out = null;
		if (log) {
		  try {
		    FileWriter log = new FileWriter("tms.log");
		    out  = new BufferedWriter(log);
		  } catch (IOException ioe) {
		    System.err.println("TMSLayouts FileWriter system error.");
		    return;
		  }
		}

		l = new TmsLog(out, log, quiet);
  
		l.log("TMSLayouts", "Logger initialized");
		l.log("TMSLayouts", "Operating system: " + system);
		l.log("TMSLayouts", "Properties: " + tmsLayoutsProps.toString());
    	
		t = new TmsTelnet(site, port, password);
		t.tms = l;
		t.debug = debug;
		if (! t.initialize()) {
		  JOptionPane.showMessageDialog(
		          frame,
		          "There was an error in attempting to connect with unit " + site,
		          "Connection Error",
		          JOptionPane.ERROR_MESSAGE
		  );
		  System.err.println("TMSLayouts Connection initialization error for site: " + site);
		  return;
		}
    	
		// Get initial status
		t.getStatus();
		// Get information about the system we are connected to
		t.getSystemInfo();
      
		PRHD = t.serialNumber.startsWith("P");
		if (t.softwareRevision.length() >= 3) {
		  try
		  {      
		    swRevision = Double.parseDouble(t.softwareRevision.substring(0,3));
		  }
		  catch (NumberFormatException nfe)
		  {
		    System.err.println("TMSLayouts software Revision Number Format Exception " + nfe.getMessage());
		  }
		};

		if (PRHD) {
		  frame = new JFrame("PRHD - " + site);
		} else {
		  frame = new JFrame("Telepresenter - " + site);
		}
      
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener((WindowListener)this);
          
		boxPane = new JPanel();
		boxPane.setLayout(new BoxLayout(boxPane, BoxLayout.PAGE_AXIS));
		frame.setContentPane(boxPane);

		int numberOfRows = rows;
		int numberOfCols = columns;
          
		tmsPane = new JPanel(new GridLayout(numberOfRows, numberOfCols, 5, 5));
		tmsPane.setMinimumSize(new Dimension(75, 20));
		boxPane.add(tmsPane);

       
		// Create some event buttons
          
		Dimension bPref = new Dimension(140, 30);
		Dimension bMax = new Dimension(200, 50);
		Dimension bMin = new Dimension(100, 15);
          
		// Add Event buttons
		for (int k = 1; k <= numberOfRows; k++) {
		  for (int i = 1; i <= numberOfCols; i++) {
		    String buttonNumber = "button-" + String.valueOf(k) + "." + String.valueOf(i);
		    String button_str = tmsLayoutsProps.getProperty(buttonNumber,buttonNumber);
		    JButton eventButton = anotherButton(tmsPane, button_str, bPref, bMax, bMin, button_str, buttonNumber);
              
		  }
		}
      
		entryPane = new JPanel();
		entryPane.setLayout(new BoxLayout(entryPane, BoxLayout.LINE_AXIS));
		boxPane.add(entryPane);
		entryButton = anotherButton(entryPane, entryString, bPref, bMax, bMin, entryString, entryString);
		entryPane.add(entryButton);
		entryText = new JTextField();
		entryText.setEditable(true);
		entryText.setBorder(BorderFactory.createLineBorder(Color.black));
		entryPane.add(entryText);
          
		//Display the window.
		frame.pack();
		frame.setVisible(true);
          
    }
    
    // Create button routine
    
    private JButton anotherButton(JPanel pnl, String label, Dimension pref, Dimension max, Dimension min, String tooltip, String action) {
    	JButton button = new JButton(label);
    	button.setPreferredSize(pref);
      button.setMaximumSize(max);
      button.setMinimumSize(min);
      button.setAlignmentX(Component.CENTER_ALIGNMENT);
      button.setToolTipText(tooltip);
      button.setActionCommand(action);
      button.addActionListener(this);
      button.setFocusable(false);
      //button.setOpaque(false);
      pnl.add(button);
      return button;
    }
 
    private void updateTMSLayouts() {
      
      String model;
      
      if (PRHD) {
        model = "PR-HD";
      } else {
        model = "Telepresenter";
      }
    	
      if (t.recording.equals("1")) {
        frame.setTitle(model + " - " + site + " - " + t.typeRole);
      } else {
        frame.setTitle(model + " - " + site + " - " + t.typeRole + " - Not recording!");
      }
      
      firstStatus = false;
      
    }
    
    public void windowClosing(WindowEvent e) {
    	exitTMSLayouts();
    	return;
    }

    private void exitTMSLayouts() {
      t.disconnect();
      l.log("TMSLayouts", "Connection closed to site " + site);
      try {
    		FileOutputStream out = new FileOutputStream("tmsLayouts.properties");
    		tmsLayoutsProps.store(out, site);
    		out.close();
    	} catch (IOException ioe) {
    	  System.err.println("TMSLayouts Properties output error." + ioe.getMessage());
    	  return;
    	}
    
    }
    
    public void actionPerformed(ActionEvent e) {
    	String action = e.getActionCommand();
    	String layoutCommand = "";
    	
    	if (action == entryString) {
    	  layoutCommand = entryText.getText();
    	} else {
    	  layoutCommand = tmsLayoutsProps.getProperty(action + "-command","");
    	}
    	
      t.sendCommand(layoutCommand.replace(";", "\n"));
      l.log("TMSLayouts", "Command: " + layoutCommand);
      return;

	}
 
 
   	private static void startTMSLayouts() {
        TMSLayouts tmsp = new TMSLayouts();
        tmsp.createTMSLayouts();
    }
    
    public static void main(String[] args) {
    	
    	if (args.length > 0) {
    		arg0 = args[0];
    	}

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startTMSLayouts();
            }
        });
    }
}
