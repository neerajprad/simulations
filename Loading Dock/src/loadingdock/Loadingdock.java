package loadingdock;

/*
 * A DTMC simulation of a loading dock given in the README file.
 *
 * usage: gamble n [-dm -t -i]
 * where
 *   n is number of simulation repetitions
 *   m is debug level (0 = none; 1 = perf measures; 2 = sample paths)
 *   -t means trial runs (uses different random numbers than production runs)
 *   -i means print out 99% confidence interval
 */

import java.text.DecimalFormat;
import java.util.Arrays;

import loadingdock.Clcg4;
import loadingdock.Params;
 
public class Loadingdock {
    private static int debug;
    private static boolean trial, ci;
    private static int numReps;
    private static DecimalFormat df1; 
    private static Estimator expected_time = new Estimator(2.58,"99%", "##.####"); // 99% CI
    private static Estimator expected_delay = new Estimator(2.58,"99%", "##.####");
    private static Estimator prob_trucksmorethan5 = new Estimator(2.58,"99%", "##.####");
    private static Estimator expected_numtrucks = new Estimator(2.58,"99%", "##.####");;
    
    public static void main(String[] args) {

        df1 = new DecimalFormat("###.###");
        debug = 0;
        trial = false;
        ci = false;
        String arg;
        int i=0;        
        // parse command line arguments
        if (args.length < 1) {
          System.out.println("usage: epidemic n [-dm -t -i]");
          return;
        }
        numReps = Integer.parseInt(args[0]);
        i++;
        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];
            if (arg.equals("-t")) {
                trial = true;
            }
            else if (arg.equals("-i")) {
                ci = true;
            }
            else if (arg.startsWith("-d")) {
                debug = Integer.parseInt(arg.substring(2));
            }
            else {
               System.out.println("bad command line arg: "+arg);
               return; 
            }
        }
        
        
        //run the simulation repetitions and collect stats
        Params.distConfig.initialize(trial);
        for (int rep = 1; rep <= numReps; rep++) {
            double[] params = doRep();
            if (debug >= 1) {
                System.out.println("Expected time for trucks to depart - Rep "+rep+": "+df1.format(params[0]));
                System.out.println("Average delay for all trucks - Rep "+rep+": "+df1.format(params[1]));
                System.out.println("Prob of more than 5 trucks at dock - Rep "+rep+": "+df1.format(params[2]));
                System.out.println("Expected number of trucks at dock - Rep "+rep+": "+df1.format(params[3]));
            }
            
            if (debug >=1) System.out.println();
            if (debug >=2) System.out.println();
        }
        
        // print results
        System.out.println("--------Final Results----------");
        System.out.print("Expected time for trucks to depart: "+df1.format(expected_time.mean()));
        if (ci){
            System.out.print(" with ");
            expected_time.printConfidenceInterval();
            System.out.println();
        }
        System.out.print("Average delay for all trucks: "+df1.format(expected_delay.mean()));
        if (ci){
            System.out.print(" with ");
            expected_delay.printConfidenceInterval();
            System.out.println();
        }
        System.out.print("Prob of more than 5 trucks at dock: "+df1.format(prob_trucksmorethan5.mean()));
        if (ci){
            System.out.print(" with ");
            prob_trucksmorethan5.printConfidenceInterval();
            System.out.println();
        }
        System.out.print("Expected number of trucks at dock: "+df1.format(expected_numtrucks.mean()));
        if (ci){
            System.out.print(" with ");
            expected_numtrucks.printConfidenceInterval();
            System.out.println();
        }
        else System.out.println();
        if (trial) {
            System.out.println("Est. # of repetitions for expected_time: +/- "+Params.epsilon+" accuracy: "
                    + expected_time.numberOfTrialsNeeded(Params.epsilon, true));
            System.out.println("Est. # of repetitions for expected_delay: +/- "+Params.epsilon+" accuracy: "
                    + expected_delay.numberOfTrialsNeeded(Params.epsilon, true));
            System.out.println("Est. # of repetitions for trucks_morethan5: +/- "+Params.epsilon+" accuracy: "
                    + prob_trucksmorethan5.numberOfTrialsNeeded(Params.epsilon, true));
            System.out.println("Est. # of repetitions for expected_numtrucks+/- "+Params.epsilon+" accuracy: "
                    + expected_numtrucks.numberOfTrialsNeeded(Params.epsilon, true));
        }
        
    }
    
    static double[] doRep() {
    	State.init(trial);
        Double elapsed_time = 0.0;
        Double trucks_delay = 0.0;
        double prob_morethan5trucks = 0;
        double trucks_atdock = 0;
        double itr_time;
        while (!State.terminal()) {
        	itr_time = State.transition();
        	elapsed_time = elapsed_time + itr_time;
        	trucks_delay = trucks_delay + itr_time * State.trucks_atdock();
        	if (debug == 2) {
        		System.out.println("Elapsed time: "+ elapsed_time.toString());
        		System.out.println("Trucks at dock: "+ State.trucks_atdock().toString());
        		System.out.print("State of the system: ");
        		System.out.println(Arrays.toString(State.cur_state));
        	}
        	if (State.trucks_atdock() > 5) {
        		prob_morethan5trucks = prob_morethan5trucks + itr_time;
        	}
        }
        if (debug == 2) {
        	System.out.println("--------Run complete---------");
        }
        trucks_delay = trucks_delay/8;
        prob_morethan5trucks = prob_morethan5trucks/elapsed_time;
        trucks_atdock = (trucks_delay * 8)/elapsed_time;
        expected_time.processNextValue(elapsed_time);
        expected_delay.processNextValue(trucks_delay);
        prob_trucksmorethan5.processNextValue(prob_morethan5trucks);
        expected_numtrucks.processNextValue(trucks_atdock);
        double[] params = {elapsed_time, trucks_delay, prob_morethan5trucks, trucks_atdock};
        return params;
    }
} 

