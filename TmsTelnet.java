package com.ncast.tms;

import java.net.*;
import java.io.*;

/**
 * TmsTelnet - The Telepresenter Management System serial command Telnet interface.
 * The Telepresenter Management System Telnet interface allows serial commands to be sent
 * to a Telepresenter for command and control and status retrieval operations.
 */

public class TmsTelnet
{
  public String site;
	public int port = 7474;
	public String password = "ncast";
	public boolean debug = false;
	public TmsLog tms;
	public String tmsmodule = "TmsTelnet";

	public boolean connected = false;
	public int errors = 0;
	public String response = "";
	public Boolean commandwasgood = false;
	public String commandreturned = "";
	public Boolean alertflag = false;
	public String status = "";
    
	public String aspectRatio = "?";
	public String audioDevice = "?";
	public String audioInput = "0";
	public String audioLocalLeft = "-100.0";
	public String audioLocalRight = "-100.0";
	public String audioLoopback = "0";
	public String audioMeter = "0";
	public String audioMeterPosition = "?";
	public String audioNetworkLeft = "-100.0";
	public String audioNetworkRight = "-100.0";
	public String audioOutput = "0";
	public String frameSize = "?";
	public String channel = "?";
	public String display = "?";
	public String error = "0";
	public String graphicsInput = "?";
	public String inputDetectMain = "0";
	public String inputDetectPIP = "0";
	public String localPlayback = "?";
	public String loopback = "0";
	public String mute = "0";
	public String noise = "?";
	public String numberUnits = "0";
	public String graphicsOverlay[] = {"?","?","?","?"};
	public String party = "0";
	public String pipState = "0";
	public String queue = "0";
	public String receivingStream = "0";
	public String recording = "0";
	public String sendingStream = "0";
	public String serialNumber = "?";
	public String softwareDate = "?";
	public String softwareFPGA = "?";
	public String softwareMicrocontroller = "?";
	public String softwareRevision = "?";
	public String systemArchitecture = "?";
	public String textOverlay[] = {"?","?","?","?"};
	public String typeRole = "?";
	public String uploading = "0";
	public String usb = "?";
	public String videoInput = "?";
	public String viewers = "0";
	public String windowMain = "?";
	public String windowPIP = "?";
    
	private Socket s;
	private DataInputStream sIn;
	private DataOutputStream sOut;
	private InputStreamReader isr;
	
	public TmsTelnet(String init_site) {
		site = init_site;
		resetStatus();
		return;
	}
	
	public TmsTelnet(String init_site, int init_port, String init_password) {
		site = init_site;
		port = init_port;
		password = init_password;
		resetStatus();
		return;
	}
	
	public TmsTelnet(String init_site, int init_port, String init_password, TmsLog logger, Boolean debugflag) {
		site = init_site;
		port = init_port;
		password = init_password;
		tms = logger;
		debug = debugflag;
		resetStatus();
		return;
	}

    public void resetStatus() {
        alertflag = false;
        commandwasgood = false;
        commandreturned = "";
        response = "";
        status = "";
        aspectRatio = "?";
        audioDevice = "?";
        audioInput = "0";
        audioLocalLeft = "-100.0";
        audioLocalRight = "-100.0";
        audioLoopback = "0";
        audioMeter = "0";
        audioMeterPosition = "?";
        audioNetworkLeft = "-100.0";
        audioNetworkRight = "-100.0";
        audioOutput = "0";
        frameSize = "?";
        channel = "?";
        display = "?";
        error = "0";
        graphicsInput = "?";
        inputDetectMain = "0";
        inputDetectPIP = "0";
        localPlayback = "?";
        loopback = "0";
        mute = "0";
        noise = "?";
        numberUnits = "0";
        graphicsOverlay[0] = "?";
        graphicsOverlay[1] = "?";
        graphicsOverlay[2] = "?";
        graphicsOverlay[3] = "?";
        party = "0";
        pipState = "0";
        queue = "0";
        receivingStream = "0";
        recording = "0";
        sendingStream = "0";
        serialNumber = "?";
        systemArchitecture = "?";
        softwareDate = "?";
        softwareFPGA = "?";
        softwareMicrocontroller = "?";
        softwareRevision = "?";
        textOverlay[0] = "?";
        textOverlay[1] = "?";
        textOverlay[2] = "?";
        textOverlay[3] = "?";
        typeRole = "?";
        uploading = "0";
        videoInput = "?";
        viewers = "0";
        windowMain = "?";
        windowPIP = "?";
        
        return;
    }
   
