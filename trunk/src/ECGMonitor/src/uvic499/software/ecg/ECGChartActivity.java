package uvic499.software.ecg;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;

public class ECGChartActivity extends Activity {

	 private XYSeries xySeries;
	 private XYMultipleSeriesDataset dataset;
	 private XYMultipleSeriesRenderer renderer;
	 private XYSeriesRenderer rendererSeries;
	 private GraphicalView view;
	 private double samplingRate = 0.6;
	 private int pointsToDisplay = 75;
	 
	 private int yMax = 600;
	 private int yMin = 500;
	 private int xScrollAhead = 35;
	 private double currentX = 0;
	 private final int chartDelay = 10; //  millisecond delay for count
	 private ChartingThread chartingThread;
	 
	 // TODO: package visibility so that queue service can see when it is ready for data
	 static boolean isActive = false;
	 
	 public LinkedBlockingQueue<Double> queue = BluetoothConnService.bluetoothQueueForUI;
	 
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	     super.onCreate(savedInstanceState);     
	     setContentView(R.layout.activity_chart);
	     
	     setChartLook();
	     dataset = new XYMultipleSeriesDataset();
	     xySeries = new XYSeries(renderer.getChartTitle());
	     dataset.addSeries(xySeries);
	     view = ChartFactory.getLineChartView(this, dataset, renderer);
	     view.refreshDrawableState();
	     currentX = 0; // reset the horizontal of the graphing
	     
	     setContentView(view);
	  
	     if (savedInstanceState != null) {
	    	 currentX = savedInstanceState.getDouble("currentX");
	     
	     }

    	 // To deal with onCreate coming from orientation change, only create chartingThread first time
	     if (chartingThread == null) {
		     ChartHandler chartUIHandler = new ChartHandler();
	    	 chartingThread = new ChartingThread(chartUIHandler);
	    	 chartingThread.start();
	     }
	     isActive = true;
	 }
	 
	 @Override
	 protected void onSaveInstanceState(Bundle b) {
		super.onSaveInstanceState(b);
		b.putDouble("currentX", currentX);
		// now stop the charting thread
		chartingThread.cancel();
		try {
			chartingThread.join();
		} catch (InterruptedException e) {
			// Not sure if this kills thread
			chartingThread.interrupt();
		}
	 }  
	 
	 @Override
	 protected void onDestroy() {
		 super.onDestroy();
		 if (this.isFinishing()) { // real stoppage
			 isActive = false;
			 chartingThread.cancel();
		 } else { // orientation change
			 // Keep threads alive on orientation change
		 }

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
	     renderer.setXTitle("X");
	     renderer.setYTitle("Y");
	     renderer.setMargins(new int []{20, 20, 50, 20}); // TODO: i doubled
	     renderer.setZoomButtonsVisible(false);
	     renderer.setZoomEnabled(false);
	     renderer.setBarSpacing(10);
	     renderer.setShowGrid(false);
	     renderer.setYAxisMax(yMax);
	     renderer.setYAxisMin(yMin);
	     //TODO: Don't show labels or legend
	     renderer.setShowLabels(false);;
	     renderer.setShowLegend(false);
	     
	     rendererSeries = new XYSeriesRenderer();
	     rendererSeries.setColor(Color.GREEN);
	     rendererSeries.setLineWidth(10f);
	     renderer.addSeriesRenderer(rendererSeries);   
	}
	
	// 
    class ChartHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg) {
  	    	double yVal = ((double)msg.arg1)/1000;
  	    	xySeries.add(currentX, yVal);
             	if (currentX - pointsToDisplay >= 0 ) {
             		renderer.setXAxisMin(currentX - pointsToDisplay);
         		}
             	renderer.setXAxisMax(currentX + xScrollAhead);
         		view.repaint();
  	    }
    }
	
	class ChartingThread extends Thread {
		private boolean continueCharting = true;
		private Handler handler;
		public ChartingThread(Handler handler) {
			this.handler = handler;
		}
		
		@Override
    	public void run() {
         	 while(continueCharting) {
         		Double yVal = null;
         		 try {
 		        	 Thread.sleep(chartDelay);
 		        	 yVal = queue.poll(2, TimeUnit.SECONDS);
         		 } catch (InterruptedException e) {
 	                 e.printStackTrace();
 	                 continue;
 	             }
               	 // skip if no data on queue
               	 if (yVal == null) {
               		 continue;
               	 }
               	 currentX = currentX+samplingRate;
            	 if (yVal > yMax) {
            		 yMax = yVal.intValue();
            		 renderer.setYAxisMax(yVal);
            	 } else if (yVal < yMin) {
            		 yMin = yVal.intValue();
            		 renderer.setYAxisMin(yVal);
            	 }
            	 
               	 // send Message to UI handler for charting.
               	 Message msg = Message.obtain();
               	 // Send as an integer. Handler converts it back to an integer
               	 msg.arg1 = (int)Math.round(yVal*1000);
               	 handler.sendMessage(msg);
           	 }
     	 }
		
		// Stops the thread
		public void cancel() {
			continueCharting = false;
		}
	}
	
}
