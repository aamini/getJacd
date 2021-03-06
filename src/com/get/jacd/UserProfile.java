package com.get.jacd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

public class UserProfile extends Activity {

	private static final int RESULT_LOAD_IMAGE = 1;
	private static final int DEFAULT_PROFILE = R.drawable.ic_launcher;
	
	private String USER_EMAIL = null; 
	private ImageButton profileImage,locationButton;
	private Bitmap profile;
	private Uri profileURI;
	ProgressDialog progress;
	
	private EditText firstBox, lastBox, locationBox;
	private Spinner ageSpinner;
	private RadioGroup genderGroup ;
	private List<RadioButton> genderRadio = new ArrayList<RadioButton>();
	private ProgressBar pb;
	
	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, App.FLURRY_ID);
		FlurryAgent.onPageView();
		//FlurryAgent.logEvent("Start: UserProfile");
		ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"Start");

	}
	
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup_profile);

		initViews();
		profileURI = Uri.parse("android.resource://com.get.jacd/" + DEFAULT_PROFILE);
		
		Intent intent = getIntent();
		USER_EMAIL = intent.getStringExtra("email"); 
		
		// Application of the Array to the Spinner
		String[] ages = new String[100];
		for (int i=0; i<100;i++)
			ages[i]=i+"";
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,   android.R.layout.simple_spinner_item, ages);
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
		ageSpinner.setAdapter(spinnerArrayAdapter);
		
		if (isNetworkAvailable()) {
			loadProfileFromParse();
		}
		
		profileImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent choosePictureIntent = new Intent(Intent.ACTION_PICK, 
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI); 
                startActivityForResult(choosePictureIntent, RESULT_LOAD_IMAGE); 				
			}
		});
		
		
		locationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Acquire a reference to the system Location Manager
				locationButton.setVisibility(View.GONE);
				pb.setVisibility(View.VISIBLE);
				
				final LocationManager locationManager = (LocationManager) UserProfile.this.getSystemService(Context.LOCATION_SERVICE);

				// Define a listener that responds to location updates
				LocationListener locationListener = new LocationListener() {
					public void onLocationChanged(Location location) {
						Geocoder geoCoder = new Geocoder(UserProfile.this,
								Locale.getDefault());
						try {
							List<Address> addresses = geoCoder.getFromLocation(location.getLatitude(),location.getLongitude(), 1);
							Address address = addresses.get(0);
							String zip = (address.getPostalCode()==null)?address.getPostalCode():"";
							locationBox.setText(address.getLocality()+ ", "+address.getAdminArea()+" "+zip);
						} catch (IOException e) {
						} catch (NullPointerException e) {}

						locationButton.setVisibility(View.VISIBLE);
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
		
		Button finishButton = (Button) findViewById(R.id.finish_button);
		finishButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishProfile();
			}
		});
	}


	/**
	 * Setup and initialize private view variables
	 */
    private void initViews() {
		firstBox = (EditText) findViewById(R.id.text_first);
		lastBox = (EditText) findViewById(R.id.text_last);
		locationBox = (EditText) findViewById(R.id.text_location); locationBox.setEnabled(false);
		pb = (ProgressBar) findViewById(R.id.progressBar);
		genderRadio.add((RadioButton) findViewById(R.id.radio_male));
		genderRadio.add((RadioButton) findViewById(R.id.radio_female));
		genderGroup = (RadioGroup) findViewById(R.id.gender_radio);
		ageSpinner = (Spinner) findViewById(R.id.spinner_age);
		locationButton = (ImageButton) findViewById(R.id.button_get_location_profile);
		profileImage = (ImageButton) findViewById(R.id.profile_image);
		
		profile = BitmapFactory.decodeResource(getResources(), DEFAULT_PROFILE);
	}


	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
 
        switch (requestCode) {
        	case RESULT_LOAD_IMAGE: //user selected new profile image
        		if (resultCode == RESULT_OK && data != null) { 
		            profileURI = data.getData(); 
		            profile = uri2Bitmap(profileURI);
		            profileImage.setImageBitmap(profile); 
        		}
        		break;
        } 
    } 
	
    private void loadProfileFromParse() {
		//FlurryAgent.logEvent("Loading UserProfile: "+USER_EMAIL);
		ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"Load");

    	progress = new ProgressDialog(this);
    	progress.setTitle("Loading");
    	progress.setMessage("Retrieving Data...");
    	progress.setCancelable(false);
    	progress.show();
    	
    	ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
    	query.whereEqualTo("Email", USER_EMAIL);
    	query.findInBackground(new FindCallback<ParseObject>() {
    	    public void done(List<ParseObject> users, ParseException e) {
    	        if (e == null) {
    	            ParseObject user = users.get(0);
    	            firstBox.setText(user.getString("First"));
    	            lastBox.setText(user.getString("Last"));
    	            locationBox.setText(user.getString("Location"));
    	            ageSpinner.setSelection(user.getInt("Age"));
    	            
					try {
						byte[] arr = user.getParseFile("Image").getData();
						profile = byteArray2Bitmap(arr);
	    	            profileImage.setImageBitmap(profile);
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
					catch(NullPointerException e2){
					    e2.printStackTrace();
					}

    	            int gender = user.getInt("Gender");
    	            for (int i=0; i<=1; i++) {
	        	            genderRadio.get(i).setChecked(i==gender);
    	            }
    	            
    	            //Dismiss dialog
    	        	progress.dismiss();
    	        }
    	    }
    	});
    }
    
    private void finishProfile() {
    	if (isNetworkAvailable()) {
	    	saveProfileToParse();
    	}
    }
    
    
    /**
     * Save all content of page to Parse database
     */
    private void saveProfileToParse() {
		//FlurryAgent.logEvent("Saving UserProfile: "+USER_EMAIL);
		ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"Saving");
		
    	progress.setMessage("Saving Data...");
    	progress.show();
    	
    	ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
    	query.whereEqualTo("Email", USER_EMAIL);
    	query.findInBackground(new FindCallback<ParseObject>() {
    	    public void done(List<ParseObject> users, ParseException e) {
    	        if (e == null) { //no error
    	            ParseObject user = users.get(0);
    	            user.put("First", firstBox.getText().toString());
    	            user.put("Last", lastBox.getText().toString());
    	            user.put("Location", locationBox.getText().toString());
    	            
					user.put("Image", new ParseFile(bitmap2ByteArray(profile)));
    	            user.put("Age", ageSpinner.getSelectedItemPosition());
    	            
    	            int selectedId = genderGroup.getCheckedRadioButtonId();    	            
    	            user.put("Gender", (selectedId == R.id.radio_male) ?0:1 );
    	            
    	          
    	            user.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							progress.dismiss();
							if (e==null) {
						        Toast.makeText(getApplication(), "Saved data successfully!", Toast.LENGTH_SHORT).show();
						    	if (getIntent().hasExtra("finishAfter") && getIntent().getBooleanExtra("finishAfter",false)==true) {
						    		finish();
						    		return;
						    	}
						    	
					    	    Intent myIntent = new Intent(UserProfile.this, MapsActivity.class);
					    	    myIntent.putExtra("email", USER_EMAIL); //Optional parameters
					    	    UserProfile.this.startActivity(myIntent);
					    	    finish();
					    	    return;
							
							} else
						        Toast.makeText(getApplication(), "Data failed to save. Please try again.", Toast.LENGTH_SHORT).show();
						}
					});
    	        } 
    	    }
    	});
    	progress.dismiss();
	}
    

    /**
     * Convert Uniform resource identifier (URi) to Bitmap
     * @param uri to convert
     * @return bitmap convertion
     */
    private Bitmap uri2Bitmap(Uri uri) {
		Bitmap image = null;
		int width = 0;
		int height = 0;
		try {
			int maxSize = 400;
			
			image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
			width = image.getWidth();
			height = image.getHeight();

			float bitmapRatio = (float)width / (float) height;
			if (bitmapRatio > 0) {
			    width = maxSize;
			    height = (int) (width / bitmapRatio);
			} else {
			    height = maxSize;
			    width = (int) (height * bitmapRatio);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    /**
     * Convert bitmap to byte array
     * @param bmp bitmap to convert
     * @return byte array of bitmap
     */
	private byte[] bitmap2ByteArray(Bitmap bmp) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
		return stream.toByteArray();
	}
    
	/**
	 * Convert byte array to bit map
	 * @param bitmapdata byte array
	 * @return converted bitmap
	 */
	private Bitmap byteArray2Bitmap(byte[] bitmapdata) {
		return BitmapFactory.decodeByteArray(bitmapdata , 0, bitmapdata.length);
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
			//FlurryAgent.logEvent("No internet available: UserProfile; "+USER_EMAIL);
			ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"No Internet");

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
		getMenuInflater().inflate(R.menu.setup_profile, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case (R.id.menu_finish):
			//FlurryAgent.logEvent("UserProfile Finish: "+USER_EMAIL);
			ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"Finish");

			finishProfile();
			return true;
		case (R.id.menu_logout):
			//FlurryAgent.logEvent("UserProfile Logout: "+USER_EMAIL);
			ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"Logout");

    		getSharedPreferences(SignIn.PREFS_NAME, MODE_PRIVATE)
    		.edit()
    		.clear()
    		.commit();
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	

	
}
