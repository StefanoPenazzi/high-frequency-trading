package models.hftlimitandmarketorders;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ejml.simple.SimpleMatrix;

import data.input.tradeformat.DataTypePriceAmountMarkord;
import data.input.tradeformat.DateTypePriceAmount;
import data.input.tradeformat.TimePriceVolume;

public final class ParametersEstimator {
	
	private static final Logger log = LogManager.getLogger(ParametersEstimator.class);
	
	private static final Integer SXDAY = 86400;
	private static final Integer SXHOUR = 3600; 
	private static final ZoneId TIMEZONE_NY = ZoneId.of("Etc/Greenwich");
	
	/**
	 * @param level1List : List of DataTypePriceAmountMarkord
	 * @return           : Return a list containing the spreads
	 * 
	 * The spread represents the difference between the ask and the bid. A new spread is generated every time the the difference 
	 * between ask and bid change. 
	 */
	public static List<Spread> getSpreadList(List<DataTypePriceAmountMarkord> level1List){
		
		List<Spread> spreadList = new ArrayList<>();
		
		Map<Long,DataTypePriceAmountMarkord> bidMap = level1List.stream()
				.filter(c -> c.getType() == 'b')
				.collect(Collectors.toMap(DataTypePriceAmountMarkord::getDate,Function.identity()));
		
		Map<Long,DataTypePriceAmountMarkord> askMap = level1List.stream()
				.filter(c -> c.getType() == 'a')
				.collect(Collectors.toMap(DataTypePriceAmountMarkord::getDate,Function.identity()));
		
		List<Long> dates = level1List.stream()
				.map(DataTypePriceAmountMarkord::getDate)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
		
		Double currSpread = 0d;
		Double cumulatedMarketOrderQuantityBuy = 0d;
		Double cumulatedMarketOrderQuantitySell = 0d;
		for(Long e: dates){
			if(bidMap.containsKey(e) && askMap.containsKey(e)) {
				DataTypePriceAmountMarkord bid = bidMap.get(e);
				DataTypePriceAmountMarkord sell = askMap.get(e);
				
				Double bidPrice = bidMap.get(e).getPrice();
				Double askPrice = askMap.get(e).getPrice();
				cumulatedMarketOrderQuantityBuy += askMap.get(e).getMarketOrders();
				cumulatedMarketOrderQuantitySell += bidMap.get(e).getMarketOrders();
				
				if(!currSpread.equals(askPrice-bidPrice)) {
					
					Spread spread = new Spread(e,bidPrice,askPrice,
							cumulatedMarketOrderQuantityBuy,
							cumulatedMarketOrderQuantitySell,
							bidMap.get(e).getAmount(),
							askMap.get(e).getAmount());
					
					spreadList.add(spread);
					currSpread = spread.getSpread();
					cumulatedMarketOrderQuantityBuy = 0d;
					cumulatedMarketOrderQuantitySell = 0d;
				}	
			}
		}
		return spreadList;
	}
	
