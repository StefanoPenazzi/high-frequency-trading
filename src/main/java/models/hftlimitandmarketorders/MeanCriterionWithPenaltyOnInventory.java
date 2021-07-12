package models.hftlimitandmarketorders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ejml.simple.SimpleMatrix;

/**
 * @author stefanopenazzi
 * 
 *
 * @see <a href="https://www.tandfonline.com/doi/full/10.1080/14697688.2012.708779?scroll=top&needAccess=true">Fabien Guilbaud and Huyen Pham</a>
 *
 *
 */
public final class MeanCriterionWithPenaltyOnInventory extends OptimalMMPolicyFrameworkAbstract {
	
	private static final Logger log = LogManager.getLogger(MeanCriterionWithPenaltyOnInventory.class);
	
	 public static class Builder extends OptimalMMPolicyFrameworkAbstract.AbstractBuilder {

		public Builder(File file, Double tick, Double vol, Path outputDir,String testName) throws IOException {
			super(file, tick, vol, outputDir,testName);
		}
		
		public MeanCriterionWithPenaltyOnInventory build() {
			return new MeanCriterionWithPenaltyOnInventory(this.startTime, this.endTime, this.tick, this.rho, this.epsilon,
					this.epsilon0, this.lambda_t, this.gamma, this.maxVolM, this.maxVolT,
					this.timeStep, this.lbShares, this.ubShares, this.proxiesBid,
					this.proxiesAsk,  this.spreadTransitionProbabMatrix,
					this.delay,this.volumeStep,this.outputDir,this.testName);
		}
		 
	 }
	MeanCriterionWithPenaltyOnInventory(Integer startTime, Integer endTime, Double tick, Double rho, Double epsilon,
			Double epsilon0, Map<Integer, Double> lambda_t, Double gamma, Integer maxVolM, Double maxVolT,
			Integer timeStep, Integer lbShares, Integer ubShares, Map<StrategyBid, Map<Integer, Double>> proxiesBid,
			Map<StrategyAsk, Map<Integer, Double>> proxiesAsk, SimpleMatrix spreadTransitionProbabMatrix,
			Integer delay,Double volumeStep ,Path outputDir,String testName) {
		super(startTime, endTime, tick, rho, epsilon, epsilon0, lambda_t, gamma, maxVolM, maxVolT, timeStep, lbShares, ubShares,
				proxiesBid, proxiesAsk, spreadTransitionProbabMatrix, delay,volumeStep,outputDir,testName);
	}

	@Override
	public Double penaltyFunction(Integer invent) {
		return Math.pow(getInventory(invent),2);
	}

	@Override
	public Double utilityFunction() {
		return 0d;
	}

}