class State {
	static int[] cur_state;
	static Clocks clock;
	static void init(boolean trial) {
		Params.initState.initialize();
		cur_state = Params.initState.curState;
    	boolean[] clocks_active = Params.initState.clockActive;
    	String[] clock_dists = Params.clockDists;
    	double[] clock_speeds = Params.initState.clockSpeed;
    	clock = new Clocks(Params.numClocks, clock_dists, clocks_active, trial, clock_speeds);
	}
	
	public static int trucks_arrived() {
		int sum = 0;
		for (int i = 0; i < 8; i++) {
			if (cur_state[i] == 1) sum ++;
		}
		return sum;
	}
	
	public static Integer trucks_atdock() {
		return Math.max(0, trucks_arrived() - cur_state[8]);
	}
	
	public static boolean terminal() {
		return (trucks_arrived() == 8 && cur_state[8] >= 8);
	}
	
	public static double transition() {
		int next_event = clock.next_event();
		double time_elapsed = clock.event_time();
		// Trucks arrived
		if (next_event < 8) {
			clock.reset_canceled(next_event);
			clock.reset_oldclocks(time_elapsed);
			cur_state[next_event] = 1;
		}
		// Item delivered
		else if (next_event == 8) {
			clock.reset_oldclocks(time_elapsed);
			clock.reset_new(next_event);
			cur_state[next_event] = cur_state[next_event] + 1;
		}
		// Conveyer belt breakdown
		else if (next_event == 9) {
			clock.reset_canceled(next_event); //conveyer belt breakdown event canceled
			clock.reset_oldclocks(time_elapsed);
			clock.reset_new(10); //start repair
			clock.reset_speed(8, 0.0);
			cur_state[next_event] = 1;
		}
		// Repair completed
		else if (next_event == 10) {
			clock.reset_canceled(next_event);
			clock.reset_oldclocks(time_elapsed);
			clock.reset_new(9); //conveyer belt breakdown scheduled
			clock.reset_speed(8, 1.0);
			cur_state[9] = 0;
		}
		return time_elapsed;
	}
}

class Clocks {
	int count;
	String[] dist;
	boolean[] active;
	double[] timers;
	double[] speeds;
	randDist clock_dist = new randDist(); 
	public Clocks (int n, String[] distns, boolean[] active_clocks, boolean trial, double[] clock_speeds) {
		assert distns.length == n;
		count = n;
		dist = distns;
		active = active_clocks;
		speeds = clock_speeds;
		timers = new double[n];
		for (int i = 0; i < n; i++) {
			if (active[i]) {
				timers[i] = clock_dist.init(dist[i]);
			}
			else {
				timers[i] = -1;
			}
		}
	}
	
