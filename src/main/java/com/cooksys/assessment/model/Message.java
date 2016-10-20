package com.cooksys.assessment.model;

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
	
//	public void send(PrintWriter writer, Message message) {
//		writer.write(mapper.writeValueAsString(message));
//		writer.flush();
//	}
}
