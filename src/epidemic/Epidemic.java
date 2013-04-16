package epidemic;
import java.util.*;


/*
 * A DTMC simulation of an epidemic given in the README file.
 *
 * usage: gamble n [-dm -t -i]
 * where
 *   n is number of simulation repetitions
 *   m is debug level (0 = none; 1 = perf measures; 2 = sample paths)
 *   -t means trial runs (uses different random numbers than production runs)
 *   -i means print out 95% confidence interval
 */


import java.text.DecimalFormat;
import epidemic.Clcg4;
 
public class Epidemic {
    private static Clcg4 unigen;
    private static Estimator gEstimator;
    private static int debug;
    private static boolean trial, ci;
    private static int numReps;
    private static DecimalFormat df1; 
    
    public static void main(String[] args) {
        gEstimator = new Estimator(1.96,"95%", "##.####"); // 95% CI
        unigen = new Clcg4();
        unigen.initDefault();
        df1 = new DecimalFormat("###.###");
        debug = 0;
        trial = false;
        ci = false;
        String arg;
        int i=0;
        double cost;
        
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

        //re-initialise generator for production runs
        if (!trial) {
            unigen.initGenerator(1, Clcg4.NewSeed);
        }
        
        //run the simulation repetitions and collect stats
        for (int rep = 1; rep <= numReps; rep++) {
            cost = doRep();
            if (debug >= 1) {
                System.out.print("Cost calculated on repetition "+rep+": "+df1.format(cost));
            }
            if (debug >=1) System.out.println();
            if (debug >=2) System.out.println();
        }
        
        // print results
        System.out.print("Average Cost to Company: "+df1.format(gEstimator.mean()));
        if (ci){
            System.out.print(" with ");
            gEstimator.printConfidenceInterval();
            System.out.println();
        }
        else System.out.println();
        if (trial) {
            System.out.println("Est. # of repetitions for +/- "+Params.epsilon+" accuracy: "
                    + gEstimator.numberOfTrialsNeeded(Params.epsilon, true));
        }
        
    }
    
    static double doRep() {
    	int infected = Params.I;
    	double cost = Params.C0;
        Employee[] employees = new Employee[Params.M];
        Employee.initialize();
        int i = 0;
        for (int j = 0; j < employees.length; j++) {
			if (i < Params.I) {
        		employees[j] = new Employee("infected");
        		i ++;
        	}
        	else employees[j] = new Employee("susceptible");
		}
        if (debug == 2){
        	System.out.print("Infected Count over "+ Params.numDays + " Days: ");
        }
        for (int day = 1; day <= Params.numDays; day++) {
        	if (debug == 3) {
        		System.out.println("Day " + day + ":");
        	}
        	infected = Employee.stateCounts.get("infected");
        	cost = cost + infected * Params.C;
        	if (debug == 2) {
        		System.out.print(infected+" ");
        	}
        	if (debug == 3) {
        		System.out.print("S: " + Employee.stateCounts.get("susceptible") + ", ");
        		System.out.print("I: " + Employee.stateCounts.get("infected") + ", ");
        		System.out.print("Q: " + Employee.stateCounts.get("quarantined") + ", ");
        		System.out.println("R: " + Employee.stateCounts.get("recovered"));
        	}
        	for (int j = 0; j < employees.length; j++) {
        		employees[j].transition(unigen);
        	}
        }
        if (debug == 2) {
        	System.out.println();
        }
        gEstimator.processNextValue(cost);
        return cost;
    }
}

class Employee {
	static double alpha0 = Params.alpha0;
	static double alpha = Params.alpha;
	static double beta = Params.beta;
	static double gamma = Params.gamma;
	String state;
	public static HashMap<String, Integer> stateCounts;
	static void initialize() {
		HashMap<String, Integer> counts= new HashMap<String, Integer>();
		counts.put("infected", 0);
		counts.put("susceptible", 0);
		counts.put("quarantined", 0);
		counts.put("recovered", 0);
		stateCounts = counts;
	}

	
	public Employee(String state) {
		stateCounts.put(state, stateCounts.get(state) + 1);
		this.state = state;
	}
	
	public String getState() {
		return this.state;
	}
	
	public void transition(Clcg4 unigen) {
		double randnum = unigen.nextValue(1);
		double p = 1 - (1 - alpha0) * (Math.pow((1 - alpha), stateCounts.get("infected")));
		if (this.state == "susceptible") {
			if (randnum < p) {
				this.state = "infected";
				stateCounts.put("infected", stateCounts.get("infected") + 1);
				stateCounts.put("susceptible", stateCounts.get("susceptible") - 1);
			}
		}
		else if (this.state == "infected") {
			if (randnum < beta ) {
				this.state = "recovered";
				stateCounts.put("recovered", stateCounts.get("recovered") + 1);
				stateCounts.put("infected", stateCounts.get("infected") - 1);
			}
			else if (randnum < (beta + (1-beta) * gamma)) {
				this.state = "quarantined";
				stateCounts.put("quarantined", stateCounts.get("quarantined") + 1);
				stateCounts.put("infected", stateCounts.get("infected") - 1);
			}
		}
		else if (this.state == "quarantined") {
			if (randnum < beta) {
				this.state = "recovered";
				stateCounts.put("recovered", stateCounts.get("recovered") + 1);
				stateCounts.put("quarantined", stateCounts.get("quarantined") - 1);
			}
		}
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
