package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
//import java.security.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}
	
//	static Set<String> users = new HashSet<String>();
	static Map<String, Socket> connectedUsers = new HashMap<String, Socket>();

	public void run() {
		try {
			

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");  // Date format for timestamp
//			String previousCommand = "";

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
														// Recording a single command pre-history:
				String command = message.getCommand();
//				command = (command == "" || command == null) ? previousCommand : command; //doesn't work yet: empty command is not passed to the server
//				previousCommand = command;           // This is moved to Client.
				
				switch (command) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						//Write to the other users:	
						message.setContents( df.format(new Date()) + ": <" + message.getUsername() + "> has connected." ); //ugly formatted output
					for (String user : connectedUsers.keySet()) {
						PrintWriter writerOther = new PrintWriter(connectedUsers.get(user).getOutputStream());
						writerOther.write(mapper.writeValueAsString(message));
						writerOther.flush();
					}
					//add user from the list:
						connectedUsers.put(message.getUsername(), socket);
						break;
					case "disconnect": 
						log.info("user <{}> disconnected", message.getUsername());
						connectedUsers.remove(message.getUsername());
						//Write to the other users:	
						message.setContents( df.format(new Date()) + ": <" + message.getUsername() + "> has disconnected."); //ugly formatted output
					for (String user : connectedUsers.keySet()) {
						PrintWriter writerOther = new PrintWriter(connectedUsers.get(user).getOutputStream());
						writerOther.write(mapper.writeValueAsString(message));
						writerOther.flush();
					}
						this.socket.close();
						break;
					case "echo": 
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						// Date dateobj = new Date();
						message.setContents( df.format(new Date()) + " <" + message.getUsername() + "> (echo): " + message.getContents()); //ugly formatted output
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "users": 
						log.info("user <{}> requested a list of currently connected users", message.getUsername());
						message.setContents(df.format(new Date()) + ": currently connected users: \n" + 
						connectedUsers.keySet().toString().replace("[", "").replace("]", "").replace(", ", "\n")); //that is ugly, will correct later
						String response1 = mapper.writeValueAsString(message);
						writer.write(response1);
						writer.flush();
						break;
//					case "":									 // in case if no commands in pre-history
//						log.info("user <{}> missed a command", message.getUsername());
//						message.setContents("a command is required"); 
//						writer.write(mapper.writeValueAsString(message));
//						writer.flush();
//						break;
					default:
						if (command.charAt(0) == '@') {        // whisper:
							String user2 = command.substring(1);
							log.info("user <{}> whispered to user <{}> message: <{}>", message.getUsername(), user2, message.getContents());
//							message.setCommand("echo");
							message.setContents( df.format(new Date()) + " <" + message.getUsername() + "> (whisper): " + message.getContents()); //ugly formatted output
							//Write to the other user:							
							PrintWriter writerOther = new PrintWriter(connectedUsers.get(user2).getOutputStream());
							writerOther.write(mapper.writeValueAsString(message));
							writerOther.flush();
							break;
						}
						log.info("Java doesn't recognize the command: <{}>", command);
						break;
				}
			}

		} catch (IOException e) {
		//	users.remove(message.getUsername());					//remove user from the list
			log.error("Something went wrong :/", e);
		}
	}

}
