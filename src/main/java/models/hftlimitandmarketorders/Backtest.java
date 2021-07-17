package models.hftlimitandmarketorders;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.ejml.simple.SimpleMatrix;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import data.utils.CSV;
import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyAsk;
import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyBid;

public class Backtest {
	
	private static RandomGenerator generator = new Well44497b(29756);
	
	private final Integer runs;
	private final BigDecimal initialPrice;
	private final Integer periods;
	private final Double drift;
	private final Double sigma;
	private final List<Double> mp;
	private final Map<Integer,Double> lambda;
	private final SimpleMatrix spreadTransitionProbabMatrix;
	private final BigDecimal tick;
	private final Double step;
	private Map<BigDecimal,EnumeratedIntegerDistribution> mmprob; 
	private TreeMap<Integer,Double> lambda_t;
	
	private Map<StrategyBid,TreeMap<BigDecimal,Double>> proxiesBid;
	private Map<StrategyAsk,TreeMap<BigDecimal,Double>> proxiesAsk;
	
	/**
	 * @param bestPolicy
	 * @param runs
	 * @param initialPrice
	 * @param periods
	 * @param drift
	 * @param sigma
	 * @param mp
	 * @param lambda
	 * @param spreadTransitionProbabMatrix
	 * @param tick
	 */
	public Backtest(Integer runs, BigDecimal initialPrice, Integer periods,Double step,Double drift, 
			Double sigma, List<Double> mp, Map<Integer,Double> lambda,
			SimpleMatrix spreadTransitionProbabMatrix, BigDecimal tick,
			Map<StrategyBid,TreeMap<BigDecimal,Double>> proxiesBid,
			Map<StrategyAsk,TreeMap<BigDecimal,Double>> proxiesAsk) {
		
		this.runs = runs;
		this.initialPrice=initialPrice;
		this.periods=periods;
		this.step = step;
		this.drift=drift;
		this.sigma=sigma;
		this.mp=mp;
		this.lambda=lambda;
		this.spreadTransitionProbabMatrix=spreadTransitionProbabMatrix;
		this.tick=tick;
		this.proxiesBid=proxiesBid;
		this.proxiesAsk=proxiesAsk;
		initialize();
	}
	
	private void initialize() {
		
		mmprob = new HashMap<>();
		int nrows = spreadTransitionProbabMatrix.numRows();
		for(int i =0;i<nrows;i++) {
			int[] support = new int[nrows];
			double[] prob = new double[nrows];
			for(int j = 0;j<nrows;j++) {
				support[j] = j+1;
				prob[j] = spreadTransitionProbabMatrix.get(i,j);
			}
			if(Arrays.stream(prob).sum() != 0) {
				mmprob.put(tick.multiply(BigDecimal.valueOf(i+1)), new EnumeratedIntegerDistribution(support,prob));
			}
			else {   //if the spread transitions have not been observed a uniform dist is used
				Double u = 1d/nrows;
				Arrays.fill(prob,u);
				mmprob.put(tick.multiply(BigDecimal.valueOf(i+1)), new EnumeratedIntegerDistribution(support,prob));
			}
			
		}
	    lambda_t = new TreeMap<>();
		lambda_t.putAll(lambda);
		
	}
	
