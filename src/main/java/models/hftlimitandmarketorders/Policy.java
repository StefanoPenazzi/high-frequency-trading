package models.hftlimitandmarketorders;

import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyAsk;
import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyBid;

public class Policy{
	private final Boolean make;
	private final Boolean take;
	private final StrategyBid bidStrategy;
	private final Double bidVolume;
	private final StrategyAsk askStrategy;
	private final Double askVolume;
	private final Double volumeTake;
	
	public static class Builder {
		
		private Boolean make = false;
		private Boolean take = false;
		private StrategyBid bidStrategy = null;
		private Double bidVolume = 0d;
		private StrategyAsk askStrategy = null;
		private Double askVolume = 0d;
		private Double volumeTake = 0d;
		
		public Builder make (Boolean make){
            this.make = make;
            return this;
        }
		public Builder take (Boolean take){
            this.take = take;
            return this;
        }
		public Builder bidStrategy(StrategyBid bidStrategy){
            this.bidStrategy = bidStrategy;
            return this;
        }
		public Builder bidVolume(Double bidVolume){
            this.bidVolume = bidVolume;
            return this;
        }
		public Builder askStrategy(StrategyAsk askStrategy){
            this.askStrategy = askStrategy;
            return this;
        }
		public Builder askVolume (Double askVolume){
            this.askVolume = askVolume;
            return this;
        }
		public Builder volumeTake(Double volumeTake){
            this.volumeTake = volumeTake;
            return this;
        }
		
		public Policy build() {
			
			//TODO params check
			
			return new Policy(this.make, this.take, this.bidStrategy,
					this.bidVolume, this.askStrategy, this.askVolume,
					this.volumeTake);
		}
		
	}
	
	public Policy(Boolean make,Boolean take,StrategyBid bidStrategy,
			Double bidVolume,StrategyAsk askStrategy,Double askVolume,
			Double volumeTake) {
		 this.make = make;
		 this.take = take;
	     this.bidStrategy = bidStrategy;
	     this.bidVolume = bidVolume;
	     this.askStrategy = askStrategy;
	     this.askVolume = askVolume;
	     this.volumeTake = volumeTake;
	}
	
	public StrategyBid getBidStrategy() {
		return this.bidStrategy;
	}
	public StrategyAsk getAskStrategy() {
		return this.askStrategy;
	}
	
	public Double getBidVolume() {
		return this.bidVolume;
	}
	
	public Double getAskVolume() {
		return this.askVolume;
	}
	
	public Double getVolumeTake() {
		return this.volumeTake;
	}
	
	public Boolean getMake(){
		return this.make;
	}
	public Boolean getTake(){
		return this.take;
	}
	
	public String print() {
		StringBuilder str = new StringBuilder();
		String bs = getBidStrategy() == null?"null":getBidStrategy().toString();
		str.append(bs);
		str.append(",");
		String as = getAskStrategy() == null?"null":getAskStrategy().toString();
		str.append(as);
		str.append(",");
		str.append(getBidVolume().toString());
		str.append(",");
		str.append(getAskVolume().toString());
		str.append(",");
		str.append(getVolumeTake().toString());
		return str.toString();
	}
	
}