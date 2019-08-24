package com.damaru.hotjava;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author mike
 */
public class SynthModel {

    private static final int DOUBLE_BUFFER_SIZE = 1024;
    private static final int BYTE_BUFFER_SIZE = (int) (Osc.FRAME_SIZE * DOUBLE_BUFFER_SIZE);
    private double[] doubleBuffer = new double[DOUBLE_BUFFER_SIZE];
    private byte[] byteBuffer = new byte[BYTE_BUFFER_SIZE];
    //private static final int NUM_OSCS = 3;
    private double amplitude = 0.5;
    // private Osc osc = new Osc(Osc.WaveformType.TRIANGLE, amplitude, 220.0);
    private ConcurrentHashMap<String, Osc> oscs  = new ConcurrentHashMap<>();
    SourceDataLine line;
    boolean running;
    private SynthRunner runner;
    private static SynthModel instance;

    public Osc getOsc(String id) {
        Osc ret = oscs.get(id);
        
        if (ret == null) {
            ret = new Osc(Osc.WaveformType.TRIANGLE, 0.0, 0.0);
            //Main.log("new osc " + id);
            oscs.put(id,  ret);
        }
        return ret;
    }

    public double getAmplitude() {
        return amplitude;
    }

    public void setAmplitude(double amplitude) {
        this.amplitude = amplitude;
    }

    public static SynthModel getInstance() throws Exception {
        if (instance == null) {
            instance = new SynthModel();
        }

        return instance;
    }

    private SynthModel() throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, Osc.AUDIO_FORMAT);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(Osc.AUDIO_FORMAT);
        Main.log("got line: " + line);
    }

    public void start() {
        line.start();
        running = true;
        runner = new SynthRunner();
        runner.start();
        Main.log("starting");
    }

    public void stop() {
        running = false;
        if (line != null) {
            line.stop();
        }
        runner = null;
        Main.log("stopped");
    }

    public void close() {
        Main.log("closing");
        if (line != null) {
            line.close();
            line = null;
        }
    }

    @Override
    public void finalize() {
        Main.log("finalize.");
        if (line.isOpen()) {
            line.close();
        }
    }

    class SynthRunner extends Thread {

        public void run() {
            while (running) {
                int numOscs = oscs.size();
                int bytesCopied = 0;
                for (int doubleIndex = 0; doubleIndex < DOUBLE_BUFFER_SIZE; doubleIndex++) {

                    double val = 0.0;
                    
                    for (String k : oscs.keySet()) {
                        Osc o = oscs.get(k);
                        val += o.currentValue();
                        o.nextStep();                        
                    }
                    
                    if (val != 0.0) {
                        val /= numOscs;
                    }
                    
                    doubleBuffer[doubleIndex] = val * amplitude;
                }
                // osc.read(doubleBuffer);
                int doubleIndex = 0;
                for (int byteIndex = 0; byteIndex < BYTE_BUFFER_SIZE; byteIndex += Osc.FRAME_SIZE, doubleIndex++) {
                    double val = doubleBuffer[doubleIndex];
                    int integerValue = (int) (val * Osc.INTEGER_CONVERSION_FACTOR);
                    byte lowByte = (byte) (integerValue & 0xff);
                    byte highByte = (byte) ((integerValue >>> 8) & 0xFF);
                    byteBuffer[byteIndex] = lowByte;
                    byteBuffer[byteIndex + 1] = highByte;
                    byteBuffer[byteIndex + 2] = lowByte;
                    byteBuffer[byteIndex + 3] = highByte;
                    bytesCopied += Osc.FRAME_SIZE;
                }
                line.write(byteBuffer, 0, bytesCopied);
                //Main.log("copied: " + bytesCopied + " Thread: " + Thread.currentThread());
                // shows Thread-1,5,main
                //        + " val: " +
                //doubleBuffer[100] + " " + byteBuffer[25]);
                //Main.log("copied: " + bytesCopied + " " + byteBuffer[25] );
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    Main.log("Caught interrupt.");
//                }
            }
        }
    }
}
