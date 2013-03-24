package uvic499.software.ecg;

import java.lang.reflect.Field;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	protected final static int REQUEST_ENABLE_BLUETOOTH = 1;
	private BluetoothAdapter BT = BluetoothAdapter.getDefaultAdapter();
	private String deviceName;
	private String deviceMAC;
	static String documentSavePath = "";
	
	private Intent btConnServiceIntent;
    
	private boolean appLoadFinished = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
    	toggleUI(false);
		
		btConnServiceIntent = new Intent(MainActivity.this, BluetoothConnService.class);
		// start services only if Bluetooth is on
        if (BT.isEnabled()) {
        	startService(btConnServiceIntent);
        }
        
        checkBluetooth();

		if (savedInstanceState != null) {
	    	documentSavePath = savedInstanceState.getString("documentSavePath");
	    }		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	// Stop the Bluetooth service when the app is closed
	// NOTE: do nothing onStop, as it is called when Activity loses focus -
	// in this case App is still running, so we want service to keep running too
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (isFinishing()) {
			// TODO: Would maybe want to save the file now
			
			// Definitely want to stop Bluetooth connections
			stopService(btConnServiceIntent);
		} else {
			// just an orientation change - do nothing
		}
		//TODO: I blocked this stopService(dataSaveServiceIntent);
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle b) {
		super.onSaveInstanceState(b);
		b.putString("documentSavePath", documentSavePath);
	}

	// Turns Bluetooth on if it is off.  Starts BT service handler afterwards
	private void checkBluetooth() {
		if (!BT.isEnabled()) {
			// Bluetooth not enabled: display a message and prompt user to turn it on			
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBT, REQUEST_ENABLE_BLUETOOTH);
		} else {
			toggleUI(true);
			appLoadFinished = true;
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
			if (resultCode == Activity.RESULT_OK) {
				deviceMAC = BT.getAddress();
				deviceName = BT.getName();
				String toastText = "Bluetooth enabled for device\n" + deviceName + " : " + deviceMAC;
				// turn UI on and show success msg
				toggleUI(true);
				Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
				

				// Now start Bluetooth
				startService(btConnServiceIntent);

			} else if (resultCode == Activity.RESULT_CANCELED) {
				System.out.println("RESULT_CANCELLED!!!");
				toggleUI(false);
			}
		}
	}
	
	// Chart Button onClick
	public void startChart(View v) {
		Intent intent = new Intent(this, ECGChartActivity.class);
		startActivity(intent);
	}
	
	// View Data Button onClick
	public void viewSavedData(View v) {
		Intent intent = new Intent(this, ViewSavedDataActivity.class);
		startActivity(intent);
	}
	
	private void toggleUI(boolean enabled) {
		System.out.println("Toggling UI to " + enabled);
		int visibility = enabled ? View.VISIBLE : View.GONE;
		for (Field f : R.id.class.getFields()) {
			try {
				int id = f.getInt(null);
				
				// If BT connection msg, do opposite action, and always hide progress bar
				View v = this.findViewById(id);
				
				if (id == R.id.Bluetooth_connection) {
					v.setEnabled(!enabled);
					int tempVis = enabled ? View.GONE : View.VISIBLE;
					v.setVisibility(tempVis);
				} else {
					v.setEnabled(enabled);
					v.setVisibility(visibility);
				}
			} catch (Exception e) {
		    	// TODO: just keep going through all fields... why not?
				System.out.println(e);
		    }
		}
	}
	
}
