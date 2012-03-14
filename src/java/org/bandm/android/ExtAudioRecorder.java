package org.bandm.android;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class ExtAudioRecorder
{
	private final static int[] sampleRates = {44100, 22050, 11025, 8000};

	public static ExtAudioRecorder getInstanse(Boolean recordingCompressed)
	{
		ExtAudioRecorder result = null;

		if (recordingCompressed) {
			result = new ExtAudioRecorder(	false,
											AudioSource.MIC,
											sampleRates[3],
											AudioFormat.CHANNEL_CONFIGURATION_MONO,
											AudioFormat.ENCODING_PCM_16BIT);
		}
		else {
			int i = 0;
			do {
				result = new ExtAudioRecorder(	true,
												AudioSource.MIC,
												sampleRates[i],
												AudioFormat.CHANNEL_CONFIGURATION_MONO,
												AudioFormat.ENCODING_PCM_16BIT);

			} while ((++i<sampleRates.length) &	// bitwise 'and', not logical
					!(result.getRecorderState() == ExtAudioRecorder.AllRecorderStates.INITIALIZING));
		}
		return result;
	}

	/** AudioRecorder states:
	* INITIALIZING : recorder is initializing;
	* READY : recorder has been initialized, recorder not yet started
	* RECORDING : recording
	* ERROR : reconstruction needed
	* STOPPED: reset needed */
	public enum AllRecorderStates {INITIALIZING, READY, RECORDING, ERROR, STOPPED};

	public static final boolean RECORDING_UNCOMPRESSED = true;
	public static final boolean RECORDING_COMPRESSED = false;

	/** The interval in which the recorded samples are output to the file. Used only in
	 * uncompressed mode */
	private static final int TIMER_INTERVAL = 120;

	/** Toggles uncompressed recording on/off; RECORDING_UNCOMPRESSED / RECORDING_COMPRESSED */
	private boolean         recordingUncompressed;

	/** Recorder used for uncompressed recording */
	private AudioRecord     audioRecorder = null;

	/** Recorder used for compressed recording */
	private MediaRecorder   mediaRecorder = null;

	/** Stores current amplitude (only in uncompressed mode) */
	private int             currentAmplitude= 0;

	/** Output file path */
	private String          filePath = null;

	/** Current recorder state; see AllRecorderStates */
	private AllRecorderStates recorderState;

	/** File writer (only in uncompressed mode) */
	private RandomAccessFile randomAccessWriter;

	private short  numberChannels;
	private int    sampleRate;
	private short  sampleSize_bits;
	private int    bufferSize;
	private int    audioSource;
	private int    audioFormat_sampleSize;

	/** Number of frames written to file on each output(only in uncompressed mode) */
	private int    framePeriod;

	/** Buffer for output(only in uncompressed mode) */
	private byte[] outBuffer;

	/** Number of bytes written to file after header(only in uncompressed mode) after stop() is
	 * called, this size is written to the header/data chunk in the wave file */
	private int    payloadSize;

	/** Returns the state of the recorder in a RehearsalAudioRecord.State typed object.
	* Useful, as no exceptions are thrown */
	public AllRecorderStates getRecorderState() { return recorderState; }

	/** Method used for recording */
	private AudioRecord.OnRecordPositionUpdateListener updateListener =
			new AudioRecord.OnRecordPositionUpdateListener()
	{

		public void onPeriodicNotification(AudioRecord recorder)
		{
			audioRecorder.read(outBuffer, 0, outBuffer.length); // Fill buffer
			try {
				randomAccessWriter.write(outBuffer); // Write buffer to file
				payloadSize += outBuffer.length;

				if (sampleSize_bits == 16) {
					for (int i=0; i<outBuffer.length/2; i++) {
						short curSample = getShort(outBuffer[i*2], outBuffer[i*2+1]);
						if (curSample > currentAmplitude) { // Check amplitude
							currentAmplitude = curSample;
						}
					}
				}
				else { // 8bit sample size
					for (int i=0; i<outBuffer.length; i++)
					{
						if (outBuffer[i] > currentAmplitude)
						{ // Check amplitude
							currentAmplitude = outBuffer[i];
						}
					}
				}
			}
			catch (IOException e) {
				Log.e(ExtAudioRecorder.class.getName(),
						"Error occured in updateListener, recording is aborted");
				//stop();
			}
		}

		public void onMarkerReached(AudioRecord recorder)
		{
			// NOT USED
		}
	};

	/** Default constructor. Instantiates a new recorder, in case of compressed recording the
	 * parameters can be left as 0. In case of errors, no exception is thrown, but the state is
	 * set to ERROR */
	public ExtAudioRecorder(boolean uncompressed, int audioSource,
			int sampleRate, int channelConfig, int audioFormat)
	{
		try {
			recordingUncompressed = uncompressed;
			if (recordingUncompressed) { // RECORDING_UNCOMPRESSED
				if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
					sampleSize_bits = 16;
				}
				else {
					sampleSize_bits = 8;
				}

				if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO) {
					numberChannels = 1;
				}
				else {
					numberChannels = 2;
				}

				this.audioSource = audioSource;
				this.sampleRate  = sampleRate;
				audioFormat_sampleSize = audioFormat;

				framePeriod = sampleRate * TIMER_INTERVAL / 1000;
				bufferSize = framePeriod * 2 * sampleSize_bits * numberChannels / 8;

				if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {
				    // Check to make sure buffer size is not smaller than the smallest allowed one
					bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
					// Set frame period and timer interval accordingly
					framePeriod = bufferSize / ( 2 * sampleSize_bits * numberChannels / 8 );
					Log.w(ExtAudioRecorder.class.getName(),
							"Increasing buffer size to " + Integer.toString(bufferSize));
				}

				audioRecorder = new AudioRecord(audioSource, sampleRate,
						channelConfig, audioFormat, bufferSize);

				if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
					throw new Exception("AudioRecord initialization failed");
				}
				audioRecorder.setRecordPositionUpdateListener(updateListener);
				audioRecorder.setPositionNotificationPeriod(framePeriod);
			}
			else { // RECORDING_COMPRESSED
				mediaRecorder = new MediaRecorder();
				mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			}
			currentAmplitude = 0;
			filePath = null;
			recorderState = AllRecorderStates.INITIALIZING;
		}
		catch (Exception e) {
			if (e.getMessage() != null) {
				Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
			}
			else {
				Log.e(ExtAudioRecorder.class.getName(),
						"Unknown error occured while initializing recording");
			}
			recorderState = AllRecorderStates.ERROR;
		}
	}

	/** Sets output file path, call directly after construction/reset */
	public void setOutputFile(String fullFilePath)
	{
		try {
			if (recorderState == AllRecorderStates.INITIALIZING) {
				filePath = fullFilePath;
				if (!recordingUncompressed) {
					mediaRecorder.setOutputFile(filePath);
				}
			}
		}
		catch (Exception e) {
			if (e.getMessage() != null) {
				Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
			}
			else {
				Log.e(ExtAudioRecorder.class.getName(),
						"Unknown error occured while setting output path");
			}
			recorderState = AllRecorderStates.ERROR;
		}
	}

	/** Returns the largest amplitude sampled since the last call to this method, or 0 when not
	 * in recording state */
	public int getMaxAmplitude()
	{
		if (recorderState == AllRecorderStates.RECORDING) {
			if (recordingUncompressed) {
				int result = currentAmplitude;
				currentAmplitude = 0;
				return result;
			}
			else {
				try {
					return mediaRecorder.getMaxAmplitude();
				}
				catch (IllegalStateException e) {
					return 0;
				}
			}
		}
		else {
			return 0;
		}
	}

	/** Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state
	 * and the file path was not set the recorder is set to the ERROR state, which makes a
	 * reconstruction necessary. In case uncompressed recording is toggled, the header of the wave
	 * file is written. In case of an exception, the state is changed to ERROR */
	public void prepare()
	{
		try {
			if (recorderState == AllRecorderStates.INITIALIZING) {
				if (recordingUncompressed) {
					if ((audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (filePath != null)) {
						// write file header

						randomAccessWriter = new RandomAccessFile(filePath, "rw");

						// Set file length to 0, to prevent unexpected behavior in case the file already existed
						randomAccessWriter.setLength(0);
						randomAccessWriter.writeBytes("RIFF");
						randomAccessWriter.writeInt(0); // Final file size not known yet, write 0
						randomAccessWriter.writeBytes("WAVE");
						randomAccessWriter.writeBytes("fmt ");
						randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
						randomAccessWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
						// Number of channels, 1 for mono, 2 for stereo
						randomAccessWriter.writeShort(Short.reverseBytes(numberChannels));
						randomAccessWriter.writeInt(Integer.reverseBytes(sampleRate)); // Sample rate
						// Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
						randomAccessWriter.writeInt(Integer.reverseBytes(sampleRate*sampleSize_bits*numberChannels/8));
						// Block align, NumberOfChannels*BitsPerSample/8
						randomAccessWriter.writeShort(Short.reverseBytes((short)(numberChannels*sampleSize_bits/8)));
						randomAccessWriter.writeShort(Short.reverseBytes(sampleSize_bits)); // Bits per sample
						randomAccessWriter.writeBytes("data");
						randomAccessWriter.writeInt(0); // Data chunk size not known yet, write 0

						outBuffer = new byte[framePeriod*sampleSize_bits/8*numberChannels];
						recorderState = AllRecorderStates.READY;
					}
					else {
						Log.e(ExtAudioRecorder.class.getName(),
								"prepare() method called on uninitialized recorder");
						recorderState = AllRecorderStates.ERROR;
					}
				}
				else {
					mediaRecorder.prepare();
					recorderState = AllRecorderStates.READY;
				}
			}
			else {
				Log.e(ExtAudioRecorder.class.getName(), "prepare() method called on illegal state");
				release();
				recorderState = AllRecorderStates.ERROR;
			}
		}
		catch(Exception e) {
			if (e.getMessage() != null) {
				Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
			}
			else {
				Log.e(ExtAudioRecorder.class.getName(), "Unknown error occured in prepare()");
			}
			recorderState = AllRecorderStates.ERROR;
		}
	}

	/** Releases the resources associated with this class, and removes the unnecessary files,
	 * when necessary */
	public void release()
	{
		if (recorderState == AllRecorderStates.RECORDING) {
			stop();
		}
		else {
			if ((recorderState == AllRecorderStates.READY) & (recordingUncompressed)) {
				try {
					randomAccessWriter.close(); // Remove prepared file
				}
				catch (IOException e) {
					Log.e(ExtAudioRecorder.class.getName(),
							"I/O exception occured while closing output file");
				}
				File file = new File(filePath);
				file.delete();
			}
		}

		if (recordingUncompressed) {
			if (audioRecorder != null) {
				audioRecorder.release();
			}
		}
		else {
			if (mediaRecorder != null) {
				mediaRecorder.release();
			}
		}
	}

	/** Resets the recorder to the INITIALIZING state, as if it was just created. In case the
	 * class was in RECORDING state, the recording is stopped. In case of exceptions the class
	 * is set to the ERROR state */
	public void reset()
	{
		try {
			if (recorderState != AllRecorderStates.ERROR) {
				release();
				filePath = null; // Reset file path
				currentAmplitude = 0; // Reset amplitude
				if (recordingUncompressed) {
					audioRecorder = new AudioRecord(audioSource, sampleRate,
							numberChannels+1, audioFormat_sampleSize, bufferSize);
				}
				else {
					mediaRecorder = new MediaRecorder();
					mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				}
				recorderState = AllRecorderStates.INITIALIZING;
			}
		}
		catch (Exception e) {
			Log.e(ExtAudioRecorder.class.getName(), e.getMessage());
			recorderState = AllRecorderStates.ERROR;
		}
	}

	/** Starts the recording, and sets the state to RECORDING. Call after prepare() */
	public void start() {
		if (recorderState == AllRecorderStates.READY) {
			if (recordingUncompressed) {
				payloadSize = 0;
				audioRecorder.startRecording();
				audioRecorder.read(outBuffer, 0, outBuffer.length);
			}
			else {
				mediaRecorder.start();
			}
			recorderState = AllRecorderStates.RECORDING;
		}
		else {
			Log.e(ExtAudioRecorder.class.getName(), "start() called on illegal state");
			recorderState = AllRecorderStates.ERROR;
		}
	}

	/** Stops the recording, and sets the state to STOPPED. In case of further usage, a reset is
	 * needed. Also finalizes the wave file in case of uncompressed recording */
	public void stop()
	{
		if (recorderState == AllRecorderStates.RECORDING) {
			if (recordingUncompressed) {
				audioRecorder.stop();

				try {
					randomAccessWriter.seek(4); // Write size to RIFF header
					randomAccessWriter.writeInt(Integer.reverseBytes(36+payloadSize));

					randomAccessWriter.seek(40); // Write size to Subchunk2Size field
					randomAccessWriter.writeInt(Integer.reverseBytes(payloadSize));

					randomAccessWriter.close();
				}
				catch(IOException e) {
					Log.e(ExtAudioRecorder.class.getName(),
							"I/O exception occured while closing output file");
					recorderState = AllRecorderStates.ERROR;
				}
			}
			else {
				mediaRecorder.stop();
			}
			recorderState = AllRecorderStates.STOPPED;
		}
		else {
			Log.e(ExtAudioRecorder.class.getName(), "stop() called on illegal state");
			recorderState = AllRecorderStates.ERROR;
		}
	}

	/** Converts a byte[2] to a short, in LITTLE_ENDIAN format */
	private short getShort(byte argB1, byte argB2) {
		return (short)(argB1 | (argB2 << 8));
	}
}
