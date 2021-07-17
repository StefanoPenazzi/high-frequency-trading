package data.input.tradeformat;

import java.math.BigDecimal;
import java.time.Instant;

import com.opencsv.bean.CsvBindByPosition;

public class DateTypePriceAmount {
	
	@CsvBindByPosition(position = 0)
	 private Long date;
	
	@CsvBindByPosition(position = 1)
	 private Character type;

	 @CsvBindByPosition(position = 2)
	  private BigDecimal  price;

	 @CsvBindByPosition(position = 3)
	  private Double amount;
	 
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

}
