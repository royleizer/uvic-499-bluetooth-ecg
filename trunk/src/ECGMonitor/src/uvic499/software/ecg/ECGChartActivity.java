package uvic499.software.ecg;

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
import android.os.Handler;
import android.os.Message;
import android.view.Menu;

public class ECGChartActivity extends Activity {

	 private static TimeSeries timeSeries;
	 private static XYMultipleSeriesDataset dataset;
	 private static XYMultipleSeriesRenderer renderer;
	 private static XYSeriesRenderer rendererSeries;
	 private static GraphicalView view;
	 private double samplingRate = 0.4;
	 private int pointsToDisplay = 15;
	 private int xScrollAhead = 3;
	 private double currentX = 0;
	 private final int chartDelay = 70; //  millisecond delay for count
	 private Thread chartingThread;
	 private boolean continueCharting = true;
	 
	 public LinkedBlockingQueue<Double> queue = BluetoothConnService.bluetoothQueue;
	 
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	     super.onCreate(savedInstanceState);     
	     setContentView(R.layout.activity_chart);
	     
	     if (savedInstanceState != null) {
	    	 currentX = savedInstanceState.getDouble("currentX");
	     }
	     
	     setChartLook();
	     dataset = new XYMultipleSeriesDataset();
	     
	     final Handler chartDrawCallback = new Handler() {
	  	    public void handleMessage(Message msg) {
	  	    	double yVal = ((double)msg.arg1)/1000;
	  	    	timeSeries.add(currentX, yVal);
	             	if (currentX - pointsToDisplay >= 0 ) {
	             		renderer.setXAxisMin(currentX - pointsToDisplay);
	         		}
	             	renderer.setXAxisMax(pointsToDisplay + currentX + xScrollAhead);
	         		view.repaint();
	  	    }
	     };
	     
	     timeSeries = new TimeSeries(renderer.getChartTitle());
	     dataset.addSeries(timeSeries);
	     view = ChartFactory.getLineChartView(this, dataset, renderer);
	     view.refreshDrawableState();
	     
	     setContentView(view);
	     
	     chartingThread = new Thread(new Runnable() {
	     	 public void run() {
             	 while(continueCharting) {
     		         try {
     		        	 Thread.sleep(chartDelay);
     	             } catch (InterruptedException e) {
     	                 e.printStackTrace();
     	                 continue;
     	             }
 	               	 currentX = currentX+samplingRate;
                 	 Double yVal = queue.poll();
 	               	 // skip if no data on queue
 	               	 if (yVal == null) {
 	               		 continue;
 	               	 }
 	               	 // send Message to UI handler for 
 	               	 Message msg = Message.obtain();
 	               	 msg.arg1 = (int)Math.round(yVal*1000);
 	               	 chartDrawCallback.sendMessage(msg);
           	     }
	         }
	     });
	     chartingThread.start();
	 }

	 @Override
	 protected void onSaveInstanceState(Bundle b) {
		super.onSaveInstanceState(b);
		b.putDouble("currentX", currentX);
		// now stop the charting thread
		continueCharting = false;
		try {
			chartingThread.join();
		} catch (InterruptedException e) {
			// Not sure if this kills thread
			chartingThread.interrupt();
		}
	 }  
	 
	 @Override
	 protected void onStart() {
	     super.onStart();
	 }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void setChartLook() {
	     renderer = new XYMultipleSeriesRenderer();
	     
	     renderer.setApplyBackgroundColor(true);
	     renderer.setBackgroundColor(Color.BLACK);//argb(100, 50, 50, 50));
	     renderer.setLabelsTextSize(35);
	     renderer.setLegendTextSize(35);
	     renderer.setAxesColor(Color.GRAY);
	     renderer.setAxisTitleTextSize(35);
	     renderer.setChartTitle("ECG Heartbeat");
	     renderer.setChartTitleTextSize(35);
	     renderer.setFitLegend(false);
	     renderer.setGridColor(Color.BLACK);
	     renderer.setPanEnabled(false, false); // TODO
	     renderer.setPointSize(1);
	     renderer.setXTitle("Time");
	     renderer.setYTitle("Num");
	     renderer.setMargins(new int []{5, 50, 50, 5}); // TODO: i doubled
	     renderer.setZoomButtonsVisible(false);
	     renderer.setZoomEnabled(false);
	     renderer.setBarSpacing(10);
	     renderer.setShowGrid(false);
	     renderer.setYAxisMax(2.4);
	     renderer.setYAxisMin(0.4);
	     
	     rendererSeries = new XYSeriesRenderer();
	     rendererSeries.setColor(Color.RED);
	     rendererSeries.setLineWidth(5f);
	     renderer.addSeriesRenderer(rendererSeries);   
	}
	
}
