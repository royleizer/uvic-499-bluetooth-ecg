package uvic499.software.ecg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.InputFilter.LengthFilter;

public class BluetoothConnService extends Service {
	
	private final BluetoothAdapter BT = BluetoothAdapter.getDefaultAdapter();
	// TODO: BT board standard one is 00001101-0000-1000-8000-00805F9B34FB. Try 00001101-0000-1000-8000-00805F9B34FA
	protected static final UUID serverUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FA");
	private BluetoothSocket connectionSocket = null;
	private BroadcastReceiver btReceiver;
	// Intent code for Bluetooth server thread to reactivate.  String is in Android manifest xml
	private final String BLUETOOTH_ACTION_DONE_READING = "android.intent.action.BLUETOOTH_READ_DONE";

	private AcceptThread acceptThread;
	private BluetoothReadThread readThread;
	
	// Queue that ECGChartActivity uses to grab Bluetooth data; so far this has been fast/efficient enough
	static LinkedBlockingQueue<Double> bluetoothQueueForSaving = new LinkedBlockingQueue<Double>();
	static LinkedBlockingQueue<Double> bluetoothQueueForUI = new LinkedBlockingQueue<Double>();
	
	@Override
	public void onDestroy() {
		unregisterReceiver(btReceiver);
		try {
			acceptThread.cancel();
			readThread.cancel();
		} catch (Exception e) {
			// If Threads have not started, I don't know what exception will be thrown:
			// so catch generic Exception, don't crash, assume Threads are gone.
		}
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    System.out.println("IN BT_CONN_SERVICE's onStartCommand!");
	    //addFileToQueue(); TODO: I am now trying Bluetooth stuff.  Forget dummy file writing for now
	    
	    // Create a BroadcastReceiver for BluetoothAdapter.ACTION_SCAN_MODE_CHANGED
	    // This will alert us when a device disconnects so we can spawn a new AcceptThread for another connection
	    btReceiver = new BroadcastReceiver() {
	        public void onReceive(Context context, Intent intent) {
	            String action = intent.getAction();
	            System.out.println("-----ACTION = "+action);
	        	// When discovery finds a device
	            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
	            	
	            	// If a server thread is active, and scan mode changed, need to make sure 
	            	// device is still discoverable
	            	if (acceptThread != null && acceptThread.isAlive()) {
	            		setDeviceToDiscoverable();
	            	}	            	
	            	
	            }
	            else if (BLUETOOTH_ACTION_DONE_READING.equals(action)) {
	                // Bluetooth channel has been disconnected.
	            	// Wait for readThread to finish, then start another acceptThread
	            	try{
	            		Thread.sleep(2000);
	            	} catch (InterruptedException e){}
	            	startBTServer();
	            }
	        }
	        
	    };
	    
	    // Now register the BroadcastReceiver to handle Bluetooth server disconnects and device scan mode changes
	    IntentFilter filter = new IntentFilter(BLUETOOTH_ACTION_DONE_READING);
	    filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
	    registerReceiver(btReceiver, filter);
	    
	    // Start first BT server Thread
	    startBTServer();
	    
	    System.out.println("--Started activity thread!");
	    
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	private void startBTServer() {
		// Always cancel device discovery - this slows BT down if it is on
		if (BT.isDiscovering()) {
			BT.cancelDiscovery();
		}
		
		// So that server socket is visible
		setDeviceToDiscoverable();
    	
		// Now close the read thread if it was running
    	try {
    		readThread.continueReading = false;
			readThread.join();
		} catch (InterruptedException e) {
			readThread.interrupt();
		} catch (NullPointerException nulle) {
			// readThread not initialized - this is fine
		}
    	
    	//TODO: Try keeping acceptThread alive
    	if (acceptThread != null && acceptThread.isAlive()) return; 
    	// Now close the server if it was running
    	try {
    		acceptThread.cancel();
			acceptThread.join();
		} catch (InterruptedException e) {
			acceptThread.interrupt();
		} catch (NullPointerException nulle) {
			// acceptThread not initialized - this is fine
		}
		
		acceptThread = new AcceptThread();
		acceptThread.start();
	}
	
	
	private void setDeviceToDiscoverable() {
		// Ensure device is discoverable so ECG can see its service.
		int scan = BT.getScanMode();
		if(scan != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	    	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	    	discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	startActivity(discoverableIntent);
		}
	}
	
