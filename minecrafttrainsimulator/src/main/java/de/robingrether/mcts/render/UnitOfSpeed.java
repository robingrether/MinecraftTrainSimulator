package de.robingrether.mcts.render;

public enum UnitOfSpeed {
	
	BLOCKS_PER_TICK("bpt", "b/t", 1.0),
	KILOMETRES_PER_HOUR("kmh", "km/h", 72.0),
	METRES_PER_SECOND("mps", "m/s", 20.0),
	MILES_PER_HOUR("mph", "mi/h", 44.73872);
	
	private final String key, symbol;
	private final double factor;
	
	private UnitOfSpeed(String key, String symbol, double factor) {
		this.key = key;
		this.symbol = symbol;
		this.factor = factor;
	}
	
	public double convert(double rawSpeed) {
		return rawSpeed * factor;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public String toString() {
		return key;
	}
	
	public static UnitOfSpeed fromString(String string) {
		switch(string) {
			case "bpt": return BLOCKS_PER_TICK;
			case "kmh": return KILOMETRES_PER_HOUR;
			case "mps": return METRES_PER_SECOND;
			case "mph": return MILES_PER_HOUR;
			default: return null;
		}
	}
	
}