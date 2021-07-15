package models.hftlimitandmarketorders;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.ejml.simple.SimpleMatrix;

import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyAsk;
import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyBid;

public class Backtest {
	
	private static RandomGenerator generator = new Well44497b(29756);
	
	private final Integer runs;
	private final Double initialPrice;
	private final Integer periods;
	private final Double drift;
	private final Double sigma;
	private final List<Double> mp;
	private final Map<Integer,Double> lambda;
	private final SimpleMatrix spreadTransitionProbabMatrix;
	private final Double tick;
	private final Double step;
	private Map<Double,EnumeratedRealDistribution> mmprob; 
	private TreeMap<Integer,Double> lambda_t;
	
	private Map<StrategyBid,TreeMap<Double,Double>> proxiesBid;
	private Map<StrategyAsk,TreeMap<Double,Double>> proxiesAsk;
	
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
	public Backtest(Integer runs, Double initialPrice, Integer periods,Double step,Double drift, 
			Double sigma, List<Double> mp, Map<Integer,Double> lambda,
			SimpleMatrix spreadTransitionProbabMatrix, Double tick,
			Map<StrategyBid,TreeMap<Double,Double>> proxiesBid,
			Map<StrategyAsk,TreeMap<Double,Double>> proxiesAsk) {
		
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
			double[] support = new double[nrows];
			double[] prob = new double[nrows];
			for(int j = 0;j<nrows;j++) {
				support[j] = (j+1)*tick;
				prob[j] = spreadTransitionProbabMatrix.get(i,j);
			}
			if(Arrays.stream(prob).sum() != 0) {
				mmprob.put((i+1)*tick, new EnumeratedRealDistribution(support,prob));
			}
			
		}
	    lambda_t = new TreeMap<>();
		lambda_t.putAll(lambda);
		
	}
	
	public void run(TreeMap<Double,TreeMap<Double,TreeMap<Double,Policy>>> bestPolicy) {
		
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
			
			TreeMap<Double,Double> longPositions = new TreeMap<>(); 
			TreeMap<Double,Double> shortPositions = new TreeMap<>(Collections.reverseOrder());
			
			List<BidAsk> newDataset = getBidAskList();
			for(BidAsk ba: newDataset) {
				double spread = ba.ask-ba.bid;
				//Inventory update
				currTotInventory = longPositions.values().stream().mapToDouble(Double::doubleValue).sum() -
						shortPositions.values().stream().mapToDouble(Double::doubleValue).sum();
				
				maxInventory= maxInventory< currTotInventory? currTotInventory: maxInventory;
				minInventory= minInventory> currTotInventory? currTotInventory: minInventory;
				
				Policy policy = bestPolicy.floorEntry(ba.getTime())
						.getValue().floorEntry(currTotInventory)
						.getValue().floorEntry(spread)
						.getValue();
				
				boolean makeBid = false;
				boolean makeAsk = false;
				
				
				if(policy.getMake()) {
					makeBid = true;
					makeAsk = true;
					if(policy.getBidStrategy().equals(StrategyBid.BPLUS) && spread == tick) {
						makeBid = false;
					}
					if(policy.getAskStrategy().equals(StrategyAsk.AMINUS) && spread == tick) {
						makeAsk = false;
					}
				}
				
				//LIMIT 
				if(makeBid) {
					//BID
					//order closed intensity based on the policy results 
					double intensityBid = proxiesBid.get(policy.getBidStrategy()).floorEntry(spread).getValue();  //double check
					double pstratBid = 1-Math.exp(-(intensityBid*step)); 
					double volBid = policy.getBidVolume();
					//orders not closed are not considered 
					if(generator.nextDouble()<pstratBid) {
						//buy the short first
						Set set = shortPositions.entrySet();
				        Iterator iter = set.iterator();
				        boolean exit = false;
				        HashMap<Double,Double> shortPositionsUpdate = new HashMap<>();
				        while (iter.hasNext() && !exit) {
				           Map.Entry<Double,Double> me = (Map.Entry)iter.next();
				           if(volBid > me.getValue()) {
				        	   cash += ((me.getKey()-ba.getBid()) * (volBid/ba.getBid()));
				        	   volBid -= me.getValue();
				        	   shortPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
				           }
				           else {
				        	   cash += ((me.getKey()-ba.getBid()) * (volBid/ba.getBid()));
				        	   shortPositionsUpdate.put(me.getKey(),me.getValue()-volBid); //entire position is eliminated
				        	   volBid = 0d;
				        	   exit = true;
				           }
				        }
				        //if the shorts are not enough open a new long position
				        if(volBid>0) {
				        	if(longPositions.containsKey(ba.getBid())) {
				        		longPositions.put(ba.getBid(),volBid+longPositions.get(ba.getBid()));
				        	}
				        	else {
				        		longPositions.put(ba.getBid(),volBid);
				        	}
				        }
				        //update ShortPositions
				        Set setSPU = shortPositionsUpdate.entrySet();
				        Iterator iterSPU = set.iterator();
				        while (iterSPU.hasNext()) {
				           Map.Entry<Double,Double> me = (Map.Entry)iterSPU.next();
				           if(me.getValue() != -1) {
				        	   shortPositions.put(me.getKey(),me.getValue());
				           }
				           else {
				        	   shortPositions.remove(me.getKey());
				           }
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
						//sell the long first
						Set set = longPositions.entrySet();
				        Iterator iter = set.iterator();
				        boolean exit = false;
				        HashMap<Double,Double> longPositionsUpdate = new HashMap<>();
				        while (iter.hasNext() && !exit) {
				           Map.Entry<Double,Double> me = (Map.Entry)iter.next();
				           if(volAsk > me.getValue()) {
				        	   cash += ((ba.getAsk()-me.getKey()) * (volAsk/ba.getAsk()));
				        	   volAsk -= me.getValue();
				        	   longPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
				           }
				           else {
				        	   cash += ((ba.getAsk()-me.getKey()) * (volAsk/ba.getAsk()));
				        	   longPositionsUpdate.put(me.getKey(),me.getValue()-volAsk); //entire position is eliminated
				        	   volAsk = 0d;
				        	   exit = true;
				           }
				        }
				        //if the longs are not enough, open a new short position
				        if(volAsk>0) {
				        	if(shortPositions.containsKey(ba.getAsk())) {
				        		shortPositions.put(ba.getAsk(),volAsk+shortPositions.get(ba.getAsk()));
				        	}
				        	else {
				        		shortPositions.put(ba.getAsk(),volAsk);
				        	}
				        }
				        //update LongPositions
				        Set setLPU = longPositionsUpdate.entrySet();
				        Iterator iterLPU = set.iterator();
				        while (iterLPU.hasNext()) {
				           Map.Entry<Double,Double> me = (Map.Entry)iterLPU.next();
				           if(me.getValue() != -1) {
				        	   longPositions.put(me.getKey(),me.getValue());
				           }
				           else {
				        	   longPositions.remove(me.getKey());
				           }
				        }
					}
				}
				
				//Take + in case the spread is equal to the tick and the strategy is BPLUS then buy at the best ask price
				if((policy.getTake() && policy.getVolumeTake() >0) || (policy.getMake() && !makeBid)) {   //MARKET
					double vol = (policy.getMake() && !makeBid) ? policy.getBidVolume() : policy.getVolumeTake();
					//buy the short first
					Set set = shortPositions.entrySet();
			        Iterator iter = set.iterator();
			        boolean exit = false;
			        HashMap<Double,Double> shortPositionsUpdate = new HashMap<>();
			        while (iter.hasNext() && !exit) {
			           Map.Entry<Double,Double> me = (Map.Entry)iter.next();
			           if(vol > me.getValue()) {
			        	   cash += ((me.getKey()-ba.getAsk()) * (vol/ba.getAsk()));
			        	   vol -= me.getValue();
			        	   shortPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
			           }
			           else {
			        	   cash += ((me.getKey()-ba.getAsk()) * (vol/ba.getAsk()));
			        	   shortPositionsUpdate.put(me.getKey(),me.getValue()-vol); //entire position is eliminated
			        	   vol = 0d;
			        	   exit = true;
			           }
			        }
			        //if the shorts are not enough, open a new long position
			        if(vol>0) {
			        	if(longPositions.containsKey(ba.getAsk())) {
			        		longPositions.put(ba.getAsk(),vol+longPositions.get(ba.getAsk()));
			        	}
			        	else {
			        		longPositions.put(ba.getAsk(),vol);
			        	}
			        }
			        //update ShortPositions
			        Set setSPU = shortPositionsUpdate.entrySet();
			        Iterator iterSPU = setSPU.iterator();
			        while (iterSPU.hasNext()) {
			           Map.Entry<Double,Double> me = (Map.Entry)iterSPU.next();
			           if(me.getValue() != -1) {
			        	   shortPositions.put(me.getKey(),me.getValue());
			           }
			           else {
			        	   shortPositions.remove(me.getKey());
			           }
			        }
					
				}
				//Take + in case the spread is equal to the tick and the strategy is AMINUS then sell at the best bid price
				if((policy.getTake() && policy.getVolumeTake() <0) || (policy.getMake() && !makeAsk)) {
					double vol = (policy.getMake() && !makeAsk) ? policy.getAskVolume() : policy.getVolumeTake();
					vol = Math.abs(vol);
					Set set = longPositions.entrySet();
			        Iterator iter = set.iterator();
			        boolean exit = false;
			        HashMap<Double,Double> longPositionsUpdate = new HashMap<>();
			        while (iter.hasNext() && !exit) {
			           Map.Entry<Double,Double> me = (Map.Entry)iter.next();
			           if(vol > me.getValue()) {
			        	   cash += ((ba.getBid()-me.getKey()) * (vol/ba.getBid()));
			        	   vol -= me.getValue();
			        	   longPositionsUpdate.put(me.getKey(),-1d); //entire position is eliminated
			           }
			           else {
			        	   cash += ((ba.getBid()-me.getKey()) * (vol/ba.getBid()));
			        	   longPositionsUpdate.put(me.getKey(),me.getValue()-vol); //entire position is eliminated
			        	   vol = 0d;
			        	   exit = true;
			           }
			        }
			        //if the longs are not enough, open a new short position
			        if(vol>0) {
			        	if( shortPositions.containsKey(ba.getBid())) {
			        		shortPositions.put(ba.getBid(),vol+shortPositions.get(ba.getBid()));
			        	}
			        	else {
			        		shortPositions.put(ba.getBid(),vol);
			        	}
			        }
			        //update LongPositions
			        Set setLPU = longPositionsUpdate.entrySet();
			        Iterator iterLPU = setLPU.iterator();
			        while (iterLPU.hasNext()) {
			           Map.Entry<Double,Double> me = (Map.Entry)iterLPU.next();
			           if(me.getValue() != -1) {
			        	   longPositions.put(me.getKey(),me.getValue());
			           }
			           else {
			        	   longPositions.remove(me.getKey());
			           }
			        }	
				}	
			}
			res.add(new BestPolicyStat(cash,nBestAskOrders,nNewAskOrders,
					nBestBidOrders,nNewBidOrders, nMarketBuyOrders,
					nMarketSellOrders,maxInventory, minInventory));
		}
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
		
		
		System.out.println();
	}
	
	/**
	 * @return
	 */
	public List<BidAsk> getBidAskList(){
		List<BidAsk> res = new ArrayList<>();
		Double spread = tick;
		for (int i = 1; i < periods; i++) {
			//geom brownian
			NormalDistribution nd = new NormalDistribution(generator,0.0,i);
			Double midPrice = initialPrice* Math.exp((drift- Math.pow(sigma, 2)/2)*i*step+sigma*nd.sample());
			Double p = 1-Math.exp(-(lambda_t.get(lambda_t.floorKey(i)))*step); 
			if(generator.nextDouble()<=p) {
				double index = spread;
				double sample = mmprob.get(index).sample();
				spread = sample;
				BidAsk ba = new BidAsk(i*step,midPrice - spread/2, midPrice+spread/2);
				res.add(ba);
			}
			else {
				BidAsk ba = new BidAsk(i*step,midPrice - spread/2, midPrice+spread/2);
				res.add(ba);
			}
        }
		return res;
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
		private Double bid;
		private Double ask;
		private Double time;
		public BidAsk(Double time,Double bid,Double ask) {
			this.time = time;
			this.bid = bid;
			this.ask = ask;
		}
		public Double getTime() {
			return this.time;
		}
		public Double getBid() {
			return this.bid;
		}
		public Double getAsk() {
			return this.ask;
		}
	}

}
