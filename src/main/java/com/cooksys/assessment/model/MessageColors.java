package com.cooksys.assessment.model;

public enum MessageColors {
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