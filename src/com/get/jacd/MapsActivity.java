package com.get.jacd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import bolts.Task;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;


public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    Location lastLocation;
    double latitude = 0;
    double longitude = 0;
    List<Marker> markers = new ArrayList<Marker>();
    
    //Navigation drawer views
    private DrawerLayout drawerLayout;
    private LinearLayout drawerLinear;
    private ListView drawerList;
	private ActionBarDrawerToggle drawerToggle;
	private ImageView drawerProfile;
	private TextView drawerName, drawerEmail;
	private LinearLayout tableLayout;
	private List<CheckBox> groupCheckBoxes = new ArrayList<CheckBox>();
	private Button startRunButton;
	
	private HashMap<String,Marker> userToMarker = new HashMap<String,Marker>();
	private HashMap<String,String> userToGroup = new HashMap<String,String>();
	private HashMap<String,Integer> groupToInd = new HashMap<String,Integer>();
	private HashMap<String,float[]> groupToColor = new HashMap<String,float[]>();
	private List<String> prevGroups = new ArrayList<String>();
	private List<String> currGroups;
	private int numCheckedGroups;
	
	private boolean isRunning = false;
	private boolean firstMapDraw = true; 
	
    private String USER_EMAIL = null;
    private ParseObject myUserData = null;
    
    AlertDialog internetAlert;
    
    
    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, App.FLURRY_ID);		
        FlurryAgent.onPageView();
		//FlurryAgent.logEvent("Start: MapsActivity");
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
        setContentView(R.layout.activity_main);
        
        Intent intent = getIntent();
        USER_EMAIL = intent.getStringExtra("email"); 
        
        updateUserParseObject();
        //FIXME: show loading progress dialog
 
        setupNavigationDrawer(); 
        setUpMapIfNeeded();
        
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
		internetAlert = builder.create();
        
    }


	@Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded()
    {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
    	firstMapDraw = true;
    	
		mMap.setMyLocationEnabled(true); 
    	mMap.setOnMyLocationChangeListener(myLocationChangeListener);
        
        final Handler handler = new Handler();
        final Runnable update = new Runnable(){
            @Override
            public void run() {
            	if (isNetworkAvailable()) {
            		updateMarkers();
            	}
                handler.postDelayed(this,5000);
            }
        };
       handler.postDelayed(update,5000);

    }


	// TODO: formalize datatypes for username and group name to stop passing so
	// many strings around
    private void updateMarkers(){
    	
		// get user locations from groups
		//userToGroup = new HashMap<String,String>();
		//groupToInd = new HashMap<String,Integer>();
		numCheckedGroups=0;
		for (CheckBox groupCheckBox : groupCheckBoxes) {
			if (groupCheckBox.isChecked()) {
				groupToInd.put(groupCheckBox.getText().toString(), numCheckedGroups);
				numCheckedGroups++;
			} else {
				groupToInd.remove(groupCheckBox.getText().toString());
			}
		}
		
		
		int ind=0;
		for (CheckBox groupCheckBox : groupCheckBoxes) {	
			//name of group
			String groupName = groupCheckBox.getText().toString();
			//user emails in group
			List<String> groupUserList = getUserList(groupName); 

			if (groupCheckBox.isChecked()) {

				for (String username : groupUserList) {
					// If they are running -> then add to list otherwise don't
					userToGroup.put(username,groupCheckBox.getText().toString());
					
					ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
			        query.whereEqualTo("Email", username);
			        query.findInBackground(updateCallback);
					
				} 
			} else { 
				for (String u : groupUserList) {
					if (userToMarker.containsKey(u)){
						Marker mark = userToMarker.get(u);
						mark.remove();
						userToMarker.remove(u);
					}
				}
			}
		}
	} 

    private FindCallback<ParseObject> updateCallback = new FindCallback<ParseObject>() { 
		@Override
		public void done(List<ParseObject> u, ParseException e) { 
			if (e!=null)
				return;
			ParseObject user = u.get(0);
			String username = user.getString("Email").toString();

			if (!user.getBoolean("Running") || username.equals(USER_EMAIL)) {
				if (userToMarker.containsKey(username)) {
					userToMarker.get(username).remove();
					userToMarker.remove(username);
				}
				return;
			}
			
            ParseGeoPoint cur = user.getParseGeoPoint("CurrentLocation");
			if (userToMarker.containsKey(username)) {
				// compare previous marker location to current
	            Marker mark = userToMarker.get(username);
	            LatLng prev = mark.getPosition();
				if (cur.getLatitude()!=prev.latitude || cur.getLongitude()!=prev.longitude) {
					mark.setPosition(new LatLng(cur.getLatitude(),cur.getLongitude()));

				}
				
			} else if (cur!=null){
				String groupName = userToGroup.get(username);
				
				updateGroupColorMap();
				int hue = (int) (groupToColor.get(groupName)[0]);
				
				Marker mark = mMap.addMarker(new MarkerOptions()
					.position(new LatLng(cur.getLatitude(),cur.getLongitude()))
					.title(username)
					.icon(BitmapDescriptorFactory
							.defaultMarker(hue)));
				
				userToMarker.put(username,mark);
			}
		}
	};
    
    
    
	// retrieve usernames from within group and return as list
	// TODO: make sure groups and users end up being unique on parse
	private List<String> getUserList(String group) {
		List<String> userList = new ArrayList<String>();
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Groups");
		query.whereEqualTo("Name", group);
 		try { 
			ParseObject matchingGroup = query.find().get(0);
			userList = matchingGroup.getList("Members");

		} catch (ParseException e) { // TODO: handle exceptions
			e.printStackTrace();
		}

		return userList;
	} 
    
    private void setupNavigationDrawer() {
        // R.id.drawer_layout should be in every activity with exactly the same id.
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLinear = (LinearLayout) findViewById(R.id.navigation_layout);

        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
                ) {
        	@Override
        	public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				// update drawer every time it is closed
				if (isNetworkAvailable()) {
					updateUserParseObject();
				}
				refreshSideBar();
			} 
        };
        
        drawerLayout.setDrawerListener(drawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        drawerList = (ListView) findViewById(R.id.left_drawer);
        
        View header = getLayoutInflater().inflate(R.layout.navigation_header, null);
        drawerList.addHeaderView(header, null, false);
        
        drawerName = (TextView) findViewById(R.id.text_header_user_name);
        drawerEmail = (TextView) findViewById(R.id.text_header_user_email);
        drawerProfile = (ImageView) findViewById(R.id.image_header_user_profile);
		tableLayout = (LinearLayout) findViewById(R.id.navigation_linear_table);
		startRunButton = (Button) findViewById(R.id.button_start_stop_run);
		
        ObjectDrawerItem[] items = new ObjectDrawerItem[2];
        items[0] = new ObjectDrawerItem(android.R.drawable.ic_menu_search, "Search");
        items[1] = new ObjectDrawerItem(android.R.drawable.ic_menu_add, "Create");

        DrawerItemCustomAdapter adapter = new DrawerItemCustomAdapter(this, R.layout.listview_item_row, items);
        drawerList.setAdapter(adapter);
                
        header.setOnClickListener(headerListener);
        drawerList.setOnItemClickListener(drawerListListener);
        startRunButton.setOnClickListener(toggleRunListener);
        
        drawerLayout.closeDrawer(drawerLinear);		
		refreshSideBar();
	}
    
	private void updateTableView() {
		groupCheckBoxes.clear();
		tableLayout.removeAllViews();
		
		for (String group : currGroups) {
			TableRow row = new TableRow(this);
			row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT));
			
			CheckBox checkBox = new CheckBox(this);
			checkBox.setId(currGroups.indexOf(group));
			checkBox.setText(group);
			checkBox.setTextSize(20);
			checkBox.setTextColor(Color.WHITE);
			checkBox.setOnCheckedChangeListener(checkBoxListener);
			groupCheckBoxes.add(checkBox);
			row.addView(checkBox);
			tableLayout.addView(row);
		}
	}
	
	/**
	 * Resets the sidebar's :
	 * 	user name/email 
	 * 	profile picture
	 *  group list if they are new
	 */
	private void refreshSideBar() {
		drawerName.setText(myUserData.getString("First") + " "
				+ myUserData.getString("Last"));
		drawerEmail.setText(myUserData.getString("Email"));

		try {
			byte[] arr = myUserData.getParseFile("Image").getData();
			drawerProfile.setImageBitmap(BitmapFactory.decodeByteArray(arr, 0,
					arr.length));
		} catch (ParseException e) {
		}

		currGroups = myUserData.getList("Groups");
		if (!currGroups.equals(prevGroups)) {
			updateTableView();
		}
		prevGroups = currGroups;
	}

	/**
	 * Queries parse to get the user data
	 */
	private void updateUserParseObject() {
		//Toast.makeText(getApplicationContext(), "updating user", Toast.LENGTH_SHORT).show();
		ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
		query.whereEqualTo("Email", USER_EMAIL);
		try {
			myUserData = query.find().get(0);
		} catch (ParseException e) {
			Log.d("error","details",e) ;
		}
	}

	GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
		@Override
		public void onMyLocationChange(Location location) {
			
			//Toast.makeText(getApplicationContext(), 
			//		"new location: "+location.getLatitude()+","+location.getLongitude(),
			//		Toast.LENGTH_SHORT).show();
			
			if (firstMapDraw) {
				LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

				mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN); // set map type
				mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));// Show the current location in Google Map
				mMap.animateCamera(CameraUpdateFactory.zoomTo(15));// Zoom in the Google Map
				firstMapDraw=false;
			}
			//FlurryAgent.logEvent("Location: "+USER_EMAIL+","+System.currentTimeMillis()+","+location.getLatitude()+","+location.getLongitude());
			ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),location.getLatitude(),location.getLongitude());

			// Saves location to server -> only if running
			if (isRunning) {
				myUserData.put("CurrentLocation", 
						new ParseGeoPoint(
								location.getLatitude(),
								location.getLongitude()));
				myUserData.saveEventually();

			}
		}
	};
    
    AdapterView.OnItemClickListener drawerListListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int pos, long arg3) {
            Toast.makeText(getApplicationContext(), "clicked: "+pos, Toast.LENGTH_SHORT).show();
            
            Intent myIntent;
            switch (pos) {
            case 1: //search
                myIntent = new Intent(MapsActivity.this, SearchGroups.class);
                myIntent.putExtra("email", USER_EMAIL); //Optional parameters
                MapsActivity.this.startActivity(myIntent);
            	break;
            case 2: //create 
                myIntent = new Intent(MapsActivity.this, CreateGroup.class);
                myIntent.putExtra("email", USER_EMAIL); //Optional parameters
                MapsActivity.this.startActivity(myIntent);
            	break;
            }
            drawerList.setItemChecked(pos, false);
            drawerLayout.closeDrawer(drawerLinear);
        }
    };
    
    View.OnClickListener headerListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
            drawerLayout.closeDrawer(drawerLinear);

            Toast.makeText(getApplicationContext(), "clicked: header", Toast.LENGTH_SHORT).show();
            //go to user profile
            Intent myIntent = new Intent(MapsActivity.this, UserProfile.class);
            myIntent.putExtra("email", USER_EMAIL); //Optional parameters
            myIntent.putExtra("finishAfter",true);

            MapsActivity.this.startActivity(myIntent);
		}
	};
	
	View.OnClickListener toggleRunListener = new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			if (startRunButton.getText().equals("Start Run!")) {
				isRunning = true;
				startRunButton.setText("End Run!");

			} else if (startRunButton.getText().equals("End Run!")) {
				isRunning = false;
				startRunButton.setText("Start Run!");
				//FlurryAgent.logEvent("MapsActivity Running: "+isRunning+";"+USER_EMAIL);

			} else {
				Toast.makeText(getApplicationContext(), "Unknown state of button!?", Toast.LENGTH_SHORT).show();
			}
			ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
			query.whereEqualTo("Email", USER_EMAIL);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> users, ParseException e) {
                    if (e == null) {
                        ParseObject user = users.get(0);
                        user.put("Running", isRunning);
                        try {
                            user.save();
							//FlurryAgent.logEvent("MapsActivity Running: "+isRunning+";"+USER_EMAIL);
							ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"Running: "+isRunning);

                        } catch (ParseException e1) { //TODO:exceptions
                        	e1.printStackTrace();
                        }
                    }
                }
            });
           }  
	};
	

    OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {  
			
			numCheckedGroups=0;
			String groupsList = "";
			for (CheckBox box: groupCheckBoxes) {
				if (box.isChecked()) {
					groupToInd.put(box.getText().toString(), numCheckedGroups);
					numCheckedGroups++;
					groupsList+=box.getText().toString()+";";
				}

			}
			ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"Checked: "+groupsList);

			
			for (CheckBox box : groupCheckBoxes) {
				if (!box.isChecked()) { 
					box.setTextColor(Color.WHITE);
					continue;
				}
				
				//name of group
				String groupName = box.getText().toString();
				
				updateGroupColorMap();
				box.setTextColor(Color.HSVToColor(groupToColor.get(groupName)));
				
				//each user in group
				for (String u : userToGroup.keySet()) {
					if (userToGroup.get(u).equals(groupName)) {
						// update color of marker
						if (userToMarker.containsKey(u)) {
							Log.d("recoloring-check", "user:" + u);
							Marker mark = userToMarker.get(u);
							int hue = (int) (groupToColor.get(groupName)[0]);

							mark.setIcon(BitmapDescriptorFactory
									.defaultMarker(hue));
							userToMarker.put(u, mark);
						}
					}
				}
			}
		}
	};
	
	private void updateGroupColorMap() {
		int i=0;
		for (CheckBox box : groupCheckBoxes) {
			if (box.isChecked()) {
				String groupName = box.getText().toString();
				//color text
				float ratio = ((float) (i)) / ((float) (numCheckedGroups));
				//Log.d("amini","updating group"+groupName+" "+i+"/"+numCheckedGroups+"="+ratio);
				float[] c = new float[]{(float) (ratio*360.0),1,1};
				groupToColor.put(groupName,c);
				i++;
			}
		}
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

		if (!available && !internetAlert.isShowing()) {
			//FlurryAgent.logEvent("MapsActivity: No Internet Available");
			ParseLog.Log(USER_EMAIL,System.currentTimeMillis(),this.getClass().getSimpleName(),"No Internet");

			internetAlert.show();
		}
		return available;
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        
        switch (item.getItemId()) {
        case (R.id.menu_main_logout):
			//FlurryAgent.logEvent("MapsActivity Logout: "+USER_EMAIL);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
    
}
