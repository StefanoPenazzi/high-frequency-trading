package models.hftlimitandmarketorders;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ejml.simple.SimpleMatrix;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import data.input.tradeformat.DataTypePriceAmountMarkord;
import data.utils.CSV;
import models.ModelInterface;
import models.ModelParameterAnnotation;
import models.ModelParameterAnnotation.ModelParameter;


/**
 * @author stefanopenazzi
 *
 */
public abstract class OptimalMMPolicyFrameworkAbstract implements ModelInterface {
	
	private static final Logger log = LogManager.getLogger(OptimalMMPolicyFrameworkAbstract.class);
	
	protected enum StrategyBid{
		LIMIT,
		MARKET,
	}
	protected enum StrategyAsk{
		LIMIT,
		MARKET,
	}
	
	//Analysis parameters
	@ModelParameter(name = "start time",description="")
	private final Integer startTime;
	@ModelParameter(name = "end time",description="")
	private final Integer endTime;
	//PARAMETERS
	//Market parameters
	@ModelParameter(name = "tick",description="")
	private final Double tick;                     //tick size
	@ModelParameter(name = "rho",description="")
	private final Double rho;                      //per share rebate
	@ModelParameter(name = "epsilon",description="")
	private final Double epsilon;                  //per shaare fee
	@ModelParameter(name = "epsilon0",description="")
	private final Double epsilon0;                 //fixed fee
	//@ModelParameter(name = "",description="")
	private final Map<Integer,Double> lambda_t;    //tick time intensity
	@ModelParameter(name = "numOfTickSteps",description="")
	private final Integer numOfTickSteps;
	//Optimization parameters
	@ModelParameter(name = "gamma",description="")
	private final Double gamma;                    //inventory penalization
	@ModelParameter(name = "maxVolM",description="")
	private final Integer maxVolM;                 //max volume make
	@ModelParameter(name = "maxVolT",description="")
	private final Double maxVolT;                 //max volume take
	//Discretization parameters
	//time
	@ModelParameter(name = "timeStep",description="")
	private final Integer timeStep;
	@ModelParameter(name = "numOfTimeStep",description="")
	private final Integer numOfTimeStep;           //time step
	//inventory
	@ModelParameter(name = "lbShares",description="")
	private final Integer lbShares;                //lower bound shares
	@ModelParameter(name = "ubShares",description="")
	private final Integer ubShares;                //upper bound shares
	@ModelParameter(name = "numOfInventorySteps",description="")
	private final Integer numOfInventorySteps;
	@ModelParameter(name = "minInvIndex",description="")
	private final Integer minInvIndex;             //inventory can be negative, ...???
	
	@ModelParameter(name = "volumeStep",description="")
	private final Double volumeStep;               //new orders (market,limit) and the inventory are discretised 
	@ModelParameter(name = "numOfMaxVolMStep",description="")
	private final Integer numOfMaxVolMStep;        //TODO
	@ModelParameter(name = "numOfMaxVolMStep",description="")
	private final Integer numOfMaxVolTStep;        //TODO
	//Spread parameters
	private final SimpleMatrix spreadTransitionProbabMatrix;
	private SimpleMatrix spreadTransitionProbabMatrixNSteps;
	//Order
	private final Map<StrategyBid,Map<Integer,Double>> proxiesBid;
	private final Map<StrategyAsk,Map<Integer,Double>> proxiesAsk;
	private Map<StrategyBid,Map<Integer,Double>> proxiesBidProb = new HashMap<>();
	private Map<StrategyAsk,Map<Integer,Double>> proxiesAskProb = new HashMap<>(); 
	@ModelParameter(name = "delay",description="")
	private final Integer delay;
	private final Path outputDir;
	private final String testName;
	
	
	
    public static class AbstractBuilder {
		
