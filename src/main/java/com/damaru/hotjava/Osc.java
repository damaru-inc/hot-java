package com.damaru.hotjava;
// todo: add switch to disable sound.
// todo: get mod working.
// todo: work out frequency range
// todo: coarse/fine freq
// todo: waveform load
import java.io.IOException;
import java.util.Formatter;

import javax.sound.sampled.AudioFormat;


public class Osc {

	public enum WaveformType {
		SAWTOOTH, SINE, SQUARE, TRIANGLE
	};

	private static final int WAVE_TABLE_LENGTH = 8192;
	public static final double TOP_LIMIT = 0.9999;
	private static final double[] SAWTOOTH_TABLE = new double[WAVE_TABLE_LENGTH];
	private static final double[] SINE_TABLE = new double[WAVE_TABLE_LENGTH];
	private static final double[] SQUARE_TABLE = new double[WAVE_TABLE_LENGTH];
	private static final double[] TRIANGLE_TABLE = new double[WAVE_TABLE_LENGTH];
	public static final double SAMPLE_RATE = 48000.0;
	public static final int SAMPLE_SIZE = 16;
	public static final int CHANNELS = 2;
	public static final int FRAME_SIZE = 4;
	public static final boolean BIG_ENDIAN = false;
	public static final boolean SIGNED = true;
	public static final AudioFormat AUDIO_FORMAT = new AudioFormat(
			(float) SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, SIGNED, BIG_ENDIAN);
	public static final double INTEGER_CONVERSION_FACTOR = Math.pow( 2.0, (SAMPLE_SIZE-1) );

	private double wavetableIndex;
	private double[] wavetable; // usually points to one of the static wave
								// tables.
	private WaveformType waveform;
	private double amplitude;
	private double frequency;
	private double period;
	private double wavetableInterval;	// number of samples to skip based on frequency

	private String name;
	
	private double vibratoFactor;
	private Osc vibratoOsc;
	

	static {
		int halfWay = WAVE_TABLE_LENGTH / 2;
		double DOUBLE_TOP_LIMIT = 2.0 * TOP_LIMIT;
		double QUAD_TOP_LIMIT = 4.0 * TOP_LIMIT;
		for (int i = 0; i < WAVE_TABLE_LENGTH; i++) {
			double soFar = i / (double) WAVE_TABLE_LENGTH;
			double val;

			// sawtooth
			val = (DOUBLE_TOP_LIMIT * soFar) - TOP_LIMIT;
			SAWTOOTH_TABLE[i] = val;

			// sine
			double radian = soFar * 2.0 * Math.PI;
			val = Math.sin(radian) * TOP_LIMIT;
			SINE_TABLE[i] = val;

			// square
			SQUARE_TABLE[i] = i < halfWay ? -TOP_LIMIT : TOP_LIMIT;

			// triangle
			if (i < halfWay) {
				TRIANGLE_TABLE[i] = (QUAD_TOP_LIMIT * soFar) - TOP_LIMIT;
			} else {
				TRIANGLE_TABLE[i] = (QUAD_TOP_LIMIT * (1.0 - soFar)) - TOP_LIMIT;
			}
		}
	}

	public Osc(WaveformType waveform, double amplitude, double frequency) {
		this.amplitude = amplitude;
		setFrequency( frequency );
		setWaveform( waveform );
		//Main.log( "period: " + period + " freq: " + frequency + " wavetableInterval: " + wavetableInterval );
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public double currentValue() {
		int index = (int) wavetableIndex;
		if (index > WAVE_TABLE_LENGTH) {
			index = 0;  // only happens because of thread issues, not a normal situation.
		}
		double val = wavetable[index] * amplitude;
		if ( val >= TOP_LIMIT ) {
			val = TOP_LIMIT;
		}
		else if ( val <= -TOP_LIMIT ) {
			val = -TOP_LIMIT;
		}
		return val;
	}
	
	public void nextStep() {
		double skipInterval = wavetableInterval;
		
		if ( vibratoFactor > 0.0 && vibratoOsc != null ) {
			double skipFactor = vibratoOsc.currentValue() * vibratoFactor;
			skipInterval = wavetableInterval + (skipFactor * wavetableInterval);
		}
		
		wavetableIndex += skipInterval;
		if (wavetableIndex >= WAVE_TABLE_LENGTH) {
			wavetableIndex -= WAVE_TABLE_LENGTH;
		}		
	}
	
	public double nextValue() {
		int index = (int) wavetableIndex;
		if (index > WAVE_TABLE_LENGTH) {
			index = 0;  // only happens because of thread issues, not a normal situation.
		}
		double val = wavetable[index] * amplitude;
		if ( val >= TOP_LIMIT ) {
			val = TOP_LIMIT;
		}
		else if ( val <= -TOP_LIMIT ) {
			val = -TOP_LIMIT;
		}
		double skipInterval = wavetableInterval;
		
		if ( vibratoFactor > 0.0 && vibratoOsc != null ) {
			double skipFactor = vibratoOsc.nextValue() * vibratoFactor;
			skipInterval = wavetableInterval + (skipFactor * wavetableInterval);
		}
		
		wavetableIndex += skipInterval;
		if (wavetableIndex >= WAVE_TABLE_LENGTH) {
			wavetableIndex -= WAVE_TABLE_LENGTH;
		}
		return val;
	}
	
	public void read( double[] buffer ) {
		int numToCopy = buffer.length;
		
		for ( int frame = 0; frame < numToCopy; frame++ ) {
			double val = nextValue();
			buffer[frame] = val;
		}
	}
	
	public int read( byte[] buffer ) throws IOException {
		return read( buffer, 0, buffer.length );
	}
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (length % FRAME_SIZE != 0) {
			throw new IOException(
					"length must be an integer multiple of frame size");
		}
		
		int bytesToCopy = length - offset;
		int bytesCopied = 0;

		for (int frame = 0; frame < bytesToCopy; frame+=FRAME_SIZE) {
			double val = nextValue();
			int integerValue = (int) (val * INTEGER_CONVERSION_FACTOR);
			byte lowByte = (byte) (integerValue & 0xff);
			byte highByte = (byte) ((integerValue >>> 8) & 0xFF);
			buffer[frame] = lowByte;
			buffer[frame+1] = highByte;
			buffer[frame+2] = lowByte;
			buffer[frame+3] = highByte;
			bytesCopied += FRAME_SIZE;
		}
		//log.debug( "bytesToCopy: " + bytesToCopy + " bytesCopied: " + bytesCopied );
		
		return bytesToCopy;
	}

