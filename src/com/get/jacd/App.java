package com.get.jacd;

import android.app.Application;

import com.parse.Parse; 

public class App extends Application { 

    @Override public void onCreate() { 
        super.onCreate();

		Parse.enableLocalDatastore(this);
		Parse.initialize(this, "sNrmuyZNFlHDBKnm6zWK9PAw268mizNbaGhV5yCA", "45eBZjg49zX6zNjpgQQfMkAFRFWAq2QrRMatxolZ");
    }
} 