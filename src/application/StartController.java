package application;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXListView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.enums.ButtonType;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class StartController implements Initializable, Updatable {
	private Model model;
	private boolean buttonReady; // prevents double clicks
	private TranslateTransition labelAnimationUp, labelAnimationDown;
	private Text textWidthRuler;

	@FXML
	private MFXButton startButton;
	@FXML
	private MFXComboBox<String> jobDropdown;
	@FXML
	private MFXComboBox<String> descriptionDropdown;
	@FXML
	private Label missingInfoLabel;
	@FXML
	private Pane timeLoggedLabel;
	@FXML
	private HBox topBar;
	@FXML
	private Label logFileFoundLabel;
	@FXML
	private AnchorPane parentAnchorPane;

	@FXML
	private Pane descriptionsEditor;
	@FXML
	private MFXListView<String> descriptionsListView;
	@FXML
	private MFXTextField newDescriptionTextField;
	@FXML
	private MFXButton removeDescriptionButton;
	@FXML
	private MFXButton addDescriptionButton;
	@FXML
	private Label descriptionLengthWarning;

	@FXML
	private Pane jobsEditor;
	@FXML
	private MFXListView<String> jobsListView;
	@FXML
	private MFXTextField clientTextField;
	@FXML
	private MFXTextField jobTextField;
	@FXML
	private MFXButton removeJobButton;
	@FXML
	private MFXButton addJobButton;
	@FXML
	private Label jobLengthWarning;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		model = ModelKeeper.getModel();
		buttonReady = false;
		textWidthRuler = new Text();
		textWidthRuler.setFont(newDescriptionTextField.getFont());
		initializeListeners();
		initializeBarHover();
		initializeTransitions();
		updateDropdowns();

		if ((new File("temp/logs.log")).exists()) {
			logFileFoundLabel.setVisible(true);
		}
	}

	@Override
	public void update() {
		buttonReady = true;
	}

	public void startPressed() {
		if (buttonReady) {
			buttonReady = false;
			String job = jobDropdown.getSelectedValue();
			String description = descriptionDropdown.getSelectedValue();
			if (job != null && description != null) {
				model.setSelectedJob(job);
				model.setSelectedDescription(description);
				model.startTime();

				missingInfoLabel.setVisible(false);
				jobDropdown.setUnfocusedLineColor(Color.web("#ababab"));
				descriptionDropdown.setUnfocusedLineColor(Color.web("#ababab"));

				SceneChanger.setScene(MyScenes.STOP);
			} else {
				missingInfoLabel.setVisible(true);
				jobDropdown.setUnfocusedLineColor(Color.web("#ee4e56"));
				descriptionDropdown.setUnfocusedLineColor(Color.web("#ee4e56"));
				buttonReady = true;
			}
		}
	}

	public void customEntryPressed() {
		buttonReady = false;
		SceneChanger.setReturnScene(MyScenes.START);
		SceneChanger.setScene(MyScenes.CUSTOMENTRY);
	}

	public void viewTimePressed() {
		buttonReady = false;
		SceneChanger.setReturnScene(MyScenes.START);
		SceneChanger.setScene(MyScenes.VIEWTIME);
	}

	public void addDescriptionClicked() {
		String newDescription = newDescriptionTextField.getText();
		if (newDescription != null) {
			newDescription = newDescription.trim();
			if (!newDescription.isEmpty()) {
				if (!model.getDescriptions().contains(newDescription)) {
					model.addDescription(newDescription);
					newDescriptionTextField.clear();
					addDescriptionButton.setDisable(true);

					descriptionsListView.getItems().add(newDescription); // update display
					if (descriptionsListView.getItems().size() == 9) {
						descriptionsLVShowScrollBars(true);
					} // fix MFXListView bug
				}
			}
		}
	}

	public void addJobClicked() {
		String clientText = clientTextField.getText();
		String jobText = jobTextField.getText();
		if (clientText != null && jobText != null) {
			String delimiter = " - ";
			if (clientText.isEmpty() || jobText.isEmpty()) {
				delimiter = "";
			}
			clientText = clientText.trim();
			jobText = jobText.trim();
			String newJob = clientText + delimiter + jobText;
			newJob = newJob.trim();
			if (!newJob.isEmpty()) {
				if (!model.getJobs().contains(newJob)) {
					model.addJob(newJob);
					clientTextField.clear();
					jobTextField.clear();
					addJobButton.setDisable(true);

					jobsListView.getItems().add(newJob); // update display
					if (jobsListView.getItems().size() == 9) {
						jobsLVShowScrollBars(true);
					} // fix MFXListView bug
				}
			}
		}
	}

	public void removeDescriptionClicked() {
		if (!descriptionsListView.getSelectionModel().isEmpty()) {
			int index = descriptionsListView.getSelectionModel().getSelectedIndex();
			model.removeDescription(index);
			descriptionsListView.getSelectionModel().clearSelection();

			descriptionsListView.getItems().remove(index); // update display
			if (descriptionsListView.getItems().size() == 8) {
				descriptionsLVShowScrollBars(false);
			} // fix MFXListView bug
			removeDescriptionButton.setDisable(true);
		}
	}

	public void removeJobClicked() {
		if (!jobsListView.getSelectionModel().isEmpty()) {
			int index = jobsListView.getSelectionModel().getSelectedIndex();
			model.removeJob(index);
			jobsListView.getSelectionModel().clearSelection();

			jobsListView.getItems().remove(index); // update display
			if (jobsListView.getItems().size() == 8) {
				jobsLVShowScrollBars(false);
			} // fix MFXListView bug
			removeJobButton.setDisable(true);
		}
	}

	/* For manually hiding descriptionsListView scroll bars (MFXListView bug) */
	private void descriptionsLVShowScrollBars(boolean visibleStatus) {
		for (Node currentNode : descriptionsListView.getChildrenUnmodifiable()) {
			if (currentNode instanceof ScrollBar) {
				ScrollBar currentScrollBar = (ScrollBar) currentNode;
				if (currentScrollBar.visibleProperty().isBound()) {
					currentScrollBar.visibleProperty().unbind();
				}
				if (currentScrollBar.getOrientation() == Orientation.VERTICAL) {
					currentScrollBar.setVisible(visibleStatus);
				} else {
					currentScrollBar.setVisible(false); // always hide horizontal scroll bar
				}
			}
		}
	}

	/* For manually hiding jobsListView scroll bars (MFXListView bug) */
	private void jobsLVShowScrollBars(boolean visibleStatus) {
		for (Node currentNode : jobsListView.getChildrenUnmodifiable()) {
			if (currentNode instanceof ScrollBar) {
				ScrollBar currentScrollBar = (ScrollBar) currentNode;
				if (currentScrollBar.visibleProperty().isBound()) {
					currentScrollBar.visibleProperty().unbind();
				}
				if (currentScrollBar.getOrientation() == Orientation.VERTICAL) {
					currentScrollBar.setVisible(visibleStatus);
				} else {
					currentScrollBar.setVisible(false); // always hide horizontal scroll bar
				}
			}
		}
	}

	public void descriptionLengthCheck() {
		textWidthRuler.setText(newDescriptionTextField.getText().trim());
		double width = textWidthRuler.getBoundsInLocal().getWidth();

		if (width > 0.0) {
			if (addDescriptionButton.isDisabled()) {
				addDescriptionButton.setDisable(false);
			}
			if (width > 155.0) {
				descriptionLengthWarning.setVisible(true);
			} else {
				if (descriptionLengthWarning.isVisible()) {
					descriptionLengthWarning.setVisible(false);
				}
			}
		} else {
			addDescriptionButton.setDisable(true);
		}
	}

	public void jobLengthCheck() {
		String clientText = clientTextField.getText().trim();
		String jobText = jobTextField.getText().trim();
		String delimiter = " - ";
		if (clientText.isEmpty() || jobText.isEmpty()) {
			delimiter = "";
		}
		textWidthRuler.setText(clientText + delimiter + jobText);
		double width = textWidthRuler.getBoundsInLocal().getWidth();

		if (width > 0.0) {
			if (addJobButton.isDisabled()) {
				addJobButton.setDisable(false);
			}
			if (width > 155.0) {
				jobLengthWarning.setVisible(true);
			} else {
				if (jobLengthWarning.isVisible()) {
					jobLengthWarning.setVisible(false);
				}
			}
		} else {
			addJobButton.setDisable(true);
		}
	}

	public void openDescriptionsEditor() {
		buttonReady = false;
		descriptionsListView.getItems().setAll(model.getDescriptions());
		if (descriptionsListView.getItems().size() > 8) {
			descriptionsLVShowScrollBars(true);
		} else {
			descriptionsLVShowScrollBars(false);
		} // fix MFXListView bug
		removeDescriptionButton.setDisable(true);
		addDescriptionButton.setDisable(true);
		newDescriptionTextField.clear();
		descriptionLengthWarning.setVisible(false);
		descriptionsEditor.setVisible(true);
	}

	public void openJobsEditor() {
		buttonReady = false;
		jobsListView.getItems().setAll(model.getJobs());
		if (jobsListView.getItems().size() > 8) {
			jobsLVShowScrollBars(true);
		} else {
			jobsLVShowScrollBars(false);
		} // fix MFXListView bug
		removeJobButton.setDisable(true);
		addJobButton.setDisable(true);
		clientTextField.clear();
		jobTextField.clear();
		jobLengthWarning.setVisible(false);
		jobsEditor.setVisible(true);
	}

	public void closeDescriptionsEditor() {
		updateDropdowns();
		descriptionsEditor.setVisible(false);
		buttonReady = true;
	}

	public void closeJobsEditor() {
		updateDropdowns();
		jobsEditor.setVisible(false);
		buttonReady = true;
	}

	@Override
	public void deselect() {
		if (!parentAnchorPane.isFocused()) {
			parentAnchorPane.requestFocus();
			parentAnchorPane.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
					true, true, true, true, true, true, true, true, true, true, null));
		}
	}

	public void showConfirmation() {
		final double INITIAL_Y = 378.0;

		timeLoggedLabel.setVisible(false);
		labelAnimationUp.stop();
		labelAnimationDown.stop();
		timeLoggedLabel.setTranslateY(0);
		timeLoggedLabel.relocate(timeLoggedLabel.getLayoutX(), INITIAL_Y);

		timeLoggedLabel.setVisible(true);
		labelAnimationUp.playFromStart();
	}

	private void initializeTransitions() {
		final double VERTICAL_SHIFT = 70.0;

		labelAnimationDown = new TranslateTransition(Duration.millis(50), timeLoggedLabel);
		labelAnimationDown.setDelay(Duration.seconds(2));
		labelAnimationDown.setByY(VERTICAL_SHIFT + timeLoggedLabel.getHeight());
		labelAnimationDown.setOnFinished(hideEvent -> timeLoggedLabel.setVisible(false));

		labelAnimationUp = new TranslateTransition(Duration.millis(250), timeLoggedLabel);
		labelAnimationUp.setByY(-VERTICAL_SHIFT);
		labelAnimationUp.setOnFinished(event -> labelAnimationDown.playFromStart());
	}

	private void updateDropdowns() {
		jobDropdown.getItems().setAll(model.getJobs());
		descriptionDropdown.getItems().setAll(model.getDescriptions());
	}

	private void initializeListeners() {
		descriptionsListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				removeDescriptionButton.setDisable(false);
			}
		});

		jobsListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				removeJobButton.setDisable(false);
			}
		});
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
}
