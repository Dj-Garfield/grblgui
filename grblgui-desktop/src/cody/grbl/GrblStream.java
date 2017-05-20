package cody.grbl;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import cody.gcode.GCodeCommand;
import cody.gcode.GCodeFile;
import cody.gcode.GCodeLine;

import com.badlogic.gdx.math.Vector3;


public class GrblStream implements GrblStreamInterface
{
    public GrblStream(String portName, int baudrate, GCodeFile file) throws Exception
    {
    	setStatus("Idle");
    	gcode = file;
        connect(portName, baudrate);
        stream(file);
    }
    public GrblStream(String portName, int baudrate) throws Exception
    {
    	setStatus("Idle");
        connect(portName, baudrate);
        createReader();
    }

    GrblStreamListener listener;
	public void setListener(GrblStreamListener listener) {
		this.listener = listener;
	}
	
    public static String[] Ports() {
    	return SerialPortList.getPortNames();
    }
    GCodeFile gcode;
    
    SerialPort serialPort;
    private Vector3 toolPosition = new Vector3();
    private Vector3 machinePosition = new Vector3();
    private Vector3 worldCoordinateOffset = new Vector3();
    private Streamer streamer;
    Updater updater;
    Reader reader;
    Thread updater_thread;
    Thread streamer_thread;
    Thread reader_thread;
    private String status;
    private synchronized void setStatus(String status) {
    	this.status = status;
    }
    public int getCurrentLine() {
    	if(streamer != null) {
    		return streamer.currentLine;
    	}
    	else {
    		return 0;
    	}
    }
    synchronized void write(byte[] data) throws SerialPortException {
    	serialPort.writeBytes(data);
    }
    
    synchronized public void send(byte[] data) {
    	try {
			write(data);
		} catch (SerialPortException e) {
			e.printStackTrace();
			System.exit(13);
		}
    }
    
    public void reset() {
    	send(new byte[]{24});
    }
    public boolean isHold() {
    	return is_paused;
    }
    boolean is_paused = false;
    
    public boolean isStreaming() {
    	return streamer != null;
    }
    public void pause() {
    	try {
			write(new byte[]{(byte) (is_paused ? '~' : '!')});
			is_paused = !is_paused;
		} catch (SerialPortException e) {
			e.printStackTrace();
			System.exit(4);
		}
    }

    void stopStreamer() {
    	if(streamer != null) {
    		streamer.exit = true;
    		try {
    			streamer_thread.join();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    			System.exit(9);
    		}
    		streamer = null;
    		streamer_thread = null;
    		System.out.println("Streamer stopped.");
    	}
    }
    
    void createReader() {
        reader = new Reader(serialPort);
        reader_thread = new Thread(reader);
        reader_thread.start();
    }
    void stopReader() {
    	if(reader != null) {
    		reader.exit = true;
    		try {
    			reader_thread.join();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    			System.exit(9);
    		}
    		reader = null;
    		reader_thread = null;
    		System.out.println("Reader stopped.");
    	}
    	
    }

    public void stream() {
    	stream(gcode);
    }
    public void stream(GCodeFile file) {
    	gcode = file;
    	Reader r = reader;
    	stopReader();
    	
        streamer = new Streamer(serialPort, file);
        streamer.buffer = r.buffer;
        streamer_thread = new Thread(streamer);
        streamer_thread.start();
    }
    public void stopStream() {
    	if(streamer != null) {
    		stopStreamer();
    		createReader();
    	}
    }
    void connect ( String portName, int baudrate ) throws Exception
    {
    	serialPort = new SerialPort(portName);
    	if(!serialPort.openPort())
    		throw new Exception("cant open port");	
    	if(!serialPort.setParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE))
    		throw new Exception("cant set port params");

