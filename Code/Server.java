import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Samuel Benison Jeyaraj Victor - 1000995539
 * The server that can be run both as a console application or a GUI
 */
public class Server {
	// a unique ID for each connection
	private static int uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	private ArrayList<String> connectedUser1 = new ArrayList<String>();
	private ArrayList<String> connectedUser2 = new ArrayList<String>();
	// if I am in a GUI
	private ServerGUI sg;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// the boolean that will be turned of to stop the server
	private boolean keepGoing;
	

	/*
	 *  server constructor that receive the port to listen to for connection as parameter
	 *  in console
	 */
	public Server(int port) {
		this(port, null);
	}
	
	public Server(int port, ServerGUI sg) {
		// GUI or not
		this.sg = sg;
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		// ArrayList for the Client list
		al = new ArrayList<ClientThread>();
	}
	
	public void start() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while(keepGoing) 
			{
				// format message saying we are waiting
				display("Server waiting for Clients on port " + port + ".");
				
				Socket socket = serverSocket.accept();  	// accept connection
				// if I was asked to stop
				if(!keepGoing)
					break;
				ClientThread t = new ClientThread(socket);  // make a thread of it
				al.add(t);									// save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
						// not much I can do
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		// something went bad
		catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}		
    /*
     * For the GUI to stop the server
     */
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement 
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
			// nothing I can really do
		}
	}
	/*
	 * Display an event (not a message) to the console or the GUI
	 */
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		if(sg == null)
			System.out.println(time);
		else
			sg.appendEvent(time + "\n");
	}
	
	/*
	 *  to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		// add HH:mm:ss and \n to the message
		String time = sdf.format(new Date());
		String messageLf = message;
				
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}
	
	/*
	 *  to display the message in Server in http format
	 */
	private synchronized void httpReflect(String user1, String user2, String message) {
		String finalMsg = "";
		finalMsg = " POST     /" + message + "      HTTP/1.1 ";
		finalMsg = finalMsg + "\n Host: " + user1;
		finalMsg = finalMsg + "\n Target: " + user2;
		finalMsg = finalMsg + "\n Accept: text/html ";
		finalMsg = finalMsg + "\n User-Agent: N/A ";
		finalMsg = finalMsg + "\n Accept-Language: en-US, en ";
		finalMsg = finalMsg + "\n Accept-Charset: ISO-8859-1,utf-8 ";
		finalMsg = finalMsg + "\n ------------------------------------- \n \n";
		
		// display message on console or GUI
		if(sg == null)
			System.out.print(finalMsg);
		else
			sg.appendRoom(finalMsg);     // append in the room window
	}
	
	/*
	 *  to inform a connection to other Clients
	 */
	private synchronized void inform(String message) {
		// adding the connection info
		String messageLf = message;
		
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}

	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// found it
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}
	
	/*
	 *  To run as a console application just open a console window and: 
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	public static void main(String[] args) {
		// start server on port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		switch(args.length) {
			case 1:
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [portNumber]");
				return;
				
		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// my unique id (easier for deconnection)
		int id;
		// the Username of the Client
		String username;
		// the only type of message a will receive
		ChatMessage cm;
		// the date I connect
		String date;

		// Constructore
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				String tempusername = (String) sInput.readObject();
				username = compareName(tempusername);  // to validate if the name is conflicting with the existing names
				display(username + " just connected.");
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			// have to catch ClassNotFoundException
			// but I read a String, I am sure it will work
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString() + "\n";
		}

		// what will run forever
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			while(keepGoing) {
				// read a String (which is an object)
				try {
					cm = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					display(username + " has closed the connection illegally, Exception: " + e);
					clearConnection(username,"Illegal"); // Update the other user if a client is disconnected illegally
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// the messaage part of the ChatMessage
				String message = cm.getMessage();

				// Switch on the type of message receive
				switch(cm.getType()) {

				case ChatMessage.MESSAGE:   // To process the actual message received from the server
					String[] connectionMsg = message.split("---->");   // Getting the message and split the contents
					String broadMsg = "";
					if(connectionMsg[0].equalsIgnoreCase("POST")) {   // To check if the message is related to chat message
						boolean validity = stillConnected(connectionMsg[1],connectionMsg[2]);  // To check if the connection is still valid
						if(validity == true) {  // Send the Post reponse based on the connectivity to the client
							broadMsg = "POSTRESP" + "---->" + connectionMsg[1] + "---->" + connectionMsg[2] + "---->" + connectionMsg[3] + "---->" + sdf.format(new Date());
						} else {
							inform("POSTERROR" + "---->" + connectionMsg[1] + "---->" + connectionMsg[2]);
						}
						httpReflect(connectionMsg[1],connectionMsg[2],connectionMsg[3]);  // Update the message to the server side
						broadcast(broadMsg);  // Update the message to the client side
					}
					break;
				case ChatMessage.LOGOUT:   // To process the Logout action in Server side
					display(username + " disconnected with a LOGOUT message.");
					clearConnection(username,"Legal");  // Update the other connected user (if any)
					keepGoing = false;  // Stop the loop
					break;
				case ChatMessage.CONNECTTO:   // To process the Connect To action in Server side
					int count1 = 0, count2 = 0;
					String[] connectMsg = message.split("---->");  // Get the message and split the content
					// if the message is related to a new connection
					if(connectMsg[0].equalsIgnoreCase("CONNECTTO")) {
						for(int i = 0; i < al.size(); i++) {  // To check the username in the Client Thread List
							ClientThread ct = al.get(i);
							if(connectMsg[1].equalsIgnoreCase(ct.username)) {
								count1 = 1;
							}
						}
						for(int i = 0; i < al.size(); i++) {      // To check the username in the Client Thread List
							ClientThread ct = al.get(i);
							if(connectMsg[2].equalsIgnoreCase(ct.username)) {
								count2 = 1;
							}
						}
						if(count1 == 0)
						{
							inform("CONNECTTORESP" + "---->" + "FAILED 001" + "---->" + connectMsg[1]);  // if the login is invalid
						}
						else if(count2 == 0)
						{
							inform("CONNECTTORESP" + "---->" + "FAILED 002" + "---->" + connectMsg[1] + "---->" + connectMsg[2]);   /// if the connecting user is invalid
						}
						else if(count1 == 1 && count2 == 1)
						{
							int valid = validateConnection(connectMsg[1],connectMsg[2]);   /// To validate the connection nd send the response based on the result
							if(valid == 1) {   // if the connection already exists
								inform("CONNECTTORESP" + "---->" + "FAILED 003" + "---->" + connectMsg[1] + "---->" + "Connection already existis for " + connectMsg[1] + " and " + connectMsg[2] + "\n");
							}
							else if(valid == 2) {
								inform("CONNECTTORESP" + "---->" + "FAILED 003" + "---->" + connectMsg[1] + "---->" + "Connection already existis for " + connectMsg[1] + "\n");
							}
							else if(valid == 3) {
								inform("CONNECTTORESP" + "---->" + "FAILED 003" + "---->" + connectMsg[1] + "---->" + "Connection already existis for " + connectMsg[2] + "\n");
							}
							else if(valid == 4) {   // Valid connection response
								inform("CONNECTTORESP" + "---->" + "PASSED" + "---->" + connectMsg[1] + "---->" + connectMsg[2] + "---->" + "Connection Established at " + sdf.format(new Date()) + "\n");
								display("Connection Established between " + connectMsg[1] + " and " + connectMsg[2] + " at " + sdf.format(new Date()));
								connectedUser1.add(connectMsg[1]);  // Add the host user to the array list
								connectedUser2.add(connectMsg[2]);  //  Add the targer user to the array list
							}
						}
					}
					// if the message is related to terminate an existing connection
					else if(connectMsg[0].equalsIgnoreCase("DISCONNECT")) {
						for(int i = 0; i < connectedUser1.size(); i++) {  // Check the names from the existing connection list
							if(connectMsg[1].equalsIgnoreCase(connectedUser1.get(i))) {
								connectedUser1.remove(i);   // Remove the name from the list
							} else if(connectMsg[2].equalsIgnoreCase(connectedUser1.get(i))) {
								connectedUser1.remove(i);  // Remove the name from the list
							}
						}
						for(int i = 0; i < connectedUser2.size(); i++) {  // Check the names from the existing connection list
							if(connectMsg[1].equalsIgnoreCase(connectedUser2.get(i))) {
								connectedUser2.remove(i);  // Remove the name from the list
							} else if(connectMsg[2].equalsIgnoreCase(connectedUser2.get(i))) {
								connectedUser2.remove(i);  // Remove the name from the list
							}
						}
						inform("DISCONNECTRESP" + "---->" + connectMsg[1] + "---->" + connectMsg[2] + "---->" + sdf.format(new Date()) + "\n");  // Send the Disconnect response to client
						display("Connection Disconnected between " + connectMsg[1] + " and " + connectMsg[2] + " at " + sdf.format(new Date()));  // Update the response on server side
					}
					break;
				case ChatMessage.WHOISIN:  // To list the available user for the client
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					// scan al the users connected
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
					}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			close();
		}
		
		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
		
		/*
		 * Check the username with the existing client name
		 */
		 private String compareName(String currUsername) {
			int status = 0, count = 1;
			String newUsername = currUsername, prevUsername = currUsername;
			do {
				for(int i = 0; i < al.size(); i++) {
					ClientThread ct = al.get(i);
					if(newUsername.equalsIgnoreCase(ct.username)) {  // Change the username if that already exists
						String counter = Integer.toString(count);
						newUsername = currUsername + counter;
						count = count + 1;
					}
				}
				if(!prevUsername.equalsIgnoreCase(newUsername)) {
					prevUsername = newUsername;
				}
				else {
					status = 1;
				}
			}while(status == 0);
			if(!currUsername.equalsIgnoreCase(newUsername)) {  // To update the change of username to the client
				writeMsg("USERCHANGE" + "---->" + currUsername + "---->" + newUsername);
			}
			return newUsername;
		}
		
		/*
		 * Check the availability of connection with the other client
		 */
		private int validateConnection(String requester, String receiver) {
			int match1 = 0, match2 = 0;
			for(int i=0;i<connectedUser1.size();i++) {
				if(requester.equals(connectedUser1.get(i))) {
					match1 = 1;
				} else if(receiver.equals(connectedUser1.get(i))) {
					match2 = 1;
				}
			}
			
			for(int i=0;i<connectedUser2.size();i++) {
				if(requester.equals(connectedUser2.get(i))) {
					match1 = 1;
				} else if(receiver.equals(connectedUser2.get(i))) {
					match2 = 1;
				}
			}
			
			if(match1 == 1 && match2 == 1) {
				return 1;
			}
			else if(match1 == 1 && match2 == 0) {
				return 2;
			}
			else if(match1 == 0 && match2 == 1) {
				return 3;
			}
			else
			{
				return 4;
			}
		}
		
		/*
		 * To check the connection info given from the client
		 */
		private boolean stillConnected(String requester, String receiver) {
			int match = 0;
			for(int i=0;i<connectedUser1.size();i++) {
				if(requester.equals(connectedUser1.get(i))) {
					if(receiver.equals(connectedUser2.get(i))) {
						match = 1;
					}
				}
			}
			if(match == 0) {
				for(int i=0;i<connectedUser2.size();i++) {
					if(requester.equals(connectedUser2.get(i))) {
						if(receiver.equals(connectedUser1.get(i))) {
							match = 1;
						}
					}
				}
			}
			if(match == 1){
				return true;
			} else {
				return false;
			}
		}
		
		/*
		 * Removing the corresponding Connection when the person is logged out
		 */
		private void clearConnection(String loggedoutuser, String discType) {
			int match1 = 0, match2 = 0;
			for(int i=0;i<connectedUser1.size();i++) {
				if(loggedoutuser.equals(connectedUser1.get(i))) {
					match1 = 1;
					match2 = 1;
					connectedUser1.remove(i);
					connectedUser2.remove(i);
				}
			}
			if(match2 == 0) {
				for(int i=0;i<connectedUser2.size();i++) {
					if(loggedoutuser.equals(connectedUser2.get(i))) {
						match1 = 1;
						connectedUser2.remove(i);
						connectedUser1.remove(i);
					}
				}
			}
			if(match1 == 1) {  // Update the Logout response to the connected user
				inform("LOGOUTRESP" + "---->" + loggedoutuser + "---->" + discType);
			}
		}
	}
}