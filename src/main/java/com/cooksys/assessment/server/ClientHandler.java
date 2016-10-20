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
import java.util.Map;

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
			String previousCommand = "";

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				String command = message.getCommand();
				String user = message.getUsername();
				boolean repeatCommand = true;
				while (repeatCommand) {
					repeatCommand = false;
					switch (command) {
					case "connect":
						// Checking out if the user is already connected:
						if (connectedUsers.keySet().contains(user)) {
							log.info("user <{}> is rejected because already connected", user);
							message.setContents(df.format(new Date()) + ": <" + user + "> is already connected.");
							String response = mapper.writeValueAsString(message);
							writer.write(response);
							writer.flush();
							this.socket.close();
							break;
						}
						// add user to the list:

						// Write to the other users:
						message.setContents(df.format(new Date()) + ": <" + user + "> has connected."); // ugly
						for (String userAnother : connectedUsers.keySet()) {
							PrintWriter writerOther = new PrintWriter(
									connectedUsers.get(userAnother).getOutputStream());
							writerOther.write(mapper.writeValueAsString(message));
							writerOther.flush();
						}
						connectedUsers.put(user, socket);
						log.info("user <{}> connected", message.getUsername());
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						connectedUsers.remove(message.getUsername());
						// Write to the other users:
						message.setContents(
								df.format(new Date()) + ": <" + message.getUsername() + "> has disconnected."); // ugly
																												// formatted
																												// output
						for (String userAnother : connectedUsers.keySet()) {
							PrintWriter writerOther = new PrintWriter(
									connectedUsers.get(userAnother).getOutputStream());
							writerOther.write(mapper.writeValueAsString(message));
							writerOther.flush();
						}
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						// Date dateobj = new Date();
						message.setContents(df.format(new Date()) + " <" + message.getUsername() + "> (echo): "
								+ message.getContents()); // ugly formatted
															// output
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						previousCommand = command;
						break;
					case "users":
						log.info("user <{}> requested a list of currently connected users", message.getUsername());
						// that is ugly formatted line (to be improved):
						message.setContents(df.format(new Date()) + ": currently connected users: \n" + connectedUsers
								.keySet().toString().replace("[", "").replace("]", "").replace(", ", "\n"));
						writer.write(mapper.writeValueAsString(message));
						writer.flush();
						previousCommand = command;
						break;
					case "broadcast":
						log.info("user <{}> connected", message.getUsername());
						// Write to the other users:
						// `${timestamp} <${username}> (all): ${contents}`
						message.setContents(df.format(new Date()) + " <" + user + "> (all): " + message.getContents()); // ugly
						for (String userAnother : connectedUsers.keySet()) {
							PrintWriter writerOther = new PrintWriter(
									connectedUsers.get(userAnother).getOutputStream());
							writerOther.write(mapper.writeValueAsString(message));
							writerOther.flush();
						}
						previousCommand = command;
						break;
					case "": // in case if it happens
						log.info("user <{}> missed a command", message.getUsername());
						message.setContents("a command is required");
						writer.write(mapper.writeValueAsString(message));
						writer.flush();
						break;
					case "@":
						log.info("user <{}> whispered to nobody message: <{}>", user, message.getContents());
						message.setContents("No username is provided. The proper usage is <@username> <message>");
						writer.write(mapper.writeValueAsString(message));
						writer.flush();
						break;
					default:
						if (command.charAt(0) != '@') { // Unknown command:
							if (previousCommand != "") {
								message.setCommand(previousCommand);
								message.setContents(command + " " + message.getContents());
								command = previousCommand;
								repeatCommand = true;
								break;
							}
							log.info("user <{}> gave non-recognised command <{}>", user, command);
							message.setContents(
									df.format(new Date()) + ": Server doesn't recognise command <" + command + ">.");
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
							break;
						}
						// Whispering:
						String user2 = command.substring(1);
						previousCommand = command;
						if (!connectedUsers.containsKey(user2)) {
							log.info("user <{}> whispered to non-connected user <{}> message <{}>", user, user2,
									message.getContents());
							message.setContents(df.format(new Date()) + ": user <" + user2 + "> is not connected");
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
							break;
						}
						if (user2 == user) {
							log.info("user <{}> whispered to himself message: <{}>", user, message.getContents());
						} else {
							log.info("user <{}> whispered to user <{}> message: <{}>", message.getUsername(), user2,
									message.getContents());
						}
						message.setContents(
								df.format(new Date()) + " <" + user + "> (whisper): " + message.getContents()); // ugly
						// Write to the other user:
						PrintWriter writerOther = new PrintWriter(connectedUsers.get(user2).getOutputStream());
						writerOther.write(mapper.writeValueAsString(message));
						writerOther.flush();
						break;
					} // switch end
				} // while repeat command end
			} // while end
		} catch (IOException e) {
		//	users.remove(message.getUsername());					//remove user from the list
			log.error("Something went wrong :/", e);
		}
	}

}
