package org.stackdroid.activities;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.stackdroid.R;
import org.stackdroid.comm.OSClient;
import org.stackdroid.comm.commands.Command;
import org.stackdroid.parse.ParseException;
import org.stackdroid.utils.Configuration;
import org.stackdroid.utils.CustomProgressDialog;
import org.stackdroid.utils.Defaults;
import org.stackdroid.utils.Flavor;
import org.stackdroid.utils.FloatingIP;
import org.stackdroid.utils.Quota;
import org.stackdroid.utils.QuotaVol;
import org.stackdroid.utils.SecGroup;
import org.stackdroid.utils.Server;
import org.stackdroid.utils.User;
import org.stackdroid.utils.Utils;;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.util.Log;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OverViewActivity extends Activity {

    private CustomProgressDialog progressDialogWaitStop = null;
    private User 		 U 			= null;
 
    /**
     *
     *
     *
     */
    public boolean onCreateOptionsMenu( Menu menu ) {
        
        super.onCreateOptionsMenu( menu );
        
        int order = Menu.FIRST;
        int GROUP = 0;
        menu.add(GROUP, 0, order++, getString(R.string.MENUHELP)    ).setIcon(android.R.drawable.ic_menu_help);
        return true;
    }

    /**
     *
     *
     *
     */
    public boolean onOptionsItemSelected( MenuItem item ) {
    	
        int id = item.getItemId();     
        
        if( id == Menu.FIRST-1 ) {
            Utils.alert( getString(R.string.NOTIMPLEMENTED) ,this );
            return true;
        }
        return super.onOptionsItemSelected( item );
    }
    
   /**
    *
    *
    *
    */
   public void update( View v ) {
	   	progressDialogWaitStop.show();
   		(new AsyncTaskQuota()).execute( );
   }

    /**
     *
     *
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView( R.layout.overview );

    	progressDialogWaitStop = new CustomProgressDialog( this, ProgressDialog.STYLE_SPINNER );
        progressDialogWaitStop.setMessage( getString(R.string.PLEASEWAITCONNECTING) );
        progressDialogWaitStop.setCancelable(false);
        progressDialogWaitStop.setCanceledOnTouchOutside(false);
        String selectedUserID = Utils.getStringPreference("SELECTEDUSER", "", this);
        setTitle(getString(R.string.USAGEOVERVIEW));
        try {
        	U = User.fromFileID( selectedUserID, Configuration.getInstance().getValue("FILESDIR",Defaults.DEFAULTFILESDIR) );
        	//setTitle(getString(R.string.USAGEOVERVIEW) + " " + U.getUserName() + " ("+U.getTenantName()+")");
        	//Log.d("OVERVIEW", "USER="+U);
        	if(U==null) {
        		Utils.alert(getString(R.string.RECREATEUSERS), this);
        		return;
        	}
        	progressDialogWaitStop.show();
        	(new AsyncTaskQuota()).execute( );
        }  catch(Exception re) {
        	Utils.alert("OverViewActivity.onCreate: " + re.getMessage(), this );
        }
        if(selectedUserID.length()!=0)
	    ((TextView)findViewById(R.id.selected_user)).setText(getString(R.string.SELECTEDUSER)+": "+U.getUserName() + " (" + U.getTenantName() + ")"); 
	else
	    ((TextView)findViewById(R.id.selected_user)).setText(getString(R.string.SELECTEDUSER)+": "+getString(R.string.NONE)); 	
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

    /**
     *
     *
     *
     *
     */
    private void refreshView( Quota Q,
    						  QuotaVol QV,
    						  Vector<Server> servers, 
    						  Vector<Flavor> flavors,
    						  Vector<FloatingIP> fips, 
    						  Vector<SecGroup> secgs) 
    {
	
    	Iterator<Server> it = servers.iterator();
    	int totMem = 0;
    	int totVCPU = 0;
    	int totInstances = 0;
	
    	Hashtable<String, Flavor> flavHash = new Hashtable<String, Flavor>();
    	Iterator<Flavor> fit = flavors.iterator();
    	while( fit.hasNext( ) ) {
    		Flavor f = fit.next();
    		flavHash.put( f.getID(), f );
    	}
    	while( it.hasNext( ) ) {
    		Server S = it.next( );
    		Flavor F = flavHash.get( S.getFlavorID( ) );
    		if(F!=null) {
    			totMem += F.getRAM( );
    			totVCPU += F.getVCPU( );
    		} else {
    			Utils.alert("FlavorID Mismatch! The instance ["+S.getID()+"] has a FlavorID ["+S.getFlavorID( )+"] which is not present in the flavor list", this);
    		}
    		totInstances++;
    	}

		//Log.d("OVERVIEW", "Max Sec Groups="+Q.getMaxSecurityGroups());
		//Log.d("OVERVIEW", QV.toString());

		((TextView)findViewById(R.id.vmusageTV)).setText("" + totInstances );
    	((TextView)findViewById(R.id.vmusageMAXTV)).setText("/" + Q.getMaxInstances() );
    	((ProgressBar)findViewById(R.id.vmusagePB)).setMax( Q.getMaxInstances() );
    	((ProgressBar)findViewById(R.id.vmusagePB)).setProgress( totInstances);
	
    	((TextView)findViewById(R.id.cpuusageTV)).setText("" + totVCPU );
    	((TextView)findViewById(R.id.cpuusageMAXTV)).setText("/" + Q.getMaxCPU() );
    	((ProgressBar)findViewById(R.id.cpuusagePB)).setMax( Q.getMaxCPU() );
    	((ProgressBar)findViewById(R.id.cpuusagePB)).setProgress( totVCPU );
	
    	((TextView)findViewById(R.id.ramusageTV)).setText("" + totMem );
    	((TextView)findViewById(R.id.ramusageMAXTV)).setText("/" + Q.getMaxRAM( ) );
    	((ProgressBar)findViewById(R.id.ramusagePB)).setMax( Q.getMaxRAM( ) );
    	((ProgressBar)findViewById(R.id.ramusagePB)).setProgress( totMem );

    	((TextView)findViewById(R.id.fipusageTV)).setText("" + (fips!=null ? fips.size() : 0) );
    	((TextView)findViewById(R.id.fipusageMAXTV)).setText("/" + Q.getMaxFloatingIP( ) );
    	((ProgressBar)findViewById(R.id.fipusagePB)).setMax( Q.getMaxFloatingIP( ) );
    	((ProgressBar)findViewById(R.id.fipusagePB)).setProgress( fips!=null ? fips.size() : 0 );
    	
    	((TextView)findViewById(R.id.volusageTV)).setText("" + QV.getVolumeUsage() );
    	((TextView)findViewById(R.id.volusageMAXTV)).setText("/" + QV.getMaxVolumes() );
    	((ProgressBar)findViewById(R.id.volumesPB)).setMax( QV.getMaxVolumes() );
    	((ProgressBar)findViewById(R.id.volumesPB)).setProgress( QV.getVolumeUsage() );
    	
    	((TextView)findViewById(R.id.gigausageTV)).setText("" + QV.getGigabyteUsage( ) );
    	((TextView)findViewById(R.id.gigausageMAXTV)).setText("/" + QV.getMaxGigabytes( ) );
    	((ProgressBar)findViewById(R.id.gigaPB)).setMax( QV.getMaxGigabytes( ) );
    	((ProgressBar)findViewById(R.id.gigaPB)).setProgress( QV.getGigabyteUsage( ) );
    	
    	
    }

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */
    protected class AsyncTaskQuota extends AsyncTask<Void, String, String>
    {
     	private  String   errorMessage     = null;
     	private  boolean  hasError         = false;
     	private  String   jsonBuf          = null;
     	private  String   jsonBufQuota     = null;
     	private  String   jsonBufFIPs      = null;
     	private  String   jsonBufSecgs     = null;
     	private  String   jsonBufferFlavor = null;
		private String    jsonBufferVolumes= null;
		private String    jsonBufferQuotaVols= null;
	
     	@Override
     	protected String doInBackground(Void... u ) 
     	{
     		OSClient osc = OSClient.getInstance(U);
	    
	    	try {
	    		jsonBufQuota 	    = osc.listQuotas( );
	    		//jsonBuf 	    	= osc.listServers( );
	    		Command cmd = Command.commandFactory(Command.commandType.LISTSERVERS, U );
	    		if(cmd!=null)
	    			cmd.execute();
	    		else {
					//Utils.alert("SEVERE ERROR: Command.commandFactory return null object!", OSImagesActivity.this);
					hasError = true;
					errorMessage = "SEVERE ERROR: Command.commandFactory has returned null object!";
     				return null;
	    		}
	    		jsonBuf = cmd.getRESTResponse();
	    		jsonBufFIPs 	    = osc.listFloatingIPs( );
	    		jsonBufSecgs 	    = osc.listSecGroups( );
	    		jsonBufferFlavor    = osc.listFlavors( );
	    		jsonBufferVolumes   = osc.listVolumes( );
	    		jsonBufferQuotaVols = osc.listVolQuotas( );
	    	} catch(Exception e) {
	    		errorMessage = e.getMessage();
	    		hasError = true;
	    		return "";
	    	}
	    
	    	return jsonBuf;
     	}
	
     	@Override
        protected void onPreExecute() {
     		super.onPreExecute();
     	}
	
     	@Override
     		protected void onPostExecute( String result ) {
     		super.onPostExecute(result);
     		
     		if(hasError) {
     			Utils.alert( errorMessage, OverViewActivity.this );
     			OverViewActivity.this.progressDialogWaitStop.dismiss( );
     			return;
     		}
	    
     		try {
     			Quota Q = Quota.parse( jsonBufQuota );
     			QuotaVol QV = QuotaVol.parse(jsonBufferQuotaVols);
     			OverViewActivity.this.refreshView( Q,
     							   QV,
     							   Server.parse( jsonBuf, null ),
     							   Flavor.parse( jsonBufferFlavor ),
     							   FloatingIP.parse( jsonBufFIPs, false ),
     							   SecGroup.parse( jsonBufSecgs )
     							 );
     		} catch(ParseException pe) {
     			Utils.alert( pe.getMessage( ), OverViewActivity.this );
     		}

     		OverViewActivity.this.progressDialogWaitStop.dismiss( );
     	}
    }
}
