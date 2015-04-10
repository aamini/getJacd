package com.get.jacd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.get.jacd.R;
import com.get.jacd.SignIn;
import com.get.jacd.UserProfile;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

public class SearchGroups extends Activity {

	private ListView groupList;
	private EditText searchInput;
	private Button searchButton;
	private String USER_EMAIL;
	
    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, App.FLURRY_ID);
		FlurryAgent.logEvent("Start: SearchGroups");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_groups);

		Intent intent = getIntent();
		USER_EMAIL = intent.getStringExtra("email"); 
		
		searchInput = (EditText) findViewById(R.id.input_text_bar);
		searchButton = (Button) findViewById(R.id.search_group_button);
		groupList = (ListView) findViewById(R.id.search_result_list);
		
		searchButton.setOnClickListener(new View.OnClickListener() { 
			@Override
			public void onClick(View v) {
				if (isNetworkAvailable()) {
					String input = searchInput.getText().toString();
					FlurryAgent.logEvent("Search requested: "+input+";"+USER_EMAIL);
					search(input);
				}
			}
		});
		
	}

	
	/**
	 * Gets the List of Groups
	 */
	private void search(String searchText) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Groups");
		query.whereStartsWith("Name", searchText);
		query.findInBackground(new FindCallback<ParseObject>() {
		    public void done(List<ParseObject> groups, ParseException e) {
		        if (e == null) {
					populateListView(groups); //populate the listView
		        } else {
		        	Log.d("Groups", "Error: " + e.getMessage());
		        }
		    }
		});
	}
	
	
	/**
	 * 
	 * @param groups
	 * populates the ListView object
	 * Also adds Listeners to each item in the listView (where if the item is clicked, it views that Group)
	 */
	protected void populateListView(List<ParseObject> groups) {
		
		List<String> groupNames = new ArrayList<String>();
		for (ParseObject group:groups) {
			groupNames.add(group.getString("Name"));
		}
		
		ArrayAdapter<String> groupAdapter = new ArrayAdapter<String>(
				this, //current Activity 
				R.layout.list_view_groups, //layout to use (XML file)
				groupNames); //items to display
		groupList.setAdapter(groupAdapter);//displays items in ListView
		groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {//add listener to respond to clicking on an item

			@Override
			public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
				TextView textView = (TextView) viewClicked; 
				
				FlurryAgent.logEvent("Clicked: "+textView.getText().toString()+";"+USER_EMAIL);
				FlurryAgent.logEvent("Transfer: SearchGroups, GroupProfile;"+USER_EMAIL);

				//the textView is clicked, so we want to progress to Group Profile
				Intent myIntent = new Intent(SearchGroups.this, GroupProfile.class); //current class, next class
		        myIntent.putExtra("email", USER_EMAIL); 
		        myIntent.putExtra("group", textView.getText().toString()); 
		        SearchGroups.this.startActivity(myIntent);
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
			
			FlurryAgent.logEvent("No internet available: SearchGroups;"+USER_EMAIL);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Internet is off! Turn on to continue!")
					.setCancelable(false)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}
		return available;
	}
}
