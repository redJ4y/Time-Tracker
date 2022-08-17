package application;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class CustomEntryController implements Initializable, Updatable {
	private Model model;
	DateTimeFormatter timeFormatter;
	private boolean editMode;
	private FileEntry entryToEdit;
	private TranslateTransition labelAnimationUp, labelAnimationDown;
	private boolean startTimeFieldClicked;
	private boolean durationFieldClicked;

	private boolean jobValid;
	private boolean descriptionValid;
	private boolean dateValid;
	private boolean startTimeValid;
	private boolean durationValid;

	@FXML
	private MFXComboBox<String> jobDropdown;
	@FXML
	private MFXComboBox<String> descriptionDropdown;
	@FXML
	private DatePicker datePicker;
	@FXML
	private MFXTextField startTimeField;
	@FXML
	private MFXTextField durationField;
	@FXML
	private MFXButton enterButton;
	@FXML
	private Label timeLabel;
	@FXML
	private AnchorPane parentAnchorPane;
	@FXML
	private Pane submittedLabel;
	@FXML
	private Label editingLabel;

	@FXML
	private Circle jobCircle;
	@FXML
	private Circle descriptionCircle;
	@FXML
	private Circle dateCircle;
	@FXML
	private Circle startTimeCircle;
	@FXML
	private Circle durationCircle;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		model = ModelKeeper.getModel();
		timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
		editMode = false;
		entryToEdit = null;
		initializeTransitions();

		jobDropdown.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			jobCircle.setFill(Color.web("#8c40ff")); // validate job
			jobValid = true;
			tryEnableEnterButton();
		});
		descriptionDropdown.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			descriptionCircle.setFill(Color.web("#8c40ff")); // validate description
			descriptionValid = true;
			tryEnableEnterButton();
		});
	}

	@Override
	public void update() {
		jobDropdown.getItems().setAll(model.getJobs());
		descriptionDropdown.getItems().setAll(model.getDescriptions());
		datePicker.setValue(null);
		startTimeField.clear();
		durationField.clear();
		startTimeFieldClicked = false;
		durationFieldClicked = false;
		enterButton.setDisable(true);
		timeLabel.setText("");

		jobValid = false;
		descriptionValid = false;
		dateValid = false;
		startTimeValid = false;
		durationValid = false;

		jobCircle.setFill(Color.web("#f4eae6"));
		descriptionCircle.setFill(Color.web("#f4eae6"));
		dateCircle.setFill(Color.web("#f4eae6"));
		startTimeCircle.setFill(Color.web("#f4eae6"));
		durationCircle.setFill(Color.web("#f4eae6"));

		editingLabel.setVisible(false);
		if (editMode && entryToEdit != null) {
			editingLabel.setVisible(true);

			if (!jobDropdown.getItems().contains(entryToEdit.getJob())) {
				jobDropdown.getItems().add(entryToEdit.getJob());
			}
			if (!descriptionDropdown.getItems().contains(entryToEdit.getDescription())) {
				descriptionDropdown.getItems().add(entryToEdit.getDescription());
			}

			jobDropdown.getSelectionModel().selectItem(entryToEdit.getJob());
			descriptionDropdown.getSelectionModel().selectItem(entryToEdit.getDescription());
			datePicker.setValue(LocalDate.parse(entryToEdit.getDateDisplay(), DateTimeFormatter.ofPattern("M/d/yy")));
			validateDate();
			startTimeField.setText(entryToEdit.getStartTimeDisplay());
			startTimeFieldClicked = true;
			validateStartTime();
			durationField.setText(entryToEdit.getDurationDisplay());
			durationFieldClicked = true;
			validateDuration();

			if (!enterButton.isDisabled()) {
				enterButton.setDisable(true);
			}
			timeLabel.setText("");
		} else if (editMode && entryToEdit == null) {
			TTErrorLogger.log("Custom entry edit mode misuse (CRASH)");
			Platform.exit();
		}
	}

	public void enterPressed() {
		enterButton.setDisable(true); // block additional clicks

		TimeEntry customEntry = new TimeEntry();
		try {
			customEntry.setJob(jobDropdown.getSelectedValue());
			customEntry.setDescripton(descriptionDropdown.getSelectedValue());
			LocalDate startTimeDate = datePicker.getValue();
			LocalTime startTimeTime = parseTime(startTimeField.getText().trim().toUpperCase());
			LocalDateTime startTime = LocalDateTime.of(startTimeDate, startTimeTime);
			customEntry.setStartTime(startTime);
			customEntry.setEndTime(startTime.plus(parseDuration(durationField.getText().trim())));
		} catch (Exception e) {
			customEntry = null;
		}

		if (customEntry != null) {
			if (customEntry.getJob() != null && customEntry.getDescripton() != null) {
				if (!customEntry.getJob().equals("NO INPUT") && !customEntry.getDescripton().equals("NO INPUT")) {
					if (customEntry.getStartTime() != null && customEntry.getEndTime() != null) {
						if (customEntry.getStartTime().getYear() > 2021) {
							if (editMode) {
								if (!entryToEdit.toString().equals((new FileEntry(customEntry)).toString())) {
									if (model.removeEntry(entryToEdit)) {
										if (model.saveEntry(customEntry)) {
											((ViewTimeController) SceneChanger.getController(MyScenes.VIEWTIME))
													.search();
											editMode = false;
											entryToEdit = null;
											SceneChanger.setSceneNoUpdate(MyScenes.VIEWTIME);
										} else {
											if (model.saveFileEntry(entryToEdit)) {
												timeLabel.setText("ERROR: Failed to save changes");
											} else {
												timeLabel.setText("ERROR: Entry lost, screenshot and re-add");
											}
										}
									} else {
										timeLabel.setText("ERROR: Failed to edit entry");
									}
								} else {
									timeLabel.setText("No change detected: Retry");
								}
							} else {
								if (model.saveEntry(customEntry)) {
									startTimeField.clear();
									durationField.clear();
									startTimeFieldClicked = false;
									durationFieldClicked = false;
									startTimeValid = false;
									durationValid = false;
									startTimeCircle.setFill(Color.web("#f4eae6"));
									durationCircle.setFill(Color.web("#f4eae6"));
									timeLabel.setText("");
									showConfirmation();
								} else {
									timeLabel.setText("ERROR: Failed to save");
								}
							}
						} else {
							if (!editMode) {
								update();
							}
							timeLabel.setText("Invalid date: Retry");
						}
					} else {
						if (!editMode) {
							update();
						}
						timeLabel.setText("Invalid time: Retry");
					}
				} else {
					if (!editMode) {
						update();
					}
					timeLabel.setText("Invalid text: Retry");
				}
			} else {
				if (!editMode) {
					update();
				}
				timeLabel.setText("Invalid text: Retry");
			}
		} else {
			if (!editMode) {
				update();
			}
			timeLabel.setText("Parsing failed: Retry");
		}
	}

	private void tryEnableEnterButton() {
		if (jobValid && descriptionValid && dateValid && startTimeValid && durationValid) {
			if (enterButton.isDisabled()) {
				enterButton.setDisable(false);
				clearSubmittedLabel();
			}
			Duration duration = parseDuration(durationField.getText().trim());
			if (duration != null) {
				updateTimeLabel(duration);
			}
		} else {
			if (!enterButton.isDisabled()) {
				enterButton.setDisable(true);
			}
			timeLabel.setText("");
		}
	}

	public void startTimeFieldSelected() {
		if (!startTimeFieldClicked) {
			startTimeFieldClicked = true;
			startTimeField.setText("12:00 PM");
			startTimeField.selectAll();
			validateStartTime();
		}
	}

	public void durationFieldSelected() {
		if (!durationFieldClicked) {
			durationFieldClicked = true;
			durationField.setText("1:00");
			durationField.selectAll();
			validateDuration();
		}
	}

	public void validateDate() {
		dateCircle.setFill(Color.web("#8c40ff"));
		dateValid = true;
		tryEnableEnterButton();
	}

	public void validateStartTime() {
		String text = startTimeField.getText().trim().toUpperCase();
		if (!text.isEmpty()) {
			if (text.contains(":") && (text.contains(" AM") || text.contains(" PM"))) {
				if (parseTime(text) != null) {
					startTimeCircle.setFill(Color.web("#8c40ff")); // purple
					startTimeValid = true;
					tryEnableEnterButton();
				} else {
					startTimeCircle.setFill(Color.web("#ee4e56")); // red
					startTimeValid = false;
					tryEnableEnterButton();
				}
			} else {
				startTimeCircle.setFill(Color.web("#ee4e56")); // red
				startTimeValid = false;
				tryEnableEnterButton();
			}
		} else {
			startTimeCircle.setFill(Color.web("#f4eae6")); // empty
			startTimeValid = false;
			tryEnableEnterButton();
		}
	}

	public void validateDuration() {
		String text = durationField.getText().trim();
		if (!text.isEmpty()) {
			Duration duration = parseDuration(text);
			if (duration != null && !duration.isZero()) {
				durationCircle.setFill(Color.web("#8c40ff")); // purple
				durationValid = true;
				tryEnableEnterButton();
			} else {
				durationCircle.setFill(Color.web("#ee4e56")); // red
				durationValid = false;
				tryEnableEnterButton();
			}
		} else {
			durationCircle.setFill(Color.web("#f4eae6")); // empty
			durationValid = false;
			tryEnableEnterButton();
		}
	}

	@Override
	public void deselect() {
		if (!parentAnchorPane.isFocused()) {
			parentAnchorPane.requestFocus();
			parentAnchorPane.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
					true, true, true, true, true, true, true, true, true, true, null));
		}
	}

	public void setEditMode(boolean editMode, FileEntry entryToEdit) {
		this.editMode = editMode;
		this.entryToEdit = entryToEdit;
	}

	public void exitScene() {
		if (editMode) {
			editMode = false;
			entryToEdit = null;
			SceneChanger.setSceneNoUpdate(MyScenes.VIEWTIME);
		} else {
			editMode = false;
			entryToEdit = null;
			SceneChanger.goToReturnScene();
		}
	}

	private void updateTimeLabel(Duration duration) {
		int minutes = (int) duration.toMinutes();
		int hours = minutes / 60;
		minutes = minutes % 60;

		String text = "";
		if (hours > 0) {
			text = hours + (hours == 1 ? " hour" : " hours");
			if (minutes > 0) {
				text = text + " " + minutes + (minutes == 1 ? " minute" : " minutes");
			}
		} else {
			if (minutes > 0) {
				text = minutes + (minutes == 1 ? " minute" : " minutes");
			}
		}
		timeLabel.setText(text);
	}

	private LocalTime parseTime(String text) {
		LocalTime time = null;
		try {
			time = LocalTime.parse(text, timeFormatter);
		} catch (DateTimeParseException e) {
			time = null; // could not parse
		}
		return time;
	}

	private Duration parseDuration(String text) {
		Duration duration = null;
		if (text.contains(":") && !(text.contains("-") || text.contains("+"))) {
			String[] sections = text.split(":");
			if (sections.length == 2) {
				int hours = 0;
				try {
					hours = Integer.parseInt(sections[0]);
				} catch (Exception e) {
					return null; // could not parse hours
				}

				int minutes = 0;
				if (sections[1].length() == 2) {
					try {
						minutes = Integer.parseInt(sections[1]);
					} catch (Exception e) {
						return null; // could not parse minutes
					}
					if (minutes > 59) {
						return null; // invalid minutes value
					}
				} else {
					return null; // invalid minute character count
				}

				duration = Duration.ofMinutes((hours * 60) + minutes);
			}
		}
		return duration;
	}

	private void showConfirmation() {
		clearSubmittedLabel();
		submittedLabel.setVisible(true);
		labelAnimationUp.playFromStart();
	}

	private void clearSubmittedLabel() {
		final double INITIAL_Y = 378.0;

		submittedLabel.setVisible(false);
		labelAnimationUp.stop();
		labelAnimationDown.stop();
		submittedLabel.setTranslateY(0);
		submittedLabel.relocate(submittedLabel.getLayoutX(), INITIAL_Y);
	}

	private void initializeTransitions() {
		final double VERTICAL_SHIFT = 44.0;

		labelAnimationDown = new TranslateTransition(javafx.util.Duration.millis(50), submittedLabel);
		labelAnimationDown.setDelay(javafx.util.Duration.seconds(1.2));
		labelAnimationDown.setByY(VERTICAL_SHIFT + submittedLabel.getHeight());
		labelAnimationDown.setOnFinished(hideEvent -> submittedLabel.setVisible(false));

		labelAnimationUp = new TranslateTransition(javafx.util.Duration.millis(150), submittedLabel);
		labelAnimationUp.setByY(-VERTICAL_SHIFT);
		labelAnimationUp.setOnFinished(event -> labelAnimationDown.playFromStart());
	}
}
