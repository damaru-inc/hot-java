package com.damaru.hotjava;

import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class Main {

    public static void log(String msg) {
        System.out.println(msg);
    }

    public static void prompt(String msg) {
        System.out.print(msg + ": ");
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            testPerf();
            //Util.pitchMapper();
        } else {
            Options options = new Options();
            options.addRequiredOption("h", "host", true, "host");
            options.addRequiredOption("p", "password", true, "password");
            options.addRequiredOption("u", "username", true, "username");
            options.addRequiredOption("v", "vpn", true, "vpn");
            options.addOption("x", "test");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            // not needed for now: boolean test = cmd.hasOption('x');

            run(cmd);
        }
    }

    private static void run(CommandLine cmd) throws Exception {
        SolaceSubscriber subscriber = new SolaceSubscriber(cmd, new OscPlayer());
        //SolaceCacheSubscriber subscriber = new SolaceCacheSubscriber(cmd);

        ShutdownHook shutdownHook = new ShutdownHook(subscriber);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        final CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }

    private static void testPerf() throws Exception {
        log("testPerf");
        SynthModel model = SynthModel.getInstance();
        model.start();
        Osc osc1 = model.getOsc("o1");
        osc1.setFrequency(200.0);
        rampUp(osc1);

        Thread.sleep(4000);

        Osc osc2 = model.getOsc("o2");
        osc2.setFrequency(200.0);
        osc1.setFrequency(205.0);
        rampUp(osc2);
        Thread.sleep(4000);

        Osc osc3 = model.getOsc("o3");
        osc3.setFrequency(210.0);
        osc1.setFrequency(208.0);
        osc2.setFrequency(190.0);
        rampUp(osc3);
        Thread.sleep(4000);

        model.stop();

    }

    private static void rampUp(Osc osc) throws Exception {
        int ms = 2000;
        int steps = 100;
        int sleep = ms / steps;
        double target = 0.5;
        double current = 0.0;
        double increment = target / steps;

        for (int i = 0; i < steps; i++) {
            current += increment;
            osc.setAmplitude(current);
            Thread.sleep(sleep);
        }
    }

    static class ShutdownHook extends Thread {
        private Solace subscriber;

        public ShutdownHook(Solace subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void run() {
            if (subscriber != null) {
                subscriber.close();
            }
            
            try {
                SynthModel sm = SynthModel.getInstance();
                sm.close();
                log("Shut down sound.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
