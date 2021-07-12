package models.hftlimitandmarketorders;

import java.util.Iterator;
import java.util.Map;

import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyAsk;
import models.hftlimitandmarketorders.OptimalMMPolicyFrameworkAbstract.StrategyBid;

public class Utils {
	
	protected static String printProxiesBid(Map<StrategyBid,Map<Integer,Double>> prox) {
		StringBuilder str = new StringBuilder();
		str.append("strategy,spread,var");
		str.append("\n");
		Iterator<Map.Entry<StrategyBid,Map<Integer,Double>>> entryStratBidIter = prox.entrySet().iterator();
		while (entryStratBidIter.hasNext()) {
			Map.Entry<StrategyBid,Map<Integer,Double>> entryStratBidPair = entryStratBidIter.next();
		    Iterator<Map.Entry<Integer,Double>> spreadIter = (entryStratBidPair.getValue()).entrySet().iterator();
		    while (spreadIter.hasNext()) {
		        Map.Entry<Integer,Double> spreadPair = spreadIter.next();
		        str.append(entryStratBidPair.getKey().toString());
		        str.append(",");
		        str.append(spreadPair.getKey().toString());
		        str.append(",");
		        str.append(spreadPair.getValue().toString());
				str.append("\n");
		    }
		}
		return str.toString();
	}
	
	protected static String printProxiesAsk(Map<StrategyAsk,Map<Integer,Double>> prox) {
		StringBuilder str = new StringBuilder();
		str.append("strategy,spread,var");
		str.append("\n");
		Iterator<Map.Entry<StrategyAsk,Map<Integer,Double>>> entryStratAskIter = prox.entrySet().iterator();
		while (entryStratAskIter.hasNext()) {
			Map.Entry<StrategyAsk,Map<Integer,Double>> entryStratAskPair = entryStratAskIter.next();
		    Iterator<Map.Entry<Integer,Double>> spreadIter = (entryStratAskPair.getValue()).entrySet().iterator();
		    while (spreadIter.hasNext()) {
		        Map.Entry<Integer,Double> spreadPair = spreadIter.next();
		        str.append(entryStratAskPair.getKey().toString());
		        str.append(",");
		        str.append(spreadPair.getKey().toString());
		        str.append(",");
		        str.append(spreadPair.getValue().toString());
				str.append("\n");
		    }
		}
		return str.toString();
	}

}
