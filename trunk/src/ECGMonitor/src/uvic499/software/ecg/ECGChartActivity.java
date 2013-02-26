package uvic499.software.ecg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ConcurrentModificationException;
import java.util.concurrent.LinkedBlockingQueue;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class ECGChartActivity extends Activity {

	 private static TimeSeries timeSeries;
	 private static XYMultipleSeriesDataset dataset;
	 private static XYMultipleSeriesRenderer renderer;
	 private static XYSeriesRenderer rendererSeries;
	 private static GraphicalView view;
	 private double samplingRate = 0.1;
	 private int pointsToDisplay = 5;
	 private int inactiveCycles = 0;
	 private int xScrollAhead = 2;
	 private double currentX = 0;
	 private LinkedBlockingQueue<Double> queue = new LinkedBlockingQueue<Double>();

	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	     super.onCreate(savedInstanceState);     
	     setContentView(R.layout.activity_chart);
	     addFileToQueue();
	     
	     dataset = new XYMultipleSeriesDataset();
	     renderer = new XYMultipleSeriesRenderer();
	     
	     renderer.setApplyBackgroundColor(true);
	     renderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
	     renderer.setLabelsTextSize(35);
	     renderer.setLegendTextSize(35);
	     renderer.setAxesColor(Color.BLUE);
	     renderer.setAxisTitleTextSize(35);
	     renderer.setChartTitle("ECG Heartbeat");
	     renderer.setChartTitleTextSize(35);
	     renderer.setFitLegend(false); //TODO
	     renderer.setGridColor(Color.LTGRAY);
	     renderer.setPanEnabled(false, false); // TODO
	     renderer.setPointSize(1);
	     renderer.setXTitle("Time");
	     renderer.setYTitle("Num");
	     renderer.setMargins(new int []{40, 60, 30, 10}); // TODO: i doubled
	     renderer.setZoomButtonsVisible(false);
	     renderer.setZoomEnabled(false);
	     renderer.setBarSpacing(10);
	     renderer.setShowGrid(false);
	     renderer.setYAxisMax(2.4);
	     renderer.setYAxisMin(0.4);
	     
	     rendererSeries = new XYSeriesRenderer();
	     rendererSeries.setColor(Color.RED);
	     renderer.addSeriesRenderer(rendererSeries);
	     
	     timeSeries = new TimeSeries("ECG data");
	     
	     new Thread(new Runnable() {
	         public void run() {
             	 while(true) {
            		 if (inactiveCycles > 40) {
     	           		 System.out.println("ENDING PRINT SERIES");
     	           		 return;
     	           	 }
     		         try {
     	                 Thread.sleep(100);
     	             } catch (InterruptedException e) {
     	                 e.printStackTrace();
     	                 continue;
     	             }
 	               	 currentX = currentX+samplingRate;
                 	 Double yVal = null;
                 	 for (int i=0; i<600; i++) {
                 		  yVal = queue.poll();
                 	 }
 	               	 // skip if no data on queue
 	               	 if (yVal == null) {
 	               		 inactiveCycles++;
 	               		 continue;
 	               	 } else {
 	               		 inactiveCycles = 0;
 	               	 }
 	               	 synchronized(timeSeries) {
 	               		timeSeries.add(currentX, yVal);
 	               	 }
 	               	 synchronized(renderer) {
	           	 		 if (currentX - pointsToDisplay >= 0 ) {
	           		   		 renderer.setXAxisMin(currentX - pointsToDisplay);
	           		   	 }
	           		   	 renderer.setXAxisMax(pointsToDisplay + currentX + xScrollAhead);
               		 }
 	               	 synchronized(view) {
               			 view.repaint();
 	               	 }
           	     }
	         }
	     }).start();
	 }
	
	 @Override
	 protected void onStart() {
	     super.onStart();
	     dataset.addSeries(timeSeries);
	     view = ChartFactory.getLineChartView(this, dataset, renderer);
	     view.refreshDrawableState();
	     view.repaint();
	     setContentView(view);
	 }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	// Read file in new Thread, add doubles to queue.
	private void addFileToQueue() {
			
		Thread t = new Thread(new Runnable() {
			public void run() {
				// Loop through the test data to get more heartbeats
				while (true) {
					if (queue.size() > 20000) {
						try{
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							continue;
						}
					}
					readFile(R.raw.ecg2_value);
				}
			}
		});
		try {
			t.start();
	    } catch (ConcurrentModificationException e) {
	    	System.out.println("CONCURRENT EXCEPTIONNAY!!\n" + e);
	    }
	}
	
	private void readFile(int resourceId) {
	    // The InputStream opens the resourceId and sends it to the buffer
	    InputStream is = this.getResources().openRawResource(resourceId);
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String readLine = null;

	    try {
	        // While the BufferedReader readLine is not null 
	        while ((readLine = br.readLine()) != null) {
	        	queue.offer(Double.valueOf(readLine));
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
