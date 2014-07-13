package org.openstack.activities;

import android.os.Bundle;
import android.os.Environment;

import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.Toast;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ActivityInfo;

import android.net.Uri;

import android.util.Log;
import android.util.DisplayMetrics;

import android.app.ActivityManager.MemoryInfo;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.app.Activity;

import android.view.MenuItem;
import android.view.Menu;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.View;

import java.io.IOException;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;

import java.io.File;

import org.openstack.comm.RESTClient;
import org.openstack.comm.NotFoundException;
import org.openstack.parse.ParseUtils;
import org.openstack.parse.ParseException;



import org.openstack.R;
import org.openstack.utils.User;
import org.openstack.utils.Utils;
import org.openstack.utils.Named;
import org.openstack.utils.Server;
import org.openstack.utils.Flavor;
import org.openstack.utils.Base64;
import org.openstack.views.UserView;
import org.openstack.views.ServerView;
import org.openstack.utils.TextViewNamed;
import org.openstack.utils.UserException;
import org.openstack.utils.ImageViewNamed;
import org.openstack.utils.ImageButtonNamed;
import org.openstack.utils.LinearLayoutNamed;

import android.graphics.Typeface;
import android.graphics.Color;

import android.os.AsyncTask;
import org.openstack.utils.CustomProgressDialog;

import java.util.HashSet;

public class ServersActivity extends Activity implements OnClickListener {

    private CustomProgressDialog progressDialogWaitStop = null;
    private User U = null;
    //private HashSet listedServers = null;

    //__________________________________________________________________________________
    public boolean onCreateOptionsMenu( Menu menu ) {
        
        super.onCreateOptionsMenu( menu );
        
        int order = Menu.FIRST;
        int GROUP = 0;
                
        menu.add(GROUP, 0, order++, getString(R.string.MENUHELP)    ).setIcon(android.R.drawable.ic_menu_help);
        menu.add(GROUP, 1, order++, getString(R.string.MENUUPDATE) ).setIcon(R.drawable.ic_menu_refresh);
	menu.add(GROUP, 2, order++, getString(R.string.MENUDELETEALL) ).setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }
    
    //__________________________________________________________________________________
    public boolean onOptionsItemSelected( MenuItem item ) {
	 
        int id = item.getItemId();     
        
        if( id == Menu.FIRST-1 ) {
            Utils.alert( getString(R.string.NOTIMPLEMENTED) ,this );
            return true;
        }
        
        if( id == Menu.FIRST ) { 
	    if(U==null) {
		Utils.alert("An error occurred recovering User from sdcard. Try to go back and return to this activity.", this);
	    } else {
		progressDialogWaitStop.show();
		AsyncTaskOSListServers task = new AsyncTaskOSListServers();
		task.execute( );
		return true;
	    }
        }

        if( id == Menu.FIRST+1 ) { 
	    if(U==null) {
		Utils.alert("An error occurred recovering User from sdcard. Try to go back and return to this activity.", this);
	    } else {
		// progressDialogWaitStop.show();
		// AsyncTaskOSListServers task = new AsyncTaskOSListServers();
		// task.execute( );
		//serverIDs = Utils.join(listedServers,",");
		progressDialogWaitStop.show();
		AsyncTaskDeleteServer task = new AsyncTaskDeleteServer();
		int numChilds = ((LinearLayout)findViewById(R.id.serverLayout)).getChildCount();
		String[] listedServers = new String[numChilds];
		for(int i = 0; i < numChilds; ++i) {
		    View sv = ((LinearLayout)findViewById(R.id.serverLayout)).getChildAt(i);
		    if(sv instanceof ServerView)
			listedServers[i] = ((ServerView)sv).getServer().getID();
		}
		task.execute( Utils.join(listedServers, ",") ) ;
		return true;
	    }
        }
	return super.onOptionsItemSelected( item );
    }

