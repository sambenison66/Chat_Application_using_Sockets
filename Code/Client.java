import java.net.*;
import java.io.*;
import java.util.*;

/*
 * Samuel Benison Jeyaraj Victor - 1000995539
 * The Client that can be run both as a console or a GUI
 */
public class Client  {

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;

	// if I use a GUI or not
	private ClientGUI cg;
	
	// the server, the port and the username
	private String server, username;
	private int port;
	// the Connecting Username
	private String connectinguser = "";
	// to inform an active connection
	private boolean activeConnection = false;

	/*
	 *  Constructor called by console mode
	 *  server: the server address
	 *  port: the port number
	 *  username: the username
	 */
	Client(String server, int port, String username) {
		// which calls the common constructor with the GUI set to null
		this(server, port, username, null);
	}

	/*
	 * Constructor call when used from a GUI
	 * in console mode the ClienGUI parameter is null
	 */
	Client(String server, int port, String username, ClientGUI cg) {
		this.server = server;
		this.port = port;
		this.username = username;
		// save if we are in GUI mode or not
		this.cg = cg;
	}
	
	/*
	 * To start the dialog
	 */
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} 
		// if it failed not much I can so
		catch(Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
	
		/* Creating both Data Stream */
		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();
		// Send our username to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects
		try
		{
			sOutput.writeObject(username);
		}
		catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}

	/*
	 * To send a message to the console or the GUI
	 */
	private void display(String msg) {
		if(cg == null)
			System.out.println(msg);      // println in console mode
		else
			cg.append(msg + "\n");		// append to the ClientGUI JTextArea (or whatever)
	}
	
	/*
	 * To send a message to the server
	 */
	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}
	
	boolean checkConnection(){
		if(activeConnection == true) {
			return true;
		}
		else {
			return false;
		}
	}

	/*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect not much to do in the catch clause
	 */
	private void disconnect() {
		try { 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {} // not much else I can do
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {} // not much else I can do
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {} // not much else I can do
		
		// inform the GUI
		if(cg != null)
			cg.connectionFailed();
			
	}
	/*
	 * To start the Client in console mode use one of the following command
	 * > java Client
	 * > java Client username
	 * > java Client username portNumber
	 * > java Client username portNumber serverAddress
	 * at the console prompt
	 * If the portNumber is not specified 1500 is used
	 * If the serverAddress is not specified "localHost" is used
	 * If the username is not specified "Anonymous" is used
	 * > java Client 
	 * is equivalent to
	 * > java Client Anonymous 1500 localhost 
	 * are eqquivalent
	 * 
	 * In console mode, if an error occurs the program simply stops
	 * when a GUI id used, the GUI is informed of the disconnection
	 */
	public static void main(String[] args) {
		// default values
		int portNumber = 1500;
		String serverAddress = "localhost";
		String userName = "Anonymous";

		// depending of the number of arguments provided we fall through
		switch(args.length) {
			// > javac Client username portNumber serverAddr
			case 3:
				serverAddress = args[2];
			// > javac Client username portNumber
			case 2:
				try {
					portNumber = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
					return;
				}
			// > javac Client username
			case 1: 
				userName = args[0];
			// > java Client
			case 0:
				break;
			// invalid number of arguments
			default:
				System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
			return;
		}
		// create the Client object
		Client client = new Client(serverAddress, portNumber, userName);
		// test if we can start the connection to the Server
		// if it failed nothing we can do
		if(!client.start())
			return;
		
		// wait for messages from user
		Scanner scan = new Scanner(System.in);
		// loop forever for message from the user
		while(true) {
			System.out.print("> ");
			// read message from user
			String msg = scan.nextLine();
			// logout if message is LOGOUT
			if(msg.equalsIgnoreCase("LOGOUT")) {
				client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
				// break to do the disconnect
				break;
			}
			// message WhoIsIn
			else if(msg.equalsIgnoreCase("WHOISIN")) {
				client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));				
			}
			else {				// default to ordinary message
				String msgContent[] = msg.split(" ");
				if(msgContent[0].equalsIgnoreCase("CONNECTTO")) {   // message ConnectTo <username> <targetname>
					if(msgContent[1].equalsIgnoreCase(msgContent[2]))
					{
						System.out.print("You cannot connect with yourself \n");
						System.out.print("> ");
					} else {
						String wholeMsg = "CONNECTTO" + "---->" + msgContent[1] + "---->" + msgContent[2];
						client.sendMessage(new ChatMessage(ChatMessage.CONNECTTO, wholeMsg));
					}
				}
				else if(msgContent[0].equalsIgnoreCase("DISCONNECT")) {    // message DisConnect <username> <targetname>
					boolean verifyCon = client.checkConnection();
					if(verifyCon == true) {
						String wholeMsg = "DISCONNECT" + "---->" + msgContent[1] + "---->" + msgContent[2];
						client.sendMessage(new ChatMessage(ChatMessage.CONNECTTO, wholeMsg));
					} else {
						System.out.print("You are not currently connected to disconnect \n");
						System.out.print("> ");
					}		
				}
				else {
					if(client.activeConnection == false){
						System.out.print("You don't have active connection to send messages \n");
						System.out.print("> ");
					} else {
						String sendingMsg = "POST" + "---->" + client.username + "---->" + client.connectinguser + "---->" + msg;
						client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, sendingMsg));
					}
				}
			}
		}
		// done disconnect
		client.disconnect();	
	}

	/*
	 * a class that waits for the message from the server and append them to the JTextArea
	 * if we have a GUI or simply System.out.println() it in console mode
	 */
	class ListenFromServer extends Thread {
		// To receive the message from the server and process it
		public void run() {
			while(true) {
				try {
					// get the message from server
					String msg = (String) sInput.readObject();
					String[] getMsg = msg.split("---->");  // Split the message based on the string divider
					boolean connStatus = true;  // value to decide whether to print the message or nor, default to true
					// if the message is related the connection response
					if(getMsg[0].equals("CONNECTTORESP")) {
						// if the connection validation is failed due to invalid login
						if(getMsg[1].equals("FAILED 001")) {
							if(getMsg[2].equals(username)) {
								msg = "Login not validated properly with server. Please logout and login again \n";
							} else {  // Default to filter the applicable Client Thread
								msg = "";
								connStatus = false;
							}
						}
						// if the connection validation is failed due to invalid connecting user
						else if(getMsg[1].equals("FAILED 002")) {
							if(getMsg[2].equals(username)) {
								msg = "Connecting Username " + getMsg[3] + " is invalid, Please check the name (CASE SENSITIVE TOO) \n";
							} else {   // Default to filter the applicable Client Thread
								msg = "";
								connStatus = false;
							}
						}
						// if the connection validation is failed due to an existing connection
						else if(getMsg[1].equals("FAILED 003")) {
							if(getMsg[2].equals(username)) {
								msg = getMsg[3];
							} else {   // Default to filter the applicable Client Thread
								msg = "";
								connStatus = false;
							}
						}
						// if the connection validation is passed and eligible to connect
						else if(getMsg[1].equals("PASSED")) {
							if(getMsg[2].equals(username)) {  // To validate the appropriate Client Thread
								if(getMsg[3].equals(connectinguser)) {
									msg = getMsg[2] + "->" + getMsg[3] + "-" + getMsg[4];  // compose the message
								} else {
									connectinguser = getMsg[3];
									msg = getMsg[2] + "->" + getMsg[3] + "-" + getMsg[4];   // compose the message
								}
								activeConnection = true;
								cg.madeConnection(getMsg[2],getMsg[3]);
							} 
							else if (getMsg[3].equals(username)) {   // To validate the appropriate Client Thread
								connectinguser = getMsg[2];
								msg = getMsg[3] + "->" + getMsg[2] + "-" + getMsg[4];  // compose the message
								activeConnection = true;
								cg.madeConnection(getMsg[3],getMsg[2]);  // update the GUI
							}
							else {   // Default to filter the applicable Client Thread
								msg = "";
								connStatus = false;
							}
						}
					}
					// if the message is related to disconnect response
					else if(getMsg[0].equals("DISCONNECTRESP")) {
						if(getMsg[1].equals(username) && getMsg[2].equals(connectinguser)) {   // if the disconneting user
							activeConnection = false;
							connectinguser = "";
							msg = "Disconnected the connection with " + getMsg[2] + " at " + getMsg[3];
							cg.terminateConnection(getMsg[1],getMsg[2]);
						}
						else if(getMsg[2].equals(username) && getMsg[1].equals(connectinguser)) {   // if the disconneted user
							activeConnection = false;
							connectinguser = "";
							msg = getMsg[1] + " has disconnected the connection with you" + " at " + getMsg[3];
							cg.terminateConnection(getMsg[2],getMsg[1]);
						}
						else {     // Default to filter the applicable Client Thread
							connStatus = false;
							msg = "";
						}
					}
					// if the message is related to logout response
					else if(getMsg[0].equals("LOGOUTRESP")) {
						if(getMsg[1].equals(connectinguser) && activeConnection == true) {  // Update the connected user about the other person's logout
							if(getMsg[2].equals("Legal")) {  // By using Logout button
								activeConnection = false;
								connectinguser = "";
								msg = getMsg[1] + " has logged out of the messenger, You are no longer connected with " + getMsg[1] + "\n";
								cg.terminateConnection(username,getMsg[1]);
							} else if(getMsg[2].equals("Illegal")) {  // By simply closing the window
								activeConnection = false;
								connectinguser = "";
								msg = getMsg[1] + " has closed the messenger illegally, You are no longer connected with " + getMsg[1] + "\n";
								cg.terminateConnection(username,getMsg[1]);
							}
						}
						else {     // Default to filter the applicable Client Thread
							connStatus = false;
							msg = "";
						}
					}
					// if the entered username conflicts with the existing one, automatically correct the username and update it
					else if(getMsg[0].equals("USERCHANGE")) {
						if(getMsg[1].equals(username)) {
							username = getMsg[2];
							msg = "Entered Username conflicted with Server, so your new username is : " + getMsg[2] + "\n";
							cg.changeMyName(getMsg[1], getMsg[2]);  // update new username to GUI
						}
						else {     // Default to filter the applicable Client Thread
							connStatus = false;
							msg = "";
						}
					}
					// if the posting message is not updated properly
					else if(getMsg[0].equals("POSTERROR")) {
						if(getMsg[1].equals(username)) {
							msg = "Your connection is not registered properly with the server for " + getMsg[2] + " , Please disconnect and connect again \n";
						}
						else {    // Default to filter the applicable Client Thread
							connStatus = false;
							msg = "";
						}
					}
					// if the message is relate to the posed chat response
					else if(getMsg[0].equals("POSTRESP")) {  /// This is the actual message exchanged between the two user
						if(getMsg[1].equals(username) && getMsg[2].equals(connectinguser)) {
							msg = getMsg[4] + " " + getMsg[1] + " : " + getMsg[3] + "\n";
						} else if(getMsg[2].equals(username) && getMsg[1].equals(connectinguser)) {
							msg = getMsg[4] + " " + getMsg[1] + " : " + getMsg[3] + "\n";
						} else {  // Default to filter the applicable Client Thread
							connStatus = false;
							msg = "";
						}
					}
					// if console mode print the message and add back the prompt
					if(connStatus == true) {
						if(cg == null) {
							System.out.println(msg);
							System.out.print("> ");
						}
						else {   // else append the message to the GUI
							cg.append(msg);
						}
					}
				}
				catch(IOException e) {
					display("Server has close your connection");  // Called when the connection is closed
					if(cg != null) 
						cg.connectionFailed();   // Reset the GUI fields
					break;
				}
				// can't happen with a String object but need the catch anyhow
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
}