    	protected Integer startTime = 0;
    	protected Integer endTime = 86400;
    	protected Integer timeStep = 5;
    	protected Double tick = 0.5;                        
    	protected Double rho = 0.0008;                      
    	protected Double epsilon = 0.0012;                  
    	protected Double epsilon0 = 0.000001;               
    	protected Map<Integer,Double> lambda_t;             
    	//Optimization parameters
    	protected Double gamma = 0.0001d;                      
    	protected Integer maxVolM = 500;                   
    	protected Double maxVolT = 500d;                   
    	//Discretization parameters
    	protected Integer DAYTIMEXSEC = 86400;
    	protected Integer numOfTimeStep = 86400;           
    	protected Integer lbShares = -5000;               
    	protected Integer ubShares = 5000;       
    	protected SimpleMatrix spreadTransitionProbabMatrix = null;
    	protected Map<StrategyBid,Map<Integer,Double>> proxiesBid = new HashMap<>();
    	protected Map<StrategyAsk,Map<Integer,Double>> proxiesAsk = new HashMap<>();
    	protected Integer delay = 4;
    	protected Double volumeStep = 10d;
    	protected Path outputDir;
    	protected String testName;
		
		public AbstractBuilder(File file,Double tick, Double vol, Path outputDir,String testName) throws IOException {
			this.tick = tick;
			List<DataTypePriceAmountMarkord> tpcl = CSV.getList(file, DataTypePriceAmountMarkord.class, 1);
			this.spreadTransitionProbabMatrix = ParametersEstimator.getEstimatedSpreadTransitionProbabilityMatrix(ParametersEstimator.getSpreadList(tpcl),this.tick,0d,4d);
			Map<String,Map<Integer,Double>> proxies = ParametersEstimator.getEstimatedExecutionParameters(ParametersEstimator.getSpreadList(tpcl),this.tick,vol);
			this.proxiesBid.put(StrategyBid.LIMIT,proxies.get("b"));
			this.proxiesBid.put(StrategyBid.MARKET,proxies.get("b+"));
			this.proxiesAsk.put(StrategyAsk.LIMIT,proxies.get("a"));
			this.proxiesAsk.put(StrategyAsk.MARKET,proxies.get("a-"));
			this.outputDir = outputDir;
			this.testName = testName;
		}
		
		public AbstractBuilder startTime(Integer startTime){
            this.startTime = startTime;
            return this;
        }
		public AbstractBuilder endTime(Integer endTime){
            this.endTime = endTime;
            return this;
        }
    	public AbstractBuilder tick(Double tick){
            this.tick = tick;
            return this;
        }
		public AbstractBuilder rho(Double rho){
            this.rho = tick;
            return this;
        }
		public AbstractBuilder epsilon(Double epsilon){
            this.epsilon = epsilon;
            return this;
        }
		public AbstractBuilder epsilon0(Double epsilon0){
            this.epsilon0 = epsilon0;
            return this;
        }
		public AbstractBuilder gamma(Double gamma){
            this.gamma = gamma;
            return this;
        }
		public AbstractBuilder maxVolM(Integer maxVolM){
            this.maxVolM = maxVolM;
            return this;
        }
		public AbstractBuilder maxVolT(Double maxVolT){
            this.maxVolT = maxVolT;
            return this;
        }
		public AbstractBuilder numOfTimeStep(Integer numOfTimeStep){
            this.numOfTimeStep = numOfTimeStep;
            return this;
        }
		public AbstractBuilder lbShares(Integer lbShares){
            this.lbShares = lbShares;
            return this;
        }
		public AbstractBuilder ubShares (Integer ubShares ){
            this.ubShares  = ubShares ;
            return this;
        }
		public AbstractBuilder volumeStep(Double volumeStep){
            this.volumeStep = volumeStep;
            return this;
        }
	}
	
	
	
