package application;

public class ModelKeeper {
	private static Model model;

	public static void initializeModel() {
		model = new Model();
	}

	public static Model getModel() {
		return model;
	}

	public static boolean modelHasExistingEntry() {
		return model.checkForExistingEntry();
	}
}
