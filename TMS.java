package com.ncast.tms;

import java.io.*;
import java.net.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
//import java.util.*;
import java.util.Properties;    

/**
 * TMS - The Telepresenter Management System.
 * The Telepresenter Management System controls all operational
 * aspects of a Telepresenter and/or a Presentation Recorder. 
 */

public class TMS extends WindowAdapter implements ActionListener {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    
    private Properties tmsProps;
    private String system;
    private TmsLog l;
    private TmsTelnet t;
    private String site;
    private int port;
    private String password;
    private String passwordApi;
    private Boolean debug;
    private Boolean quiet;
    private Boolean log;
    private String channel;
    private String audio;
    private static String arg0 = "";
    private Boolean firstStatus = true;
    private int thumbnailUpdate = 0;
    private Boolean PRHD = false;
    private double swRevision = 0.0;
    private String httpApi;
    
    enum Layouts { SessionStartStop, SessionStartStopText, Record, RecordText, Mixer, ControlPanel, ChannelSelector }
    private Layouts layout;
    
    private JFrame frame;
    private JPanel tmsPane;
    private JPanel tmsPaneL;
    private JPanel tmsPaneC;
    private JPanel tmsPaneR;
    private JPanel tmsLeftBar;
    private JPanel tmsRightBar;
    private JLabel thumbnailLabel;
    private JLabel sessionLabel;
    private JLabel recordLabel;
    private JLabel mainLabel;
    private JLabel pipLabel;
    private JLabel graphicLabel;
    private JLabel textLabel;
    private JLabel audioLabel;
    private JLabel channelLabel;
    private JLabel titleLabel;
    private JLabel presenterLabel;
    private JLabel descriptionLabel;
    private JLabel usbLabel;
    private JLabel uploadingLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton pipButton;
    private JButton swapButton;
    private JButton record_startButton;
    private JButton record_continueButton;
    private JButton record_pauseButton;
    private JButton record_stopButton;
    private JButton main_vgaButton;
    private JButton main_dviaButton;
    private JButton main_dvidButton;
    private JButton main_svideoButton;
    private JButton main_compositeButton;
    private JButton main_hdmiButton;
    private JButton pip_vgaButton;
    private JButton pip_dviaButton;
    private JButton pip_dvidButton;
    private JButton pip_svideoButton;
    private JButton pip_compositeButton;
    private JButton pip_hdmiButton;
    private JButton[] graphic_overlayButton;
    private JButton[] text_overlayButton;
    private JCheckBox audio_meterCheckBox;
    private JRadioButton line_inRadioButton;
    private JRadioButton mic_inRadioButton;
    private JRadioButton usb_inRadioButton;
    private JRadioButton xlr_inRadioButton;
    private ButtonGroup audio_inButtonGroup;
    private JSpinner channelSpinner;
    private SpinnerModel spinner_number;
    private JSlider audio_in_levelSlider;
    private JSlider audio_out_levelSlider;
    private JProgressBar audio_leftBar;
    private JProgressBar audio_rightBar;
    private JTextField titleTextField;
    private JTextField presenterTextField;
    private JTextField descriptionTextField;
    
