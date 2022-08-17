module timeTracker {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires MaterialFX;
	requires javafx.base;
	requires java.xml;
	requires java.logging;
	requires java.desktop;

	opens application to javafx.graphics, javafx.fxml;

	exports application;
}
