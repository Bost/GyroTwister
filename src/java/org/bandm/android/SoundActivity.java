package org.bandm.android;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;

public class SoundActivity extends Activity {

	private final String outFileName = "soundSample.wav";
	private ExtAudioRecorder recorder = null;

	private final String verbose = "V";
	private final String debug = "D";
	private final String info = "I";
	private final String warn = "W";
	private final String error = "E";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// here on this place the logAction(..) cannot be used - the app the not initialized yet
		Log.v(SoundActivity.class.getSimpleName(), "onCreate(..) {");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final TextView valAllTimeHigh = (TextView) findViewById(R.id.valAllTimeHigh);
		final TextView valRevolvingSpeed = (TextView) findViewById(R.id.valRevolvingSpeed);

		final Button startRecBtn = (Button) findViewById(R.id.start);
		final Button stopRecBtn = (Button) findViewById(R.id.stop);
		final Button clearBtn = (Button) findViewById(R.id.clear);

		// No files can be saved to the external storage if an USB cable is connected
		boolean isExtStorageRW = !isUSBCableConnected();
		String logLevel = isExtStorageRW ? debug : warn;
		String msg = "External storage writable: " + isExtStorageRW;
		logAction(logLevel, msg);

		startRecBtn.setEnabled(isExtStorageRW);
		stopRecBtn.setEnabled(false);

		OnClickListener startListener = new OnClickListener() {
			public void onClick(View v) {
				startRecord(startRecBtn, stopRecBtn);
			}
		};
		startRecBtn.setOnClickListener(startListener);

		OnClickListener stopListener = new OnClickListener() {
			public void onClick(View v) {
				stopRecord(startRecBtn, stopRecBtn);
			}
		};
		stopRecBtn.setOnClickListener(stopListener);

		OnClickListener clearListener = new OnClickListener() {
			public void onClick(View v) {
				clearRecord(valAllTimeHigh, valRevolvingSpeed);
			}
		};
		clearBtn.setOnClickListener(clearListener);