    private void createTMS() {
    
       system = System.getProperty("os.name");
        
		try {
		   	// create and load default properties
			Properties defaultTMSProps = new Properties();
			FileInputStream in = new FileInputStream("tms_default.properties");
			defaultTMSProps.load(in);
			in.close();
			
			// create application properties with default
			tmsProps = new Properties(defaultTMSProps);

			// now load properties from last invocation
			in = new FileInputStream("tms.properties");
			tmsProps.load(in);
			in.close();
		  } catch (FileNotFoundException fnf) {
		    System.err.println("TMS Properties file not found error: " + fnf.getMessage());
		    //return;
		  } catch (IOException ioe) {
		    System.err.println("TMS Properties system error: " + ioe.getMessage());
		    return;
		  }
        
		  site = tmsProps.getProperty("site", "");
		  if (arg0.length() > 0) {
		    site = arg0;
		    tmsProps.setProperty("site", site);
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
		          tmsProps.setProperty("site",site);
		      } else {
		        //If here, the return value was null/empty.
		        JOptionPane.showMessageDialog(
		                 frame,
	                  "There was an error in the IP address",
	                  "Unit IP Address Error",
	                  JOptionPane.ERROR_MESSAGE
	                  );
		      System.err.println("Usage: java TMS site");
		      System.exit(-1);
		      }
		    }
		  }
		  String port_str = tmsProps.getProperty("port");
		  password = tmsProps.getProperty("password");
		  passwordApi = tmsProps.getProperty("passwordapi");
		  String debug_str = tmsProps.getProperty("debug");
		  String quiet_str = tmsProps.getProperty("quiet");
		  String log_str = tmsProps.getProperty("log");
		  channel = tmsProps.getProperty("channel");
		  audio = tmsProps.getProperty("audio");
		  String layout_str = tmsProps.getProperty("layout");
        
		  port = Integer.parseInt(port_str);
		  debug = debug_str.equals("true");
		  quiet = quiet_str.equals("true");
		  log = log_str.equals("true");
		  
		  // If a layout was specified check it, otherwise put up a dialog bo and ask the user for it
		  if (layout_str != null && layout_str.length() > 0) {
		    try {
		      layout = Layouts.valueOf(layout_str);
		    } catch (IllegalArgumentException iae) {
		      JOptionPane.showMessageDialog(
		              frame,
		              "There was an error in the layout specification: " + layout_str,
		              "Layout Specification Error",
		              JOptionPane.ERROR_MESSAGE
		      );
		      System.err.println("TMS Properties layout value error: " + iae.getMessage());
		      System.exit(-1);
		    }	  
		  } else {
		    // No layout was specified. Let the user pick one.
		    JFrame layoutframe = new JFrame("Layout Dialog");
        Object[] layoutChoices = {"SessionStartStop", "SessionStartStopText", "Record", "RecordText", "Mixer", "ControlPanel", "ChannelSelector"};
        String s = (String)JOptionPane.showInputDialog(
                            layoutframe,
                            "Choose a Layout",
                            "Select the layout for this session",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            layoutChoices,
                            layoutChoices[0]);
        //If a string was returned, setup site value.
        if ((s != null) && (s.length() > 0)) {
            layout_str = s;
            layout = Layouts.valueOf(layout_str);
            tmsProps.setProperty("layout",layout_str);
        } else {
          //If here, the return value was null/empty.
          JOptionPane.showMessageDialog(
                   frame,
                  "No layout was selected",
                  "Layout Selection Error",
                  JOptionPane.ERROR_MESSAGE
                  );
        System.err.println("Layout selection error from layout dialog.");
        System.exit(-1);
        }
		  }

        // Initialize logging facility
        BufferedWriter out = null;
        if (log) {
          try {
            FileWriter log = new FileWriter("tms.log");
            out  = new BufferedWriter(log);
          } catch (IOException ioe) {
            System.err.println("TMS FileWriter system error.");
            return;
          }
        }

        l = new TmsLog(out, log, quiet);
    
        l.log("TMS", "Logger initialized");
        l.log("TMS", "Operating system: " + system);
        l.log("TMS", "Properties: " + tmsProps.toString());
        
        // Initialize connection to site
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
          System.err.println("TMS Connection initialization error for site: " + site);
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
            System.err.println("TMS software Revision Number Format Exception " + nfe.getMessage());
          }
        };

        if (PRHD) {
          frame = new JFrame("PRHD - " + site);
        } else {
          frame = new JFrame("Telepresenter - " + site);
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener((WindowListener)this);
   
        // Setup GUI
        int numberOfRows = 7;
        int numberOfCols = 7;
        
        switch (layout) {
        case SessionStartStop:
          numberOfRows = 1;
        	numberOfCols = 3;
        	break;
        case SessionStartStopText:
          numberOfRows = 4;
          numberOfCols = 3;
          break;
        case Record:
          numberOfRows = 2;
          numberOfCols = 3;
          break;
        case RecordText:
          numberOfRows = 5;
        	numberOfCols = 3;
        	break;
        case Mixer:
        	numberOfRows = 4;
        	numberOfCols = 7;
        	break;
        case ControlPanel:
        	numberOfRows = 7;
        	numberOfCols = 7;
        	break;
        case ChannelSelector:
        	numberOfRows = 5;
        	numberOfCols = 1;
        	break;
        }
     
        switch (layout) {
        
        case SessionStartStop:
        case SessionStartStopText:
        case Record:
        case RecordText:
        case Mixer:
        	tmsPaneL = new JPanel(new GridLayout(numberOfRows, numberOfCols, 5, 5));
        	tmsPaneL.setMinimumSize(new Dimension(75, 20));
        	frame.setContentPane(tmsPaneL);
        	break;
        
        case ControlPanel:
        	//java.net.URL imgURL = getClass().getResource("TelepresenterM4-1196x229.jpg");
        	//if (imgURL != null) {
        	//	Toolkit toolkit = Toolkit.getDefaultToolkit();
        	//	M4 = toolkit.getImage(imgURL);
        	//} else {
        	//	System.err.println("File not found: TelepresenterM4-1196x229.jpg");
        	//}
        	//tmsPane = new ImagePanel();
        	tmsPane = new JPanel(new BorderLayout(5, 5));
        	tmsPane.setLayout(new BorderLayout(5, 5));
 
    		//Dimension m4Size = new Dimension(1196, 229);
    		//tmsPane.setPreferredSize(m4Size);
    		//tmsPane.setMinimumSize(m4Size);
    		//tmsPane.setImage(M4);

        	tmsPaneL = new JPanel(new GridLayout(numberOfRows, numberOfCols, 5, 5));
        	tmsPaneC = new JPanel();
        	tmsPaneR = new JPanel(new GridLayout(1, 4, 5, 5));
        	tmsLeftBar = new JPanel();
        	tmsLeftBar.setLayout(new BoxLayout(tmsLeftBar, BoxLayout.Y_AXIS));
        	tmsRightBar = new JPanel();
        	tmsRightBar.setLayout(new BoxLayout(tmsRightBar, BoxLayout.Y_AXIS));
        	tmsPane.add(tmsPaneL,BorderLayout.LINE_START);
        	tmsPane.add(tmsPaneC,BorderLayout.CENTER);
        	tmsPane.add(tmsPaneR,BorderLayout.LINE_END);
        	frame.setContentPane(tmsPane);
        	break;
    		
        case ChannelSelector:
        	tmsPane = new JPanel(new BorderLayout(5, 5));
        	tmsPane.setLayout(new BorderLayout(5, 5));
        	tmsPaneL = new JPanel();
        	tmsPaneL.setLayout(new BoxLayout(tmsPaneL, BoxLayout.Y_AXIS));
        	tmsPaneR = new JPanel(new GridLayout(1, 3, 5, 5));
        	tmsLeftBar = new JPanel();
        	tmsLeftBar.setLayout(new BoxLayout(tmsLeftBar, BoxLayout.Y_AXIS));
        	tmsRightBar = new JPanel();
        	tmsRightBar.setLayout(new BoxLayout(tmsRightBar, BoxLayout.Y_AXIS));
        	tmsPane.add(tmsPaneL,BorderLayout.LINE_START);
        	tmsPane.add(tmsPaneR,BorderLayout.LINE_END);
        	frame.setContentPane(tmsPane);
        	break;
        }

        
        // Create the TMS menu bar.
        //JMenuBar tmsMenuBar = new JMenuBar();
        //tmsMenuBar.setOpaque(true);
        //tmsMenuBar.setBackground(new Color(128, 128, 192));
        //tmsMenuBar.setPreferredSize(new Dimension(200, 20));
        // Set the menu bar.
        //frame.setJMenuBar(tmsMenuBar);
        
        // Create some control buttons
        
        Dimension bPref = new Dimension(80, 30);
        Dimension bMax = new Dimension(150, 50);
        Dimension bMin = new Dimension(25, 15);
        
        // Create Session label.
        sessionLabel = new JLabel("Session");
        sessionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tmsPaneL.add(sessionLabel);
        
        // Add Start button
        startButton = anotherButton(tmsPaneL, "Start", bPref, bMax, bMin, "Starts a Recording/Streaming Session", "start");
        
        // Add Stop button
        stopButton = anotherButton(tmsPaneL, "Stop", bPref, bMax, bMin, "Stops a Recording/Streaming Session", "stop");

        // If layout == SessionStartStop, create a Window with a single row.
        // If layout == SessionStartStopText, create a Window with a single row and three columns plus three lines of text.  
        // If layout == Record, create a Window with two rows, but no Pip or Swap buttons.
        // If layout == RecordText, create a Window with two rows, but no Pip or Swap buttons plus three lines of text.
        
        // Add Pip controls only for layouts Mixer, ControlPanel
        
        switch (layout) {
        case Mixer:
        case ControlPanel:
          
        	// Add Pip button
        	pipButton = anotherButton(tmsPaneL, "PiP", bPref, bMax, bMin, "Toggles PIP window", "pip");
        
        	// Add Swap button
        	swapButton = anotherButton(tmsPaneL, "Swap", bPref, bMax, bMin, "Swaps Main and PIP inputs", "swap");
          
        	// Add spacer text
          JLabel text16 = new JLabel("");
          tmsPaneL.add(text16);
          JLabel text17 = new JLabel("");
          tmsPaneL.add(text17);
  
        }
        
        // Create Window with at least two rows
        
        switch (layout) {      
        case Record:
        case RecordText:
        case Mixer:
        case ControlPanel:
        	// Create Record label.
        	recordLabel = new JLabel("Record");
        	recordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        	tmsPaneL.add(recordLabel);
        
        	// Add Record button
        	record_startButton = anotherButton(tmsPaneL, "Start", bPref, bMax, bMin, "Start recording", "record_start");
        }
        
        switch (layout) {
        case Mixer:
        case ControlPanel:
        
        	// Add Pause button
        	record_pauseButton = anotherButton(tmsPaneL, "Pause", bPref, bMax, bMin, "Pause recording", "record_pause");
        
        	// Add Continue button
        	record_continueButton = anotherButton(tmsPaneL, "Continue", bPref, bMax, bMin, "Continue recording", "record_continue");
        }
        
        switch (layout) {
        case Record:
        case RecordText:
        case Mixer:
        case ControlPanel:       
        	// Add Record stop button
        	record_stopButton = anotherButton(tmsPaneL, "Stop", bPref, bMax, bMin, "Stop recording", "record_stop");
        }
        
        // Add three text input lines
        switch (layout) {
        case SessionStartStopText: 
        case RecordText:
          Color darkGreen = new Color(0,96,0);
          // Add label and input field for the Title
          titleLabel = new JLabel("Title");
          titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
          tmsPaneL.add(titleLabel);
          titleTextField = new JTextField(20);
          tmsPaneL.add(titleTextField);
          usbLabel = new JLabel("");
          usbLabel.setHorizontalAlignment(SwingConstants.CENTER);
          usbLabel.setForeground(darkGreen);
          tmsPaneL.add(usbLabel);
          // Add label and input field for the Presenter
          presenterLabel = new JLabel("Presenter");
          presenterLabel.setHorizontalAlignment(SwingConstants.CENTER);
          tmsPaneL.add(presenterLabel);
          presenterTextField = new JTextField(20);
          tmsPaneL.add(presenterTextField);
          uploadingLabel = new JLabel("");
          uploadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
          uploadingLabel.setForeground(darkGreen);
          tmsPaneL.add(uploadingLabel);
          // Add label and input field for the Description
          descriptionLabel = new JLabel("Description");
          descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
          tmsPaneL.add(descriptionLabel);
          descriptionTextField = new JTextField(20);
          tmsPaneL.add(descriptionTextField);
          JLabel text20 = new JLabel("");
          tmsPaneL.add(text20);
        }
        

        switch (layout) {
        case Mixer:
        case ControlPanel:       
          // Add spacer text
          JLabel text26 = new JLabel("");
          tmsPaneL.add(text26);
          JLabel text27 = new JLabel("");
          tmsPaneL.add(text27);
        }

        // Create window with four rows
        switch (layout) {
        case Mixer:
        case ControlPanel:
        	// Create Main input label.
        	mainLabel = new JLabel("Main input");
        	mainLabel.setHorizontalAlignment(SwingConstants.CENTER);
        	tmsPaneL.add(mainLabel);
        
        	// Add Main VGA button
        	main_vgaButton = anotherButton(tmsPaneL, "VGA", bPref, bMax, bMin, "Select VGA for main input", "main_vga");
        
        	// Add Main DVI-D button
        	main_dvidButton = anotherButton(tmsPaneL, "DVI-D", bPref, bMax, bMin, "Select DVI-D for main input", "main_dvid");

        	// Add Main DVI-A button
          main_dviaButton = anotherButton(tmsPaneL, "DVI-A", bPref, bMax, bMin, "Select DVI-A for main input", "main_dvia");
       
        	// Add Main SVideo button
        	main_svideoButton = anotherButton(tmsPaneL, "S-Video", bPref, bMax, bMin, "Select s-video for main input", "main_svideo");
        
        	// Add Main Composite button
        	main_compositeButton = anotherButton(tmsPaneL, "Composite", bPref, bMax, bMin, "Select composite for main input", "main_composite");

          // Add Main HDMI button
          main_hdmiButton = anotherButton(tmsPaneL, "HDMI", bPref, bMax, bMin, "Select HDMI for main input", "main_hdmi");

        	// Create PIP input label.
        	pipLabel = new JLabel("PiP input");
        	pipLabel.setHorizontalAlignment(SwingConstants.CENTER);
        	tmsPaneL.add(pipLabel);
        
        	// Add PIP VGA button
        	pip_vgaButton = anotherButton(tmsPaneL, "VGA", bPref, bMax, bMin, "Select VGA for pip input", "pip_vga");
        
        	// Add PIP DVI-D button
        	pip_dvidButton = anotherButton(tmsPaneL, "DVI-D", bPref, bMax, bMin, "Select DVI-D for pip input", "pip_dvid");

        	// Add PIP DVI-A button
          pip_dviaButton = anotherButton(tmsPaneL, "DVI-A", bPref, bMax, bMin, "Select DVI-A for pip input", "pip_dvia");
      
        	// Add PIP SVideo button
        	pip_svideoButton = anotherButton(tmsPaneL, "S-Video", bPref, bMax, bMin, "Select s-video for pip input", "pip_svideo");
        
        	// Add PIP Composite button
        	pip_compositeButton = anotherButton(tmsPaneL, "Composite", bPref, bMax, bMin, "Select composite for pip input", "pip_composite");

        	// Add PIP HDMI button
          pip_hdmiButton = anotherButton(tmsPaneL, "HDMI", bPref, bMax, bMin, "Select HDMI for pip input", "pip_hdmi");

        }
        
        switch (layout) {
        case ControlPanel:
        	// Create Graphic Overlay label and buttons.
        	graphicLabel = new JLabel("Graphic overlays");
        	graphicLabel.setHorizontalAlignment(SwingConstants.CENTER);
        	tmsPaneL.add(graphicLabel);
        	graphic_overlayButton = new JButton[4];
        	for (int i = 0; i < 4; i++) {
        		graphic_overlayButton[i] = anotherButton(tmsPaneL, "Graphic " + (i+1), bPref, bMax, bMin, "Enable/Disable graphical overlay", "graphical_overlay" + (i+1));
        	}
        	
          // Add Softwware Revision text
          JLabel swrev = new JLabel("Revision " + t.softwareRevision);
          swrev.setHorizontalAlignment(SwingConstants.CENTER);
          tmsPaneL.add(swrev);
          JLabel swdate = new JLabel(t.softwareDate);
          swdate.setHorizontalAlignment(SwingConstants.CENTER);
          tmsPaneL.add(swdate);
          
        	// Create Text Overlay label and buttons.
           	textLabel = new JLabel("Text overlays");
        	textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        	tmsPaneL.add(textLabel);
        	text_overlayButton = new JButton[4];
        	for (int i = 0; i < 4; i++) {
        		text_overlayButton[i] = anotherButton(tmsPaneL, "Text " + (i+1), bPref, bMax, bMin, "Enable/Disable text overlay", "text_overlay" + (i+1));
        	}
        	
          channelLabel = new JLabel("Channel");
          channelLabel.setHorizontalAlignment(SwingConstants.CENTER);
          tmsPaneL.add(channelLabel);
          spinner_number = new SpinnerNumberModel(1, 1, 100, 1);
          channelSpinner = new JSpinner(spinner_number);
          channelSpinner.setToolTipText("Set the Channel number");
          channelSpinner.addChangeListener(new SpinnerListener());
          channelSpinner.setFocusable(false);
          channelSpinner.setValue(Integer.valueOf(channel));
          tmsPaneL.add(channelSpinner);
        	
          audioLabel = new JLabel("Audio input");
          audioLabel.setHorizontalAlignment(SwingConstants.CENTER);
          tmsPaneL.add(audioLabel);
          
          line_inRadioButton = new JRadioButton("Line In");
          line_inRadioButton.setToolTipText("Set audio input to Line In");
          line_inRadioButton.setActionCommand("line_in");
          line_inRadioButton.addActionListener(this);
          line_inRadioButton.setFocusable(false);
          tmsPaneL.add(line_inRadioButton);
          
          mic_inRadioButton = new JRadioButton("Mic In");
          mic_inRadioButton.setToolTipText("Set audio input to Mic In");
          mic_inRadioButton.setActionCommand("mic_in");
          mic_inRadioButton.addActionListener(this);
          mic_inRadioButton.setFocusable(false);
          tmsPaneL.add(mic_inRadioButton);
          
          usb_inRadioButton = new JRadioButton("USB In");
          if (PRHD) usb_inRadioButton.setForeground(Color.gray);
          usb_inRadioButton.setToolTipText("Set audio input to USB");
          usb_inRadioButton.setActionCommand("usb_in");
          usb_inRadioButton.addActionListener(this);
          usb_inRadioButton.setFocusable(false);
          tmsPaneL.add(usb_inRadioButton);
          
          xlr_inRadioButton = new JRadioButton("XLR In");
          if (!PRHD) xlr_inRadioButton.setForeground(Color.gray);
          xlr_inRadioButton.setToolTipText("Set audio input to XLR");
          xlr_inRadioButton.setActionCommand("xlr_in");
          xlr_inRadioButton.addActionListener(this);
          xlr_inRadioButton.setFocusable(false);
          tmsPaneL.add(xlr_inRadioButton);
          
          audio_meterCheckBox = new JCheckBox("Audio Meter");
          audio_meterCheckBox.setToolTipText("Set audio meter on/off.");
          audio_meterCheckBox.setActionCommand("audio_meter");
          audio_meterCheckBox.addActionListener(this);
          audio_meterCheckBox.setFocusable(false);
          tmsPaneL.add(audio_meterCheckBox);
        
          audio_inButtonGroup = new ButtonGroup();
          audio_inButtonGroup.add(line_inRadioButton);
          audio_inButtonGroup.add(mic_inRadioButton);
          audio_inButtonGroup.add(usb_inRadioButton);
          audio_inButtonGroup.add(xlr_inRadioButton);

          // Add spacer text

          JLabel text77 = new JLabel("");
          tmsPaneL.add(text77);
  
        }
        
        switch (layout) {
        case ControlPanel:
          // Install the custom authenticator - image is password protected
          Authenticator.setDefault(new MyAuthenticator()); 
          Integer imageWidth;
          Integer imageHeight;
          BufferedImage originalImage = null;
          BufferedImage scaledImage = null;
          if (swRevision < 6.5) {
            httpApi = "http://" + site + "/api.cgi?cmd=capture&format=jpg&width==256&height=144";
          } else {
            httpApi = "http://" + site + "/rest/files/preview.jpg";
          };
          try {
            // Read from a URL
            URL url = new URL(httpApi);
            originalImage = ImageIO.read(url);
            if (PRHD) {           
              imageWidth = originalImage.getWidth();
              imageHeight = originalImage.getHeight();
              Double sx = 256.0 / (double) imageWidth;
              Double sy = 144.0 / (double) imageHeight;
              AffineTransformOp op = new AffineTransformOp( AffineTransform.getScaleInstance(sx, sy), AffineTransformOp.TYPE_BICUBIC);
              scaledImage = op.filter(originalImage, null);
            }
          } catch (IOException ioe) {
            System.err.println("Image IO Exception: " + ioe.getMessage());
          }
        
          // Use a label to display the image thumbnail

          thumbnailLabel = new JLabel();
          //Set the position of the text, relative to the icon:
          thumbnailLabel.setVerticalTextPosition(JLabel.BOTTOM);
          thumbnailLabel.setHorizontalTextPosition(JLabel.CENTER);

          if (originalImage != null) {
            if (PRHD) {
              thumbnailLabel.setIcon(new ImageIcon(scaledImage));
            } else {
              thumbnailLabel.setIcon(new ImageIcon(originalImage));
            }
          }
          tmsPaneC.add(thumbnailLabel);

        }
     
        switch (layout) {
        case ControlPanel:
        case ChannelSelector:
        	audio_in_levelSlider = new JSlider(JSlider.VERTICAL);
        	audio_in_levelSlider.setMajorTickSpacing(10);
        	audio_in_levelSlider.setPaintTicks(true);
        	audio_in_levelSlider.setPaintLabels(true);
        	audio_in_levelSlider.setToolTipText("Set audio input level.");
        	audio_in_levelSlider.addChangeListener(new AudioInListener());
        	audio_in_levelSlider.setFocusable(false);
        	tmsPaneR.add(audio_in_levelSlider);
        }
        
        switch (layout) {
        case ControlPanel:
        	audio_out_levelSlider = new JSlider(JSlider.VERTICAL);
        	audio_out_levelSlider.setMajorTickSpacing(10);
        	audio_out_levelSlider.setPaintTicks(true);
        	audio_out_levelSlider.setPaintLabels(true);
        	audio_out_levelSlider.setToolTipText("Set audio output level.");
        	audio_out_levelSlider.addChangeListener(new AudioOutListener());
        	audio_out_levelSlider.setFocusable(false);
        	tmsPaneR.add(audio_out_levelSlider);
        }
        
        switch (layout) {
        case ControlPanel:
        case ChannelSelector:
            JLabel lbt = new JLabel("Audio");
            lbt.setAlignmentX(Component.CENTER_ALIGNMENT);
            tmsLeftBar.add(lbt);
            audio_leftBar = new JProgressBar(JProgressBar.VERTICAL, 0, 40);
            //audio_leftBar.setBorderPainted(true);
            tmsLeftBar.add(audio_leftBar);
            JLabel lbb = new JLabel("Left");
            lbb.setAlignmentX(Component.CENTER_ALIGNMENT);
            tmsLeftBar.add(lbb);
            tmsPaneR.add(tmsLeftBar);
            JLabel rbt = new JLabel("Audio");
            rbt.setAlignmentX(Component.CENTER_ALIGNMENT);
            tmsRightBar.add(rbt);
            audio_rightBar = new JProgressBar(JProgressBar.VERTICAL, 0, 40);
            audio_rightBar.setOpaque(false);
            //audio_rightBar.setBorderPainted(true);
            tmsRightBar.add(audio_rightBar);
            JLabel rbb = new JLabel("Right");
            rbb.setAlignmentX(Component.CENTER_ALIGNMENT);
            tmsRightBar.add(rbb);
            tmsPaneR.add(tmsRightBar);
        }
        
        switch (layout) {
        case ChannelSelector:
        	tmsPaneL.add(Box.createVerticalGlue());
        	channelLabel = new JLabel("Channel");
        	channelLabel.setHorizontalAlignment(SwingConstants.CENTER);
        	channelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        	tmsPaneL.add(channelLabel);
        	spinner_number = new SpinnerNumberModel(1, 1, 100, 1);
        	channelSpinner = new JSpinner(spinner_number);
        	channelSpinner.setPreferredSize(new Dimension(80, 30));
        	channelSpinner.setMaximumSize(new Dimension(150, 50));
        	channelSpinner.setMinimumSize(new Dimension(25, 15));
        	channelSpinner.setToolTipText("Set the Channel number");
        	channelSpinner.addChangeListener(new SpinnerListener());
        	channelSpinner.setFocusable(false);
        	channelSpinner.setValue(Integer.valueOf(channel));
        	tmsPaneL.add(channelSpinner);
        }
        
        // Set default button
        //frame.getRootPane().setDefaultButton(startButton);
        frame.setFocusable(false);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        
        // Setup a timer
        int delay = 1000; //milliseconds
        ActionListener taskPerformer = new ActionListener() {
      		public void actionPerformed(ActionEvent evt) {
          	//...Perform a task...
          	t.getStatus();
          	t.getAudioLevels();
          	updateTMS();
      		}
  		};
  		
  		new Timer(delay, taskPerformer).start();
 
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
    
    /**
     * ImagePanel - A sub-class of JPanel to allow placement of a graphic on a panel.
     */
    
    class ImagePanel extends JPanel {
    	Image image;
    	 
    	public void setImage(Image image) {
    		this.image = image;
    	}
    	 
    	public void paintComponent(Graphics g) {
    		super.paintComponent(g);
    		g.drawImage(image, 0, 0, this);
    	}
    }
    
    public class MyAuthenticator extends Authenticator {
      // This method is called when a password-protected URL is accessed
      protected PasswordAuthentication getPasswordAuthentication() {
        String prompt = getRequestingPrompt();
        String hostname = getRequestingHost();
        InetAddress ipaddr = getRequestingSite();
        int port = getRequestingPort();

        String username = "admin";
        if (swRevision < 6.5) username = "api";
        
        // Return the information
        return new PasswordAuthentication(username, passwordApi.toCharArray());
        }
      } 
    
    private void updateTMS() {
    	
    	if (PRHD) {
    	  frame.setTitle("PR-HD - " + site + " - " + t.typeRole);
    	} else {
    	  frame.setTitle("Telepresenter - " + site + " - " + t.typeRole);
    	}
    	
    	if (t.connected) {
    		sessionLabel.setForeground(Color.black);
    	} else {
    		sessionLabel.setForeground(Color.red);
    	}
    	
    	if (t.sendingStream.equals("1")) {
    		startButton.setForeground(Color.green);
    		stopButton.setForeground(Color.black);
    	} else {
    		startButton.setForeground(Color.black);
    		stopButton.setForeground(Color.red);
    	}
    	
    	// Update record line of buttons for layouts Record, Mixer, ControlPanel
    	switch (layout) {
    	case Record:
    	case RecordText:
    	case Mixer:
    	case ControlPanel:
    		
    		record_startButton.setForeground(Color.black);
    		record_stopButton.setForeground(Color.black);
    	
    		if (t.recording.equals("0")) record_stopButton.setForeground(Color.red);
    		if (t.recording.equals("1")) record_startButton.setForeground(Color.green);

    	}
    	
      // Update USB & Uploading indicator for layouts SessionStartStopText and RecordText
      switch (layout) {
      case SessionStartStopText:
      case RecordText:
        if (t.usb.equals("1")) {
          usbLabel.setText("USB Available");
        } else {
          usbLabel.setText("");
        }
        if (t.uploading.equals("1")) {
          uploadingLabel.setText("Uploading Archive");
        } else {
          uploadingLabel.setText("");
        }
      }
        
    	// Add Pause, Continue, Main and Pip items for Mixer, ControlPanel
    	switch (layout) {
    	case Mixer:
    	case ControlPanel:
    		record_pauseButton.setForeground(Color.black);
    		if (t.recording.equals("2")) record_pauseButton.setForeground(Color.magenta);
    		
    		if (t.inputDetectMain.equals("1")) {
    			mainLabel.setForeground(Color.black);
    		} else {
    			mainLabel.setForeground(Color.red);
    		}
    		if (t.inputDetectPIP.equals("1")) {
    			pipLabel.setForeground(Color.black);
    		} else {
    			pipLabel.setForeground(Color.red);
    		}
   
    		if (t.pipState.equals("1")) {
    			pipButton.setForeground(Color.blue);
    		} else {
    			pipButton.setForeground(Color.black);
    		}
    	
    		main_vgaButton.setForeground(Color.black);
    		main_dvidButton.setForeground(Color.black);
    		main_svideoButton.setForeground(Color.black);
    		main_compositeButton.setForeground(Color.black);
        if (PRHD) {
          main_dviaButton.setForeground(Color.black);
          main_hdmiButton.setForeground(Color.black);
        } else {
          main_dviaButton.setForeground(Color.gray);
          main_hdmiButton.setForeground(Color.gray);
        }
    	
    		if (t.graphicsInput.equals("VGA")) main_vgaButton.setForeground(Color.blue);
        if (t.graphicsInput.equals("DVI-A")) main_dviaButton.setForeground(Color.blue);
    		if (t.graphicsInput.equals("DVI-D")) main_dvidButton.setForeground(Color.blue);
    		if (t.graphicsInput.equals("S-video")) main_svideoButton.setForeground(Color.blue);
    		if (t.graphicsInput.equals("Composite")) main_compositeButton.setForeground(Color.blue);
        if (t.graphicsInput.equals("HDMI")) main_hdmiButton.setForeground(Color.blue);
    	
    		pip_vgaButton.setForeground(Color.black);
    		pip_dvidButton.setForeground(Color.black);
    		pip_svideoButton.setForeground(Color.black);
    		pip_compositeButton.setForeground(Color.black);
        if (PRHD) {
          pip_dviaButton.setForeground(Color.black);
          pip_hdmiButton.setForeground(Color.black);
        } else {
          pip_dviaButton.setForeground(Color.gray);
          pip_hdmiButton.setForeground(Color.gray);
        }
    	
    		if (t.videoInput.equals("VGA")) pip_vgaButton.setForeground(Color.blue);
    		if (t.videoInput.equals("DVI-A")) pip_dviaButton.setForeground(Color.blue);
    		if (t.videoInput.equals("DVI-D")) pip_dvidButton.setForeground(Color.blue);
    		if (t.videoInput.equals("S-video")) pip_svideoButton.setForeground(Color.blue);
    		if (t.videoInput.equals("Composite")) pip_compositeButton.setForeground(Color.blue);
    		if (t.videoInput.equals("HDMI")) pip_hdmiButton.setForeground(Color.blue);
    	}
    	
       	// Add Graphics overlays, Text overlays and other items	for ControlPanel
    	switch (layout) {
    	case ControlPanel:
    		
    	  for (int i = 0; i < 4; i++) {
    	    if (t.graphicsOverlay[i].equals("1")) {
    	      graphic_overlayButton[i].setForeground(Color.blue);
    	    } else {
    	      graphic_overlayButton[i].setForeground(Color.black);
    	    }
    	  }
    		
    		for (int i = 0; i < 4; i++) {
    			if (t.textOverlay[i].equals("1")) {
        			text_overlayButton[i].setForeground(Color.blue);
    			} else {
        			text_overlayButton[i].setForeground(Color.black);
    			}
    		}
       		
    		line_inRadioButton.setSelected(t.audioDevice.equals("Line"));
    		mic_inRadioButton.setSelected(t.audioDevice.equals("Microphone"));
    		usb_inRadioButton.setSelected(t.audioDevice.equals("USB"));
    		xlr_inRadioButton.setSelected(t.audioDevice.equals("XLR"));
    		audio_meterCheckBox.setSelected(t.audioMeter.equals("1"));
       		
    		try {
    		  audio_out_levelSlider.setValue(Integer.valueOf(t.audioOutput));
    		} catch (NumberFormatException nfe) {
    		  // ignore
    		}
    		
        String aspectRatio = t.aspectRatio;
        // Check if a valid status string is present (w:h) 
        if (aspectRatio.length() > 1 && thumbnailUpdate == 0) {
          // Re-adjust the size of the thumbnail if the frame size changed
          Integer paneWidth = tmsPane.getWidth();
          Integer paneLWidth = tmsPaneL.getWidth();
          Integer paneCWidth = tmsPaneC.getWidth();
          Integer paneRWidth = tmsPaneR.getWidth();
          //System.err.println("Image: " + paneWidth + " " + paneLWidth + " " + paneCWidth + " " + paneRWidth);
          String aspectValues[] = aspectRatio.split(":");
          Integer aspectWidth = Integer.valueOf(aspectValues[0]);
          Integer aspectHeight = Integer.valueOf(aspectValues[1]);
          // Compute width of desired image, but at least 128 pixels wide.
          Integer thumbnailWidth = java.lang.Math.max(paneWidth-paneLWidth-paneRWidth-10,128);
          Integer thumbnailHeight = thumbnailWidth*aspectHeight/aspectWidth;
          String width = String.valueOf(thumbnailWidth);
          String height = String.valueOf(thumbnailHeight);
          Integer imageWidth;
          Integer imageHeight;
          
          BufferedImage originalImage = null;
          BufferedImage scaledImage = null;
          if (swRevision < 6.5) {
            httpApi = "http://" + site + "/api.cgi?cmd=capture&format=jpg&width=" + width + "&height=" + height ;
          } else {
            httpApi = "http://" + site + "/rest/files/preview.jpg";
          };
          try {
            // Read from a URL
            URL url = new URL(httpApi);
            originalImage = ImageIO.read(url);
            if (originalImage == null) {
              System.err.println("updateTMS: originalImage is null, url = " + url);
            } else if (PRHD) {
              imageWidth = originalImage.getWidth();
              imageHeight = originalImage.getHeight();
              Double sx = (double) thumbnailWidth / (double) imageWidth;
              Double sy = (double) thumbnailHeight / (double) imageHeight;
              AffineTransformOp op = new AffineTransformOp( AffineTransform.getScaleInstance(sx, sy), AffineTransformOp.TYPE_BICUBIC);
              scaledImage = op.filter(originalImage, null);
            }
          } catch (IOException ioe) {
            System.err.println("Image IO Exception: " + ioe.getMessage());
          }
        
          // Update the label to display the latest image thumbnail

          if (originalImage != null) {
            if (PRHD) {
              thumbnailLabel.setIcon(new ImageIcon(scaledImage));
            } else {
              thumbnailLabel.setIcon(new ImageIcon(originalImage));
            }
          }
          
          // Image update time (seconds) in Live mode
          thumbnailUpdate = 60;
          
        } // if (aspectRatio.length() > ...
  
        if (t.typeRole == "No Session") {
          thumbnailLabel.setText("Preview");
          // In Preview mode, get a thumbnail image every second
          thumbnailUpdate = 0;
        } else {
          // In Live mode use Image update time
          //thumbnailLabel.setText("Live (" + thumbnailUpdate + ")");
          thumbnailLabel.setText("Live");
          // Count down image update time
          thumbnailUpdate = thumbnailUpdate - 1;
        }
          
    	} // switch (layout) ...
    	
    	// If an audio value >= 0 has been supplied, force the slider to this value
  		if (firstStatus) {
  			int audioLevelNumber = Integer.valueOf(audio);
  			if (audioLevelNumber >= 0 && audioLevelNumber <= 100) {
  				t.audioInput = audio;
  				t.setAudioInputGain(t.audioDevice, audio);
  				
  			}
   		} // if (firstStatus) ...
    	
  		// Add Channel Selector, Audio Input Control, Audio Meters for ControlPanel, ChannelSelector
  		switch (layout) {
  		case ControlPanel:
  		case ChannelSelector:
    		
       		if (firstStatus) {
       			try {
       				// channelSpinner.setValue(Integer.valueOf(t.channel));
       			} catch (NumberFormatException nfe) {
       				// ignore
       			}
       		}
       		
       		try {
       			audio_in_levelSlider.setValue(Integer.valueOf(t.audioInput));
       		} catch (NumberFormatException nfe) {
       			// ignore
       		}
       		
            try {
            	Float aLL = Float.valueOf(t.audioLocalLeft);
            	int leftLevel = aLL.intValue()+40;
            	if (leftLevel < 0) leftLevel = 0;
                audio_leftBar.setValue(leftLevel);
            } catch (NumberFormatException nfe) {
               	audio_leftBar.setValue(0);
            }
            
            try {
            	Float aLR = Float.valueOf(t.audioLocalRight);
            	int rightLevel = aLR.intValue()+40;
            	if (rightLevel < 0) rightLevel = 0;
                audio_rightBar.setValue(rightLevel);
            } catch (NumberFormatException nfe) {
            	audio_rightBar.setValue(0);
            }
 
      	}
      	
		firstStatus = false;
    }
    
    public void windowClosing(WindowEvent e) {
    	exitTMS();
    	return;
    }

    private void exitTMS() {
       	t.disconnect();
    	l.log("TMS", "Connection closed to site " + site);
    	try {
    		FileOutputStream out = new FileOutputStream("tms.properties");
			tmsProps.store(out, site);
			out.close();
        } catch (IOException ioe) {
            System.err.println("TMS Properties output error." + ioe.getMessage());
           	return;
       	}
    
    }
    
    public void actionPerformed(ActionEvent e) {
      if ("start".equals(e.getActionCommand())) {
        t.activateSessionStart(channel);
        // Update Title, Presenter and Description information
        switch (layout) {
        case SessionStartStopText:
        case RecordText:
          t.setRecordingTitle(titleTextField.getText());
          t.setRecordingPresenter(presenterTextField.getText());
          t.setRecordingDescription(descriptionTextField.getText());
        }
        return;
      }
      if ("stop".equals(e.getActionCommand())) {
        t.activateSessionStop();
        return;
      }
      if ("pip".equals(e.getActionCommand())) {
        if (t.pipState.equals("1")) {
          t.setPIP("0");
        } else {
          t.setPIP("1");
        }
        return;
      }
      if ("swap".equals(e.getActionCommand())) {
        t.swap();
        return;
      }
      if ("record_start".equals(e.getActionCommand())) {
        t.record("1");   
        // Update Title, Presenter and Description information
        switch (layout) {
        case RecordText:
          t.setRecordingTitle(titleTextField.getText());
          t.setRecordingPresenter(presenterTextField.getText());
          t.setRecordingDescription(descriptionTextField.getText());
        }
        return;
      }
      if ("record_pause".equals(e.getActionCommand())) {
        t.record("2");
        return;
      }
      if ("record_continue".equals(e.getActionCommand())) {
        t.record("3");
        return;
      }
      if ("record_stop".equals(e.getActionCommand())) {
        t.record("0");
        return;
      }
      if ("main_composite".equals(e.getActionCommand())) {
        t.setGraphicsInput("1");
        return;
      }
      if ("main_svideo".equals(e.getActionCommand())) {
        t.setGraphicsInput("2");
        return;
      }
      if ("main_vga".equals(e.getActionCommand())) {
        t.setGraphicsInput("3");
        return;
      }
      if ("main_dvid".equals(e.getActionCommand())) {
        t.setGraphicsInput("4");
        return;
      }
      if ("main_dvia".equals(e.getActionCommand())) {
        t.setGraphicsInput("6");
        return;
      }
      if ("main_hdmi".equals(e.getActionCommand())) {
        t.setGraphicsInput("7");
        return;
      }
      if ("pip_composite".equals(e.getActionCommand())) {
        t.setVideoInput("1");
        return;
      }
      if ("pip_svideo".equals(e.getActionCommand())) {
        t.setVideoInput("2");
        return;
      }
      if ("pip_vga".equals(e.getActionCommand())) {
        t.setVideoInput("3");
        return;
      }
      if ("pip_dvid".equals(e.getActionCommand())) {
        t.setVideoInput("4");
        return;
      }
      if ("pip_dvia".equals(e.getActionCommand())) {
        t.setVideoInput("6");
        return;
      }
      if ("pip_hdmi".equals(e.getActionCommand())) {
        t.setVideoInput("7");
        return;
      }
      if ("graphical_overlay1".equals(e.getActionCommand())) {
        if (t.graphicsOverlay[0].equals("1")) {
          t.setGraphicsOverlay(1, "0");
        } else {
          t.setGraphicsOverlay(1, "1");
        }
        return;
      }
     	if ("graphical_overlay2".equals(e.getActionCommand())) {
     	  if (t.graphicsOverlay[1].equals("1")) {
     	    t.setGraphicsOverlay(2, "0");
     	  } else {
     	    t.setGraphicsOverlay(2, "1");
     	  }
     	  return;
     	}
     	if ("graphical_overlay3".equals(e.getActionCommand())) {
     	  if (t.graphicsOverlay[2].equals("1")) {
     	    t.setGraphicsOverlay(3, "0");
     	  } else {
     	    t.setGraphicsOverlay(3, "1");
     	  }
     	  return;
     	}
     	if ("graphical_overlay4".equals(e.getActionCommand())) {
     	  if (t.graphicsOverlay[3].equals("1")) {
     	    t.setGraphicsOverlay(4, "0");
     	  } else {
     	    t.setGraphicsOverlay(4, "1");
     	  }
     	  return;
     	}
     	if ("text_overlay1".equals(e.getActionCommand())) {
     	  if (t.textOverlay[0].equals("1")) {
     	    t.setTextOverlay(1, "0");
     	  } else {
     	    t.setTextOverlay(1, "1");
     	  }
     	  return;
     	}
     	if ("text_overlay2".equals(e.getActionCommand())) {
     	  if (t.textOverlay[1].equals("1")) {
     	    t.setTextOverlay(2, "0");
     	  } else {
     	    t.setTextOverlay(2, "1");
     	  }
     	  return;
     	}
     	if ("text_overlay3".equals(e.getActionCommand())) {
     	  if (t.textOverlay[2].equals("1")) {
     	    t.setTextOverlay(3, "0");
     	  } else {
     	    t.setTextOverlay(3, "1");
     	  }
     	  return;
     	}
     	if ("text_overlay4".equals(e.getActionCommand())) {
     	  if (t.textOverlay[3].equals("1")) {
     	    t.setTextOverlay(4, "0");
     	  } else {
     	    t.setTextOverlay(4, "1");
     	  }
     	  return;
    	}
     	if ("line_in".equals(e.getActionCommand())) {		
     	  t.setAudioInput("Line");
     	  return;
    	}
    	if ("mic_in".equals(e.getActionCommand())) {
    	  t.setAudioInput("Mic");
    	  return;
    	}
     	if ("usb_in".equals(e.getActionCommand())) { 		
     	  t.setAudioInput("USB");
     	  return;
    	}
      if ("xlr_in".equals(e.getActionCommand())) {    
        t.setAudioInput("XLR");
        return;
      }
    	if ("audio_meter".equals(e.getActionCommand())) {
    		Boolean am = audio_meterCheckBox.isSelected();
     		if (am) {
     			t.setAudioMeter("1");
     		} else {
     			t.setAudioMeter("0");
     		}
     		return;
    	}
    }
 
    /**
     * SpinnerListener - Allows selection of a Channel for Session start.
     */
    
    class SpinnerListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSpinner source = (JSpinner) e.getSource();
            SpinnerModel sm = source.getModel();
            int channelNumber = (Integer) ((SpinnerNumberModel) sm).getNumber();
            channel = String.valueOf(channelNumber);
        }
    }
 
    /**
     * AudioInListener - Required to set the audio in gain slider.
     */
    
    class AudioInListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
        	if ( firstStatus ) return;
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
            	int sv = source.getValue();
            	t.setAudioInputGain(t.audioDevice, String.valueOf(sv));
            }    
        }
    }
    
    /**
     * AudioOutListener - Required to set the audio out gain slider.
     */
    
    class AudioOutListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
        	if ( firstStatus ) return;
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
            	int sv = source.getValue();
            	t.setAudioOutputGain(String.valueOf(sv));
            }    
        }
    }
 
   	private static void startTMS() {
        TMS tmsp = new TMS();
        tmsp.createTMS();
    }
    
    public static void main(String[] args) {
    	
    	if (args.length > 0) {
    		arg0 = args[0];
    	}

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startTMS();
            }
        });
    }
}