        updater_thread = new Thread(updater = new Updater(serialPort));
        updater_thread.start();
    }

    
    public class Reader implements Runnable 
    {
        private SerialPort in;
        private byte[] buffer = new byte[1024];
        public boolean exit = false;
        
        public Reader ( SerialPort in )
        {
            this.in = in;
        }
        
        public void run ()
        {
            try
            {
        		byte data;
        		int len = 0;

				while (!exit) {
					//System.out.println("1");
					while (in.getInputBufferBytesCount() > 0 && (!exit || len != 0)) {
						//System.out.println("2");
						if ((data = in.readBytes(1)[0]) > -1) {
							//System.out.println("3");
							//System.out.println(data);
							if ((data == '\n' || data == '\r')) {
								//System.out.println("4");
								if (len > 0) {
									String output = new String(buffer, 0, len);
									len = 0;
									System.out.println("GrblReader Received: "+ output);
									
									boolean print = true;
									if (output.equals("ok")) {
									}
									else if (output.startsWith("<")) {
										String mpos = output.substring(output.indexOf("MPos:") + 5);
										String[] s = mpos.split("[,|]");
										getMachinePosition().x = Float.parseFloat(s[0]);
										getMachinePosition().y = Float.parseFloat(s[1]);
										getMachinePosition().z = Float.parseFloat(s[2]);
										if(output.contains("WCO:"))
										{
											String wco = output.substring(output.indexOf("WCO:") + 4);
											String[] s2 = wco.split("[,>]");
											getWorldCoordinateOffset().x = Float.parseFloat(s2[0]);
											getWorldCoordinateOffset().y = Float.parseFloat(s2[1]);
											getWorldCoordinateOffset().z = Float.parseFloat(s2[2]);
										}

										getToolPosition().x = getMachinePosition().x - getWorldCoordinateOffset().x;
										getToolPosition().y = getMachinePosition().y - getWorldCoordinateOffset().y;
										getToolPosition().z = getMachinePosition().z - getWorldCoordinateOffset().z;
										
										setStatus(output.substring(1, output.indexOf('|')));
										
										print = false;
									} else if (output.startsWith("Grbl ")) {
										if(!output.startsWith("Grbl 1.")) {
											listener.received("Wrong Grbl version. Should be 1.1.");
											System.out.println("Wrong Grbl version. Should be 1.1.");
										}
									} else if (output.startsWith("'$' ")) {
									} else if (output.startsWith("$")) {
									} else if (output.startsWith("~")) {
									} else if (output.startsWith("!")) {
									} else if (output.startsWith("?")) {
									} else if (output.startsWith("ctrl-x")) {
									} else if (output.startsWith("['$H'|'$X' to unlock]")) {
										//send("$X\n".getBytes());
									} else if (output.startsWith("[Caution: Unlocked]")) {
										System.out.println("Unlocked!");
									} else {
										System.out.println("GrblReader Error: "+ output);
										//System.exit(2);
										//return;
									}

									if(listener != null && print)
										listener.received(output);
								}
								if (exit)
									return;
							} else
								buffer[len++] = (byte) data;
						}
					}
					if (exit && len == 0)
						return;

        		try {
					Thread.sleep(20, 0);
				} catch (InterruptedException e) {
					e.printStackTrace();
	                System.exit(-1);
					
				}
        		}
            }
            catch ( NumberFormatException | SerialPortException e )
            {
                e.printStackTrace();
                System.exit(-1);
            }    
        }
    }
    
    public class Streamer implements Runnable 
    {
        private byte[] buffer = new byte[1024];
        GCodeFile gcode;
        public int currentLine;
        public boolean exit = false;
        SerialPort port;
        
        public Streamer ( SerialPort port, GCodeFile gcode)
        {
            this.port = port;
            this.gcode = gcode;
        }
        
        public void run ()
        {
            try
            {
        		byte data;
                
                System.out.println("GrblStream: Start streaming...");
                
                currentLine = 0;
                int errors = 0;
            	lines:for(GCodeLine line : gcode.gcode) {
            		String line_string = line.getContent(speed);
            		for(GCodeCommand cmd : line.commands) {
            			if(cmd.cmd == 'T' || cmd.cmd == 'M') {
                    		System.out.println("GrblStream ignored line: " + line_string);
            				currentLine++;
            				continue lines;
            			}
            		}
            		
            		System.out.println("GrblStream Write: " + line_string);
            		send( (line_string + "\n").getBytes());
                    currentLine ++;
            		
                    boolean ok = false;
            		int len = 0;
            		while(!ok) {
            			
                    while ( port.getInputBufferBytesCount() > 0 ) {
                    	data = port.readBytes(1)[0];
                        if ( (data == '\n' || data == '\r')) {
                        	if(len > 0) {
	                            String output = new String(buffer,0,len);
	                        	len = 0;
	                    		System.out.println("GrblStream Received: " + output);
	                        	if(output.equals("ok")) {
	                        		ok = true;
	                        		break;
	                        	} else if (output.startsWith("<")) {

									String mpos = output.substring(output.indexOf("MPos:") + 5);
									String[] s = mpos.split("[,|]");
									getMachinePosition().x = Float.parseFloat(s[0]);
									getMachinePosition().y = Float.parseFloat(s[1]);
									getMachinePosition().z = Float.parseFloat(s[2]);
									if(output.contains("WCO:"))
									{
										String wco = output.substring(output.indexOf("WCO:") + 4);
										String[] s2 = wco.split("[,>]");
										getWorldCoordinateOffset().x = Float.parseFloat(s2[0]);
										getWorldCoordinateOffset().y = Float.parseFloat(s2[1]);
										getWorldCoordinateOffset().z = Float.parseFloat(s2[2]);
									}

									getToolPosition().x = getMachinePosition().x - getWorldCoordinateOffset().x;
									getToolPosition().y = getMachinePosition().y - getWorldCoordinateOffset().y;
									getToolPosition().z = getMachinePosition().z - getWorldCoordinateOffset().z;
									
									setStatus(output.substring(1, output.indexOf('|')));
									
								} else {
	                        		System.out.println("GrblStream Error: " + output);
	                        		errors++;
	                        		if(errors != 1) {
		                        		System.exit(2);
		                        		return;
	                        		}
	                        		else
	                        			System.out.println("GrblStream Error ignored.");
	                        	}
                        	}
                        	if(exit)
                        		break;
                        }
                        else
                        	buffer[len++] = (byte) data;
                    }
            		if(exit) {
                		System.out.println("GrblStream thread exit.");
            			return;
            		}
                    try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
            		}
                    if(!ok) {
                    	System.out.println("GrblStream fail.");
                    	System.out.println("GrblStream thread exit.");
                		System.exit(6);
                		return;
                	}
            	}
                System.out.println("Stream complete!");
                createReader();
                streamer = null;
            }
            catch (  NumberFormatException | SerialPortException e )
            {
                e.printStackTrace();
                System.exit(-1);
            }        
        }
    }

    public class Updater implements Runnable 
    {
        public boolean exit = false;
        SerialPort port;
        public Updater ( SerialPort out )
        {
            this.port = out;
        }
        
        public void run ()
        {
            try
            {
            	while(!exit) {
            		send( "?".getBytes());
            		Thread.sleep(1000/8, 0);
            	}
            }catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(10);
			}            
        }
    }
    
    public void dispose() {
    	stopStreamer();
    	stopReader();
    	
    	if(updater != null) {
    		updater.exit = true;
    		try {
    			updater_thread.join();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    			System.exit(9);
    		}
    		updater = null;
    		updater_thread = null;
    		System.out.println("Updater stopped.");
    	}
    	try {
			serialPort.closePort();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
    	serialPort = null;
    }
	public Vector3 getToolPosition() {
		return toolPosition;
	}
	public void setToolPosition(Vector3 toolPosition) {
		this.toolPosition = toolPosition;
	}
	public Vector3 getMachinePosition() {
		return machinePosition;
	}
	public Vector3 getWorldCoordinateOffset(){
		return worldCoordinateOffset;
	}
	public void setMachinePosition(Vector3 machinePosition) {
		this.machinePosition = machinePosition;
	}
	@Override
	public void setSpeed(int percent) {
		speed = percent;
		if(gcode == null)
			return;
		GCodeLine line = gcode.gcode.get(streamer.currentLine);
		if(line == null)
			return;
		int current_feedrate = (int)line.feedrate;
		int new_feedrate = current_feedrate * percent / 100;
		send(("F" + Integer.toString(new_feedrate)+"\n").getBytes());
		
	}

	int speed = 100;
	@Override
	public int getSpeed() {
		return speed;
	}
	@Override
	public synchronized String getStatus() {
		return status;
	}
}