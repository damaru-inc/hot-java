package com.damaru.hotjava;

public class OscPlayer {

    private SynthModel synthModel;
    double inLo = 16.0;
    double inHi = 36.0;
    double outLo = 100;
    double outHi = 6400;

    // slew settings
    int steps = 900;
    int sleep = 1;

    public OscPlayer() throws Exception {
        synthModel = SynthModel.getInstance();
        synthModel.start();
    }

    public void handleMessage(String id, double temp) throws Exception {

        double freq = tempToFreq(temp);
        String msg = String.format("id: %6s temp: %2.2f freq: %8.2f", id, temp, freq);
        Main.log(msg);
        Osc osc = synthModel.getOsc(id);

        // Thread: Context_1_ConsumerDispatcher,5,main
        // amp of 0 means it's a new osc.
        if (osc.getAmplitude() == 0.0) {
            osc.setFrequency(freq);
           rampUp(osc);
            //osc.setAmplitude(0.5);
        } else {
            //osc.setFrequency(freq);
            slew(osc, freq);
        }
    }

    private double tempToFreq(double temp) {
        double freq = Util.mapLog(temp, inLo, inHi, outLo, outHi);
        return freq;
    }

    private void slew(Osc osc, double target) throws Exception {
        double current = osc.getFrequency();
        double increment = (target - current) / steps;

        for (int i = 0; i < steps; i++) {
            current += increment;
            osc.setFrequency(current);
            Thread.sleep(sleep);
        }

    }
    
    private void rampUp(Osc osc) throws Exception {
        double target = 0.5;
        double current = 0.0;
        double increment = target / steps;

        for (int i = 0; i < steps; i++) {
            current += increment;
            osc.setAmplitude(current);
            Thread.sleep(sleep);
        }
    }


}