    //__________________________________________________________________________________
    @Override
    public void onClick( View v ) {
	if(v instanceof ImageButtonNamed) {
	    if( ((ImageButtonNamed)v).getType() == ImageButtonNamed.BUTTON_DELETE_SERVER ) {
		// Delete the server
		String serverid = ((ImageButtonNamed)v).getServerView( ).getServer().getID();
		progressDialogWaitStop.show();
		AsyncTaskDeleteServer task = new AsyncTaskDeleteServer();
		String[] ids = new String[1];
		ids[0] = serverid;
		task.execute( ids ) ;
		return;
	    }
	    if( ((ImageButtonNamed)v).getType() == ImageButtonNamed.BUTTON_SNAP_SERVER ) {
		Utils.alert(getString(R.string.NOTIMPLEMENTED), this);
		return;
	    }
	}
    }

    //__________________________________________________________________________________
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView( R.layout.serverlist );
	
	//listedServers = new HashSet();

	progressDialogWaitStop = new CustomProgressDialog( this, ProgressDialog.STYLE_SPINNER );
        progressDialogWaitStop.setMessage( "Please wait: connecting to remote server..." );
	
	String selectedUser = Utils.getStringPreference("SELECTEDUSER", "", this);
	try {
	    U = User.fromFileID( selectedUser );
	} catch(RuntimeException re) {
	    Utils.alert("ServersActivity.onCreate: "+re.getMessage(), this );
	    return;
	}

