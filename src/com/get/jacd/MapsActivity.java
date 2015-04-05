package com.get.jacd;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.get.jacd.R;
import com.get.jacd.DrawerItemCustomAdapter;
import com.get.jacd.ObjectDrawerItem;
import com.flurry.android.FlurryAgent;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;


public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private String USER_EMAIL = null;
    Location lastLocation;
    double latitude = 0;
    double longitude = 0;
    //private Marker currentMarker; //Current location marker of user
    //keep a list of groups -> each group is a color - when refreshing can iterate through groups
    List<String> filteredGroups = new ArrayList<String>(); //Arrays.asList("Test") for testing
    List<Marker> markers = new ArrayList<Marker>();
    
    //Navigation drawer variables
    private DrawerLayout drawerLayout;
    private LinearLayout drawerLinear;
    private ListView drawerList;
	private ActionBarDrawerToggle drawerToggle;

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
        setContentView(R.layout.activity_main);
        
        Intent intent = getIntent();
        USER_EMAIL = intent.getStringExtra("email"); 
        
        setupNavigationDrawer();
        
        setUpMapIfNeeded();
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
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker").snippet("Snippet"));


        setCurrentMarkerPosition(true);
        

        final Handler handler = new Handler();
        final Runnable update = new Runnable(){
            @Override
            public void run(){
                updateMarkers();
                handler.postDelayed(this,7000);
            }
        };
       handler.postDelayed(update,5000);


    }


    //Draws blue marker where user currently is
   private void setCurrentMarkerPosition(boolean setup){
       // Enable MyLocation Layer of Google Map
       mMap.setMyLocationEnabled(true);
       // Get LocationManager object from System Service LOCATION_SERVICE
       LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

       // Create a criteria object to retrieve provider
       Criteria criteria = new Criteria();

       // Get the name of the best provider
       String provider = locationManager.getBestProvider(criteria, true);

       // Get Current Location
       Location myLocation = locationManager.getLastKnownLocation(provider);//TODO: use different function to get location
       lastLocation = myLocation;
       // set map type
       mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
       if(myLocation!=null) {
            // Get latitude of the current location
            latitude = myLocation.getLatitude();
            // Get longitude of the current location
            longitude = myLocation.getLongitude();
        }
       // Create a LatLng object for the current location
       LatLng latLng = new LatLng(latitude, longitude);

       if(setup){
       // Show the current location in Google Map
       mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

       // Zoom in the Google Map
       mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
       //mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("You are here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
       }
       ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
       query.whereEqualTo("Email", USER_EMAIL);
       query.findInBackground(new FindCallback<ParseObject>() {
           public void done(List<ParseObject> users, ParseException e) {
             if (e == null) {
               ParseObject user=users.get(0);
               user.put("CurrentLocation", new ParseGeoPoint(latitude,longitude));
               try {
                user.save();
                //Log.d("test","latitude: "+ latitude +" longitude:  " + longitude);
            } catch (ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
               //TODO: handle exceptions
             }
           }
         });
     
       
       
       
   }

//TODO: formalize datatypes for username and group name to stop passing so many strings around
    private void updateMarkers(){
        //mMap.clear();
        for(Marker mark: markers)
        {
            mark.remove();
        }
        //TODO: change map clear to just remove markers ->
        setCurrentMarkerPosition(false);
        //get user locations from groups
        int colorCounter=0;
        for(String groupName:filteredGroups) {
            Log.d("testing","INSIDE GROUP LOOP");
            List<String> groupUserList = getUserList(groupName);
            Log.d("testing2","UserList: " + groupUserList);
            List<LatLng> userLocationList = new ArrayList<LatLng>();
            Log.d("testing3","userLocationList: "+userLocationList);
            for(String username:groupUserList) 
            {
                LatLng loc = getUserLocation(username);
                userLocationList.add(loc);
            }
            addMarkers(colorCounter % 360, userLocationList);
            colorCounter+=30; 
        } 
    }
    //Add markers to all the point in the list using the color given
    public void addMarkers(float hue, List<LatLng> points){
        for(LatLng i:points){
            //TODO: change icon for marker
            Log.d("test2","latitude: " +i.latitude + "longitude: " + i.longitude);
            markers.add(mMap.addMarker(new MarkerOptions().position(i).title("Username?").
                          icon(BitmapDescriptorFactory.defaultMarker(hue))));
        }
    }

    //Given a group, adds to list of groups that need to be filtered
    public void addGroupFilter(String group)
    {
        filteredGroups.add(group);
    }
    
    //removes group name from filtered groups
    public void removeFilter(String group){
        filteredGroups.remove(group);
    }
    
   //retrieve usernames from within group and return as list
    //TODO: make sure groups and users end up being unique on parse
    private List<String> getUserList(String group){
        List<String> userList = new ArrayList<String>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Groups");
        query.whereEqualTo("Name", group);
        //TODO: deal with asynchronity
        ParseObject matchingGroup;
        try {
            matchingGroup = query.find().get(0);
            userList = matchingGroup.getList("Members");
            Log.d("testing4","Members List: "+ userList);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /*List<String> tempList = new ArrayList<String>();
        tempList = matchingGroup.getList("Members");*/
       
       /* for(String user:tempList)
        {
            Log.d("testing5","users in list: " + user);
            userList.add(user);
        }*/
        //TODO: handle exceptions
        return userList;
    }
    //retrieve location from server given a username
    private LatLng getUserLocation(String username){
        //final List<LatLng> userLocations = new ArrayList<LatLng>();
        double lat = 0;
        double lon= 0;
        ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
        query.whereEqualTo("Email", username);
        try {
            ParseObject user=query.find().get(0);
            ParseGeoPoint loc = user.getParseGeoPoint("CurrentLocation");
            lat = loc.getLatitude();
            lon = loc.getLongitude();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /*query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> users, ParseException e) {
              if (e == null) {
                ParseObject user=users.get(0);
                ParseGeoPoint loc = user.getParseGeoPoint("CurrentLocation");
                userLocations.add(new LatLng(loc.getLatitude(),loc.getLongitude()));
              }
            }
          });*/
        return new LatLng(lat,lon);
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
                ) {};
        
        drawerLayout.setDrawerListener(drawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        drawerList = (ListView) findViewById(R.id.left_drawer);
        
        View header = getLayoutInflater().inflate(R.layout.navigation_header, null);
        drawerList.addHeaderView(header, null, false);
        
        ObjectDrawerItem[] items = new ObjectDrawerItem[2];
        items[0] = new ObjectDrawerItem(android.R.drawable.ic_menu_search, "Search");
        items[1] = new ObjectDrawerItem(android.R.drawable.ic_menu_add, "Create");

        DrawerItemCustomAdapter adapter = new DrawerItemCustomAdapter(this, R.layout.listview_item_row, items);
        drawerList.setAdapter(adapter);
        
        LinearLayout tableLayout = (LinearLayout)findViewById(R.id.navigation_linear_table);
        List<String> groups = new ArrayList<String>(Arrays.asList("Group1","Test Group"));
        for (String group: groups) {        	
        	TableRow row = new TableRow(this);
            row.setId(groups.indexOf(group));
            row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
            CheckBox checkBox = new CheckBox(this);
            checkBox.setOnCheckedChangeListener(checkBoxListener);
            checkBox.setId(groups.indexOf(group));
            checkBox.setText(group);
            checkBox.setTextSize(20);   
            checkBox.setTextColor(Color.WHITE);
            row.addView(checkBox);  
            tableLayout.addView(row);
        }
        
        
        header.setOnClickListener(headerListener);
        drawerList.setOnItemClickListener(drawerListListener);
        drawerLayout.closeDrawer(drawerLinear);		
	}
    
	OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			Toast.makeText(getApplicationContext(), buttonView.getId()+" checked="+isChecked, Toast.LENGTH_SHORT).show();
            
		}
	};
    
    AdapterView.OnItemClickListener drawerListListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int pos, long arg3) {
            Toast.makeText(getApplicationContext(), "clicked: "+pos, Toast.LENGTH_SHORT).show();
            switch (pos) {
            case 0: //search
            	break;
            case 1: //create 
            	break;
            }
            drawerList.setItemChecked(pos, false);
            drawerLayout.closeDrawer(drawerLinear);
        }
    };
    
    View.OnClickListener headerListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "clicked: header", Toast.LENGTH_SHORT).show();
            //go to user profile
		}
	};
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
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
