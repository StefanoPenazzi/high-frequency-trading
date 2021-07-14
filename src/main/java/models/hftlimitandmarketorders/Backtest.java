package models.hftlimitandmarketorders;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.ejml.simple.SimpleMatrix;

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
	private Map<Integer,EnumeratedIntegerDistribution> mmprob; 
	private TreeMap<Integer,Double> lambda_t;
	
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
			SimpleMatrix spreadTransitionProbabMatrix, Double tick) {
		
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
		initialize();
	}
	
	private void initialize() {
		mmprob = new HashMap<>();
		int nrows = spreadTransitionProbabMatrix.numRows();
		for(int i =0;i<nrows;i++) {
			int[] support = new int[nrows];
			double[] prob = new double[nrows];
			for(int j = 0;j<nrows;j++) {
				support[j] = j;
				prob[j] = spreadTransitionProbabMatrix.get(i,j);
			}
			if(Arrays.stream(prob).sum() != 0) {
				mmprob.put(i, new EnumeratedIntegerDistribution(support,prob));
			}
			
		}
	    lambda_t = new TreeMap<>();
		lambda_t.putAll(lambda);
	}
	
	public void run(Policy[][][] bestPolicy) {
		
		List<BestPolicyStat> res = new ArrayList<>();
		for(int i=0;i<runs;i++) {
			List<BidAsk> newDataset = getBidAskList();
			System.out.println();
		}
	}
	
	/**
	 * @return
	 */
	public List<BidAsk> getBidAskList(){
		
		List<BidAsk> res = new ArrayList<>();
		Double spread = 0d;
		
		for (int i = 1; i < periods; i++) {
			//geom brownian
			NormalDistribution nd = new NormalDistribution(generator,0.0,i);
			Double midPrice = initialPrice* Math.exp((drift- Math.pow(sigma, 2)/2)*i*step+sigma*nd.sample());
			Double p = 1-Math.exp(-(lambda_t.get(lambda_t.floorKey(i)))*step); 
			if(generator.nextDouble()<=p) {
				int index = (int)(spread/tick);
				int sample = mmprob.get(index).sample();
				spread = sample*tick;
				BidAsk ba = new BidAsk(midPrice - spread/2, midPrice+spread/2);
				res.add(ba);
			}
			else {
				BidAsk ba = new BidAsk(midPrice - spread/2, midPrice+spread/2);
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
		public BidAsk(Double bid,Double ask) {
			this.bid = bid;
			this.ask = ask;
		}
		public Double getBid() {
			return this.bid;
		}
		public Double getAsk() {
			return this.ask;
		}
	}

}
