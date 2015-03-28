package com.get.jacd;

import java.util.List;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class SignIn extends Activity {

	private final static int PICK_ACCOUNT_REQUEST = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_in);
		
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
            
        	ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
        	query.whereEqualTo("Email", accountName);
        	try {
				List<ParseObject> users = query.find();

				if (users.size()==0) {
					ParseObject user = new ParseObject("User");
					user.put("Email", accountName);
					user.save();
				}
			} catch (ParseException e) {
				e.printStackTrace();
			} 

        	
            
            Intent myIntent = new Intent(SignIn.this, SetupProfile.class);
            myIntent.putExtra("email", accountName); //Optional parameters
            SignIn.this.startActivity(myIntent);
            finish();

        }
    }

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
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sign_in, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
