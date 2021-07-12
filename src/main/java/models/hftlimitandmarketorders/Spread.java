package models.hftlimitandmarketorders;

public final class Spread {
	
	private final Long date;
	private final Double bid;
	private final Double ask;
	private Double spread;
	private final Double cumulatedMarketOrderQuantityBuy;
	private final Double cumulatedMarketOrderQuantitySell;
	private final Double bidAmount;
	private final Double askAmount;
	
	public Spread(Long date, Double bid, Double ask,
			Double cumulatedMarketOrderQuantityBuy,
			Double cumulatedMarketOrderQuantitySell,
			Double bidAmount,
			Double askAmount) {
		this.date = date;
		this.bid = bid;
		this.ask = ask;
		this.spread = this.ask-this.bid;
		this.cumulatedMarketOrderQuantityBuy = cumulatedMarketOrderQuantityBuy;
		this.cumulatedMarketOrderQuantitySell = cumulatedMarketOrderQuantitySell;
		this.bidAmount = bidAmount;
		this.askAmount = askAmount;
	}
	
	public Long getDate() {
		return this.date;
	}
	public Double getBid() {
		return this.bid;
	}
	public Double getAsk() {
		return this.ask;
	}
	public Double getSpread() {
		return this.spread;
	}
	public Double getCumulatedMarketOrderQuantityBuy() {
		return this.cumulatedMarketOrderQuantityBuy;
	}
	public Double getCumulatedMarketOrderQuantitySell() {
		return this.cumulatedMarketOrderQuantitySell;
	}
	public Double getBidAmount() {
		return this.bidAmount;
	}
	public Double getAskAmount() {
		return this.askAmount;
	}

}
