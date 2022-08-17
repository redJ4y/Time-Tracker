package application;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class TTErrorLogger {
	public static void log(String error) {
		try {
			Logger logger = Logger.getLogger("TTErrorLogger");
			FileHandler fileHandler = new FileHandler("temp/errors.log", true);
			logger.addHandler(fileHandler);
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			logger.info("ERROR: " + error + "\n");
			fileHandler.flush();
			fileHandler.close();
		} catch (Exception e) {
			System.out.println("ERROR: TTErrorLogger failed LOL");
		}
	}
}
