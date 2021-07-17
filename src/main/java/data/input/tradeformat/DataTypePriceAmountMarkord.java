package data.input.tradeformat;

import java.math.BigDecimal;

import com.opencsv.bean.CsvBindByPosition;

public class DataTypePriceAmountMarkord {
	
	@CsvBindByPosition(position = 0)
	 private Long date;
	@CsvBindByPosition(position = 1)
	 private Character type;
	@CsvBindByPosition(position = 2)
	 private BigDecimal price;
	@CsvBindByPosition(position = 3)
	 private Double amount;
	@CsvBindByPosition(position = 4)
	 private Double marketOrders;
	 
	 public DataTypePriceAmountMarkord( Long date,Character type,BigDecimal price,
			 Double amount,Double marketOrders) {
		 this.date = date;
		 this.type = type;
		 this.price = price;
		 this.amount = amount;
		 this.marketOrders = marketOrders;
	 }
	 
	 public DataTypePriceAmountMarkord() {
	 }
	 
	
	 public Long getDate() {
		 return this.date;
	 }
	 
	 public Character getType() {
		 return this.type;
	 }
	 
	 public BigDecimal getPrice() {
		 return this.price;
	 }
	 
	 public Double getAmount() {
		 return this.amount;
	 }
	 
	 public Double getMarketOrders() {
		 return this.marketOrders;
	 }

}