	/**
	 * @param spreadList
	 * @param tick        
	 * @param min         
	 * @param max         
	 * @return
	 */
	public static SimpleMatrix getEstimatedSpreadTransitionProbabilityMatrix( List<Spread> spreadList, Double tick, Double min,Double max) {
		
		//TODO the list must be sorted
		
		Map<Double,Map<Double,Double>> tpMatrix = new TreeMap<>();
		double[][] tpMatrix2D;
		
		Double maxSpread = (Double)spreadList.stream()
				.mapToDouble(Spread::getSpread)
				.max()
				.orElse(0);
		Integer ticks = maxSpread>max ? (int)Math.ceil(max/tick) :(int)Math.ceil(maxSpread/tick);
		tpMatrix2D = new double[ticks][ticks];
		
		for(int i = 1;i<= ticks;i++) {
			Double spreadFrom = tick*i;
			Long spreadCount = spreadList.stream()
					.filter(c -> c.getSpread().equals(spreadFrom))
					.count();
			if (spreadCount != 0 && i == 1) {
				spreadCount = spreadCount-1;
			} 
			Map<Double,Double> row = new TreeMap<>();
			for(int j = 1;j<= ticks;j++) {
				Double spreadTo = tick*j;
				if(spreadCount == 0) {
					row.put(spreadTo, 0.);
					tpMatrix2D[i-1][j-1] = 0d;
				}
				else {
					Double nTransitions = 0.;
					for(int k = 0;k<spreadList.size()-1;k++) {
						if(spreadList.get(k).getSpread().equals(spreadFrom) &&
								spreadList.get(k+1).getSpread().equals(spreadTo)) {
							nTransitions++;
						}
					}
					row.put(spreadTo,nTransitions/spreadCount);
					tpMatrix2D[i-1][j-1] = nTransitions/spreadCount;
				}
			}
			tpMatrix.put(spreadFrom, row);
		}
		
		SimpleMatrix sm = new SimpleMatrix(tpMatrix2D);
		
		return sm;
	}
	
	/**
	 * @param spreadList
	 * @param interval
	 * @return
	 */
	public static Map<Integer,Double> getEstimatedSpreadIntensityFunction(List<Spread> spreadList,Integer interval){
		
		//TODO the list must be sorted
		
		Map<Integer,Double> spreadIntensityFunction = new TreeMap<>();
		
		LocalDate ld = LocalDate.ofInstant(Instant.ofEpochMilli(spreadList.get(0).getDate()),TIMEZONE_NY);
		long startOfDay = ld
			      .atStartOfDay(TIMEZONE_NY)
			      .toInstant()
			      .toEpochMilli();
		
		for(int i = 0;i<=Math.floor(SXDAY/interval);i++) {
			Integer i_ = i;
			Integer n = Math.toIntExact(spreadList.stream()
					.filter(c -> Double.valueOf((c.getDate()-startOfDay)/1000) >= Double.valueOf(i_*interval) &&
					Double.valueOf((c.getDate()-startOfDay)/1000) < Double.valueOf((i_+1)*interval))
					.count());
			spreadIntensityFunction.put(interval*i, (double)n/interval);
		}
		return spreadIntensityFunction;
	}
	
