package com.cooksys.assessment.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

//import com.cooksys.assessment.server.MessageColors;

//import java.io.PrintWriter;

public class Message {

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
	
	public synchronized void broadcast(Map<String, Socket> connectedUsers) {
		// Write to Server:
//		log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
		// Write to the other users:
		this.setColor(MessageColors.broadcast.color());
		for (String userAnother : connectedUsers.keySet()) {
			try {
				PrintWriter writerOther = new PrintWriter(
						connectedUsers.get(userAnother).getOutputStream());
				writerOther.write(new ObjectMapper().writeValueAsString(this));
				writerOther.flush();
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
enum MessageColors {
	alert("red"), warning("yellow"), users("blue"), echo("white"), broadcast("cyan"), whisper("gray"), 
	connect("green"), disconnect("magenta");

	private String color;

	MessageColors(String color) {
		this.color = color;
	}

	public String color() {
		return color;
	}
}