	progressDialogWaitStop.show();
	AsyncTaskOSListServers task = new AsyncTaskOSListServers();
	task.execute( );
    }
    
    //__________________________________________________________________________________
    @Override
    public void onResume( ) {
	super.onResume( );
    }
 
    /**
     *
     *
     *
     *
     */
    @Override
    public void onDestroy( ) {
	super.onDestroy( );
	progressDialogWaitStop.dismiss();
    }

    //__________________________________________________________________________________
    private void refreshView( Vector<Server> servers, Hashtable<String, Flavor> flavors ) {
	((LinearLayout)findViewById(R.id.serverLayout)).removeAllViews();
	Iterator<Server> it = servers.iterator();
	while(it.hasNext()) {
	    Server s = it.next();
	    Flavor F = flavors.get( s.getFlavorID( ) );
	    if( F != null)
		s.setFlavor( F );
	    ServerView sv = new ServerView(s, this);
	    ((LinearLayout)findViewById( R.id.serverLayout) ).addView( sv );
	    View space = new View( this );
	    space.setMinimumHeight(10);
	    ((LinearLayout)findViewById(R.id.serverLayout)).addView( space );
	    //listedServers.add( sv.getServer( ).getID( ) );
	}
    }













    //  ASYNC TASKS.....











    
    //__________________________________________________________________________________
    protected class AsyncTaskOSListServers extends AsyncTask<Void, String, String>
    {
     	private  String   errorMessage     = null;
	private  boolean  hasError         = false;
	private  String   jsonBuf          = null;
	private  String   jsonBufferFlavor = null;
	private  String   username         = null;

	@Override
	protected String doInBackground( Void... v ) 
	{
	    if(U.getTokenExpireTime() <= Utils.now() + 5) {
		try {
		    jsonBuf = RESTClient.requestToken( U.getEndpoint(),
						       U.getTenantName(),
						       U.getUserName(),
						       U.getPassword(),
						       U.useSSL() );
		    String  pwd = U.getPassword();
		    String  edp = U.getEndpoint();
		    boolean ssl = U.useSSL();
		    U = ParseUtils.parseUser( jsonBuf );
		    U.setPassword( pwd );
		    U.setEndpoint( edp );
		    U.setSSL( ssl );
		    U.toFile();// to save new token + expiration
		} catch(Exception e) {
		    errorMessage = e.getMessage();
		    hasError = true;
		    return "";
		}
	    }

	    username = U.getUserName();

	    try {
		jsonBuf = RESTClient.requestServers( U.getEndpoint(), U.getToken(), U.getTenantID(), U.getTenantName() );
		jsonBufferFlavor = RESTClient.requestFlavors( U.getEndpoint(), U.getToken(), U.getTenantID(), U.getTenantName() );
	    } catch(Exception e) {
		errorMessage = e.getMessage();
		hasError = true;
		return "";
	    }
	    
	    return jsonBuf;
	}
	
	@Override
	    protected void onPostExecute( String result ) {
	    super.onPostExecute(result);
	    
 	    if(hasError) {
 		Utils.alert( errorMessage, ServersActivity.this );
 		ServersActivity.this.progressDialogWaitStop.dismiss( );
 		return;
 	    }
	    
	    try {
		Vector<Server> servers = ParseUtils.parseServers( jsonBuf, username );
		Hashtable<String, Flavor> flavors = ParseUtils.parseFlavors( jsonBufferFlavor );
		ServersActivity.this.refreshView( servers, flavors );
	    } catch(ParseException pe) {
		Utils.alert("ServersActivity.AsyncTaskOSListServers.onPostExecute: "+pe.getMessage( ), ServersActivity.this );
	    }
	    ServersActivity.this.progressDialogWaitStop.dismiss( );
	}
    }
    
    //__________________________________________________________________________________
    protected class AsyncTaskDeleteServer extends AsyncTask<String, Void, Void>
    {
     	private  String   errorMessage     = null;
	private  boolean  hasError         = false;
	private  String   jsonBuf          = null;
	private  String   jsonBufferFlavor = null;
	private  String[] serverids        = null;
	private  String   username         = null;
	private  boolean  not_found        = false;
	@Override
	protected Void doInBackground(String... args ) 
	{
	    serverids = args[0].split(",");
	    if(U.getTokenExpireTime() <= Utils.now() + 5) {
		try {
		    jsonBuf = RESTClient.requestToken( U.getEndpoint(),
						       U.getTenantName(),
						       U.getUserName(),
						       U.getPassword(),
						       U.useSSL() );
		    String  pwd = U.getPassword();
		    String  edp = U.getEndpoint();
		    boolean ssl = U.useSSL();
		    U = ParseUtils.parseUser( jsonBuf );
		    U.setPassword( pwd );
		    U.setEndpoint( edp );
		    U.setSSL( ssl );
		    U.toFile();// to save new token + expiration
		    username = U.getUserName();
		} catch(Exception e) {
		    errorMessage = e.getMessage();
		    hasError = true;
		    return null;
		}
	    }

	    try {
		for(int i = 0; i<serverids.length; ++i) {
		    try {
			RESTClient.deleteInstance( U.getEndpoint(), U.getToken(), U.getTenantID(), serverids[i] );
		    } catch(NotFoundException nfe) {
			not_found = true;
		    }
		}
		// jsonBuf = RESTClient.requestServers( U.getEndpoint(), U.getToken(), U.getTenantID(), U.getTenantName() );
		// jsonBufferFlavor = RESTClient.requestFlavors( U.getEndpoint(), U.getToken(), U.getTenantID(), U.getTenantName() );
	    } catch(Exception e) {
		errorMessage = e.getMessage();
		hasError = true;
		return null;
	    }
	    
	    return null;
	}
	
	@Override
        protected void onPostExecute( Void v ) {
	    super.onPostExecute(v);
	    ServersActivity.this.progressDialogWaitStop.dismiss( );
	    if(not_found) {
		Utils.alert(ServersActivity.this.getString(R.string.SOMEDELETIONFAILED), ServersActivity.this );
		return;
	    }
 	    if(hasError)
 		Utils.alert( errorMessage, ServersActivity.this );
 	    else
		Utils.alert(getString(R.string.DELETEDINSTSANCES), ServersActivity.this );
	    
	}
    }
}
