package data.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.input.tradeformat.DataTypePriceAmountMarkord;
import data.input.tradeformat.DateTypePriceAmount;

public final class OrderBook {
	
	private static final Logger log = LogManager.getLogger(OrderBook.class);
	
	/**
	 * @param orderBookSnapshot
	 * @return
	 */
	public static List<DateTypePriceAmount> orderBookSnapshot2Level1(List<DateTypePriceAmount> orderBookSnapshot) {
		
		Map<Long, DateTypePriceAmount> bidMap = orderBookSnapshot.stream()
				.filter(c -> c.getType().equals('b'))
			    .collect(Collectors.toMap(DateTypePriceAmount::getDate, Function.identity(),
			        BinaryOperator.maxBy(Comparator.comparing(DateTypePriceAmount::getPrice))));
		
		Map<Long, DateTypePriceAmount> askMap = orderBookSnapshot.stream()
				.filter(c -> c.getType().equals('a'))
			    .collect(Collectors.toMap(DateTypePriceAmount::getDate, Function.identity(),
			        BinaryOperator.minBy(Comparator.comparing(DateTypePriceAmount::getPrice))));
		
		List<DateTypePriceAmount> level1List = Stream.concat(
				bidMap.values().stream(),
				askMap.values().stream())
				.sorted(Comparator.comparingLong(DateTypePriceAmount::getDate))
				.sorted(Comparator.comparing(DateTypePriceAmount::getType))
				.collect(Collectors.toList());
		
		return level1List;
		
	}
	
    /**
     * @param level1
     * @param cancellationRate
     * @param maxMarketOrdersRate
     * @return
     */
    public static List<DataTypePriceAmountMarkord> addOrderMarket2Level1(List<DateTypePriceAmount> level1, Double cancellationRate, Double maxMarketOrdersRate) {
    	
    	//TODO central rand
    	//TODO separating "a" "b"
    	Random rand = new Random();
		
    	List<DataTypePriceAmountMarkord> level1PlusMarketOrders = new ArrayList<>();
    	for(int i = 1;i<level1.size();i++) {
    		DateTypePriceAmount dtp_t = level1.get(i);
    		DateTypePriceAmount dtp_t_1 = level1.get(i-1);
    		
    		Double quoteDiff = dtp_t.getAmount()-dtp_t_1.getAmount();
    		quoteDiff -= Math.abs(dtp_t_1.getAmount()*cancellationRate);
    		Double minMarketOrders = quoteDiff < 0 ? Math.abs(quoteDiff):0;
    		Double maxMarketOrders = minMarketOrders < dtp_t_1.getAmount() * maxMarketOrdersRate ?
    				dtp_t_1.getAmount() * maxMarketOrdersRate : 
    					minMarketOrders * (1+maxMarketOrdersRate);
    		Double marketOrd = rand.nextDouble() * (maxMarketOrders-minMarketOrders);
    		
    		level1PlusMarketOrders.add(new DataTypePriceAmountMarkord(dtp_t.getDate(),dtp_t.getType(),dtp_t.getPrice(),dtp_t.getAmount(),marketOrd));
    	}
		
		return level1PlusMarketOrders;
		
	}

}
