package org.bandm.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.bandm.android.R;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SoundActivity extends Activity {

	/** at least 2 times higher than the sound frequency */
	public static final int RECORDER_SAMPLE_RATE = 32000; 
	public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private AudioRecord recorder = null;
	private int minBufferSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;

	private Button startRecBtn;
	private Button stopRecBtn;

	private TextView valAllTimeHigh;
	private TextView valRevolvingSpeed;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		valAllTimeHigh = (TextView) findViewById(R.id.valAllTimeHigh);
		valRevolvingSpeed = (TextView) findViewById(R.id.valRevolvingSpeed);

		startRecBtn = (Button) findViewById(R.id.start);
		stopRecBtn = (Button) findViewById(R.id.stop);

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
			Log.e(this.getLocalClassName(), msg);
			break;
		case AudioRecord.ERROR:
			msg = "The implementation was unable to query the hardware for its output properties, or the minimum " +
				"buffer size expressed in bytes";
			Log.e(this.getLocalClassName(), msg);
			break;
		default:
			Log.d(this.getLocalClassName(), "minBufferSize: " + minBufferSize);
			break;
		}
	}

	public void startRecording(View view) {
		Log.d(this.getLocalClassName(), "Start Recording");

		startRecBtn.setEnabled(false);
		stopRecBtn.setEnabled(true);
		stopRecBtn.requestFocus();

		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, minBufferSize);

		recorder.startRecording();
		isRecording = true;
		recordingThread = new Thread(new Runnable() {

			//@Override
			public void run() {
				writeAudioDataToTempFile();
			}
		}, "AudioRecorder Thread");

		recordingThread.start();
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
		return (file.getAbsolutePath() + "/" + "signal.raw");
	}

	private void writeAudioDataToTempFile() {
		byte data[] = new byte[minBufferSize];
		String filename = getTempFilename();
		FileOutputStream outStream = null;

		try {
			outStream = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String msg;
		int bytesRead = 0;
		if (outStream != null) {
			while (isRecording) {
				bytesRead = recorder.read(data, 0, minBufferSize);
				if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
					msg = "The parameters don't resolve to valid data and indexes";
					Log.e(this.getLocalClassName(), msg);
					break;
				}
				else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
					msg = "The parameters don't resolve to valid data and indexes";
					Log.e(this.getLocalClassName(), msg);
					break;
				}
				msg = "Bytes read: "+bytesRead;
				Log.e(this.getLocalClassName(), msg);
				try {
					outStream.write(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				outStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void deleteTempFile() {
		File file = new File(getTempFilename());
		file.delete();
	}

	public void stopRecording(View view) {
		Log.d(this.getLocalClassName(), "Stop recording");

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

	public void clearHandler(View view) {
		valAllTimeHigh.setText("0");
		int randomNum = (int) Math.ceil(Math.random() * 100);
		valRevolvingSpeed.setText(""+randomNum);
	}

}