	OptimalMMPolicyFrameworkAbstract(Integer startTime,Integer endTime,Double tick,Double rho,Double epsilon,
			Double epsilon0,Map<Integer,Double> lambda_t,Double gamma,Integer maxVolM,Double maxVolT,
			Integer timeStep,Integer lbShares,Integer ubShares,Map<StrategyBid,Map<Integer,Double>> proxiesBid,
			Map<StrategyAsk,Map<Integer,Double>> proxiesAsk,SimpleMatrix spreadTransitionProbabMatrix,Integer delay,Double volumeStep,
			Path outputDir, String testName)
	{
		 this.startTime = startTime;
		 this.endTime = endTime;
		 this.tick = tick;                     
		 this.rho = rho;                      
		 this.epsilon = epsilon;                  
		 this.epsilon0 = epsilon0;                 
		 this.lambda_t = lambda_t;           
		 this.gamma = gamma;                    
		 this.maxVolM = maxVolM;                 
		 this.maxVolT = maxVolT;  
		 this.timeStep = timeStep;
		 this.volumeStep = volumeStep;
		 this.numOfTimeStep = (int)(endTime-startTime)/timeStep;
		 this.lbShares = lbShares;                
		 this.ubShares = ubShares;    
		 this.numOfInventorySteps = (int)Math.ceil((this.ubShares - this.lbShares)/this.volumeStep);
	     this.minInvIndex =  (int)Math.floor(this.numOfInventorySteps/2);
	     this.spreadTransitionProbabMatrix = spreadTransitionProbabMatrix;
	     this.numOfTickSteps = this.spreadTransitionProbabMatrix.numRows();      //guarda gli 0
	     this.delay = delay;
	     this.spreadTransitionProbabMatrixNSteps = matrixPow(this.spreadTransitionProbabMatrix,this.delay);
	     this.numOfMaxVolMStep = (int)Math.ceil(this.maxVolM/this.volumeStep);
	     this.numOfMaxVolTStep = (int)Math.ceil(this.maxVolT/this.volumeStep);
	     this.proxiesBid = proxiesBid;
	     this.proxiesAsk = proxiesAsk;
	     this.outputDir = outputDir;
	     this.testName = testName;
	     initialize();
	    
	     }
	
	
	//must be a non-negative convex function
	public abstract Double penaltyFunction(Integer i);
	public abstract Double utilityFunction();
	
	public void initialize() {
		proxiesBidProb.put(StrategyBid.LIMIT, new HashMap<>());
		proxiesBidProb.put(StrategyBid.MARKET, new HashMap<>());
		proxiesAskProb.put(StrategyAsk.LIMIT, new HashMap<>());
		proxiesAskProb.put(StrategyAsk.MARKET, new HashMap<>());
		setProxiesProb();
	}
	