    private void open() {
 
        connected = false;
        
        try {
        		s = new Socket();
        		InetSocketAddress tp = new InetSocketAddress(site, port);
        		s.connect(tp, 3000);
        		s.setSoTimeout(2000);
        		sIn = new DataInputStream(s.getInputStream());		
        		sOut = new DataOutputStream(s.getOutputStream());
                isr = new InputStreamReader(sIn, "UTF8");
        	} catch (SocketException se) {
        		tms.log(tmsmodule, "Socket exception error for site " + site + " " + se.getMessage());
          		errors++;
        		s = null;
        		return;
        	} catch (UnknownHostException uhe) {
        		tms.log(tmsmodule, "Host error for site " + site + " " + uhe.getMessage());
           		errors++;
        		s = null;
        		return;
        	} catch (IOException ioe) {
        		tms.log(tmsmodule, "Socket init error for site " + site + " " + ioe.getMessage());
           		errors++;
           		s = null;
        		return;
        	}

		errors = 0;
        connected = true;
        return;
    }

    private void close() {
        connected = false;
        if (s != null) {
        	try {
        		s.close();
        		s = null;
        	} catch (IOException ioe) {
        		s = null;
        	}
        }
        return;
    }

    private void send(String text) {
    	char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    	byte[] utf8bytes;
        try {
        	utf8bytes = text.getBytes("UTF8");
            sOut.write(utf8bytes);
            if (debug) {
            	char[] cmsg = new char[5*utf8bytes.length];
            	for (int k = 0; k < utf8bytes.length; k++) {
            		cmsg[k*5] = '0';
            		cmsg[k*5+1] = 'x';
            		cmsg[k*5+2] = hex[((int)utf8bytes[k])>>4];
            		cmsg[k*5+3] = hex[((int)utf8bytes[k])&15];
            		cmsg[k*5+4] = ' ';
            		}
            	String msg = String.valueOf(cmsg);
            	tms.log(tmsmodule, "Output: " + text);
            	tms.log(tmsmodule, "Output: " + msg);
            }
        } catch (IOException ioe) {
        	tms.log(tmsmodule, "Send error for site " + site + " " + ioe.getMessage());
    	}
        return;
    }

    private void receive() {
        Boolean done;
        char[] data = new char[1500];
        int dataLength;
       
        alertflag = false;
        commandreturned = "";
        response = "";
        
        while (true) {
            try {
                dataLength = isr.read(data);
                if (debug) {
                	tms.log(tmsmodule, "Received data length = " + String.valueOf(dataLength));
                }
            } catch (IOException ioe) {
                tms.log(tmsmodule, "Receive error for site " + site + " " + ioe.getMessage());
                return;
            }
            
            commandwasgood = false;
            if (dataLength <= 0) {
            	return;
            }
            done = false;
            String sdata = String.valueOf(data, 0, dataLength);
            if (debug) {
            	tms.log(tmsmodule, "Read [" + sdata + "]");
            }
            String[] r = sdata.split("\n");
            for (int x=0; x<r.length; x++) {
                if (r[x] == "") {
                    continue;
                }
                if (r[x].charAt(0) == ',') {
                    response = response + r[x];
                    continue;
                }
                if (r[x].charAt(0) == '&') {
                    continue;
                }
                if (r[x].charAt(0) == '+') {
                    commandwasgood = true;
                    commandreturned = r[x].substring(1);
                    done = true;
                    break;
                }
                if (r[x].charAt(0) == '-') {
                    commandreturned = r[x].substring(1);
                    done = true;
                    continue;
                }
                if (r[x].charAt(0) == '!') {
                    alertflag = true;
                    continue;
                }
                response = response + r[x].substring(1) + "\n";
            }
            if (done) {
                break;
            }
        }
        if (debug) {
        	tms.log(tmsmodule, "Read return [" + response + "]");
        }
        return;
    }
    
