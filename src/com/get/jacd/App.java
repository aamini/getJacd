package com.get.jacd;

import com.flurry.android.FlurryAgent;
import com.parse.Parse;

import android.app.Application;
import android.util.Log;

public class App extends Application { 
	
	public final static String FLURRY_ID = "JF4J366GH4R34GTNF5P7";
	
    @Override public void onCreate() { 
        super.onCreate();

        //Initialize Parse
		Parse.enableLocalDatastore(this);
		Parse.initialize(this, "sNrmuyZNFlHDBKnm6zWK9PAw268mizNbaGhV5yCA", "45eBZjg49zX6zNjpgQQfMkAFRFWAq2QrRMatxolZ");
		
		//Initialize Flurry
	    FlurryAgent.init(this, FLURRY_ID );
	    FlurryAgent.setLogEnabled(true);
	    FlurryAgent.setLogEvents(true);
	    FlurryAgent.setLogLevel(Log.VERBOSE);
		
    }
} 

