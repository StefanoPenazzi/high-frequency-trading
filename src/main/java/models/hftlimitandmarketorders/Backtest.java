package models.hftlimitandmarketorders;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public class Backtest {
	
	private static RandomGenerator generator;
	
	public static void run(Policy[][][] bestPolicy) {
		
	}
	
	public static List<Double> midPrice(Double initialPrice,Integer periods,Double drift,Double sigma){
		List<Double> res = new ArrayList<>();
		
		for (int i = 0; i < periods; i++) {
			NormalDistribution nd = new NormalDistribution(generator,0,i);
			Double val = initialPrice* Math.exp((drift- Math.pow(sigma, 2)/2)*i+sigma*nd.sample());
			res.add(val);
        }
		
		
		return res;
	}

}
