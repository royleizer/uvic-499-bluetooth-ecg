package uvic499.software.ecg;

import java.util.concurrent.LinkedBlockingQueue;


// TODO: Make this a static class to hold data/file saving stuff
public class DataSaving {

	public LinkedBlockingQueue<Double> queue = BluetoothConnService.bluetoothQueueForSaving;
	
	/* 
	 * Saves history queue data to file - empties the queue in the process
	 */
	static void startSave(String savePath) {
		/*
		//TODO: Saving data to file and Reading from file should be synchronized
		synchronized(MainActivity.fileAccess) {
			FileOutputStream outStream = null;
			try{
				outStream = openFileOutput(savePath, Context.MODE_PRIVATE);
			} catch (FileNotFoundException e){
				System.out.println("---saveFile not found: "+e);
				return;
			}
			
			while (!queue.isEmpty()) {
				try {
					Double d = queue.poll();
					if (d == null) return; // We shouldn't get here
					outStream.write(d.toString().getBytes());
					// write a new-line character too!
					outStream.write("\n".getBytes());
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
		*/	
	}
	
}
