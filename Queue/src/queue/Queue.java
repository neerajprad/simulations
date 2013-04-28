package queue;


import java.text.DecimalFormat;
import java.util.Arrays;

 
public class Queue {
    private static int debug;
    private static String queue_dist;
    private static boolean trial, ci;
    private static int numReps;
    private static DecimalFormat df1; 
    private static Estimator expected_itemsinqueue = new Estimator(2.58,"99%", "##.####"); // 99% CI
    
    public static void main(String[] args) {

        df1 = new DecimalFormat("###.###");
        debug = 0;
        trial = false;
        ci = false;
        String arg;
        int i=0;        
        // parse command line arguments
        if (args.length < 1) {
          System.out.println("usage: queue n [-dist -dm -t -i]");
          return;
        }
        numReps = Integer.parseInt(args[0]);
        i++;
        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];
            if (arg.equals("-dist")) {
            	queue_dist = args[i++];
            }
            else if (arg.equals("-t")) {
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
            double val = doRep(queue_dist);
            if (debug >= 1) {
                System.out.println("Expected items in queue - Rep "+rep+": "+df1.format(val));
            }
            
            if (debug >=1) System.out.println();
            if (debug >=2) System.out.println();
        }
        
        // print results
        System.out.println("--------Final Results----------");
        System.out.print("Expected items in queue: "+df1.format(expected_itemsinqueue.mean()));
        if (ci){
            System.out.print(" with ");
            expected_itemsinqueue.printConfidenceInterval();
            System.out.println();
        }
        else System.out.println();
        if (trial) {
            System.out.println("Est. # of repetitions for expected_time: +/- "+Params.epsilon+" accuracy: "
                    + expected_itemsinqueue.numberOfTrialsNeeded(Params.epsilon, true));
        }
        
    }
    
    static double doRep(String queue_dist) {
    	State.init(trial, queue_dist);
        Double elapsed_time = 0.0;
        double queue_time = 0.0;
        int itr = 0;
        double itr_time;
        while (itr < Params.sim_lifetime) {
        	if (debug == 2) {
        		System.out.println("Elapsed time: "+ elapsed_time.toString());
        		System.out.println("Items in queue: "+ State.items_inqueue().toString());
        		State.printState();
        		State.clock.printClocks();
        	}
        	Integer items_inqueue  = State.items_inqueue();
        	itr_time = State.transition();
        	elapsed_time = elapsed_time + itr_time;
        	queue_time = queue_time + itr_time * items_inqueue;
        	itr ++;
        }
        if (debug == 2) {
        	System.out.println("--------Run complete---------");
        }
        queue_time = queue_time/elapsed_time;
        expected_itemsinqueue.processNextValue(queue_time);
        return queue_time;
    }
} 


class State {
	static int[] cur_state;
	static Clocks clock;
	static void init(boolean trial, String queue_dist) {
		Params.initState.initialize(queue_dist);
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
	
	public static Integer items_inqueue() {
		return cur_state[0];
	}
	
	
	public static double transition() {
		int next_event = clock.next_event();
		double time_elapsed = clock.event_time();
		// Item arrived
		if (next_event == 1) {
			clock.reset_oldclocks(time_elapsed);
			clock.reset_new(next_event);
			cur_state[0] ++;
			if (cur_state[0] == 1) {
				clock.reset_new(0);
			}
		}
		// Dispatched
		else if (next_event == 0) {
			clock.reset_oldclocks(time_elapsed);
			clock.reset_new(next_event);
			cur_state[0] --;
			if (cur_state[0] == 0) {
				clock.reset_canceled(0);
			}
		}
		return time_elapsed;
	}
	public static void printState() {
		System.out.print("System state: ");
		System.out.println(Arrays.toString(cur_state));
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
	
	public void printClocks() {
		System.out.print("Event clocks: ");
		System.out.println(Arrays.toString(timers));
		System.out.print("Active clocks: ");
		System.out.println(Arrays.toString(active));
	}
}

class randDist{
	public double init(String dist) {
		if (dist.equals("triangular")) return triangular(Params.distConfig.triangular_V, 
				Params.distConfig.unigen1);
	
		else if (dist.equals("poisson")) return poisson(Params.distConfig.poisson_lambda, 
				Params.distConfig.unigen2);

		else if (dist.equals("weibull1")) return weibull(Params.distConfig.weibull_lambda1, 
				Params.distConfig.weibull_alpha1, Params.distConfig.unigen2); 
		
		else if (dist.equals("weibull2")) return weibull(Params.distConfig.weibull_lambda2, 
				Params.distConfig.weibull_alpha2, Params.distConfig.unigen2); 

		return -100;
	}
	
	public double weibull (double lambda, double alpha, Clcg4 unigen1) {
		double randnum =  unigen1.nextValue(Params.generator);
		return Math.pow(-Math.log(randnum), 1.0/alpha)/lambda;
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
	
	public double poisson (double lambda, Clcg4 unigen) {
		double L = Math.exp(-lambda);
		int k = 0;
		double p = 1.0;
		do {
			k++;
			p = p * unigen.nextValue(Params.generator);
		}
		while (p > L);
		return k - 1;
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