	@Override
	public void run() {
		//time, inventory, spread
		Double[][][] valueFunction = new Double[numOfTimeStep][this.numOfInventorySteps][numOfTickSteps];
		Policy[][][] bestPolicy = new Policy[numOfTimeStep][this.numOfInventorySteps][numOfTickSteps];
		
		//at last time step the inventory must be 0
		for(int i = 0; i<this.numOfInventorySteps;i++) {
			for(int j =0;j<this.numOfTickSteps;j++) {
				valueFunction[numOfTimeStep-1][i][j] = - Math.abs(getInventory(i))*((j+1)*this.tick/2) - this.epsilon0 ;
			}
		}
		
		//previous time steps
		for(int t = numOfTimeStep-2;t>=0;t--) {
			for(int i = 0; i<this.numOfInventorySteps;i++) {
				for(int j =0;j<this.numOfTickSteps;j++) {
					getPi(bestPolicy,valueFunction,t,i,j);
				}
			}
		}
		try {
			writeOutput(bestPolicy,null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param bestPolicy
	 * @param valueFunction
	 * @param time
	 * @param invent
	 * @param spread
	 * @return
	 */
	private Object[] getPi(Policy[][][] bestPolicy,Double[][][] valueFunction, Integer time, Integer invent, Integer spread) {
		
		Object[] piTilde = getPiTilde(valueFunction,time,invent,spread);
		Double[] markOrd = getSupMarkord(valueFunction,time,invent,spread);
		Double res = Math.max((double)piTilde[0], markOrd[0]);
		valueFunction[time][invent][spread] = res;
		
		if(res.equals(markOrd[0])) {
			Policy p = new Policy.Builder()
					.take(true)
					.volumeTake(markOrd[1])
					.build();
			bestPolicy[time][invent][spread] = p;
		}
		else {
			Policy p = new Policy.Builder()
					.make(true)
					.bidStrategy((StrategyBid) piTilde[1])
					.bidVolume((double)piTilde[2])
					.askStrategy((StrategyAsk) piTilde[3])
					.askVolume((double)piTilde[4])
					.build();
			bestPolicy[time][invent][spread] = p;
		}
		
		return null;
	}
	
	private Object[] getPiTilde(Double[][][] valueFunction, Integer time, Integer invent, Integer spread) {
		
		Object[] supBid = getSupBid(valueFunction,time,invent,spread);
		Object[] supAsk = getSupAsk(valueFunction,time,invent,spread);
		
		Double res = 0.25*(valueFunction[time+1][invent][spread]
				+getSpreadExpVal(valueFunction,time,invent,spread)
				+(double)supBid[0]
				+(double)supAsk[0]
				-this.timeStep*this.gamma*penaltyFunction(invent));   //TODO extenal policy this.timeStep*this.gamma*(Math.pow(getInventory(invent),2))
		
		return new Object[] {res,supBid[1],supBid[2],supAsk[1],supAsk[2]};
	}
	
	private Double getSpreadExpVal(Double[][][] valueFunction, Integer time, Integer invent, Integer spread) {
		Double res = 0d;
		for(int i=0;i<this.numOfTickSteps;i++) {
			Double p = this.spreadTransitionProbabMatrixNSteps.get(spread,i);
			res += p * valueFunction[time+1][invent][spread];
		} 
		return res;
	}
	
	private Object[] getSupBid(Double[][][] valueFunction, Integer time, Integer invent, Integer spread) {
		
		StrategyBid bestStrategy = null;
		Double bestVolume = 0d;
		Double bestResult = -Double.MAX_VALUE;
		
		for(int i = 0;i<this.numOfMaxVolMStep;i++) {
			for(StrategyBid sb: StrategyBid.values()){
				Double currResult = 0d;
				
				if(sb.equals(StrategyBid.MARKET)) {
					Double proxyVal = this.proxiesBidProb.get(StrategyBid.MARKET).get(spread);
					proxyVal = proxyVal == null? 0: proxyVal;   //TODO
					currResult += (spread*this.tick/2);
					currResult -=  this.tick;
					currResult = currResult* i*this.volumeStep;
					currResult = currResult * proxyVal;
					Integer inventoryIndex = getInventoryIndex(getInventory(invent)+i*this.volumeStep);
					currResult += inventoryIndex > this.numOfInventorySteps-1 ?
							-Double.MAX_VALUE: valueFunction[time+1][inventoryIndex][spread]*proxyVal; 
				}
				else {
					Double proxyVal = this.proxiesBidProb.get(StrategyBid.LIMIT).get(spread);
					proxyVal = proxyVal == null? 0: proxyVal; 
					currResult += (spread*this.tick/2);
					currResult = currResult * i * this.volumeStep;
					currResult = currResult * proxyVal;
					Integer inventoryIndex = getInventoryIndex(getInventory(invent)+i*this.volumeStep);
					currResult += inventoryIndex > this.numOfInventorySteps-1 ? 
							-Double.MAX_VALUE: valueFunction[time+1][inventoryIndex][spread]*proxyVal; 
					
				}
				//update
				if(currResult > bestResult) {
					bestResult = currResult;
					bestStrategy = sb;
					bestVolume = i*this.volumeStep;
				}
			}		
		}
		return new Object[] {bestResult,bestStrategy,bestVolume};
	}
	
	private Object[] getSupAsk(Double[][][] valueFunction, Integer time, Integer invent, Integer spread) {
		StrategyAsk bestStrategy = null;
		Double bestVolume = 0d;
		Double bestResult = -Double.MAX_VALUE;
		
		for(int i = 1;i<this.numOfMaxVolMStep;i++) {
			for(StrategyAsk sb: StrategyAsk.values()){
				Double currResult = 0d;
				if(sb.equals(StrategyAsk.MARKET)) {
					Double proxyVal = this.proxiesAskProb.get(StrategyAsk.MARKET).get(spread);
					proxyVal = proxyVal == null? 0: proxyVal; 
					currResult += (spread*this.tick/2);
					currResult -=  this.tick;
					currResult = currResult* i*this.volumeStep;
					currResult = currResult * proxyVal;
					Integer inventoryIndex = getInventoryIndex(getInventory(invent)-i*this.volumeStep);
					currResult += inventoryIndex < 0 ? -Double.MAX_VALUE: valueFunction[time+1][inventoryIndex][spread]*proxyVal; 
				}
				else {
					Double proxyVal = this.proxiesAskProb.get(StrategyAsk.LIMIT).get(spread);
					proxyVal = proxyVal == null? 0: proxyVal; 
					currResult += (spread*this.tick/2);
					currResult = currResult * i * this.volumeStep;
					currResult = currResult * proxyVal;
					Integer inventoryIndex = getInventoryIndex(getInventory(invent)-i*this.volumeStep);
					currResult += inventoryIndex < 0 ? -Double.MAX_VALUE: valueFunction[time+1][inventoryIndex][spread]*proxyVal; 
				}
				if(currResult > bestResult) {
					bestResult = currResult;
					bestStrategy = sb;
					bestVolume = i*this.volumeStep;
				}
			}		
		}
		return new Object[] {bestResult,bestStrategy,bestVolume};
	}
	
	private Double[] getSupMarkord(Double[][][] valueFunction, Integer time, Integer invent, Integer spread) {
		
		Double bestResult = -Double.MAX_VALUE;
		Double bestVolumeT = 0d;
		for(int i = -this.numOfMaxVolTStep;i<=this.numOfMaxVolTStep;i++) {
			Double currResult = 0d;
			Integer inventoryIndex = getInventoryIndex(getInventory(invent)+(i * this.volumeStep));
			if(inventoryIndex > this.numOfInventorySteps-1 || inventoryIndex<0) {
				currResult = -Double.MAX_VALUE;
			}
			else {
				currResult = (-spread*this.tick/2) * Math.abs(i * this.volumeStep) - this.epsilon0 +
						valueFunction[time+1][inventoryIndex][spread];
			}
						
			if(currResult > bestResult) {
				bestResult = currResult;
				bestVolumeT = i * this.volumeStep;
			}
		}
		return new Double[] {bestResult,bestVolumeT};
	}
	
	private SimpleMatrix matrixPow(SimpleMatrix matrix, Integer p){
		SimpleMatrix newMatrix = new SimpleMatrix(matrix);
		for(int i=1;i<p;i++) {
			newMatrix = newMatrix.mult(matrix);
		}
		return newMatrix;
	}
	
	protected Double getInventory(Integer i) {
		 Double d = (i-this.minInvIndex)*this.volumeStep;
		 return d;
	}
	
	protected Integer getInventoryIndex(Double v) {
		Integer i= (int)Math.floor(v/this.volumeStep) + this.minInvIndex;
		return i;
	}
	
	@SuppressWarnings("unchecked")
	private void setProxiesProb() {
		
		Iterator<Map.Entry<StrategyBid,Map<Integer,Double>>> entryStratBidIter = proxiesBid.entrySet().iterator();
		while (entryStratBidIter.hasNext()) {
			Map.Entry<StrategyBid,Map<Integer,Double>> entryStratBidPair = entryStratBidIter.next();
		    Iterator<Map.Entry<Integer,Double>> spreadIter = (entryStratBidPair.getValue()).entrySet().iterator();
		    while (spreadIter.hasNext()) {
		        Map.Entry<Integer,Double> spreadPair = spreadIter.next();
		        Double l = proxiesBid.get(entryStratBidPair.getKey()).get(spreadPair.getKey());
				Double p = 1-Math.exp(-(l*this.delay*this.timeStep));                                        
				proxiesBidProb.get(entryStratBidPair.getKey()).put(spreadPair.getKey(),p);
		    }
		}
		
		Iterator<Map.Entry<StrategyAsk,Map<Integer,Double>>> entryStratAskIter = proxiesAsk.entrySet().iterator();
		while (entryStratAskIter.hasNext()) {
			Map.Entry<StrategyAsk,Map<Integer,Double>> entryStratAskPair = entryStratAskIter.next();
		    Iterator<Map.Entry<Integer,Double>> spreadIter = (entryStratAskPair.getValue()).entrySet().iterator();
		    while (spreadIter.hasNext()) {
		        Map.Entry<Integer,Double> spreadPair = spreadIter.next();
		        Double l = proxiesAsk.get(entryStratAskPair.getKey()).get(spreadPair.getKey());
				Double p = 1-Math.exp(-(l*this.delay*this.timeStep));                                
				proxiesAskProb.get(entryStratAskPair.getKey()).put(spreadPair.getKey(),p);
		    }
		}
	}
	
	private String printBestPolicy(Policy[][][] bestPolicy) {

		StringBuilder str = new StringBuilder();
		str.append("time,inventory,spread,bid_strategy,ask_strategy,bid_ord_vol,ask_ord_vol,take_vol");
		str.append("\n");
		for(int t = this.numOfTimeStep-2;t>=0;t--) {
			for(int i = 0; i<this.numOfInventorySteps;i++) {
				for(int j =0;j<this.numOfTickSteps;j++) {
					str.append(String.valueOf(t*this.timeStep));
					str.append(",");
					str.append(String.valueOf(getInventory(i)));
					str.append(",");
					str.append(String.valueOf((j+1)*this.tick));
					str.append(",");
					str.append(bestPolicy[t][i][j].print());
					str.append("\n");
				}
			}
		}
		return str.toString();
	}
	
	
	public String printInputData() {
		
		StringBuilder str = new StringBuilder();
		str.append("Model input parameters report");
		str.append("\n");
		str.append("Name | Description | value");
		str.append("\n");
		
		for(Field f: this.getClass().getSuperclass().getDeclaredFields()) {
			if(f.isAnnotationPresent(ModelParameter.class)) {
				ModelParameter osr = f.getDeclaredAnnotation(ModelParameter.class);
				try {
					str.append(osr.name());
					str.append(" | ");
					str.append(osr.description());
					str.append(" | ");
					str.append(f.get(this));
					str.append("\n");
				} catch (IllegalAccessException | IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
		return str.toString();
	}
	
	private void writeOutput(Policy[][][] bestPolicy,String name) throws IOException {
		
		String testDir = this.outputDir.toString();
		testDir += name != null ? "/"+name :  "/"+UUID.randomUUID().toString() ;
		File testDir_ = new File(testDir);     
		testDir_.mkdirs();  
		
		//model input parameters
		try {
			File tm = new File( new StringBuilder()
					.append(testDir)
					.append("/model_input_parameters.csv")
					.toString());
			CSV.writeTo(tm,printInputData());
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		} 
		
		//print spread transition matrix
		try {
			File tm = new File( new StringBuilder()
					.append(testDir)
					.append("/spread_transition_matrix.csv")
					.toString());
			CSV.writeTo(tm,this.spreadTransitionProbabMatrix.toString());
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		} 
		
		//print bid proxies
		try {
			File pr = new File( new StringBuilder()
					.append(testDir)
					.append("/proxies_bid.csv")
					.toString());
			CSV.writeTo(pr,Utils.printProxiesBid(this.proxiesBid));
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		} 
		
		//print bid proxies prob.
		try {
			File pr = new File( new StringBuilder()
					.append(testDir)
					.append("/proxies_bid_prob.csv")
					.toString());
			CSV.writeTo(pr,Utils.printProxiesBid(this.proxiesBidProb));
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		}  
		
		//print bid proxies
		try {
			File pr = new File( new StringBuilder()
					.append(testDir)
					.append("/proxies_ask.csv")
					.toString());
			CSV.writeTo(pr,Utils.printProxiesAsk(this.proxiesAsk));
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		} 
		
		//print bid proxies prob.
		try {
			File pr = new File( new StringBuilder()
					.append(testDir)
					.append("/proxies_ask_prob.csv")
					.toString());
			CSV.writeTo(pr,Utils.printProxiesAsk(this.proxiesAskProb));
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		}  
		
		//print the best policy
		try {
			File of = new File( new StringBuilder()
					.append(testDir)
					.append("/bestPolicy.csv")
					.toString());
			CSV.writeTo(of,printBestPolicy(bestPolicy));
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		}   
	}
	
}
