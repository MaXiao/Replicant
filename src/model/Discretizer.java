package model;

/**
 * Discretize visual info and action
 * parameters to make predictions a 
 * little bit fuzzy.
 * 
 * TODO: add a trigger in ModelBuilder
 * 		 and Brain to switch Fuzzy and
 * 		 Non-Fuzzy
 * 
 * @author: Xiao Ma
 * @since: March 23, 2012
 */
@SuppressWarnings("unused")
public class Discretizer {
	// Notes:
	// The field is 105 * 68
	// Normal vision cone: 90 degrees
	
	// How much degrees, meters or power per slot
	private static int distanceDiscretizer = 10;
	private static int powerDiscretizer = 10;
	private static int angleDiscretizer = 10;
	// For absolute position approach. No use for now.
	private static double xDiscretizer = 105. / 10.;
	private static double yDiscretier = 68. / 10.;
	
	public static void setDistanceDiscretizer(int distanceDiscretizer) {
		Discretizer.distanceDiscretizer = distanceDiscretizer;
	}

	public static void setPowerDiscretizer(int powerDiscretizer) {
		Discretizer.powerDiscretizer = powerDiscretizer;
	}

	public static void setAngleDiscretizer(int angleDiscretizer) {
		Discretizer.angleDiscretizer = angleDiscretizer;
	}

	public static void setxDiscretizer(double xDiscretizer) {
		Discretizer.xDiscretizer = xDiscretizer;
	}

	public static void setyDiscretier(double yDiscretier) {
		Discretizer.yDiscretier = yDiscretier;
	}
	

}