	public void run(TreeMap<Double,TreeMap<Double,TreeMap<Double,Policy>>> bestPolicy, String outputDirTest) {
		
		List<BestPolicyStat> res = new ArrayList<>();
		for(int i=0;i<runs;i++) {
			
			double cash = 0;
			double currTotInventory = 0;
			int nBestAskOrders = 0;
			int nNewAskOrders = 0;
			int nBestBidOrders = 0;
			int nNewBidOrders = 0;
			int nMarketBuyOrders = 0;
			int nMarketSellOrders = 0;
			double maxInventory = 0d;
			double minInventory = 0d;
			double maxGainSingleTrade = 0d;
			double maxLossSingleTrade = 0d;
			
			
			TreeMap<BigDecimal,Double> longPositions = new TreeMap<>(); 
			TreeMap<BigDecimal,Double> shortPositions = new TreeMap<>(Collections.reverseOrder());
			
			List<BidAsk> newDataset = getSimulatedBidAskList();
			
			//iterate through the simulated bid-ask
			for(BidAsk ba: newDataset) {
				BigDecimal spread = ba.ask.subtract(ba.bid);
				
				//Inventory update
				currTotInventory = longPositions.values().stream().mapToDouble(Double::doubleValue).sum() -
						shortPositions.values().stream().mapToDouble(Double::doubleValue).sum();
				
				//update stat
				maxInventory= maxInventory< currTotInventory? currTotInventory: maxInventory;
				minInventory= minInventory> currTotInventory? currTotInventory: minInventory;
				
				Policy policy = bestPolicy.floorEntry(ba.getTime())
						.getValue().floorEntry(currTotInventory)
						.getValue().floorEntry(spread.doubleValue())
						.getValue();
				

				/**
				 * When the spread is equal to the tick
				 * 1)BPLUS = buy at best ask
				 * 2)AMINUS = sell at best bid
				 */
				boolean makeBid = false;
				boolean makeAsk = false;
				
				if(policy.getMake()) {
					makeBid = true;
					makeAsk = true;
					if(policy.getBidStrategy() == StrategyBid.BPLUS && spread.compareTo(tick)==-1) {
						makeBid = false;
					}
					if(policy.getAskStrategy() == StrategyAsk.AMINUS && spread.compareTo(tick)==-1) {
						makeAsk = false;
					}
				}
				
				//-------------------------LIMIT Strategy-------------------------
				if(makeBid) {
					//BID
					//order closed intensity based on the policy results 
					double intensityBid = proxiesBid.get(policy.getBidStrategy())
							.floorEntry(spread).getValue(); 
					double pstratBid = 1-Math.exp(-(intensityBid*step)); 
					double volBid = policy.getBidVolume(); 
					//orders not closed are not considered 
					if(generator.nextDouble()<pstratBid) {
						//update long/short positions
						cash += makeBidUpdatePositions(longPositions,shortPositions,volBid,ba.getBid());
				        //update stat
				        if(policy.getBidStrategy().equals(StrategyBid.BPLUS)) {
				        	nNewBidOrders++;
				        }
				        else {
				        	nBestBidOrders++;
				        }
					}
				}
				if(makeAsk) {
					//ASK
					//order closed intensity based on the policy results 
					double intensityAsk = proxiesAsk.get(policy.getAskStrategy()).floorEntry(spread).getValue();
					double pstratAsk = 1-Math.exp(-(intensityAsk*step)); 
					double volAsk = policy.getAskVolume();
					//orders not closed are not considered 
					if(generator.nextDouble()<pstratAsk) {
						cash+=makeAskUpdatePositions(longPositions, shortPositions,volAsk,ba.getAsk());
				        //update stat
				        if(policy.getAskStrategy().equals(StrategyAsk.AMINUS)) {
				        	nNewAskOrders++;
				        }
				        else {
				        	nBestAskOrders++;
				        }
					}
				}
			
				//-------------------------MARKET strategy-------------------------
				//BUY
				if((policy.getTake() && policy.getVolumeTake() >0) || (policy.getMake() && !makeBid)) {   
					double vol = (policy.getMake() && !makeBid) ? policy.getBidVolume() : policy.getVolumeTake();
					cash += takeBuyUpdatePositions(longPositions,shortPositions,vol,ba.getAsk());
			        //stat
			        ++nMarketBuyOrders;
					
				}
				//SELL
				if((policy.getTake() && policy.getVolumeTake() <0) || (policy.getMake() && !makeAsk)) {
					double vol = (policy.getMake() && !makeAsk) ? policy.getAskVolume() : policy.getVolumeTake();
					cash += takeSellUpdatePositions(longPositions,shortPositions,vol,ba.getBid());
			      //stat
			      ++nMarketSellOrders;
				}	
			}
			res.add(new BestPolicyStat(cash,nBestAskOrders,nNewAskOrders,
					nBestBidOrders,nNewBidOrders, nMarketBuyOrders,
					nMarketSellOrders,maxInventory, minInventory));
		}
		
		writeBestPolicyStat(printBestPolicyStat(res),outputDirTest);
		
		
		System.out.println();
	}
	
