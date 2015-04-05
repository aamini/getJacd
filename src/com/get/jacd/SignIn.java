package com.get.jacd;

import java.util.List;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;

public class SignIn extends Activity {

	private final static int PICK_ACCOUNT_REQUEST = 1;
	public final static String PREFS_NAME = "SIGN_IN_PREFRENCES";
	private final static String PREF_EMAIL = "SAVED_EMAIL_ADDRESS";
	
	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, App.FLURRY_ID);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_in);
		
        SharedPreferences pref = getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
        String rememberedEmail = pref.getString(PREF_EMAIL, null);
        if (rememberedEmail!=null) {
        	signIn(rememberedEmail,true);
        }
        
		
		ImageButton button = (ImageButton) findViewById(R.id.button_sign_in);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent googlePicker =AccountPicker.newChooseAccountIntent(null,null,
					new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},true,null,null,null,null) ;
				startActivityForResult(googlePicker,PICK_ACCOUNT_REQUEST);
			}
		});
	}

    @Override
    protected void onActivityResult(final int requestCode, 
                                    final int resultCode, final Intent data) {
        if (requestCode == PICK_ACCOUNT_REQUEST && resultCode == RESULT_OK && isNetworkAvailable() ) {
        	String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        	
        	CheckBox rememberCheck = (CheckBox) findViewById(R.id.remember_me_check);
        	signIn(accountName,rememberCheck.isChecked());
        }
    }

    
    /**
     * Signs into app with accountName
     * @param accountName account to sign in with
     */
    private void signIn(String accountName, boolean rememberMe) {
    	ProgressDialog progress = new ProgressDialog(this);
    	progress.setTitle("Loading");
    	progress.setMessage("Signing In...");
    	progress.setCancelable(false);
    	progress.show();
    	
    	ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
    	query.whereEqualTo("Email", accountName);
    	try {
			List<ParseObject> users = query.find();

			if (users.size()==0) { //new user
				ParseObject user = new ParseObject("User");
				user.put("Email", accountName);
				user.save();
			} else { //user already exists
				//TODO: move directly to map
				Intent myIntent = new Intent(SignIn.this, MapsActivity.class);
	    	    myIntent.putExtra("email", accountName); //Optional parameters
	    	    SignIn.this.startActivity(myIntent);
	    	    finish();
	    	    return;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
    	
    	if (rememberMe) {
    		getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    		.edit()
    		.putString(PREF_EMAIL, accountName)
    		.commit();
    	} else {
    		getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    		.edit()
    		.clear()
    		.commit();
    	}
    	
    	progress.dismiss();
    	
        //start profile setup intent 
        Intent myIntent = new Intent(SignIn.this, UserProfile.class);
        myIntent.putExtra("email", accountName); //Optional parameters
        SignIn.this.startActivity(myIntent);
        Toast.makeText(getApplication(), "Signed in successfully!", Toast.LENGTH_SHORT).show();
        finish();	
        return;
	}

	/**
     * Check if network is available on device
     * @return true if network is connected, false otherwise
     */
	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		boolean available = activeNetworkInfo != null
				&& activeNetworkInfo.isConnected();

		if (!available) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Internet is off! Turn on to continue!")
					.setCancelable(false)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// do things
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}
		return available;
	}
	

	
}
