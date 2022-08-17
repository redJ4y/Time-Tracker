package application;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.enums.ButtonType;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class StopController implements Initializable, Updatable {
	private Model model;
	private boolean buttonReady; // prevents double clicks
	private LocalDateTime timeClosed;

	private int timerMinutes;
	private Timeline recordingTimer;

	@FXML
	private MFXButton stopButton;
	@FXML
	private Label jobDescriptionText;
	@FXML
	private Label recordingText;
	@FXML
	private Pane stopWhenClosedPrompt;
	@FXML
	private Label timeClosedText;
	@FXML
	private HBox topBar;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		model = ModelKeeper.getModel();
		buttonReady = false;
		initializeBarHover();
		timeClosed = model.getTimeClosed(); // null written to file
		if (timeClosed != null) {
			timeClosedText.setText("Time: " + timeClosed.format(DateTimeFormatter.ofPattern("h:mm a M/d/yy")));
			stopWhenClosedPrompt.setVisible(true);
			PauseTransition hidePrompt = new PauseTransition(Duration.seconds(70));
			hidePrompt.setOnFinished(event -> closeSWCPrompt());
			hidePrompt.play();
		}

		timerMinutes = -1;
		recordingTimer = new Timeline(new KeyFrame(Duration.minutes(1), event -> updateRecordingText()));
		recordingTimer.setCycleCount(Animation.INDEFINITE);
	}

	@Override
	public void update() {
		String labelText = model.getSelectedJob() + " (" + model.getSelectedDescription() + ")";
		jobDescriptionText.setText(labelText);

		timerMinutes = model.getTimerStartingMinutes();
		updateRecordingText();
		recordingTimer.play();

		buttonReady = true;
	}

	public void stopPressed() {
		if (buttonReady) {
			buttonReady = false;
			model.stopTime();
			recordingTimer.stop();
			stopWhenClosedPrompt.setVisible(false);
			SceneChanger.setScene(MyScenes.START);
		}
	}

	public void customEntryPressed() {
		buttonReady = false;
		recordingTimer.stop();
		SceneChanger.setReturnScene(MyScenes.STOP);
		SceneChanger.setScene(MyScenes.CUSTOMENTRY);
	}

	public void viewTimePressed() {
		buttonReady = false;
		recordingTimer.stop();
		SceneChanger.setReturnScene(MyScenes.STOP);
		SceneChanger.setScene(MyScenes.VIEWTIME);
	}

	public void stopWhenClosedPressed() {
		if (buttonReady && timeClosed != null) {
			buttonReady = false;
			model.stopTimeAt(timeClosed);
			recordingTimer.stop();
			stopWhenClosedPrompt.setVisible(false);
			SceneChanger.setScene(MyScenes.START);
		}
	}

	public void closeSWCPrompt() {
		if (stopWhenClosedPrompt.isVisible()) {
			TranslateTransition promptHideAnimation;
			promptHideAnimation = new TranslateTransition(Duration.millis(300), stopWhenClosedPrompt);
			promptHideAnimation.setByY(stopWhenClosedPrompt.getHeight() + 15);
			promptHideAnimation.setOnFinished(hideEvent -> stopWhenClosedPrompt.setVisible(false));
			promptHideAnimation.playFromStart();
		}
	}

	private void updateRecordingText() {
		timerMinutes++;
		int hours = timerMinutes / 60;
		int minutes = timerMinutes % 60;
		recordingText.setText(String.format("Recording (%d:%02d)", hours, minutes));
	}

	private void initializeBarHover() {
		ObservableList<Node> topBarNodes = topBar.getChildren();
		for (Node currentNode : topBarNodes) {
			if (currentNode instanceof MFXButton) {
				MFXButton currentButton = (MFXButton) currentNode;

				currentButton.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent e) {
						currentButton.setStyle("-fx-background-color: white;");
						currentButton.setButtonType(ButtonType.RAISED);
					}
				});

				currentButton.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent e) {
						currentButton.setStyle("-fx-background-color: #ffffff00;");
						currentButton.setButtonType(ButtonType.FLAT);
					}
				});
			}
		}
	}

	@Override
	public void deselect() {
		// Do nothing - no need to deselect here
	}
}
