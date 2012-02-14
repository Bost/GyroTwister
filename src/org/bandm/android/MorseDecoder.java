package org.bandm.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.util.Log;

public class MorseDecoder extends AsyncTask<File, Void, Void> {
    private FileInputStream is = null;

    @Override
    protected Void doInBackground(File... files) {
        int index;
        //double magnitudeSquared; 
        double magnitude; 

        int bufferSize = AudioRecord.getMinBufferSize(
        		SoundActivity.RECORDER_SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        GoertzelFilter filter = new GoertzelFilter(
        		SoundActivity.RECORDER_SAMPLE_RATE,
        		1200,
        		bufferSize);
        filter.init();

        for (int i = 0; i < files.length; i++) {
            byte[] data = new byte[bufferSize];

            try {
                is = new FileInputStream(files[i]);

                while(is.read(data) != -1) {
                    ShortBuffer sbuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                    short[] audioShorts = new short[sbuf.capacity()];
                    sbuf.get(audioShorts);

                    float[] audioFloats = new float[audioShorts.length];

                    for (int j = 0; j < audioShorts.length; j++) {
                        audioFloats[j] = ((float)audioShorts[j]) / 0x8000;
                    }

                    for (index = 0; index < audioFloats.length; index++) { 
                        filter.processSample(data[index]); 

                        magnitude = Math.sqrt(filter.getMagnitudeSquared());
                        Log.d("SoundCompare", "Relative magnitude = " + magnitude);
                    }
                    //magnitude = Math.sqrt(g.getMagnitudeSquared());
                    //Log.d("SoundCompare", "Relative magnitude = " + magnitude);

                    filter.reset();
                }

                is.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}