package uvic499.software.ecg;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	protected final static int REQUEST_ENABLE_BLUETOOTH = 1;
	private BluetoothAdapter BT = BluetoothAdapter.getDefaultAdapter();
	private String deviceName;
	private String deviceMAC;
	
	private Intent btConnServiceIntent;
	private Intent dataSaveServiceIntent;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        toggleUI(false);
        
        btConnServiceIntent = new Intent(this.getApplicationContext(), BluetoothConnService.class);
        checkBluetooth();
        
        dataSaveServiceIntent = new Intent(this.getApplicationContext(), DataSaveService.class);
        
        // start services only once Bluetooth is on
        if (BT.isEnabled()) {
        	startService(btConnServiceIntent);
        	startService(dataSaveServiceIntent);
        }
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	// Stop the Bluetooth service when the app is closed
	// NOTE: onStop is called when Activity loses focus -
	// in this case App is still running, so we want service to keep running too
	@Override
	public void onDestroy() {
		stopService(btConnServiceIntent);
		stopService(dataSaveServiceIntent);
		super.onDestroy();
	}

	// Turns Bluetooth on if it is off.  Starts BT service handler afterwards
	private void checkBluetooth() {
		if (!BT.isEnabled()) {
			// Bluetooth not enabled: display a message and prompt user to turn it on			
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBT, REQUEST_ENABLE_BLUETOOTH);
		} else {
			toggleUI(true);
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
				
				// Now start Bluetooth service
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
	
	// TODO: Try to read from a paired device
	public void readBT(View v) throws IOException {
		Set<BluetoothDevice> deviceSet = BT.getBondedDevices();
		
		if (deviceSet.isEmpty()) {
			String toastText = "No bluetooth object is paired with this device.  Pair one first.";
			Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
			return;
		}
		
		BluetoothDevice device = (BluetoothDevice)deviceSet.toArray()[0];
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
