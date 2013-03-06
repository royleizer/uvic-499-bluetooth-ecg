package uvic499.software.ecg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;



public class BluetoothConnService extends Service {
	
	private final BluetoothAdapter BT = BluetoothAdapter.getDefaultAdapter();
	// TODO: BT board standard one is 00001101-0000-1000-8000-00805F9B34FB. Try 00001101-0000-1000-8000-00805F9B34FA
	protected static final UUID serverUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FA");
	private BluetoothSocket serverSocket = null;
	
	// Queue that ECGChartActivity uses to grab Bluetooth data
	public static LinkedBlockingQueue<Double> bluetoothQueue = new LinkedBlockingQueue<Double>();

	public void onCreate(Bundle savedInstance) {
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    System.out.println("IN BT_CONN_SERVICE's onStartCommand!");
	    addFileToQueue();
	    
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = BT.listenUsingRfcommWithServiceRecord("ECG Monitor", serverUUID);
	        } catch (IOException e) { }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	    	System.out.println("Starting BT server...");
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                serverSocket = mmServerSocket.accept();
	            } catch (IOException e) {
	                break;
	            }
	            // If a connection was accepted
	            if (serverSocket != null) {
	                // Do work to manage the connection (in a separate thread)
	                // TODO: work the queue here? or in a new thread now?
	                try {
	                	// do work to managed connection
	                	mmServerSocket.close();
	                	break;
	                } catch (IOException e) {
	                	break;
	                }
	            }
	        }
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	
	class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            tmp = device.createInsecureRfcommSocketToServiceRecord(serverUUID);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        BT.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        //manageConnectedSocket(mmSocket);
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	/* Method to add data to the Bluetooth queue - for now, loop through a file because
	 * we don't have BT connection stuff.
	 * Read file in new Thread, add doubles to queue.
	 */
	private void addFileToQueue() {
			
		Thread t = new Thread(new Runnable() {
			public void run() {
				// Loop through the test data to get more heartbeats
				while (true) {
					if (bluetoothQueue.size() > 2000) {
						try{
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							continue;
						}
					}
					readFile(R.raw.short_ecg);
				}
			}
		});
		t.start();
	}
	
	// Helper method for adding hard-coded ecg file data to Bluetooth queue
	private void readFile(int resourceId) {
	    // The InputStream opens the resourceId and sends it to the buffer
	    InputStream is = this.getResources().openRawResource(resourceId);
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String readLine = null;

	    try {
	        // While the BufferedReader readLine is not null 
	        while ((readLine = br.readLine()) != null) {
	        	bluetoothQueue.offer(Double.valueOf(readLine));
	        }

	    // Close the InputStream and BufferedReader
	    is.close();
	    br.close();
	    
	    System.out.println("Done reading file!\n");
	    
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}