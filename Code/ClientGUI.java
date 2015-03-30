import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/*
 * Samuel Benison Jeyaraj Victor - 1000995539
 * The Client with its GUI
 */
public class ClientGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	// will first hold "Username:", later on "Enter message"
	private JLabel label;
	// to hold the Username and later on the messages
	private JTextField tf;
	// to hold the server address an the port number
	private JTextField tfServer, tfPort;
	// to Logout and get the list of the users
	private JButton login, logout, whoIsIn, connectTo;
	// for the chat room
	private JTextArea ta;
	// if it is for connection
	private boolean connected;
	// the Client object
	private Client client;
	// the default port number
	private int defaultPort;
	private String defaultHost;
	// the Username value
	private String username;
	// the Connecting User value
	private String connectinguser = "";
	// to inform an active connection
	private boolean activeConnection = false;

	// Constructor connection receiving a socket number
	ClientGUI(String host, int port) {

		super("Chat Client");
		defaultPort = port;
		defaultHost = host;
		
		// The NorthPanel with:
		JPanel northPanel = new JPanel(new GridLayout(3,1));
		// the server name anmd the port number
		JPanel serverAndPort = new JPanel(new GridLayout(1,5, 1, 3));
		// the two JTextField with default value for server address and port number
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Server Address:  "));
		serverAndPort.add(tfServer);
		serverAndPort.add(new JLabel("Port Number:  "));
		serverAndPort.add(tfPort);
		serverAndPort.add(new JLabel(""));
		// adds the Server an port field to the GUI
		northPanel.add(serverAndPort);

		// the Label and the TextField
		label = new JLabel("Enter your username below", SwingConstants.CENTER);
		northPanel.add(label);
		tf = new JTextField("");
		tf.setBackground(Color.WHITE);
		northPanel.add(tf);
		add(northPanel, BorderLayout.NORTH);

		// The CenterPanel which is the chat room
		ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1,1));
		centerPanel.add(new JScrollPane(ta));
		ta.setEditable(false);
		add(centerPanel, BorderLayout.CENTER);

		// the 3 buttons
		login = new JButton("Login");
		login.addActionListener(this);
		logout = new JButton("Logout");
		logout.addActionListener(this);
		logout.setEnabled(false);		// you have to login before being able to logout
		whoIsIn = new JButton("Who is in");
		whoIsIn.addActionListener(this);
		whoIsIn.setEnabled(false);		// you have to login before being able to Who is in
		connectTo = new JButton("Connect To");
		connectTo.addActionListener(this);
		connectTo.setEnabled(false);		// you have to login before being able to Connect with other user

		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(whoIsIn);
		southPanel.add(connectTo);
		add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		tf.requestFocus();

	}

	// called by the Client to append text in the TextArea 
	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}
	
	// adjusting the name when conflicting with the existing 
	void changeMyName(String user1, String user2) {
		if(user1.equalsIgnoreCase(username)) {
			username = user2;
		}
	}
	
	// called by the Client to update the GUI when connection is made 
	void madeConnection(String user1, String user2) {
		if(!user1.equalsIgnoreCase(username)) {
			username = user1;
		}
		activeConnection = true;
		connectinguser = user2;
		connectTo.setText("Disconnect");
		label.setText("Enter your message below");
		tf.setText("");
	}
	
	// called by the Client to update the GUI when connection is terminated 
	void terminateConnection(String user1, String user2) {
		if(user1.equalsIgnoreCase(username)) {
			if(user2.equalsIgnoreCase(connectinguser)) {
				activeConnection = false;
				connectinguser = "";
			}
		}
		connectTo.setText("Connect To");
		label.setText("Enter the name of the person and click Connect");
	}
	
	// called by the GUI is the connection failed
	// this will reset our buttons, label, textfield
	void connectionFailed() {
		username = "";
		connectinguser = "";
		activeConnection = false;
		connectTo.setText("Connect To");
		login.setEnabled(true);
		logout.setEnabled(false);
		whoIsIn.setEnabled(false);
		connectTo.setEnabled(false);
		label.setText("Enter your username below");
		//tf.setText("Anonymous");
		// reset port number and host name as a construction time
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		// let the user change them
		tfServer.setEditable(true);
		tfPort.setEditable(true);
		// don't react to a <CR> after the username
		tf.removeActionListener(this);
		connected = false;
	}
		
	/*
	* Button or JTextField clicked
	*/
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		// if it is the Logout button
		if(o == logout) {
			client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
			connectionFailed();
			return;
		}
		// if it is the Connect To button
		if(o == connectTo) {
			String action = (String) connectTo.getText();
			// Acion to be performed when the button is Connect TO
			if(action.equals("Connect To")) {
				if(username.equalsIgnoreCase(tf.getText())) {   // When the username is same as the connecting name
					append("You cannot connect with yourself \n");
				} else {
					String sendingMsg = "CONNECTTO" + "---->" + username + "---->" + tf.getText();
					client.sendMessage(new ChatMessage(ChatMessage.CONNECTTO, sendingMsg));   // Sending the message as an object through ChatMessage
				}
			}
			// Acion to be performed when the button is Disconnect
			else if(action.equals("Disconnect")) {
				if(activeConnection == true) {
					String sendingMsg = "DISCONNECT" + "---->" + username + "---->" + connectinguser;
					client.sendMessage(new ChatMessage(ChatMessage.CONNECTTO, sendingMsg));
				}
				else {
					append("No Active Connection to disconnect");
				}
			}
			return;
		}
		// if it the who is in button
		if(o == whoIsIn) {
			client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));				
			return;
		}

		// ok it is coming from the JTextField
		if(connected) {
			if(activeConnection == false){
				append("You don't have active connection to send messages \n");   // If the connection is not made
			} else {
				// just have to send the message
				String sendingMsg = "POST" + "---->" + username + "---->" + connectinguser + "---->" + tf.getText();
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, sendingMsg));
			}
			tf.setText("");
			return;
		}
		
		// Action while pressing the Login button
		if(o == login) {
			// ok it is a connection request
			username = tf.getText().trim();
			// empty username ignore it
			if(username.length() == 0)
				return;
			// empty serverAddress ignore it
			String server = tfServer.getText().trim();
			if(server.length() == 0)
				return;
			// empty or invalid port numer, ignore it
			String portNumber = tfPort.getText().trim();
			if(portNumber.length() == 0)
				return;
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			}
			catch(Exception en) {
				return;   // nothing I can do if port number is not valid
			}

			// try creating a new Client with GUI
			client = new Client(server, port, username, this);
			// test if we can start the Client
			if(!client.start()) 
				return;
			tf.setText("");
			label.setText("Enter the name of the person and click Connect");
			connected = true;
			
			// disable login button
			login.setEnabled(false);
			// enable the 2 buttons
			logout.setEnabled(true);
			whoIsIn.setEnabled(true);
			connectTo.setEnabled(true);
			// disable the Server and Port JTextField
			tfServer.setEditable(false);
			tfPort.setEditable(false);
			// Action listener for when the user enter a message
			tf.addActionListener(this);
		}

	}

	// to start the whole thing the server
	public static void main(String[] args) {
		new ClientGUI("localhost", 1500);
	}

}