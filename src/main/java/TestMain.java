import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.input.tradeformat.DateTypePriceAmount;
import data.output.Utils;
import data.utils.CSV;
import data.utils.OrderBook;
import models.hftlimitandmarketorders.InputAnalysis;
import models.hftlimitandmarketorders.MeanCriterionWithPenaltyOnInventory;
import models.hftlimitandmarketorders.MeanCriterionWithPenaltyOnInventory.Builder;
import picocli.CommandLine;

public class TestMain implements Callable<Integer> {
	
	@CommandLine.Command(
			name = "RunHFT",
			description = "Test and Run a HFT Strategy",
			showDefaultValues = true,
			mixinStandardHelpOptions = true
	)
	
	@CommandLine.Option(names = {"--inputFile","-if"}, description = "The .csv file containing the historical market data")
	private Path inputFile;
	
	@CommandLine.Option(names = {"--outputDir","-od"}, description = "The directory containing the output files")
	private Path outputDir;
	
	@CommandLine.Option(names = {"--testName","-tn"}, description = "The name is used to create a new folder in outputDir in which to save the results. If this is not specified a new folder with a random name is created")
	private String testName = null;
	
	@CommandLine.Option(names = "--threads", defaultValue = "4", description = "Number of threads to use concurrently")
	private int threads;
	
	private static final Logger log = LogManager.getLogger(TestMain.class);

	public static void main(String[] args) {
		System.exit(new CommandLine(new TestMain()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		
		//marketorders
		//CSV.writeTo(new File("/home/stefanopenazzi/projects/HFT/binance_BTCUSDT_quotes_2018_07_11_marketord.csv"), OrderBook.addOrderMarket2Level1(CSV.getList(inputFile.toFile(), DateTypePriceAmount.class, 0),0.3,0.3));
		
		MeanCriterionWithPenaltyOnInventory hft = ((Builder) new MeanCriterionWithPenaltyOnInventory.Builder(
				new InputAnalysis.Builder()
				.inputFile(inputFile.toFile())
				.volumeProxy(300d)
				.maxTransitionMatrixSpread(new BigDecimal("4.0"))
				.build(),Utils.createOutputDirectory(outputDir,testName))
				.endTime(1000)
				.timeStep(10d)
				.volumeStep(30d)
				.gamma(0.0001)
				.maxVolM(300d)
				.maxVolT(300d)
				.lbShares(-2000d)
				.ubShares(2000d)
				.backTest(true)
				.backTestPeriods(99)
				.backTestStep(10d)
				.backTestDrift(0d)
				.backTestRuns(1000))
				.build();
		hft.run();
		return 1;
	}
	

}
