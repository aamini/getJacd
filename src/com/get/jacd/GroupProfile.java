package com.get.jacd;

import java.util.List;

import com.flurry.android.FlurryAgent;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Contacts.Groups;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

public class GroupProfile extends Activity {
	
	private String USER_EMAIL,GROUP_NAME;
	
	private TextView name, visibility, location, description; 
	private ListView membersList;
	private TableRow membersRow;
	private Button inviteButton,toggleMembershipButton;
	
	private ProgressDialog progress; 
	private ParseObject user;
	private ParseObject group;
	private boolean isMember=false;

	private static final String BUTTON_JOIN = "Join";
	private static final String BUTTON_UNJOIN = "Unjoin";
	
    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, App.FLURRY_ID);			
		FlurryAgent.logEvent("Start: GroupProfile");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_profile);
		
		Intent intent = getIntent();
		USER_EMAIL = intent.getStringExtra("email");
		GROUP_NAME = intent.getStringExtra("group"); 
		FlurryAgent.logEvent("Open Group: "+USER_EMAIL+","+GROUP_NAME);

		
		initViews();
		loadUserGroupParse();
		
		populateViews();
		
	}
	
	private void populateViews() {
		//start progress dialog
    	progress.setTitle("Loading");
    	progress.setMessage("Retrieving Data...");
    	progress.setCancelable(false);
    	progress.show();
		
		
		name.setText(group.getString("Name"));
		visibility.setText(group.getBoolean("Public")==true?"Yes":"No");
		location.setText(group.getString("Location"));
		description.setText(group.getString("Description"));
		
		if (isMember) {
			inviteButton.setVisibility(View.VISIBLE);
			toggleMembershipButton.setVisibility(View.VISIBLE);
			membersRow.setVisibility(View.VISIBLE);
			
			toggleMembershipButton.setText(BUTTON_UNJOIN);
			
			List<String> mems = group.getList("Members");
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					this, //current Activity 
					R.layout.list_view_groups, //layout to use (XML file)
					mems); //items to display
			membersList.setAdapter(adapter);//displays items in ListView
			
		} else {
			inviteButton.setVisibility(View.GONE);
			membersRow.setVisibility(View.GONE);
			
			toggleMembershipButton.setText(BUTTON_JOIN);
			if (group.getBoolean("Public")) {
				toggleMembershipButton.setVisibility(View.VISIBLE);
			} else {
				toggleMembershipButton.setVisibility(View.GONE);
			}
		}
		
		
		progress.dismiss();
	}

	private void loadUserGroupParse() {
		FlurryAgent.logEvent("Loading GroupProfile Data: "+USER_EMAIL+","+GROUP_NAME);
		List<ParseObject> users = queryTableForValue("User", "Email", USER_EMAIL);
		if (users!=null && users.size()>0) {
			user = users.get(0);
		}
		
		List<ParseObject> groups = queryTableForValue("Groups", "Name", GROUP_NAME);
		if (groups!=null && groups.size()>0) {
			group = groups.get(0);
		}
		
		if (groups==null || user==null) {
			errorDialog(new Exception("Connection failed"));
		}
		isMember = ((group.getList("Members").contains(USER_EMAIL)) && 
				(user.getList("Groups").contains(GROUP_NAME)));
			
	}
	
	private List<ParseObject> queryTableForValue(String table, String key, String value) {
		if (isNetworkAvailable()) {
			try {
				ParseQuery<ParseObject> query = ParseQuery.getQuery(table);
				query.whereEqualTo(key, value);
				List<ParseObject> results = query.find();
				return results;
			} catch (ParseException e) {	
				errorDialog(e);
			}
		}
		return null;
	}
	
	private void errorDialog(Exception e) {
		progress.dismiss();	

		
		FlurryAgent.logEvent("GroupProfile error: "+e.getMessage()+";"+USER_EMAIL+","+GROUP_NAME);

		new AlertDialog.Builder(this)
	    .setTitle("Error")
	    .setMessage("Oops! We got an error: "+e.getMessage())
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {}
	     })
	    .setIcon(android.R.drawable.ic_dialog_alert)
	    .show();
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
			errorDialog(new Exception("Internet is off. Turn on to continue!"));
		}
		return available;
	}
	
	private void initViews() {
		progress = new ProgressDialog(this);
		
		name = (TextView) findViewById(R.id.group_profile_name);
		visibility = (TextView) findViewById(R.id.group_profile_visible);		
		location = (TextView) findViewById(R.id.group_profile_location);
		description = (TextView) findViewById(R.id.group_profile_description);
		
		membersList = (ListView) findViewById(R.id.group_profile_members_list);
		
		membersRow = (TableRow) findViewById(R.id.group_profile_members_row);
		
		inviteButton = (Button) findViewById(R.id.group_profile_add_user_button);
		toggleMembershipButton = (Button) findViewById(R.id.group_profile_toggle_member_button);
		
		inviteButton.setOnClickListener(addUserListener);
		toggleMembershipButton.setOnClickListener(toggleListener);
	}
	
	OnClickListener toggleListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			String title = toggleMembershipButton.getText().toString();

			List<String> members = group.getList("Members");
			List<String> joinedGroups = user.getList("Groups");
			
			if (title.equals(BUTTON_JOIN)) {
				if (!members.contains(USER_EMAIL)) {
					members.add(USER_EMAIL);
				}
				if (!joinedGroups.contains(GROUP_NAME)) {
					joinedGroups.add(GROUP_NAME);
				}
			} else if (title.equals(BUTTON_UNJOIN)) {
				if (members.contains(USER_EMAIL)) {
					members.remove(USER_EMAIL);
				}
				if (joinedGroups.contains(GROUP_NAME)) {
					joinedGroups.remove(GROUP_NAME);
				}
			}
			
			try {
				user.save();
				group.save();
				FlurryAgent.logEvent("GroupProfile "+title+": "+USER_EMAIL+","+GROUP_NAME);
			} catch (ParseException e) {
				errorDialog(e);
			}
			
			refreshPage();
		}
	};
	
	OnClickListener addUserListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder alert = new AlertDialog.Builder(GroupProfile.this);

			alert.setTitle("Add Member");
			alert.setMessage("Enter email of member to add:");

			// Set an EditText view to get user input 
			final EditText input = new EditText(GroupProfile.this);
			alert.setView(input);

			alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String newUserEmail = input.getText().toString();
					
					List<ParseObject> matches = queryTableForValue("User", "Email", newUserEmail);
					if (matches!=null && matches.size()>0) {
						ParseObject newUser = matches.get(0);
						List<String> members = group.getList("Members");
						List<String> joinedGroups = newUser.getList("Groups");
						
						if (!members.contains(USER_EMAIL)) {
							members.add(USER_EMAIL);
						}
						if (!joinedGroups.contains(GROUP_NAME)) {
							joinedGroups.add(GROUP_NAME);
						}
						
						try {
							user.save();
							group.save();
							FlurryAgent.logEvent("GroupProfile Added user: "+newUserEmail
									+";"+USER_EMAIL+","+GROUP_NAME);
						} catch (ParseException e) {
							errorDialog(e);
						}
						
						refreshPage();
					} else {
						errorDialog(new Exception("User does not exist!?"));
					}
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) { }
			});

			alert.show();
		}
	};
	
	private void refreshPage() {
		loadUserGroupParse();
		populateViews();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.group_profile, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case (R.id.menu_refresh):
			FlurryAgent.logEvent("GroupProfile Refresh: "+USER_EMAIL+","+GROUP_NAME);
			refreshPage();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