	/**
	 * @param spreadList
	 * @param tick
	 * @param typicalVolume
	 * @return
	 */
	public static Map<String,Map<Integer,Double>> getEstimatedExecutionParameters(List<Spread> spreadList, Double tick, Double typicalVolume){
		
		Map<String,Map<Integer,Double>> proxies = new HashMap<>();
		proxies.put("b", new HashMap<>());
		proxies.put("b+", new HashMap<>());
		proxies.put("a", new HashMap<>());
		proxies.put("a-", new HashMap<>());
		Map<Integer,Long> times = new HashMap<>();
		
		for(int i = 1; i<spreadList.size();i++) {
			Spread spread = spreadList.get(i);
			Spread spread_1 = spreadList.get(i-1);
			Integer intTick = (int) Math.floor(spread_1.getSpread()/tick);
			
			if(typicalVolume < spread.getCumulatedMarketOrderQuantitySell()) {
				if(proxies.get("b+").containsKey(intTick)) {
					proxies.get("b+").put(intTick,proxies.get("b+").get(intTick)+1);
				}
				else {
					proxies.get("b+").put(intTick,1d);
				}
			}
			if(typicalVolume + spread_1.getBidAmount() < spread.getCumulatedMarketOrderQuantitySell()) {
				if(proxies.get("b").containsKey(intTick)) {
					proxies.get("b").put(intTick,proxies.get("b").get(intTick)+1);
				}
				else {
					proxies.get("b").put(intTick,1d);
				}
			}
            if(typicalVolume < spread.getCumulatedMarketOrderQuantityBuy()) {
            	if(proxies.get("a-").containsKey(intTick)) {
					proxies.get("a-").put(intTick,proxies.get("a-").get(intTick)+1);
				}
				else {
					proxies.get("a-").put(intTick,1d);
				}
			}
			if(typicalVolume + spread_1.getAskAmount() < spread.getCumulatedMarketOrderQuantityBuy()) {
				if(proxies.get("a").containsKey(intTick)) {
					proxies.get("a").put(intTick,proxies.get("a").get(intTick)+1);
				}
				else {
					proxies.get("a").put(intTick,1d);
				}
			}
			
			if(times.containsKey(intTick)) {
				times.put(intTick, (times.get(intTick)+spread.getDate()-spread_1.getDate())/1000L);
			}
			else {
				times.put(intTick, (spread.getDate()-spread_1.getDate())/1000L);
			}
		}
		
		for(String keyStrategy: proxies.keySet()) {
			for(Integer KeyIntTick: proxies.get(keyStrategy).keySet()) {
				Long t = times.get(KeyIntTick);
				proxies.get(keyStrategy).put(KeyIntTick, (Double)proxies.get(keyStrategy).get(KeyIntTick)/t);
			}
		}
		
		return proxies;
	}
	
	
public static Map<String,TreeMap<Double,Double>> getEstimatedExecutionParametersTreeMap(List<Spread> spreadList, Double tick, Double typicalVolume){
		
		Map<String,TreeMap<Double,Double>> proxies = new HashMap<>();
		proxies.put("b", new TreeMap<>());
		proxies.put("b+", new TreeMap<>());
		proxies.put("a", new TreeMap<>());
		proxies.put("a-", new TreeMap<>());
		Map<Double,Long> times = new HashMap<>();
		
		for(int i = 1; i<spreadList.size();i++) {
			Spread spread = spreadList.get(i);
			Spread spread_1 = spreadList.get(i-1);
			Double intTick = spread_1.getSpread();
			
			if(typicalVolume < spread.getCumulatedMarketOrderQuantitySell()) {
				if(proxies.get("b+").containsKey(intTick)) {
					proxies.get("b+").put(intTick,proxies.get("b+").get(intTick)+1);
				}
				else {
					proxies.get("b+").put(intTick,1d);
				}
			}
			if(typicalVolume + spread_1.getBidAmount() < spread.getCumulatedMarketOrderQuantitySell()) {
				if(proxies.get("b").containsKey(intTick)) {
					proxies.get("b").put(intTick,proxies.get("b").get(intTick)+1);
				}
				else {
					proxies.get("b").put(intTick,1d);
				}
			}
            if(typicalVolume < spread.getCumulatedMarketOrderQuantityBuy()) {
            	if(proxies.get("a-").containsKey(intTick)) {
					proxies.get("a-").put(intTick,proxies.get("a-").get(intTick)+1);
				}
				else {
					proxies.get("a-").put(intTick,1d);
				}
			}
			if(typicalVolume + spread_1.getAskAmount() < spread.getCumulatedMarketOrderQuantityBuy()) {
				if(proxies.get("a").containsKey(intTick)) {
					proxies.get("a").put(intTick,proxies.get("a").get(intTick)+1);
				}
				else {
					proxies.get("a").put(intTick,1d);
				}
			}
			
			if(times.containsKey(intTick)) {
				times.put(intTick, (times.get(intTick)+spread.getDate()-spread_1.getDate())/1000L);
			}
			else {
				times.put(intTick, (spread.getDate()-spread_1.getDate())/1000L);
			}
		}
		
		for(String keyStrategy: proxies.keySet()) {
			for(Double KeyIntTick: proxies.get(keyStrategy).keySet()) {
				Long t = times.get(KeyIntTick);
				proxies.get(keyStrategy).put(KeyIntTick, (Double)proxies.get(keyStrategy).get(KeyIntTick)/t);
			}
		}
		
		return proxies;
	}
}