	private double makeBidUpdatePositions(TreeMap<BigDecimal,Double> longPositions,TreeMap<BigDecimal,Double> shortPositions,double volBid,BigDecimal bidPrice) {
		
		Set set = shortPositions.entrySet();
        Iterator iter = set.iterator();
        boolean exit = false;
        double cash = 0;
        HashMap<BigDecimal,Double> shortPositionsUpdate = new HashMap<>();
        //first close opened short positions
        while (iter.hasNext() && !exit) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iter.next();
           cash += (me.getKey().subtract(bidPrice).doubleValue() * (volBid/bidPrice.doubleValue()));
           if(volBid > me.getValue()) {
        	   volBid -= me.getValue();
        	   shortPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
           }
           else {
        	   shortPositionsUpdate.put(me.getKey(),me.getValue()-volBid); //entire position is eliminated
        	   volBid = 0d;
        	   exit = true;
           }
        }
        //if the shorts are not enough open a new long position
        if(volBid>0) {
        	if(longPositions.containsKey(bidPrice)) {
        		longPositions.put(bidPrice,volBid+longPositions.get(bidPrice));
        	}
        	else {
        		longPositions.put(bidPrice,volBid);
        	}
        }
        //update ShortPositions
        Set setSPU = shortPositionsUpdate.entrySet();
        Iterator iterSPU = set.iterator();
        while (iterSPU.hasNext()) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iterSPU.next();
           if(me.getValue() != -1) {
        	   shortPositions.put(me.getKey(),me.getValue());
           }
           else {
        	   shortPositions.remove(me.getKey());
           }
        }
		
		return cash;
	}
	
	private double makeAskUpdatePositions(TreeMap<BigDecimal,Double> longPositions,TreeMap<BigDecimal,Double> shortPositions,double volAsk,BigDecimal askPrice) {
		Set set = longPositions.entrySet();
        Iterator iter = set.iterator();
        boolean exit = false;
        double cash = 0;
        HashMap<BigDecimal,Double> longPositionsUpdate = new HashMap<>();
        while (iter.hasNext() && !exit) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iter.next();
           cash += (askPrice.subtract(me.getKey()).doubleValue() * (volAsk/askPrice.doubleValue()));
           if(volAsk > me.getValue()) {
        	   volAsk -= me.getValue();
        	   longPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
           }
           else {
        	   longPositionsUpdate.put(me.getKey(),me.getValue()-volAsk); //entire position is eliminated
        	   volAsk = 0d;
        	   exit = true;
           }
        }
        //if the longs are not enough, open a new short position
        if(volAsk>0) {
        	if(shortPositions.containsKey(askPrice)) {
        		shortPositions.put(askPrice,volAsk+shortPositions.get(askPrice));
        	}
        	else {
        		shortPositions.put(askPrice,volAsk);
        	}
        }
        //update LongPositions
        Set setLPU = longPositionsUpdate.entrySet();
        Iterator iterLPU = set.iterator();
        while (iterLPU.hasNext()) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iterLPU.next();
           if(me.getValue() != -1) {
        	   longPositions.put(me.getKey(),me.getValue());
           }
           else {
        	   longPositions.remove(me.getKey());
           }
        }
		return cash;
		
	}
	
	private double takeBuyUpdatePositions(TreeMap<BigDecimal,Double> longPositions,TreeMap<BigDecimal,Double> shortPositions,double vol,BigDecimal askPrice) {
		
		Set set = shortPositions.entrySet();
        Iterator iter = set.iterator();
        boolean exit = false;
        double cash = 0;
        HashMap<BigDecimal,Double> shortPositionsUpdate = new HashMap<>();
        while (iter.hasNext() && !exit) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iter.next();
           cash += (me.getKey().subtract(askPrice).doubleValue() * (vol/askPrice.doubleValue()));
           if(vol > me.getValue()) {
        	   vol -= me.getValue();
        	   shortPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
           }
           else {
        	   shortPositionsUpdate.put(me.getKey(),me.getValue()-vol); //entire position is eliminated
        	   vol = 0d;
        	   exit = true;
           }
        }
        //if the shorts are not enough, open a new long position
        if(vol>0) {
        	if(longPositions.containsKey(askPrice)) {
        		longPositions.put(askPrice,vol+longPositions.get(askPrice));
        	}
        	else {
        		longPositions.put(askPrice,vol);
        	}
        }
        //update ShortPositions
        Set setSPU = shortPositionsUpdate.entrySet();
        Iterator iterSPU = setSPU.iterator();
        while (iterSPU.hasNext()) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iterSPU.next();
           if(me.getValue() != -1) {
        	   shortPositions.put(me.getKey(),me.getValue());
           }
           else {
        	   shortPositions.remove(me.getKey());
           }
        }
		
		return 0;
		
	}
	
	private double takeSellUpdatePositions(TreeMap<BigDecimal,Double> longPositions,TreeMap<BigDecimal,Double> shortPositions,double vol,BigDecimal bidPrice) {
		vol = Math.abs(vol);
		Set set = longPositions.entrySet();
        Iterator iter = set.iterator();
        boolean exit = false;
        double cash = 0;
        HashMap<BigDecimal,Double> longPositionsUpdate = new HashMap<>();
        while (iter.hasNext() && !exit) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iter.next();
           cash += bidPrice.subtract(me.getKey()).doubleValue() * (vol/bidPrice.doubleValue());
           if(vol > me.getValue()) {
        	   vol -= me.getValue();
        	   longPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
           }
           else {
        	   longPositionsUpdate.put(me.getKey(),me.getValue()-vol); //entire position is eliminated
        	   vol = 0d;
        	   exit = true;
           }
        }
        //if the longs are not enough, open a new short position
        if(vol>0) {
        	if( shortPositions.containsKey(bidPrice)) {
        		shortPositions.put(bidPrice,vol+shortPositions.get(bidPrice));
        	}
        	else {
        		shortPositions.put(bidPrice,vol);
        	}
        }
        //update LongPositions
        Set setLPU = longPositionsUpdate.entrySet();
        Iterator iterLPU = setLPU.iterator();
        while (iterLPU.hasNext()) {
           Map.Entry<BigDecimal,Double> me = (Map.Entry)iterLPU.next();
           if(me.getValue() != -1) {
        	   longPositions.put(me.getKey(),me.getValue());
           }
           else {
        	   longPositions.remove(me.getKey());
           }
        }	
		return cash;
	}
	
	/**
	 * @return
	 */
	public List<BidAsk> getSimulatedBidAskList(){
		List<BidAsk> res = new ArrayList<>();
		BigDecimal spread = BigDecimal.valueOf(tick.doubleValue());
		for (int i = 1; i < periods; i++) {
			//geom brownian
			NormalDistribution nd = new NormalDistribution(generator,0.0,i);
			BigDecimal midPrice = initialPrice.multiply(BigDecimal.valueOf(Math.exp((drift- Math.pow(sigma, 2)/2)*i*step+sigma*nd.sample())));
			Double p = 1-Math.exp(-(lambda_t.get(lambda_t.floorKey(i)))*step); 
			//spread -> Poisson
			if(generator.nextDouble()<=p) {
				//transition matrix 
				EnumeratedIntegerDistribution ei =  mmprob.get(spread);
				Integer sample = mmprob.get(spread).sample();
				spread = tick.multiply(BigDecimal.valueOf(sample));
			}
			BidAsk ba = new BidAsk(i*step,midPrice.subtract(spread.divide(BigDecimal.valueOf(2))), midPrice.add(spread.divide(BigDecimal.valueOf(2))));
			res.add(ba);
        }
		return res;
	}
	
	private void stat(List<BestPolicyStat> res ) {
		//STAT
		double avgCash = res.stream()
				.mapToDouble(BestPolicyStat::getCash)
				.average()
				.orElse(Double.NaN);
		
		double varianceCash = res.stream()
				.mapToDouble(BestPolicyStat::getCash)
                .map(i -> i - avgCash)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdCash = Math.sqrt(varianceCash);
		
		double avgMaxInventory = res.stream()
				.mapToDouble(BestPolicyStat::getMaxInventory)
				.average()
				.orElse(Double.NaN);
		
		double varianceMaxInventory = res.stream()
				.mapToDouble(BestPolicyStat::getMaxInventory)
                .map(i -> i - avgMaxInventory)
                .map(i -> i*i)
                .average().getAsDouble();

		double sdMaxInventory = Math.sqrt(varianceMaxInventory);
		
		double avgMinInventory = res.stream()
				.mapToDouble(BestPolicyStat::getMinInventory)
				.average()
				.orElse(Double.NaN);
		
		double varianceMinInventory = res.stream()
				.mapToDouble(BestPolicyStat::getMinInventory)
                .map(i -> i - avgMinInventory)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdMinInventory = Math.sqrt(varianceMinInventory);
		
		double avgNBestBidOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNBestBidOrders)
				.average()
				.orElse(Double.NaN);
		
		double varianceNBestBidOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNBestBidOrders)
                .map(i -> i - avgNBestBidOrders)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdNBestBidOrders = Math.sqrt(varianceNBestBidOrders);
		
		double avgNNewBidOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNNewBidOrders)
				.average()
				.orElse(Double.NaN);
		
		double varianceNNewBidOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNNewBidOrders)
                .map(i -> i - avgNNewBidOrders)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdNNewBidOrders = Math.sqrt(varianceNNewBidOrders);
		
		double avgNBestAskOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNBestAskOrders)
				.average()
				.orElse(Double.NaN);
		
		double varianceNBestAskOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNBestAskOrders)
                .map(i -> i - avgNBestAskOrders)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdNBestAskOrders = Math.sqrt(varianceNBestBidOrders);
		
		double avgNNewAskOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNNewAskOrders)
				.average()
				.orElse(Double.NaN);
		
		double varianceNNewAskOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNNewAskOrders)
                .map(i -> i - avgNNewBidOrders)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdNNewAskOrders = Math.sqrt(varianceNNewAskOrders);
		
		double avgNMarketBuyOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNMarketBuyOrders)
				.average()
				.orElse(Double.NaN);
		
		double varianceNMarketBuyOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNMarketBuyOrders)
                .map(i -> i - avgNMarketBuyOrders)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdNMarketBuyOrders = Math.sqrt(varianceNMarketBuyOrders);
		
		double avgNMarketSellOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNMarketSellOrders)
				.average()
				.orElse(Double.NaN);
		
		double varianceNMarketSellOrders = res.stream()
				.mapToDouble(BestPolicyStat::getNMarketSellOrders)
                .map(i -> i - avgNMarketSellOrders)
                .map(i -> i*i)
                .average().getAsDouble();
		
		double sdNMarketSellOrders = Math.sqrt(varianceNMarketSellOrders);
		
		double informationRatio = avgCash/sdCash;
		
		
		System.out.println();
				
	}
	
	private String printBestPolicyStat(List<BestPolicyStat> res) {
		StringBuilder str = new StringBuilder();
		str.append("cash,n_best_ask_orders,n_new_ask_orders,n_best_bid_orders,n_new_bid_orders,n_market_buy_orders,n_market_sell_orders,max_inventory,min_inventory");
		str.append("\n");
		for(BestPolicyStat bps: res) {
			str.append(bps.getCash());
			str.append(",");
			str.append(bps.getNBestAskOrders());
			str.append(",");
			str.append(bps.getNNewAskOrders());
			str.append(",");
			str.append(bps.getNBestBidOrders());
			str.append(",");
			str.append(bps.getNNewBidOrders());
			str.append(",");
			str.append(bps.getNMarketBuyOrders());
			str.append(",");
			str.append(bps.getNMarketSellOrders());
			str.append(",");
			str.append(bps.getMaxInventory());
			str.append(",");
			str.append(bps.getMinInventory());
			str.append("\n");
		}
		return str.toString();
	}
	
	private void writeBestPolicyStat(String s, String outputDirTest) {
		//print the best policy
		try {
			File of = new File( new StringBuilder()
					.append(outputDirTest)
					.append("/best_policy_backtest_stat.csv")
					.toString());
			CSV.writeTo(of,s);
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		}   
	}
	
	public class BestPolicyStat {
		private final Double cash;
		private final Integer nBestAskOrders;
		private final Integer nNewAskOrders;
		private final Integer nBestBidOrders;
		private final Integer nNewBidOrders;
		private final Integer nMarketBuyOrders;
		private final Integer nMarketSellOrders;
		private final Double maxInventory;
		private final Double minInventory;
		
		public BestPolicyStat( Double cash,Integer nBestAskOrders,Integer nNewAskOrders,
				Integer nBestBidOrders,Integer nNewBidOrders,Integer nMarketBuyOrders,
				Integer nMarketSellOrders,Double maxInventory,Double minInventory) {
			
			this.cash=cash;
			this.nBestAskOrders=nBestAskOrders;
			this.nNewAskOrders=nNewAskOrders;
			this.nBestBidOrders=nBestBidOrders;
			this.nNewBidOrders=nNewBidOrders;
			this.nMarketBuyOrders=nMarketBuyOrders;
			this.nMarketSellOrders=nMarketSellOrders;
			this.maxInventory=maxInventory;
			this.minInventory=minInventory;
		}
		
		public Double getCash() {
			return this.cash;
		}
		public Double getMaxInventory() {
			return this.maxInventory;
		}
		public Double getMinInventory() {
			return this.minInventory;
		}
		public Integer getNBestAskOrders() {
			return this.nBestAskOrders;
		}
		public Integer getNNewAskOrders() {
			return this.nNewAskOrders;
		}
		public Integer getNBestBidOrders() {
			return this.nBestBidOrders;
		}
		public Integer getNNewBidOrders() {
			return this.nNewBidOrders;
		}
		public Integer getNMarketBuyOrders() {
			return this.nMarketBuyOrders;
		}
		public Integer getNMarketSellOrders() {
			return this.nMarketSellOrders;
		}
	}
	
	
	public class BidAsk {
		private BigDecimal bid;
		private BigDecimal ask;
		private Double time;
		public BidAsk(Double time,BigDecimal bid,BigDecimal ask) {
			this.time = time;
			this.bid = bid;
			this.ask = ask;
		}
		public Double getTime() {
			return this.time;
		}
		public BigDecimal getBid() {
			return this.bid;
		}
		public BigDecimal getAsk() {
			return this.ask;
		}
	}

}
