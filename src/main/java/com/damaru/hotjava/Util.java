package com.damaru.hotjava;

public class Util {

    private static double LOG2 = Math.log(2);

    public static void pitchMapper() {
    	int inLo = 24; //10;
    	int inHi = 40; // 50;
    	int outLo = 200;
    	int outHi = 3200;
    	
    	for (int i = inLo; i <= inHi; i += 2) {
    		//double linear = mapLinear(i, inLo, inHi, outLo, outHi);
    		//logger.info("linear: {} -> {}", i, linear);
    		double log = mapLog(i, inLo, inHi, outLo, outHi);
    		Main.log(String.format("log: %d -> %f", i, log));
    	}
    }
    
    public static double mapLog(double in, double inLo, double inHi, double outLo, double outHi) {
    	double outRange = (outHi / outLo);
    	double logRange = log2(outRange);
    	double logMapped = mapLinear(in, inLo, inHi, 0, logRange);
    	//logger.info("outRange: {} logRange {} logMapped {}", outRange, logRange, logMapped);
    	double ret = outLo * Math.pow(2, logMapped);
    	return ret;
    }
    
    public static double mapLinear(double in, double inLo, double inHi, double outLo, double outHi) {
    	double ret = 0;
    	double inRange = inHi - inLo;
    	double outRange = outHi - outLo;
    	double inVal = (in - inLo) / inRange;
    	ret = inVal * outRange + outLo;
    	return ret;
    }
    
    public static double log2(double x) {
    	return Math.log(x) / LOG2;
    }
    
    public static int log(int x, int base)
    {
        return (int) (Math.log(x) / Math.log(base));
    }

}
