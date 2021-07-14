import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
		MeanCriterionWithPenaltyOnInventory hft = ((Builder) new MeanCriterionWithPenaltyOnInventory.Builder(inputFile.toFile(),0.5,1000d,outputDir,testName)
				.endTime(300)
				.volumeStep(30d)
				.gamma(0.0002)
				.backTestPeriods(10000)
				.backTestStep(8d)
				.backTestDrift(0d))
				.build();
		hft.run();
		return 1;
	}
	

}
