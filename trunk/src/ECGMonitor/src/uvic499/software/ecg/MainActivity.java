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
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	protected final static int REQUEST_ENABLE_BLUETOOTH = 1;
	private BluetoothAdapter BT = BluetoothAdapter.getDefaultAdapter();
	private String deviceName;
	private String deviceMAC;
	private BluetoothConnService serviceBT = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        toggleUI(false);
        
        serviceBT = new BluetoothConnService();
        checkBluetooth();
        
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private void checkBluetooth() {
		// check if the Bluetooth is going.
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
			} else if (resultCode == Activity.RESULT_CANCELED) {
				System.out.println("RESULT_CANCELLED!!!");
				toggleUI(false);
			}
		}
	}
	
	
	public void startChart(View v) {
		Intent intent = new Intent(this, ECGChartActivity.class);
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
		/* TODO: Where to read Bluetooth stuff?  Where to put it?
		Thread t = serviceBT.new ConnectThread(device);
		t.start();
		
		BluetoothSocket btSocket = device.createInsecureRfcommSocketToServiceRecord(BluetoothConnService.serverUUID);
		btSocket.connect();
		InputStream input = btSocket.getInputStream();
		DataInputStream dinput = new DataInputStream(input);
		*/
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




