package org.bandm.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
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

	/** at least 2 times higher than the sound frequency */
	public static final int RECORDER_SAMPLE_RATE = 32000;
	public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private AudioRecord recorder = null;
	private int minBufferSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final TextView valAllTimeHigh = (TextView) findViewById(R.id.valAllTimeHigh);
		final TextView valRevolvingSpeed = (TextView) findViewById(R.id.valRevolvingSpeed);

		final Button startRecBtn = (Button) findViewById(R.id.start);
		final Button stopRecBtn = (Button) findViewById(R.id.stop);
		final Button clearBtn = (Button) findViewById(R.id.clear);

		startRecBtn.setEnabled(true);
		stopRecBtn.setEnabled(false);

		minBufferSize = AudioRecord.getMinBufferSize(
				RECORDER_SAMPLE_RATE,
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING);

		String msg;
		switch (minBufferSize) {
		case AudioRecord.ERROR_BAD_VALUE:
			msg = "The recording parameters are not supported by the hardware, or an invalid parameter was passed";
			Log.e(SoundActivity.class.getSimpleName(), msg);
			break;
		case AudioRecord.ERROR:
			msg = "The implementation was unable to query the hardware for its output properties, or the minimum " +
				"buffer size expressed in bytes";
			Log.e(SoundActivity.class.getSimpleName(), msg);
			break;
		default:
			Log.d(SoundActivity.class.getSimpleName(), "minBufferSize: " + minBufferSize);
			break;
		}

		OnClickListener startListener = new OnClickListener() {
			public void onClick(View v) {
				startRecording(startRecBtn, stopRecBtn);
			}
		};
		startRecBtn.setOnClickListener(startListener);

		OnClickListener stopListener = new OnClickListener() {
			public void onClick(View v) {
				stopRecording(startRecBtn, stopRecBtn);
			}
		};
		stopRecBtn.setOnClickListener(stopListener);

		OnClickListener clearListener = new OnClickListener() {
			public void onClick(View v) {
				clearHandler(valAllTimeHigh, valRevolvingSpeed);
			}
		};
		clearBtn.setOnClickListener(clearListener);

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

		// init example series data
		GraphViewSeries exampleSeries = new GraphViewSeries(arrGraphViewData);

		GraphView graphView = new BarGraphView(
				this // context
				, "" // heading
				);
		graphView.addSeries(exampleSeries); // data

		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(graphView);
	}

	private String getTempFilename() {
		File workDir = getFilesDir();
		File file = new File(workDir, "tempaudio");
		if (!file.exists()) {
			file.mkdirs();
		}
		File tempFile = new File(workDir, "signal.raw");
		if (tempFile.exists()) {
			tempFile.delete();
		}
		String tempFilename = file.getAbsolutePath() + "/" + "signal.raw";
		return tempFilename;
	}

	private void writeAudioDataToTempFile() {
		Log.d(SoundActivity.class.getSimpleName(), "writeAudioDataToTempFile(..)");

		byte data[] = new byte[minBufferSize];
		String filename = getTempFilename();
		FileOutputStream outStream = null;

		try {
			outStream = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			Log.d(SoundActivity.class.getSimpleName(), e.getMessage());
			e.printStackTrace();
		}

		String msg;
		int bytesRead = 0;
		if (outStream != null) {
			while (isRecording) {
				bytesRead = recorder.read(data, 0, minBufferSize);
				if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
					msg = "The parameters don't resolve to valid data and indexes";
					Log.e(SoundActivity.class.getSimpleName(), msg);
					break;
				}
				else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
					msg = "The parameters don't resolve to valid data and indexes";
					Log.e(SoundActivity.class.getSimpleName(), msg);
					break;
				}
				msg = "Bytes read: "+bytesRead;
				Log.e(SoundActivity.class.getSimpleName(), msg);
				try {
					outStream.write(data);
				} catch (IOException e) {
					Log.d(SoundActivity.class.getSimpleName(), e.getMessage());
					e.printStackTrace();
				}
			}

			try {
				outStream.close();
			} catch (IOException e) {
				Log.d(SoundActivity.class.getSimpleName(), e.getMessage());
				e.printStackTrace();
			}
		}
	}

//	private void deleteTempFile() {
//		File file = new File(getTempFilename());
//		file.delete();
//	}

	public void clearHandler(TextView valAllTimeHigh, TextView valRevolvingSpeed) {
		Log.d(SoundActivity.class.getSimpleName(), "clearHandler(..)");
		valAllTimeHigh.setText("0");
		Double d = new Double(Math.ceil(Math.random() * 100));
		int randNum = d.intValue();
		valRevolvingSpeed.setText(randNum + "");
	}

	private void stopRecording(final Button startRecBtn, final Button stopRecBtn) {
		Log.d(SoundActivity.class.getSimpleName(), "stopRecording(..)");

		startRecBtn.setEnabled(true);
		stopRecBtn.setEnabled(false);
		startRecBtn.requestFocus();

		if (recorder != null) {
			isRecording = false;

			recorder.stop();
			recorder.release();

			recorder = null;
			recordingThread = null;
		}

		String tempFileName = getTempFilename();
		File file = new File(tempFileName);
		MorseDecoder decoder = new MorseDecoder();
		decoder.execute(file);
	}

	private void startRecording(final Button startRecBtn, final Button stopRecBtn) {
		Log.d(SoundActivity.class.getSimpleName(), "startRecording(..)");

		startRecBtn.setEnabled(false);
		stopRecBtn.setEnabled(true);
		stopRecBtn.requestFocus();

		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, minBufferSize);

		recorder.startRecording();
		isRecording = true;

		Runnable runnable = new Runnable() {
			public void run() {
				writeAudioDataToTempFile();
			}
		};
		recordingThread = new Thread(runnable, "AudioRecorder Thread");

		recordingThread.start();
	}
}