    public Boolean initialize() {

        // Clear out all Status indicators
        resetStatus();

        // Open the serial link
        open();

        // Send initial sign-on command
        sendCommand("IdTelnet,002," + password + "\n");

        return connected;
    }

    public void disconnect() {
    	
    	// Send a quit command
        sendCommand("QT\n");

        // Close the serial link
        close();

        // Clear out all Status indicators
        resetStatus();
    }

    public String sendCommand(String command) {
    	
    	response = "";
    	commandwasgood = false;
    	alertflag = false;
    	int i;

        if (connected) {
            if (debug) {
                tms.log(tmsmodule, "Send command: " + command);            
            }
            send(command);
        }

        if (connected) {
            for (i=0; i<5; ++i) {
              receive();
              if (debug) {
                tms.log(tmsmodule, "Received " + response);
              }
              if (commandwasgood || (response.length() > 0)) break;
            }
        }

        return response;
    }

    public String getStatus() {
    	
      status = "";
  
      // Check if still connected
      if (connected == false) {
        return status;
      }

      // Send ? command
      sendCommand("?\n");

      if (! commandwasgood) {
          return status;
      }

      // re-sync to "?" command if not aligned correctly
      while(true) {

        if (commandreturned.charAt(0) == '?') {
            break;
        }
        
        // Check if still connected
        if (connected == false) {
          return status;
        }
        
        receive();
        
        if (commandreturned == "") {
          return status;
        }
        
      }
      
      // See if we got a response
      if (response == "") {
        return status;
      }

        String rt = response.trim();
        String[] r = rt.split(",");
        for (int x=0; x<r.length; x++) {
        	char[] code = r[x].toCharArray();

            if (code[0] == 'A') {
                if (code[1] == '1') {
                    audioDevice = "Microphone";
                }
                if (code[1] == '2') {
                    audioDevice = "Line";
                }
                if (code[1] == '3') {
                    audioDevice = "USB";
                }
                if (code[1] == '4') {
                  audioDevice = "XLR";
              }
                audioInput = r[x].substring(3);
            }
            if (code[0] == 'a') {
            	aspectRatio = r[x].substring(1);
            }
            if (code[0] == 'C') {
                channel = r[x].substring(1);
            }
            if (code[0] == 'c') {
            	frameSize = r[x].substring(1);
            }
            if (code[0] == 'D') {
                if (code[1] == '0') {
                    display = "Auto";
                    usb = "0";
                }
                if (code[1] == '1') {
                    display = "VGA";
                    usb = "1";
                }
                if (code[1] == '2') {
                    display = "SVGA";
                }
                if (code[1] == '3') {
                    display = "XGA";
                }
                if (code[1] == '4') {
                    display = "SXGA";
                }
                if (code[1] == '5') {
                    display = "UXGA";
                }
                if (code[1] == '6') {
                    display = "WUXGA";
                }
            }
            if (code[0] == 'E') {
                error = r[x].substring(2);
            }
            if (code[0] == 'G') {
                if (code[1] == '1') {
                    graphicsInput = "Composite";
                }
                if (code[1] == '2') {
                    graphicsInput = "S-video";
                }
                if (code[1] == '3') {
                    graphicsInput = "VGA";
                }
                if (code[1] == '4') {
                    graphicsInput = "DVI-D";
                }
                if (code[1] == '5') {
                    graphicsInput = "Auto";
                }
                if (code[1] == '6') {
                    graphicsInput = "DVI-A";
                }
                if (code[1] == '7') {
                    graphicsInput = "HDMI";
                }
            }
            if (code[0] == 'I') {
                inputDetectMain = String.valueOf(code[1]);
            }
            if (code[0] == 'J') {
                inputDetectPIP = String.valueOf(code[1]);
            }
            if (code[0] == 'L') {
                loopback = String.valueOf(code[1]);
            }
            if (code[0] == 'l') {
              localPlayback = String.valueOf(code[1]);
            } 
            if (code[0] == 'M') {
                mute = String.valueOf(code[1]);
            }
            if (code[0] == 'm') {
                if (code[1] == '0' || code[1] == '1') {
                    audioMeter = String.valueOf(code[1]);
                }
                else {
                    audioMeterPosition = r[x].substring(1);
                }
            }
            if (code[0] == 'N') {
                numberUnits = r[x].substring(1);
            }
            if (code[0] == 'n') {
                if (code[1] == '0') {
                    noise = "Off";
                }
                if (code[1] == '1') {
                    noise = "Low";
                }
                if (code[1] == '2') {
                    noise = "Medium";
                }
                if (code[1] == '3') {
                    noise = "High";
                }
            }
            if (code[0] == 'O') {
              if (code[1] == '0') {
                audioOutput = r[x].substring(3);
              } else {
                audioLoopback = r[x].substring(3);
              }
            }
            if (code[0] == 'o') {
            	String index = String.valueOf(code[1]);
            	graphicsOverlay[Integer.valueOf(index)-1] = String.valueOf(code[2]);
            }
            if (code[0] == 'P') {
                party = r[x].substring(1);
            }
            if (code[0] == 'p') {
                pipState = String.valueOf(code[1]);
            }
            if (code[0] == 'Q') {
                queue = String.valueOf(code[1]);
            }
            if (code[0] == 'R') {
                receivingStream = String.valueOf(code[1]);
            }
            if (code[0] == 'r') {
                recording = String.valueOf(code[1]);
            }
            if (code[0] == 'S') {
                sendingStream = String.valueOf(code[1]);
            }
            if (code[0] == 'T') {
                if (code[2] == 'A') {
                  typeRole = "RTSP Streaming";
                }
                if (code[2] == 'C') {
                    typeRole = "Coordinator";
                }
                if (code[2] == 'D') {
                  typeRole = "Full Duplex";
                }
                if (code[2] == 'L') {
                  typeRole = "Recording";
                }
                if (code[2] == 'N') {
                  typeRole = "No Session";
                }
                if (code[2] == 'P') {
                    typeRole = "Participant";
                }
                if (code[2] == 'R') {
                    typeRole = "Receiver";
                }
                if (code[2] == 'S') {
                    typeRole = "Streaming";
                }
                if (code[2] == 'T') {
                  typeRole = "RTMP Streaming";
                }
                if (code[2] == 'W') {
                  typeRole = "Waiting";
                }
            }
            if (code[0] == 't') {
            	String index = String.valueOf(code[1]);
            	textOverlay[Integer.valueOf(index)-1] = String.valueOf(code[2]);
            }
            if (code[0] == 'U') {
                uploading = String.valueOf(code[1]);
            }
            if (code[0] == 'V' && code[1] == ':' ) {
                viewers = r[x].substring(2);
            }
            if (code[0] == 'V' && code[1] != ':' ) {
                if (code[1] == '1') {
                    videoInput = "Composite";
                }
                if (code[1] == '2') {
                    videoInput = "S-video";
                }
                if (code[1] == '3') {
                    videoInput = "VGA";
                }
                if (code[1] == '4') {
                    videoInput = "DVI-D";
                }
                if (code[1] == '5') {
                    videoInput = "Auto";
                }
                if (code[1] == '6') {
                    videoInput = "DVI-A";
                }
                if (code[1] == '7') {
                    videoInput = "HDMI";
                }
            }
            if (code[0] == 'W') {
            	windowMain = r[x].substring(1);            
            }
            if (code[0] == 'w') {
            	windowPIP = r[x].substring(1);
            }
        }

        return status;
    }

