package uvic499.software.ecg;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.IBinder;

public class DataSaveService extends Service {

	// TODO: make package visibility so that ViewDataActivity can see it
	static DataThread writeThread;
	public LinkedBlockingQueue<Double> queue = BluetoothConnService.bluetoothQueueForSaving;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		
		/*
		 * thread:
		 * 	set up file (possibly based on user)
		 * 	save data in queue to file
		 * 	work until queue is empty.
		 * 
		 * (extra): have a way of viewing the file
		 */
		
		String filepath = getActiveSavePath();
		
		writeThread = new DataThread(filepath);
		writeThread.start();
		
		// Keep running, so sticky
		return START_STICKY;
	}
	/*TODO: encapsulate this cross-app
	 * Gets the appropriate file to write to.  If multiple users are supported, should have a new file for each
	 * and some way to know who's file to access.
	 * 
	 * For now, just use/overwrite the same file each time the service is started.
	 */
	String getActiveSavePath() {	
		Context appContext = getApplicationContext();
		return appContext.getString(R.string.default_save_file);
		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onDestroy() {
		writeThread.cancel();
		super.onDestroy();
	}

	class DataThread extends Thread {
		private final FileOutputStream outStream;
		private boolean continueWriting = true;
		
		public DataThread(String filepath) {
			FileOutputStream tmp = null;
			try{
				tmp = openFileOutput(filepath, Context.MODE_PRIVATE);
			} catch (FileNotFoundException e){
				System.out.println(e);
			}
			outStream = tmp;
			
			// Make sure BluetoothConnService has run first and initialized the queue before trying to run
			try {
				while (queue == null) {
					Thread.sleep(2000);
				}
			} catch(InterruptedException e) {}
		}
		
		@Override
		public void run() {
			System.out.println("---MY ACTIVE LISTS:");
			for (String s : getApplicationContext().fileList())
				System.out.println("\t" + s);
			while (continueWriting) {
				try {
					// Wait on the queue for up to 5-seconds, since it doesn't matter if we are slow in data writing
					Double d = queue.poll(5, TimeUnit.SECONDS);
					if (d == null) continue;
					outStream.write(d.toString().getBytes());
					// write a new-line character too!
					outStream.write("\n".getBytes());
				} catch (InterruptedException e){
					break;
				} catch (IOException e) {
					System.out.println(e+"\nError writing to file!");
					break;
				}
			}
			
			try {
				outStream.close();
			} catch (IOException e) {
				System.out.println(e+"\nError closing file");
			}
		}
		
		public void cancel() {
			continueWriting = false;
		}
	}
	
}
