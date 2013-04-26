package loadingdock;

import java.util.Arrays;

public class Params {
	public static int generator = 3;
	public static double epsilon = 0.01;
	public static int numStates = 10;
	public static int numClocks = 11;
	public static String[] clockDists = {"erlang2", "erlang2", "erlang2", "erlang2", "erlang2", "erlang2", 
		"erlang2", "erlang2", "fixed", "triangular", "uniform"};
	
	static class initState {
		public static boolean[] clockActive = new boolean[numClocks];
		public static double[] clockSpeed = new double[numClocks];
		public static int[] curState = new int[numStates];
		public static void initialize() {
			Arrays.fill(clockActive, true);
			clockActive[clockActive.length - 1] = false;
			Arrays.fill(clockSpeed, 1);
			Arrays.fill(curState, 0);
		}
	}
	
	static class distConfig {
		public static double erlang_lambda = 1.0/30; //parameters for generating distribution from random numbers
		public static double triangular_V = 60;
		public static double uniform_wmin = 30;
		public static double uniform_wmax = 45;
		public static double fixed_Q = 20;
		
		public static Clcg4 unigen1 = new Clcg4(); //instantiating random number generators using Clcg4
		public static Clcg4 unigen2 = new Clcg4();
		public static Clcg4 unigen3 = new Clcg4();
		public static Clcg4 unigen4 = new Clcg4();

		public static void initialize(boolean trial) { //initializing random number generators
			long[] seed2 = {22, 33, 44, 55};
			long[] seed3 = {111, 222, 333, 444};
			long[] seed4 = {3333, 4444, 5555, 6666};
			unigen1.initDefault();
			unigen2.initDefault();
			unigen3.initDefault();
			unigen4.initDefault();
			unigen2.setInitialSeed(seed2);
			unigen3.setInitialSeed(seed3);
			unigen4.setInitialSeed(seed4);
			if (!trial) {
				unigen1.initGenerator(Params.generator, Clcg4.NewSeed);
				unigen2.initGenerator(Params.generator, Clcg4.NewSeed);
				unigen3.initGenerator(Params.generator, Clcg4.NewSeed);
				unigen4.initGenerator(Params.generator, Clcg4.NewSeed);
			}
		}
	}
}