	class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	    
	    public AcceptThread() {
	    	// Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	        	// TODO: try sleeping before making a new thread so that SDP entry can be fully removed
	        	Thread.sleep(2000);
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = BT.listenUsingRfcommWithServiceRecord("ECG Monitor", serverUUID);
	            
	        } catch (IOException e) {
	        	System.out.println("-- Socket listen() failed!");
	        } catch (InterruptedException e){}
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	    	System.out.println("Starting BT server...");
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {	
	            try {
	                connectionSocket = mmServerSocket.accept();
	            } catch (IOException e) {
	                System.out.println(e+"\nCouldn't mmServerSocket.accept()!");
	                break;
	            }
	            
	            // If a connection was accepted
	            if (connectionSocket != null) {
	                // Do work to manage the connection (in a separate thread for reading)
	                
	            	readThread = new BluetoothReadThread();
	            	readThread.start();
	            	
	            	
	            	// TODO: Instead of closing socket, try just keeping it alive!
	            	/*
                	try {
						mmServerSocket.close();
					} catch (IOException e) { }
                	
                	break; // Exit the loop - the connection is done
                	*/
	            }
	        }
	        
	        System.out.println("////// EXITED SERVER READ LOOP");
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	class BluetoothReadThread extends Thread {
		
		private final InputStream iStream;
		private boolean continueReading = true;
		
		public BluetoothReadThread() {
			InputStream tmp = null;
			try {
				tmp = connectionSocket.getInputStream();
			} catch (IOException e) {	
			}
			iStream = tmp;
		}
		
		@Override
		public void run() {
			List<Character> doubleBuilder = new ArrayList<Character>();
			int i = 0;
			char c;
			int waitCount=0;
        	while (continueReading) {
				try {
					// Read integer values from Bluetooth stream.
					// Assemble them manually into doubles, split on newline (\n) character
            		if (iStream.available() > 0) {
            			waitCount = 0;
            			c = (char)iStream.read();
            			if (c == '\n') {
            				// end of the double, put it in the queue
            				char charArray[] = new char[doubleBuilder.size()];
            				for (char b : doubleBuilder) {
            					charArray[i] = b;
            					i++;
            				}
            				Double d = Double.valueOf(String.copyValueOf(charArray));
            				System.out.println("--- queueing:" + String.copyValueOf(charArray));
            				bluetoothQueueForSaving.offer(d);
            				if (ECGChartActivity.isActive)
            					bluetoothQueueForUI.offer(d);
            				i = 0;
            				doubleBuilder.clear();
            			} else {
            				doubleBuilder.add(c);
            			}
            		} else { 
            			if (waitCount >= 500000) {
            				// No data ready in 500000 loop cycles, ECG has probably been disconnected. Close self.
            				waitCount = 0;
            				System.out.println("----wait count expired");
            				continueReading = false;
            				this.stopAndSendIntent();
            			} else {
            				waitCount++;
            			}
            		}
            		
	        	} catch (IOException e) {
	        		System.out.println(e+"\nError sending data n shit\n");
	        		// Bluetooth error! Stop reading.
	        		this.stopAndSendIntent();
	        	}
        	}
		}
		
		public void stopAndSendIntent() {
			
			this.cancel();
			
			Intent intent = new Intent();
			intent.setAction(BLUETOOTH_ACTION_DONE_READING);
			sendBroadcast(intent);
		}
		
		public void cancel() {
			System.out.println("-----Cancelling readThread!!");
			try{
				iStream.close();
			} catch (IOException e) {
			} catch (NullPointerException e){};
			
			continueReading = false;
		}
	}
	/*
	 * TODO: This method is probably obsolete now
	 * 
	 * Method to add data to the Bluetooth queue - for now, loop through a file because
	 * we don't have BT connection stuff.
	 * Read file in new Thread, add doubles to queue.
	 */
	private void addFileToQueue() {
			
		Thread t = new Thread(new Runnable() {
			public void run() {
				// Loop through the test data to get more heartbeats
				while (true) {
					if (bluetoothQueueForSaving.size() > 2000) {
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
	        	bluetoothQueueForSaving.offer(Double.valueOf(readLine));
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