		if (isExtStorageRW) {
			// display log messages instead of the graph
		}
		else {
			TextView logEntry;
			logEntry = (TextView) findViewById(R.id.warnUSBConnected0);
			logEntry.setText("WARN:");
			logEntry = (TextView) findViewById(R.id.warnUSBConnected1);
			logEntry.setText("USB connected!");

			// init example series data
			int i = 0;
			GraphViewData[] arrGraphViewData = new GraphViewData[] {
					new GraphViewData(i++, 0.4d)
					, new GraphViewData(i++, 0.8d)
					, new GraphViewData(i++, 1.1d)
					, new GraphViewData(i++, 1.4d)
					, new GraphViewData(i++, 1.6d)
					, new GraphViewData(i++, 1.8d)
					, new GraphViewData(i++, 1.9d)
					, new GraphViewData(i++, 2.0d)
					, new GraphViewData(i++, 2.05d)
					, new GraphViewData(i++, 2.1d)
					, new GraphViewData(i++, 2.12d)
					, new GraphViewData(i++, 2.13d)};

			GraphViewSeries exampleSeries = new GraphViewSeries(arrGraphViewData);
			GraphView graphView = new BarGraphView(this , "");	// context: this, heading: ""

			graphView.addSeries(exampleSeries); // data

			LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
			layout.addView(graphView);
		}
		Log.v(SoundActivity.class.getSimpleName(), "onCreate(..) }");
	}

	public void clearRecord(TextView valAllTimeHigh, TextView valRevolvingSpeed) {
		logAction(verbose, "clearHandler(..) {");

		valAllTimeHigh.setText("0");
		Double d = new Double(Math.ceil(Math.random() * 100));
		int randNum = d.intValue();
		valRevolvingSpeed.setText(randNum + "");
		logAction(info, "Record cleared.");

		logAction(verbose, "clearHandler(..) }");
	}

	private void stopRecord(final Button startRecBtn, final Button stopRecBtn) {
		logAction(verbose, "stopRecord(..) {");

		startRecBtn.setEnabled(true);
		stopRecBtn.setEnabled(false);
		startRecBtn.requestFocus();

		try {
			recorder.stop();
		} catch (IllegalStateException e) {
			logAction(error, e.getMessage());
			e.printStackTrace();
			return;
		}
		recorder.reset();
		recorder.release();
		recorder = null;
		logAction(info, "Recording stopped.");

		logAction(verbose, "stopRecord(..) }");
	}

	private void startRecord(final Button startRecBtn, final Button stopRecBtn) {
		logAction(verbose, "startRecord(..) {");

		startRecBtn.setEnabled(false);
		stopRecBtn.setEnabled(true);
		stopRecBtn.requestFocus();

		//extAudioRecorder = ExtAudioRecorder.getInstanse(true);	  // Compressed recording (AMR)
		recorder = ExtAudioRecorder.getInstanse(false); // Uncompressed recording (WAV)

		File filesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

		File file = new File(filesDir, outFileName);
		String filePath = file.getAbsolutePath();

		boolean fileExists = file.exists();
		if (fileExists) {
			logAction(verbose, "Deleting existing file: " + filePath);
			file.delete();
			logAction(verbose, "Existing file deleted: " + filePath);
		}

		logAction(verbose, "Creating new file: "+filePath);
		try {
			file.createNewFile();
		} catch (IOException e) {
			logAction(error, "Error creating new file. " + e.getClass().getSimpleName() +": "+ e.getMessage());
			e.printStackTrace();
			return;
		}
		String msg;
		if (fileExists) {
			msg = "New file created: "+filePath;
		}
		else {
			msg = "Existing file overwritten: "+filePath;
		}
		logAction(info, msg);

		recorder.setOutputFile(filePath);


		try {
			recorder.prepare();
		} catch (IllegalStateException e) {
			logAction(error, "Error preparing recorder. " + e.getClass().getSimpleName() +": "+ e.getMessage());
			e.printStackTrace();
			return;
		}
		logAction(verbose, "recorder prepared");

		try {
			recorder.start();
		} catch (IllegalStateException e) {
			logAction(error, "Error starting recorder. " + e.getClass().getSimpleName() +": "+ e.getMessage());
			e.printStackTrace();
			return;
		}
		logAction(info, "Recording started. Out-file: " + filePath);

		logAction(verbose, "startRecord(..) }");
	}

	/** The app works only if no USB cable is connected otherwise the external storage does not work.
	 * In such a case no reasonable logging mechanism is available so print the log messages directly
	 * on the screen. */
	private void logAction(String level, String msg)  {
		boolean isUSBNotConnected = !isUSBCableConnected();
		if (isUSBNotConnected) {
			if (!verbose.equals(level)) {
				int[] arrLogId = {	R.id.log0, R.id.log1, R.id.log2, R.id.log3, R.id.log4, R.id.log5,
						R.id.log6, R.id.log7 };
				for (int i = 1; i < arrLogId.length; i++) {
					int oldId = arrLogId[i - 1];
					TextView oldLogEntry = (TextView) findViewById(oldId);

					int newId = arrLogId[i];
					TextView newLogEntry = (TextView) findViewById(newId);

					String newMsg = newLogEntry.getText().toString();
					oldLogEntry.setText(newMsg);
				}
				TextView logEntry = (TextView) findViewById(R.id.log7);
				logEntry.setText(level+":"+msg);
			}
		}

		String tag = SoundActivity.class.getSimpleName();
		if (verbose.equals(level)) {
			Log.v(tag, msg);
		}
		else if (debug.equals(level)) {
			Log.d(tag, msg);
		}
		else if (info.equals(level)) {
			Log.i(tag, msg);
		}
		else if (warn.equals(level)) {
			Log.w(tag, msg);
		}
		else if (error.equals(level)) {
			Log.e(tag, msg);
		}
		else {
			Log.e(tag, "Unknown log level: '"+level+"'");
		}
	}

	/** No extermal storage is available and writable when a USB cabel is connected */
	public boolean isUSBCableConnected() {
		boolean extStorAvailable = false;
		boolean extStorWriteable = false;
		String extStorState = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(extStorState)) {
			// We can read and write the media
			extStorAvailable = extStorWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorState)) {
			// We can only read the media
			extStorAvailable = true;
			extStorWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			extStorAvailable = extStorWriteable = false;
		}
		boolean isUSBCableConnected = !(extStorAvailable && extStorWriteable);
		return isUSBCableConnected;
	}
}