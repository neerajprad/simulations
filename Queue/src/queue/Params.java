package queue;
import java.util.Arrays;
import org.apache.commons.math3.distribution.NormalDistribution;
public class Params {
	public static int sim_lifetime = 1000;
	public static int generator = 1;
	public static double epsilon = 0.01;
	public static int numStates = 1;
	public static int numClocks = 2;
	public static String[] clockDists;
	public static String delivery_clock = "triangular";
	
	static class initState {
		public static boolean[] clockActive = new boolean[numClocks];
		public static double[] clockSpeed = new double[numClocks];
		public static int[] curState = new int[numStates];
		
		public static void initialize(String queue_clock) {
			clockDists = new String[] {delivery_clock, queue_clock};
			Arrays.fill(clockActive, true);
			clockActive[0] = false;
			Arrays.fill(clockSpeed, 1);
			Arrays.fill(curState, 0);
		}
	}
	
	static class distConfig {
		public static NormalDistribution ndist = new NormalDistribution();
		public static double triangular_V = 1.98;
		public static double poisson_lambda = 1.0; //parameters for generating distribution from random numbers
		public static double weibull_lambda1 = 0.8856899;
		public static double weibull_alpha1 = 2.1013491;
		public static double weibull_lambda2 = 1.7383757;
		public static double weibull_alpha2 = 0.5426926;
		
		public static Clcg4 unigen1 = new Clcg4(); //instantiating random number generators using Clcg4
		public static Clcg4 unigen2 = new Clcg4();
		public static Clcg4 unigen3 = new Clcg4();

		public static void initialize(boolean trial) { //initializing random number generators
			long[] seed2 = {22, 33, 44, 55};
			long[] seed3 = {222,3333, 4444, 55555};
			
			
			unigen1.initDefault();
			unigen2.initDefault();
			unigen3.initDefault();
			unigen2.setInitialSeed(seed2);
			unigen3.setInitialSeed(seed3);
			if (!trial) {
				unigen1.initGenerator(Params.generator, Clcg4.NewSeed);
				unigen2.initGenerator(Params.generator, Clcg4.NewSeed);
				unigen3.initGenerator(Params.generator, Clcg4.NewSeed);
			}
		}
	}
}
