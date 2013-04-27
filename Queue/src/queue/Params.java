package queue;

public class Params {
	public static int generator = 1;
	public static double epsilon = 0.01;
	public static int numStates = 1;
	public static int numClocks = 1;
	
	static class initState {
		public static boolean clockActive = true;
		public static double clockSpeed = 1.0;
		public static int curState = 1;
	}
	
	static class distConfig {
		public static double poisson_lambda = 1.0; //parameters for generating distribution from random numbers
		public static double weibull_lambda1 = 0.8856899;
		public static double weibull_alpha1 = 2.1013491;
		public static double weibull_lambda2 = 1.7383757;
		public static double weibull_alpha2 = 0.5426926;
		
		public static Clcg4 unigen1 = new Clcg4(); //instantiating random number generators using Clcg4
		public static Clcg4 unigen2 = new Clcg4();

		public static void initialize(boolean trial) { //initializing random number generators
			long[] seed2 = {22, 33, 44, 55};
			
			unigen1.initDefault();
			unigen2.initDefault();
			unigen2.setInitialSeed(seed2);
			if (!trial) {
				unigen1.initGenerator(Params.generator, Clcg4.NewSeed);
				unigen2.initGenerator(Params.generator, Clcg4.NewSeed);
			}
		}
	}
}
