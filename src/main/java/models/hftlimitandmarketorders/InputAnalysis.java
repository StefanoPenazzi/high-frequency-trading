package models.hftlimitandmarketorders;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ejml.simple.SimpleMatrix;

import data.input.tradeformat.DataTypePriceAmountMarkord;
import data.utils.CSV;
import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyAsk;
import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyBid;

public final class InputAnalysis {
	
	private File inputFile;
	private BigDecimal tick;
	private Double volumeProxy;
	private String outputDir;
	private BigDecimal minTransitionMatrixSpread;
	private BigDecimal maxTransitionMatrixSpread;
	private Integer lambdaInterval;
	private SimpleMatrix spreadTransitionProbabMatrix = null;
	private Map<StrategyBid,Map<Integer,Double>> proxiesBid = new HashMap<>();
	private Map<StrategyAsk,Map<Integer,Double>> proxiesAsk = new HashMap<>();
	private Map<StrategyAsk,TreeMap<BigDecimal,Double>> proxiesAsk_=new HashMap();
	private Map<StrategyBid,TreeMap<BigDecimal,Double>> proxiesBid_=new HashMap();
	private Map<Integer,Double> lambda; 
	
	
	
	public static class Builder{
		
		private File inputFile = null;
		private BigDecimal tick = new BigDecimal("0.01");
		private Double volumeProxy = 100d;
		private String outputDir = null;
		private BigDecimal minTransitionMatrixSpread = new BigDecimal("0.01");
		private BigDecimal maxTransitionMatrixSpread = new BigDecimal("3.0");
		private Integer lambdaInterval = 86401;
		
		
		
		public Builder inputFile (File inputFile) {
			this.inputFile = inputFile ;
			return this;
		}
		
		public Builder tick (BigDecimal tick) {
			this.tick = tick;
			return this;
		}
		
		public Builder volumeProxy(Double volumeProxy) {
			this.volumeProxy = volumeProxy;
			return this;
		}
		
		public Builder outputDir(String outputDir) {
			this.outputDir = outputDir;
			return this;
		}
		
		public Builder minTransitionMatrixSpread(BigDecimal minTransitionMatrixSpread) {
			this.minTransitionMatrixSpread = minTransitionMatrixSpread;
			return this;
		}
		
		public Builder maxTransitionMatrixSpread(BigDecimal maxTransitionMatrixSpread) {
			this.maxTransitionMatrixSpread = maxTransitionMatrixSpread;
			return this;
		}
		
		public Builder lambdaInterval(Integer lambdaInterval) {
			this.lambdaInterval = lambdaInterval;
			return this;
		}
		
        public InputAnalysis build() {
			return new InputAnalysis(this.inputFile,this.tick,this.volumeProxy,
					this.outputDir,this.minTransitionMatrixSpread,
					this.maxTransitionMatrixSpread,this.lambdaInterval);
		}
	}
	
	public InputAnalysis(File inputFile,BigDecimal tick,Double volumeProxy,
			String outputDir,BigDecimal minTransitionMatrixSpread,
			BigDecimal maxTransitionMatrixSpread,Integer lambdaInterval) {
		 this.inputFile=inputFile;
		 this.tick=tick;
		 this.volumeProxy=volumeProxy;
		 this.outputDir=outputDir;
		 this.minTransitionMatrixSpread=minTransitionMatrixSpread;
		 this.maxTransitionMatrixSpread=maxTransitionMatrixSpread;
		 this.lambdaInterval=lambdaInterval;
	}
	
	public void run() throws IOException {
		List<DataTypePriceAmountMarkord> tpcl = CSV.getList(inputFile, DataTypePriceAmountMarkord.class, 1);
		List<Spread> spreads = ParametersEstimator.getSpreadList(tpcl);
		lambda = ParametersEstimator.
				getEstimatedSpreadIntensityFunction(spreads,this.lambdaInterval);
		this.spreadTransitionProbabMatrix = ParametersEstimator.
				getEstimatedSpreadTransitionProbabilityMatrix(spreads,this.tick,this.minTransitionMatrixSpread,this.maxTransitionMatrixSpread);
		Map<String,Map<Integer,Double>> proxies = ParametersEstimator.
				getEstimatedExecutionParameters(spreads,this.tick,volumeProxy);
		
		//this is only useful for the backtest
		Map<String,TreeMap<BigDecimal,Double>> proxies_ = ParametersEstimator.
				getEstimatedExecutionParametersTreeMap(spreads,this.tick,volumeProxy);
		
		this.proxiesBid.put(StrategyBid.B,proxies.get("b"));
		this.proxiesBid.put(StrategyBid.BPLUS,proxies.get("b+"));
		this.proxiesAsk.put(StrategyAsk.A,proxies.get("a"));
		this.proxiesAsk.put(StrategyAsk.AMINUS,proxies.get("a-"));
		
		this.proxiesBid_.put(StrategyBid.B,proxies_.get("b"));
		this.proxiesBid_.put(StrategyBid.BPLUS,proxies_.get("b+"));
		this.proxiesAsk_.put(StrategyAsk.A,proxies_.get("a"));
		this.proxiesAsk_.put(StrategyAsk.AMINUS,proxies_.get("a-"));
	}
	
	
	public SimpleMatrix getSpreadTransitionProbabMatrix() {
		return this.spreadTransitionProbabMatrix;
	}
	
	public Map<StrategyBid,Map<Integer,Double>> getProxiesBid(){
		return this.proxiesBid;
	} 
	
	public Map<StrategyAsk,Map<Integer,Double>> getProxiesAsk(){
		return this.proxiesAsk;
	}
	
	public Map<StrategyBid,TreeMap<BigDecimal,Double>> getProxiesBid_(){
		return this.proxiesBid_;
	}

	public Map<StrategyAsk,TreeMap<BigDecimal,Double>> getProxiesAsk_(){
		return this.proxiesAsk_;
	}
	
	public Map<Integer,Double> getLambda(){
		return this.lambda;
	}
	
	

}