    public Boolean activateSessionStart(String channel) {

        // Start Session on Channel
        sendCommand("C" + channel + "\n");
        return commandwasgood;
        }
    
    public Boolean activateSessionStop() {

        // End current Session
        sendCommand("PE\n");
        return commandwasgood;
    }

    public Boolean record(String option) {

        // Execute recording option
        sendCommand("R" + option + "\n");
        return commandwasgood;
    }
    
    public Boolean setGraphicsInput(String device) {

        // Select an input device
        sendCommand("G" + device + "\n");
        return commandwasgood;
    }

    public Boolean setVideoInput(String device) {

        // Select an input device
        sendCommand("V" + device + "\n");
        return commandwasgood;
    }

    public Boolean setPIP(String state) {

        // Select PIP state
        sendCommand("p" + state + "\n");
        return commandwasgood;
    }

    public Boolean swap() {

        // Swap PIP
        sendCommand("SW\n");
        return commandwasgood;
    }

    public Boolean getSystemInfo() {

      // Check if still connected
      if (connected == false) {
          return false;
      }
      
      // Request system information
      sendCommand("Is\n");
      String rt = response.trim();
      String[] info = rt.split(",");
      if (commandwasgood && info.length == 6) {
        serialNumber = info[0];
        systemArchitecture = info[1];
        softwareRevision = info[2];
        softwareDate = info[3];
        softwareMicrocontroller = info[4];
        softwareFPGA = info[5];
      } else {
        serialNumber = "?";
        systemArchitecture = "?";
        softwareRevision = "?";
        softwareDate = "?";
        softwareMicrocontroller = "?";
        softwareFPGA = "?";
      }
      return commandwasgood;
    }
    
