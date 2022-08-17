package application;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneChanger {
	private static HashMap<MyScenes, Parent> rootMap;
	private static HashMap<MyScenes, Updatable> controllerMap;
	private static Stage primaryStage;
	private static Scene main;
	private static MyScenes returnScene;
	private static boolean processingChange;

	@SuppressWarnings("exports")
	public static void initializeSC(Scene mainScene, Stage primaryStageFromMain) {
		rootMap = new HashMap<>();
		controllerMap = new HashMap<>();
		primaryStage = primaryStageFromMain;
		main = mainScene;
		returnScene = null;

		processingChange = false;
		primaryStage.xProperty().addListener((obs, oldVal, newVal) -> stageChanged());
		primaryStage.yProperty().addListener((obs, oldVal, newVal) -> stageChanged());
		primaryStage.focusedProperty().addListener((obs, oldVal, newVal) -> stageChanged());
	}

	@SuppressWarnings("exports")
	public static void addLoader(MyScenes name, FXMLLoader loader) throws IOException {
		Parent root = loader.load();
		rootMap.put(name, root);

		Object controller = loader.getController();
		if (controller instanceof Updatable) {
			controllerMap.put(name, (Updatable) controller);
		} else {
			controllerMap.put(name, null);
		}
	}

	public static void setScene(MyScenes name) {
		Updatable controller = controllerMap.get(name);
		if (controller != null) {
			controller.update();
		}
		main.setRoot(rootMap.get(name));
		primaryStage.sizeToScene();
	}

	public static void setSceneNoUpdate(MyScenes name) {
		main.setRoot(rootMap.get(name));
		primaryStage.sizeToScene();
	}

	@SuppressWarnings("exports")
	public static Updatable getController(MyScenes name) {
		return controllerMap.get(name);
	}

	public static void goToReturnScene() {
		setScene(returnScene);
	}

	public static void setReturnScene(MyScenes name) {
		returnScene = name;
	}

	public static void showWaitCursor(boolean show) {
		if (show) {
			main.setCursor(Cursor.WAIT);
		} else {
			main.setCursor(Cursor.DEFAULT);
		}
	}

	/* call deselect() on current scene when window moves or focus changes */
	private static void stageChanged() {
		if (!processingChange) {
			processingChange = true;
			Updatable controller = getController(getKeyByValue(rootMap, main.getRoot()));
			if (controller != null) {
				controller.deselect();
			}
			processingChange = false;
		}
	}

	/* not my code */
	private static <T, E> T getKeyByValue(Map<T, E> map, E value) {
		for (Entry<T, E> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}
}
