package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Timestamp;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.cooksys.assessment.model.MessageColors;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private Map<String, Socket> connectedUsers;

	public ClientHandler(Socket socket, Map<String, Socket> connectedUsers) {
		super();
		this.socket = socket;
		this.connectedUsers = connectedUsers;
	}

//	final DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");  // Date format for timestamp

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			String previousCommand = "";

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				message.setTimestamp(new Timestamp(new Date().getTime()).toString());

				String command = message.getCommand();
				String user = message.getUsername();
				boolean repeatCommand = true;
				while (repeatCommand) {
					repeatCommand = false;
					switch (command) {
					case "connect":
						// Checking out if the user is already connected:
						if (connectedUsers.keySet().contains(user)) {
							message.setColor(MessageColors.alert.color());
							message.setCommand("");
							message.setContents("<" + user + "> is already connected.");
							String response = mapper.writeValueAsString(message);
							synchronized (message) {
								writer.write(response);
								writer.flush();
								log.info("user <{}> is rejected because already connected", user);
							}
							this.socket.close();
							break;
						}
						// add user to the list:

						// Write to the other users:
						message.connect(connectedUsers);
						connectedUsers.put(user, socket);
						break;
					case "disconnect":
						message.disconnect(connectedUsers);
						connectedUsers.remove(message.getUsername());
						this.socket.close();
						break;
					case "echo":
						message.echo(writer);
						previousCommand = command;
						break;
					case "users":
						message.users(writer, connectedUsers);
						previousCommand = command;
						break;
					case "broadcast":
//						log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
						message.broadcast(connectedUsers);
						previousCommand = command;
						break;
					case "": // in case if it happens
						message.setColor(MessageColors.warning.color());
						message.setContents("a command is required");
						synchronized (message) {
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
							log.info("user <{}> missed a command", message.getUsername());
						}
						break;
					case "@":
						message.setColor(MessageColors.warning.color());
						message.setCommand("");
						message.setContents("No username is provided. The proper usage is <@username message>");
						synchronized (message) {
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
							log.info("user <{}> whispered to nobody message: <{}>", user, message.getContents());
						}
						previousCommand = command;
						break;
					default:
						if (command.charAt(0) != '@') { // Unknown command:
							if (previousCommand != "") { // Checking for a previous command:
								message.setCommand(previousCommand);
								message.setContents(command + " " + message.getContents());
								command = previousCommand;
								repeatCommand = true;
								break;
							} // if no Previous command is available:
							
							message.setColor(MessageColors.warning.color());
							message.setCommand("");
							message.setContents("Server doesn't recognise command <" + command + ">.");
							synchronized (message) {
								writer.write(mapper.writeValueAsString(message));
								writer.flush();
								log.info("user <{}> gave non-recognised command <{}>", user, command);
							}
							break;
						}
						// Whispering:
						String user2 = command.substring(1);
						previousCommand = command;
						if (!connectedUsers.containsKey(user2)) { // destination user not found
							message.setColor(MessageColors.warning.color());
							message.setCommand("");
							message.setContents("user <" + user2 + "> is not connected.");
							synchronized (message) {
								writer.write(mapper.writeValueAsString(message));
								writer.flush();
								log.info("user <{}> whispered to non-connected user <{}> message <{}>", user, user2,
										message.getContents());
							}
							break;
						}
						message.whisper(user2, writer, connectedUsers);
						break;
					} // switch end
				} // while repeat command end
			} // while end
		} catch (IOException e) {
			log.error("ClientHandler: Something went wrong :/", e);
		}
	}

}
