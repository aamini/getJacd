package com.get.jacd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.flurry.android.FlurryAgent;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

public class CreateGroup extends Activity {

	private String USER_EMAIL = null; 
	
	private EditText name, location, description;
	private RadioGroup privacy;
	private Button finishButton;
	private ImageButton locationSearchButton;
	private ProgressBar pb;

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, App.FLURRY_ID);		
        FlurryAgent.onPageView();
		FlurryAgent.logEvent("Start: CreateGroup");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_group);
		
		Intent intent = getIntent();
		USER_EMAIL = intent.getStringExtra("email"); 
		
		initVars();
		

		locationSearchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Acquire a reference to the system Location Manager
				locationSearchButton.setVisibility(View.GONE);
				pb.setVisibility(View.VISIBLE);
				
				final LocationManager locationManager = (LocationManager) CreateGroup.this.getSystemService(Context.LOCATION_SERVICE);

				// Define a listener that responds to location updates
				LocationListener locationListener = new LocationListener() {
					public void onLocationChanged(Location loc) {
						Geocoder geoCoder = new Geocoder(CreateGroup.this,
								Locale.getDefault());
						try {
							List<Address> addresses = geoCoder.getFromLocation(loc.getLatitude(),loc.getLongitude(), 1);
							Address address = addresses.get(0);
							String zip = (address.getPostalCode()==null)?"":address.getPostalCode();
							location.setText(address.getLocality()+ ", "+address.getAdminArea()+" "+zip);
						} catch (IOException e) {
						} catch (NullPointerException e) {}

						locationSearchButton.setVisibility(View.VISIBLE);
						pb.setVisibility(View.GONE);
						
						locationManager.removeUpdates(this);
					}

				    public void onStatusChanged(String provider, int status, Bundle extras) {}
				    public void onProviderEnabled(String provider) {}
				    public void onProviderDisabled(String provider) {}
				  };

				// Register the listener with the Location Manager to receive location updates
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

				
				
			}
		});
		
		
		finishButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isNetworkAvailable()) {
					saveFormData();
					//TODO: move onto next activity or finish() this one

				}
			}
		});
		
	}
	
	private void initVars() {
		name = (EditText) findViewById(R.id.text_group_name);
		location = (EditText) findViewById(R.id.text_group_location); location.setEnabled(false);
		description = (EditText) findViewById(R.id.text_group_description);

		privacy = (RadioGroup) findViewById(R.id.radio_group_visibility);
		
		locationSearchButton = (ImageButton) findViewById(R.id.button_get_location_group);
		finishButton = (Button) findViewById(R.id.create_group_button);
		
		pb = (ProgressBar) findViewById(R.id.progress_bar_group);
	}
	
	
	private void saveFormData() {
		final ProgressDialog progress = new ProgressDialog(this);
    	progress.setTitle("Saving");
    	progress.setMessage("Creating Group...");
    	progress.setCancelable(false);
    	progress.show();
    	
    	ParseQuery<ParseObject> query = ParseQuery.getQuery("Groups");
    	query.whereEqualTo("Name", name.getText().toString());
    	query.findInBackground(new FindCallback<ParseObject>() {
    	    public void done(List<ParseObject> groups, ParseException e) {
    	        if (e == null) { //no error
    	            if (groups.size()>0) { //if group already exists
    	            	progress.dismiss(); //get rid of loading dialog
    	            	
    	            	//start alert dialog 
    	            	new CustomAlertDialog(
    	            			getApplicationContext(), 
    	            			"A group with this name already exists.\nPlease pick a different name.")
    	            	.show();
    	            	return;
    	            }
    	            
    	            //group does not exist so add it
    	            ParseObject group = new ParseObject("Groups");
    	            group.put("Name", name.getText().toString());
    	            group.put("Members", new ArrayList<String>(Arrays.asList(USER_EMAIL)));
    	            group.put("ScheduledRuns", new ArrayList<String>());
    	            group.put("Public", privacy.getCheckedRadioButtonId()==R.id.radio_public);
    	            group.put("Location", location.getText().toString());
    	            group.put("Description", description.getText().toString());

    	            try {
	    	        	ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
	    	        	query.whereEqualTo("Email", USER_EMAIL);
	    	        	ParseObject u = query.find().get(0);
	    	        	List<String> g = u.getList("Groups");
	    	        	if (!g.contains(name.getText().toString()))
	    	        			g.add(name.getText().toString());
	    	        	u.put("Groups",g);
	    	        	u.save();
						
						group.save();
						
						Toast.makeText(getApplication(), "New Group: "+name.getText()+" created!", Toast.LENGTH_SHORT).show();
						FlurryAgent.logEvent("CreateGroup Created: "+USER_EMAIL+";"+name.getText());

						progress.dismiss();
						Intent myIntent = new Intent(CreateGroup.this, GroupProfile.class);
		                myIntent.putExtra("email", USER_EMAIL); //Optional parameters
		                myIntent.putExtra("group", name.getText().toString());
		                CreateGroup.this.startActivity(myIntent);
		            	finish();
					} catch (ParseException e1) {
						FlurryAgent.logEvent("CreateGroup Error Saving: "+e.getMessage()+";"+USER_EMAIL+";"+name.getText());

    	            	new CustomAlertDialog(
    	            			getApplicationContext(), 
    	            			"Error saving group. Details: "+e.getMessage())
    	            	.show();
					}
    	            progress.dismiss();
    	        }
    	    }
    	});
	}
	
	/**
	 * Check if network is turned on device
	 * @return true is connected, false otherwise
	 */
	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		boolean available = activeNetworkInfo != null
				&& activeNetworkInfo.isConnected();

		if (!available) {
			FlurryAgent.logEvent("CreateGroup No Internet: "+USER_EMAIL+";"+name.getText());

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
