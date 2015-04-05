package com.get.jacd;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class CustomAlertDialog extends AlertDialog.Builder
{
    public CustomAlertDialog(final Context context, String message)
    {
        // Set your theme here
        super(context);
        
        this.setTitle("Error!");
        this.setMessage(message);
        
        this.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		
        	}
        });
        
        this.setCancelable(false);
        this.setIcon(android.R.drawable.ic_dialog_alert);
    }

	@Override
	public AlertDialog show() {
		return super.show();
	}
	
    
}