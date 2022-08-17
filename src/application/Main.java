package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;

public class Main extends Application {

	@SuppressWarnings("exports")
	@Override
	public void start(Stage primaryStage) {
		try {
			primaryStage.setResizable(false);
			primaryStage.setTitle("Time Tracker");
			primaryStage.getIcons()
					.add(new Image(getClass().getResourceAsStream("/customStyles/icons/applicationIcon.png")));
			Parent initialRoot = FXMLLoader.load(getClass().getResource("/loadingGUI.fxml"));
			Scene scene = new Scene(initialRoot);
			primaryStage.setScene(scene);
			primaryStage.show();

			ModelKeeper.initializeModel(); // initialize model

			SceneChanger.initializeSC(scene, primaryStage); // initialize SceneChanger
			SceneChanger.addLoader(MyScenes.START, new FXMLLoader(getClass().getResource("/startGUI.fxml")));
			SceneChanger.addLoader(MyScenes.STOP, new FXMLLoader(getClass().getResource("/stopGUI.fxml")));
			SceneChanger.addLoader(MyScenes.CUSTOMENTRY,
					new FXMLLoader(getClass().getResource("/customEntryGUI.fxml")));
			SceneChanger.addLoader(MyScenes.VIEWTIME, new FXMLLoader(getClass().getResource("/viewTimeGUI.fxml")));

			if (ModelKeeper.modelHasExistingEntry()) {
				SceneChanger.setScene(MyScenes.STOP);
			} else {
				SceneChanger.setScene(MyScenes.START);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		ModelKeeper.getModel().appClosed();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
