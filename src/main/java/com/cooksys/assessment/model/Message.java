package com.cooksys.assessment.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.server.ClientHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

//import com.cooksys.assessment.server.MessageColors;

//import java.io.PrintWriter;

public class Message {
	
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private String username;
	private String command;
	private String contents;
	private String color;
	
	private String timestamp;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
	
	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public synchronized void disconnect(Map<String, Socket> connectedUsers) {
		log.info("user <{}> disconnected", this.getUsername());
		// Write to the other users:
		this.setColor(MessageColors.disconnect.color());
		for (String userAnother : connectedUsers.keySet()) {
			try {
				PrintWriter writerOther = new PrintWriter(connectedUsers.get(userAnother).getOutputStream());
				writerOther.write(new ObjectMapper().writeValueAsString(this));
				writerOther.flush();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void connect(Map<String, Socket> connectedUsers) {
		this.setColor(MessageColors.connect.color());
		for (String userAnother : connectedUsers.keySet()) {
			try {
				PrintWriter writerOther = new PrintWriter(connectedUsers.get(userAnother).getOutputStream());
				writerOther.write(new ObjectMapper().writeValueAsString(this));
				writerOther.flush();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.info("user <{}> connected", this.getUsername());
	}
	
	public synchronized void users(PrintWriter writer, Map<String, Socket> connectedUsers) throws JsonProcessingException {
		log.info("user <{}> requested a list of currently connected users", this.getUsername());
		this.setColor(MessageColors.users.color());
		this.setUsername(new ObjectMapper().writeValueAsString(connectedUsers.keySet()));
		writer.write(new ObjectMapper().writeValueAsString(this));
		writer.flush();
	}
	
	public synchronized void whisper(String user2, PrintWriter writer, Map<String, Socket> connectedUsers) {
		this.setCommand("@");
		this.setColor(MessageColors.whisper.color());
		try {
			if (user2 == this.username) { // whisper to yourself:
				log.info("user <{}> whispered to himself message: <{}>", this.username, this.contents);
				writer.write(new ObjectMapper().writeValueAsString(this));
				writer.flush();
			} else { // Whisper to another user:
				log.info("user <{}> whispered to user <{}> message: <{}>", this.getUsername(), user2,
						this.getContents());
				PrintWriter writerOther = new PrintWriter(connectedUsers.get(user2).getOutputStream());
				writerOther.write(new ObjectMapper().writeValueAsString(this));
				writerOther.flush();
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public synchronized void echo(PrintWriter writer) {
		log.info("user <{}> echoed message <{}>", this.getUsername(), this.getContents());
		this.setColor(MessageColors.echo.color());
		try {
			writer.write(new ObjectMapper().writeValueAsString(this));
			writer.flush();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	public synchronized void broadcast(Map<String, Socket> connectedUsers) {
		// Write to Server:
		log.info("user <{}> broadcast message <{}>", this.getUsername(), this.getContents());
		// Write to the other users:
		this.setColor(MessageColors.broadcast.color());
		for (String userAnother : connectedUsers.keySet()) {
			try {
				PrintWriter writerOther = new PrintWriter(
						connectedUsers.get(userAnother).getOutputStream());
				writerOther.write(new ObjectMapper().writeValueAsString(this));
				writerOther.flush();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
