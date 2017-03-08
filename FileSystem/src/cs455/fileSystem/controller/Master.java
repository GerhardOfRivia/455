package cs455.fileSystem.controller;

import java.io.BufferedReader;

/**
 * 
 * @author G van Andel
 *
 */

import java.io.IOException;
import java.io.InputStreamReader;

public class Master {
	
	// Class variables *************************************************

	final public static boolean debug = false;

	// Instance variables **********************************************

	private MasterServer server;

	// Constructors ****************************************************

	/**
	 * Constructs an instance of the registry server.
	 *
	 * @param server
	 * 		The server for the nodes to connect to.
	 */

	public Master(MasterServer server) {
		this.server = server;
	}

	// Instance methods ************************************************
	
	public void runConsole() {
		try {
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			String message;
			while (true) {
				message = fromConsole.readLine();
				if (message.trim().equals(""))
					continue;
				
				getAction(message);
				
			}
		} catch(Exception e) {
			System.out.println("Unexpected error while reading from console!");
		}
	}
	
	/**
	 *            
	 * This method implements the commands for the server. Called when a command 
	 * is entered into the server while listening for connections.
	 *
	 * @param message
	 *            The message received from the client with a # at index 0.
	 */

	private void getAction(String message){
		String[] tokens = message.split(" ");
		switch(tokens[0]){
			case "get-port":
				if (tokens.length == 1) {
					System.out.println(server.getPort());
				} else {
					this.invalid(message);
				}
				break;
			case "get-host":
				if (tokens.length == 1) {
					System.out.println(server.getHost());
				} else {
					this.invalid(message);
				}
				break;
			default:
				this.invalid(message);
		}
	}
	
	/**
	 * It displays small help onto the screen.
	 *
	 * @param message
	 *            The string to be displayed with error format.
	 */
	public void invalid(String message) {
		String info = "invalid command \"" + message + "\" try:\n";
		info += "\t[ setup-overlay | send-overlay-link-weights | start # ]\n";
		info += "\t[ list-messaging | list-weights | print-stats | get-port | get-host ]";
		System.err.println(info);
	}

	
	/**
	 * It closes the server.
	 */
	public void shutdown() throws IOException {
		server.interrupt();
	}

	public static void main(String [] args) {
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException ex) {
			System.err.println("Port argument must be a number.");
			System.exit(1);
		} catch (ArrayIndexOutOfBoundsException ex) {
			port = 60100;
		}
		
		MasterServer server = null;
		
		try {
			server = new MasterServer(port);
			server.listen();
			server.start();
		} catch (IOException ex) {
			System.out.println(ex.toString());
		}
		
		Master r = new Master(server);
		r.runConsole();
	}
	
}
