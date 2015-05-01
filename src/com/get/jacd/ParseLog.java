package com.get.jacd;

import android.app.TabActivity;

import com.parse.ParseObject;

public class ParseLog {
	
	private final static String TABLE_NAME = "Log";
	
	private final static String EMAIL_ = "Email";
	private final static String TIME_ = "Timestamp";
	private final static String LATITUDE_ = "Latitude";
	private final static String LONGITUDE_ = "Longitude";
	private final static String SCREEN_ = "Screen";
	private final static String DETAILS_ = "Details";
	

	public static void Log(String username, double time, String screen, String details) {
		ParseObject gameScore = new ParseObject(TABLE_NAME);
		gameScore.put(EMAIL_,username);
		gameScore.put(TIME_,time);
		gameScore.put(SCREEN_,screen);
		gameScore.put(DETAILS_,details);
		gameScore.saveEventually();
	}
	
	public static void Log(String username, double time, double lat, double lon) {
		ParseObject gameScore = new ParseObject(TABLE_NAME);
		gameScore.put(EMAIL_,username);
		gameScore.put(TIME_,time);
		gameScore.put(LATITUDE_,lat);
		gameScore.put(LONGITUDE_,lon);
		gameScore.saveEventually();
	}
}
