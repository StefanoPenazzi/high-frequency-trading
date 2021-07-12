package data.input.tradeformat;

import com.opencsv.bean.CsvBindByPosition;

public final class TimePriceVolume {
	
	 @CsvBindByPosition(position = 0)
	 private Double time;

	 @CsvBindByPosition(position = 1)
	  private Double price;

	 @CsvBindByPosition(position = 2)
	  private Double volume;

}