    public Boolean setAudioInput(String device) {

    	String code;
    	
        // Select an input device
    	  if (device == "XLR") {
    	    code = "4";
    	  } else if (device == "USB") {
    	    code = "3";
        } else if (device == "Line") {
          code = "2";
        } else {
        	code = "1";
        }
        sendCommand("A" + code + "\n");
        return commandwasgood;
    }

    public Boolean setAudioInputGain(String device, String gain) {

        String code;
        
        // Select an input device, gain
        if (device == "XLR") {
          code = "4";
        } else if (device == "USB") {
          return true;
        } else if (device == "Line") {
          code = "2";
        } else {
          code = "1";
        }
        sendCommand("A" + code + "," + gain + "\n");
        return commandwasgood;
    }

    public Boolean setAudioOutputGain(String gain) {

        // Set output gain
        sendCommand("O0," + gain + "\n");
        return commandwasgood;
    }

    public Boolean setAudioMeter(String state) {

        // Set meter on/off
        sendCommand("m" + state + "\n");
        return commandwasgood;
    }

    public Boolean getAudioLevels() {

        // Check if still connected
        if (connected == false) {
            return false;
        }
        
        // Request update on audio level measurements
        sendCommand("ma\n");
        String rt = response.trim();
        String[] levels = rt.split(",");
        if (commandwasgood && levels.length == 4) {
        	audioLocalLeft = levels[0];
        	audioLocalRight = levels[1];
        	audioNetworkLeft = levels[2];
        	audioNetworkRight = levels[3];
        } else {
          audioLocalLeft = "-100.0";
        	audioLocalRight = "-100.0";
        	audioNetworkLeft = "-100.0";
        	audioNetworkRight = "-100.0";
        }
        return commandwasgood;
    }

    public Boolean setGraphicsOverlay(int overlay, String state) {

        // Set overlay on/off
        sendCommand("OG" + overlay + "," + state + "\n");
        return commandwasgood;
    }

    public Boolean setTextOverlay(int overlay, String state) {

        // Set overlay on/off
        sendCommand("OT" + overlay + "," + state + "\n");
        return commandwasgood;
    }

    public Boolean setSubtitleWithoutDuration(String subtitle) {

      // Set subtitle without any duration
      sendCommand("sn," + subtitle + "\n");
      return commandwasgood;
  }

    public Boolean setRecordingTitle(String title) {

      // Set recording archive title in XML file
      sendCommand("RT," + title + "\n");
      return commandwasgood;
  }

    public Boolean setRecordingPresenter(String presenter) {

      // Set recording archive presenter in XML file
      sendCommand("RP," + presenter + "\n");
      return commandwasgood;
  }
  
    public Boolean setRecordingDescription(String description) {

      // Set recording archive description in XML file
      sendCommand("RI," + description + "\n");
      return commandwasgood;
  }

    public void Debug(Boolean debugflag) {
        debug = debugflag;
    }

}