	public double getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(double amplitude) {
		this.amplitude = amplitude;
	}

	public double getFrequency() {
		return frequency;
	}

	public void setFrequency(double frequency) {
		this.frequency = frequency;
		period = SAMPLE_RATE / frequency;
		wavetableInterval = WAVE_TABLE_LENGTH / period;
	}

	public double getVibratoFactor() {
		return vibratoFactor;
	}

	public void setVibratoFactor(double vibratoFactor) {
		this.vibratoFactor = vibratoFactor;
	}

	public Osc getVibratoOsc() {
		return vibratoOsc;
	}

	public void setVibratoOsc(Osc vibratoOsc) {
		this.vibratoOsc = vibratoOsc;
	}

	public WaveformType getWaveform() {
		return waveform;
	}

	public void setWaveform(WaveformType waveform) {
		this.waveform = waveform;

		switch (waveform) {
		case SAWTOOTH:
			wavetable = SAWTOOTH_TABLE;
			break;
		case SINE:
			wavetable = SINE_TABLE;
			break;
		case SQUARE:
			wavetable = SQUARE_TABLE;
			break;
		case TRIANGLE:
			wavetable = TRIANGLE_TABLE;
			break;
		}
	}

	private static void dump(Formatter formatter, String title, double wave[]) {
		System.out.println(title);
		int numEachLine = 3;
		for (int i = 0; i < WAVE_TABLE_LENGTH;) {
			double val = wave[i];
			int integerValue = (int) (val * INTEGER_CONVERSION_FACTOR);
			byte lowByte = (byte) (integerValue & 0xff);
			byte highByte = (byte) ((integerValue >>> 8) & 0xFF);
			formatter.format("%8.4f %8d %02X%02X  ", val, integerValue, highByte, lowByte  );
			if ( ++i % numEachLine == 0 ) {
				System.out.println();				
			}
		}
		System.out.println();
	}

	public static void main(String args[]) {
		Formatter formatter = new Formatter(System.out);
		dump(formatter, "saw:", SAWTOOTH_TABLE);
		dump(formatter, "sine:", SINE_TABLE);
		dump(formatter, "square:", SQUARE_TABLE);
		dump(formatter, "triangle:", TRIANGLE_TABLE);
		/*
		double factor = Math.pow( 2.0, (SAMPLE_SIZE-1) );
		double val = TOP_LIMIT;
		int integerValue = (int) (val * INTEGER_CONVERSION_FACTOR);
		byte lowByte = (byte) (integerValue & 0xff);
		byte highByte = (byte) ((integerValue >>> 8) & 0xFF);
		log.debug( "integerValue: " + integerValue + " low: " + lowByte + " high: " + highByte );
		
		integerValue = (int) (-val * factor);
		lowByte = (byte) (integerValue & 0xff);
		highByte = (byte) ((integerValue >> 8) & 0xFF);
		log.debug( "integerValue: " + integerValue + " low: " + lowByte + " high: " + highByte );

		factor = Math.pow( 2.0, (SAMPLE_SIZE) );
		integerValue = (int) (val * factor);
		lowByte = (byte) (integerValue & 0xff);
		highByte = (byte) ((integerValue >>> 8) & 0xFF);
		log.debug( "integerValue: " + integerValue + " low: " + lowByte + " high: " + highByte );
		
		integerValue = (int) (-val * factor);
		lowByte = (byte) (integerValue & 0xff);
		highByte = (byte) ((integerValue >>> 8) & 0xFF);
		log.debug( "integerValue: " + integerValue + " low: " + lowByte + " high: " + highByte );
	*/
	}
}