	public void reset_new(int j) {
		timers[j] = clock_dist.init(dist[j]);
		active[j] = true;
	}
	
	public void reset_old(int j, double time_elapsed, double speed) {
		timers[j] = timers[j] - time_elapsed * speed;
	}
	
	public void reset_canceled(int j) {
		timers[j] = -1;
		active[j] = false;
	}
	
	public void reset_speed(int j, double speed) {
		speeds[j] = speed;
	}
	
	public void reset_oldclocks(double hold_time) {
		for (int i = 0 ; i < count; i++) {
			if (this.active[i]) {
				this.reset_old(i, hold_time, speeds[i]);
			}
		}
	}
	
	public int next_event() {
		double min = Double.POSITIVE_INFINITY;
		int next_event = -1;
		for (int i = 0; i < count; i++) {
			if (active[i]) {
				Double time_untilevent = timers[i]/speeds[i];
				if (time_untilevent < min) {
					min = time_untilevent;
					next_event = i;
				}
			}
		}
		return next_event;
	}
	
	public double event_time() {
		int next_event = next_event();
		return timers[next_event];
	}	
}

class randDist{
	public double init(String dist) {
		if (dist == "erlang2") return erlang2(Params.distConfig.erlang_lambda, 
				Params.distConfig.unigen1, Params.distConfig.unigen2);
		else if (dist == "triangular") return triangular(Params.distConfig.triangular_V, 
				Params.distConfig.unigen3);
		else if (dist == "uniform") return uniform(Params.distConfig.uniform_wmin, 
				Params.distConfig.uniform_wmax, Params.distConfig.unigen4);
		else if (dist == "fixed") return fixed(Params.distConfig.fixed_Q);
		return -1;
	}
	
	public double erlang2 (double lambda, Clcg4 unigen1, Clcg4 unigen2) {
		double randnum1 =  unigen1.nextValue(Params.generator);
		double randnum2 = unigen2.nextValue(Params.generator);
		return -Math.log((1-randnum1) * (1-randnum2))/lambda;
	}
	
	public double triangular (double V, Clcg4 unigen) {
		double randnum = unigen.nextValue(Params.generator);
		if (randnum <= 0.5) {
			return Math.sqrt((randnum * Math.pow(V, 2) / 2));
		}
		else {
			return V * (1 - Math.sqrt((1 - randnum)/2));
		}
	}
	
	public double fixed (double Q) {
		return Q;
	}
	
	public double uniform (double w_min, double w_max, Clcg4 unigen) {
		double randnum = unigen.nextValue(Params.generator);
		return w_min + (w_max - w_min) * randnum;
	}
}

class Estimator { // computes point estimates and confidence intervals
    private double k; // # of values processed so far
    private double sum; // running sum of values
    private double v; // running value of (k-1)*variance
    private double z; // quantile for normal confidence intervals
    private String confStr; // string of form "xx%" for xx% confidence interval
    private DecimalFormat df; // for printing CI
	
   public Estimator(double z, String confStr, String dfStr) {
        k = 0;
        sum = 0;
        v = 0;
        this.z = z;
        this.confStr = confStr;
        df = new DecimalFormat(dfStr);
    }
   
   public void reset() {
       k = 0;
       sum = 0;
       v = 0;
   }
	
    public void processNextValue(double value) {
        k++;
        if (k>1) {
            double diff = sum - (k-1)*value;
            v += (diff/k) * (diff/(k-1));
        }
        sum += value;
    }

    public double variance() {
        double var = 0;
        if (k>1) {
            var = v/(k-1);
        }
        return var;
    }
	
    public void printConfidenceInterval(){
        double hw = z * Math.sqrt(variance()/k);
        double pointEstimate = mean();
        double cLower = pointEstimate - hw;
        double cUpper = pointEstimate + hw;
        System.out.print(confStr+" Confidence Interval ["+df.format(cLower)+", "+df.format(cUpper)+"]");
    }
	
    public double mean() {
        double mu=0;
        if (k >1) {
            mu = sum/k;
        }
        return mu;
    }

    public long numberOfTrialsNeeded(double epsilon, boolean relative) {
        double var = variance();
        double width = epsilon;
        if (relative) {
            width = mean() * epsilon;
        }
        return  (long)((var * z * z)/(width * width));
    }

    public double sum() {
        return sum;
    }
}
