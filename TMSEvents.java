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
 * TMSEvents - The Telepresenter Management System Events Generator.
 * The Events Generator provides for creation of TMS events.
 */

public class TMSEvents extends WindowAdapter implements ActionListener {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    
    private Properties tmsEventsProps;
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
    private static String arg0 = "";
    private Boolean firstStatus = true;
    
    private JFrame frame;
    private JPanel boxPane;
    private JPanel tmsPane;
    private JPanel entryPane;
    private JButton entryButton;
    private String entryString = "Comment";
    private JTextField entryText;
    
    private void createTMSEvents() {
    
    	system = System.getProperty("os.name");
        
		try {
		   	// create and load default properties
			Properties defaultTMSEventsProps = new Properties();
			FileInputStream in = new FileInputStream("tmsEvents_default.properties");
			defaultTMSEventsProps.load(in);
			in.close();
			
			// create application properties with default
			tmsEventsProps = new Properties(defaultTMSEventsProps);

			// now load properties from last invocation
			in = new FileInputStream("tmsEvents.properties");
			tmsEventsProps.load(in);
			in.close();
        } catch (FileNotFoundException fnf) {
            System.err.println("TMSEvents Properties file not found error: " + fnf.getMessage());
     		//return;
        } catch (IOException ioe) {
            System.err.println("TMSEvents Properties system error: " + ioe.getMessage());
           	return;
        }
        
        site = tmsEventsProps.getProperty("site", "");
        if (arg0.length() > 0) {
        	site = arg0;
        	tmsEventsProps.setProperty("site", site);
        } else {
        	if (site.length() == 0) {
       		System.err.println("Usage: java TMSEvents site");
    		System.exit(-1);
        	}
        }
        String port_str = tmsEventsProps.getProperty("port");
        password = tmsEventsProps.getProperty("password");
        String debug_str = tmsEventsProps.getProperty("debug");
        String quiet_str = tmsEventsProps.getProperty("quiet");
        String log_str = tmsEventsProps.getProperty("log");
        String columns_str = tmsEventsProps.getProperty("columns");
        String rows_str = tmsEventsProps.getProperty("rows");
        
        port = Integer.parseInt(port_str);
        debug = debug_str.equals("true");
        quiet = quiet_str.equals("true");
        log = log_str.equals("true");
        columns = Integer.parseInt(columns_str);
        rows = Integer.parseInt(rows_str);

        frame = new JFrame("Telepresenter - " + site);
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
            String button_str = tmsEventsProps.getProperty(buttonNumber,buttonNumber);
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
        
        // Setup a timer
        int delay = 1000; //milliseconds
  		ActionListener taskPerformer = new ActionListener() {
      		public void actionPerformed(ActionEvent evt) {
          	//...Perform a task...
          	t.getStatus();
          	updateTMSEvents();
      		}
  		};
  		
  		new Timer(delay, taskPerformer).start();
 
		BufferedWriter out = null;
  		if (log) {
  			try {
  				FileWriter log = new FileWriter("tms.log");
  				out  = new BufferedWriter(log);
  			} catch (IOException ioe) {
  				System.err.println("TMSEvents FileWriter system error.");
  				return;
  			}
  		}

     	l = new TmsLog(out, log, quiet);
  
    	l.log("TMSEvents", "Logger initialized");
      l.log("TMSEvents", "Operating system: " + system);
    	l.log("TMSEvents", "Properties: " + tmsEventsProps.toString());
    	
      t = new TmsTelnet(site, port, password);
      t.tms = l;
    	t.debug = debug;
    	if (! t.initialize()) {
    		System.err.println("TMSEvents Connection initialization error for site: " + site);
    		return;
    	}
 
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
 
    private void updateTMSEvents() {
    	
      if (t.recording.equals("1")) {
        frame.setTitle("Telepresenter - " + site + " - " + t.typeRole);
      } else {
        frame.setTitle("Telepresenter - " + site + " - " + t.typeRole + " - Not recording!");
      }
      firstStatus = false;
      
    }
    
    public void windowClosing(WindowEvent e) {
    	exitTMSEvents();
    	return;
    }

    private void exitTMSEvents() {
       	t.disconnect();
    	l.log("TMSEvents", "Connection closed to site " + site);
    	try {
    		FileOutputStream out = new FileOutputStream("tmsEvents.properties");
			tmsEventsProps.store(out, site);
			out.close();
        } catch (IOException ioe) {
            System.err.println("TMSEvents Properties output error." + ioe.getMessage());
           	return;
       	}
    
    }
    
    public void actionPerformed(ActionEvent e) {
    	String action = e.getActionCommand();
    	String subtitle = "";
    	
    	if (action == entryString) {
    	  subtitle = entryText.getText();
    	} else {
    	  subtitle = tmsEventsProps.getProperty(action,action);
    	}
    	
      t.setSubtitleWithoutDuration(subtitle);
      l.log("TMSEvents", "Subtitle: " + subtitle);
      return;

	}
 
 
   	private static void startTMSEvents() {
        TMSEvents tmsp = new TMSEvents();
        tmsp.createTMSEvents();
    }
    
    public static void main(String[] args) {
    	
    	if (args.length > 0) {
    		arg0 = args[0];
    	}

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startTMSEvents();
            }
        });
    }
}
