package application;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.legacy.MFXLegacyTableView;
import io.github.palexdev.materialfx.effects.DepthLevel;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class ViewTimeController implements Initializable, Updatable {
	private Model model;
	private Duration timeLogged;
	private Duration timeLoggedSelection;
	private boolean roundTimeLogged;
	private boolean searching;
	private boolean hasSearched;
	private boolean confirmPurgeReady;
	private boolean confirmRemoveReady;
	private FileEntry entryToRemove;
	private Label noResultsLabel;

	@FXML
	private MFXComboBox<String> jobDropdown;
	@FXML
	private MFXComboBox<String> descriptionDropdown;
	@FXML
	private MFXComboBox<String> timePeriodDropdown;
	@FXML
	private MFXLegacyTableView<FileEntry> table;
	@FXML
	private MFXButton searchButton;
	@FXML
	private MFXButton editButton;
	@FXML
	private MFXButton removeButton;
	@FXML
	private Label timeLoggedTitle;
	@FXML
	private Label timeLabel;
	@FXML
	private Label timeHoursLabel;
	@FXML
	private MFXRadioButton roundingToggle;
	@FXML
	private AnchorPane parentAnchorPane;

	@FXML
	private Pane confirmPurgePrompt;
	@FXML
	private Pane confirmRemovePrompt;
	@FXML
	private Label confirmRemovalLabel1;
	@FXML
	private Label confirmRemovalLabel2;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		model = ModelKeeper.getModel();
		roundTimeLogged = roundingToggle.isSelected();
		initializeDropdownListeners();

		TableColumn<FileEntry, String> jobColumn = new TableColumn<>("Job");
		jobColumn.setCellValueFactory(job -> new ReadOnlyStringWrapper(job.getValue().getJob()));
		TableColumn<FileEntry, String> descriptionColumn = new TableColumn<>("Description");
		descriptionColumn.setCellValueFactory(desc -> new ReadOnlyStringWrapper(desc.getValue().getDescription()));
		TableColumn<FileEntry, String> dateColumn = new TableColumn<>("Date");
		dateColumn.setCellValueFactory(date -> new ReadOnlyStringWrapper(date.getValue().getDateDisplay()));
		TableColumn<FileEntry, String> startTimeColumn = new TableColumn<>("Start Time");
		startTimeColumn.setCellValueFactory(start -> new ReadOnlyStringWrapper(start.getValue().getStartTimeDisplay()));
		TableColumn<FileEntry, String> durationColumn = new TableColumn<>("Duration");
		durationColumn.setCellValueFactory(dur -> new ReadOnlyStringWrapper(dur.getValue().getDurationDisplay()));
		table.getColumns().addAll(jobColumn, descriptionColumn, dateColumn, startTimeColumn, durationColumn);

		noResultsLabel = new Label("No Results");
		noResultsLabel.setTextFill(Color.web("#e47272"));
		noResultsLabel.setStyle("-fx-font-size: 14px;");
		noResultsLabel.setTextAlignment(TextAlignment.CENTER);
		table.setPlaceholder(noResultsLabel);

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.getSelectionModel().getSelectedIndices().addListener(new ListChangeListener<Integer>() {
			@Override
			public void onChanged(Change<? extends Integer> c) {
				tableSelectionChanged();
			}
		});
	}

	@Override
	public void update() {
		timeLogged = Duration.ZERO;
		timeLoggedSelection = null;
		searching = false;
		confirmPurgeReady = false;
		confirmRemoveReady = false;
		entryToRemove = null;

		noResultsLabel.setVisible(false);
		editButton.setVisible(false);
		removeButton.setVisible(false);
		table.getItems().clear();
		table.refresh();
		fixTableScrollBar();
		updateTimeDisplay(0);

		jobDropdown.getItems().clear();
		jobDropdown.getItems().add("Any Job "); // trailing space makes it unique
		jobDropdown.getItems().addAll(model.getAllUniqueJobs());
		descriptionDropdown.getItems().clear();
		descriptionDropdown.getItems().add("Any Description "); // trailing space makes it unique
		descriptionDropdown.getItems().addAll(model.getAllUniqueDescriptions());
		timePeriodDropdown.getItems().clear();
		timePeriodDropdown.getItems().add("Any Time");
		timePeriodDropdown.getItems().add("Today");
		for (int i = CustomDateTime.daysThisWeek().length; i > 0; i--) {
			timePeriodDropdown.getItems().add(DayOfWeek.of(i).getDisplayName(TextStyle.FULL, Locale.US));
		}
		timePeriodDropdown.getItems().add("Since Week Start");
		timePeriodDropdown.getItems().add("Since Month Start");

		jobDropdown.getSelectionModel().selectItem("Any Job ");
		descriptionDropdown.getSelectionModel().selectItem("Any Description ");
		timePeriodDropdown.getSelectionModel().selectItem("Any Time");
		searchButton.setStyle("-fx-background-color: #b68bfc;");
		searchButton.setDepthLevel(DepthLevel.LEVEL1);
		hasSearched = false;
	}

	public void search() {
		if (!searching) {
			searching = true;
			SceneChanger.showWaitCursor(true);
			if (!noResultsLabel.isVisible()) {
				noResultsLabel.setVisible(true); // enable "no results" after first search
			}

			String selectedTimePeriod = timePeriodDropdown.getSelectedValue();
			String selectedJob = jobDropdown.getSelectedValue();
			String selectedDescription = descriptionDropdown.getSelectedValue();

			long searchTimePeriod = 0;
			String searchJob = null;
			String searchDescription = null;
			boolean specific = false;

			if (selectedTimePeriod != null && !selectedTimePeriod.isEmpty()) {
				switch (selectedTimePeriod) {
				case "Today":
					searchTimePeriod = CustomDateTime.today();
					specific = true;
					break;
				case "Since Week Start":
					searchTimePeriod = CustomDateTime.thisWeekStart();
					break;
				case "Since Month Start":
					searchTimePeriod = CustomDateTime.thisMonthStart();
					break;
				default:
					if (!selectedTimePeriod.equals("Any Time")) {
						int index = DayOfWeek.valueOf(selectedTimePeriod.toUpperCase()).getValue() - 1;
						long[] daysThisWeek = CustomDateTime.daysThisWeek();
						if (index >= 0 && index < daysThisWeek.length) {
							searchTimePeriod = daysThisWeek[index];
							specific = true;
						}
					}
				}
			}
			if (selectedJob != null && !selectedJob.isEmpty()) {
				if (!selectedJob.equals("Any Job ")) {
					searchJob = selectedJob;
				}
			}
			if (selectedDescription != null && !selectedDescription.isEmpty()) {
				if (!selectedDescription.equals("Any Description ")) {
					searchDescription = selectedDescription;
				}
			}

			ArrayList<FileEntry> results = model.searchData(searchTimePeriod, searchJob, searchDescription, specific);
			timeLogged = Duration.ZERO;
			for (FileEntry currentEntry : results) {
				timeLogged = timeLogged.plus(currentEntry.getDuration());
			}

			ObservableList<FileEntry> tableData = FXCollections.observableArrayList(results);
			results.clear(); // don't wait for garbage collection
			table.setItems(tableData);
			table.refresh();
			fixTableScrollBar();
			updateTimeDisplay(0);
			searchButton.setStyle("-fx-background-color: #ddc9ff;");
			searchButton.setDepthLevel(DepthLevel.LEVEL0);
			hasSearched = true;

			SceneChanger.showWaitCursor(false);
			searching = false;
		}
	}

	private void updateTimeDisplay(int numSelected) {
		// numSelected = -1 only changes rounding
		// numSelected = 0 is for updating to complete search values
		// numSelected > 1 is for updating to selection values
		Duration timeLoggedToUse = Duration.ZERO;
		if (numSelected != -1) {
			if (numSelected > 1 && timeLoggedSelection != null) {
				timeLoggedToUse = timeLoggedSelection;
				timeLoggedTitle.setText("Time Logged (" + numSelected + " selected)");
			} else {
				timeLoggedToUse = timeLogged;
				timeLoggedTitle.setText("Time Logged");
			}
		} else {
			if (timeLoggedSelection != null
					&& timeLoggedTitle.getText().charAt(timeLoggedTitle.getText().length() - 1) == ')') {
				timeLoggedToUse = timeLoggedSelection;
			} else {
				timeLoggedToUse = timeLogged;
			}
		}

		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		double totalHours = 0.0;
		if (roundTimeLogged) {
			long totalMinutes = (timeLoggedToUse.toMinutes() / 15) * 15; // rounded
			hours = (int) (totalMinutes / 60);
			minutes = (int) (totalMinutes % 60);
			totalHours = totalMinutes / (double) 60.0;
		} else {
			hours = (int) timeLoggedToUse.toHours();
			minutes = (int) (timeLoggedToUse.toMinutes() % 60);
			seconds = (int) (timeLoggedToUse.toSeconds() % 60);
			totalHours = timeLoggedToUse.toMinutes() / (double) 60.0;
		}
		timeLabel.setText(hours + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
		timeHoursLabel.setText(String.format("%.2f", totalHours) + " hours");
	}

	public void roundingChanged() {
		roundTimeLogged = roundingToggle.isSelected();
		updateTimeDisplay(-1);
	}

	private void tableSelectionChanged() {
		ObservableList<FileEntry> selectedEntries = table.getSelectionModel().getSelectedItems();
		int selectedListSize = selectedEntries.size();
		if (selectedListSize == 1 && hasSearched) {
			editButton.setVisible(true);
			removeButton.setVisible(true);
		} else {
			editButton.setVisible(false);
			removeButton.setVisible(false);
		}
		if (selectedListSize > 1) {
			timeLoggedSelection = Duration.ZERO;
			for (FileEntry currentEntry : selectedEntries) {
				timeLoggedSelection = timeLoggedSelection.plus(currentEntry.getDuration());
			}
			updateTimeDisplay(selectedListSize);
		} else {
			if (timeLoggedSelection != null) {
				timeLoggedSelection = null;
				updateTimeDisplay(0);
			}
		}
	}

	public void editPressed() {
		ObservableList<FileEntry> selection = table.getSelectionModel().getSelectedItems();
		if (selection.size() == 1) {
			FileEntry entryToEdit = selection.get(0);
			((CustomEntryController) SceneChanger.getController(MyScenes.CUSTOMENTRY)).setEditMode(true, entryToEdit);
			SceneChanger.setScene(MyScenes.CUSTOMENTRY);
		}
	}

	public void confirmPurgePressed() {
		if (confirmPurgeReady) {
			confirmPurgeReady = false;
			model.purgeData();
			update();
			hidePrompts();
		}
	}

	public void confirmRemovePressed() {
		if (confirmRemoveReady && entryToRemove != null) {
			confirmRemoveReady = false;
			if (model.removeEntry(entryToRemove)) {
				search();
			} else {
				TTErrorLogger.log("Failed to remove entry specified");
			}
			hidePrompts();
		}
	}

	public void purgePressed() {
		confirmPurgePrompt.setVisible(true);
		confirmPurgeReady = true;
	}

	public void removePressed() {
		ObservableList<FileEntry> selection = table.getSelectionModel().getSelectedItems();
		if (selection.size() == 1) {
			entryToRemove = selection.get(0);
			confirmRemovalLabel1.setText(entryToRemove.getJob() + " (" + entryToRemove.getDescription() + ")");
			confirmRemovalLabel2.setText(entryToRemove.getStartTimeDisplay() + " on " + entryToRemove.getDateDisplay()
					+ " for " + entryToRemove.getDurationDisplay());
			confirmRemovePrompt.setVisible(true);
			confirmRemoveReady = true;
		}
	}

	public void hidePrompts() {
		confirmPurgeReady = false;
		confirmRemoveReady = false;
		entryToRemove = null;
		confirmPurgePrompt.setVisible(false);
		confirmRemovePrompt.setVisible(false);
		confirmRemovalLabel1.setText("Error! Please cancel");
		confirmRemovalLabel2.setText("Error! Please cancel");
	}

	public void viewProgramFiles() {
		File programFolder = new File(System.getProperty("user.dir"));
		if (programFolder.exists()) {
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.open(programFolder);
			} catch (IOException e) {
				TTErrorLogger.log("Could not open program files");
			}
		}
	}

	private void initializeDropdownListeners() {
		jobDropdown.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			searchButton.setStyle("-fx-background-color: #b68bfc;");
			searchButton.setDepthLevel(DepthLevel.LEVEL1);
			hasSearched = false;
			editButton.setVisible(false);
			removeButton.setVisible(false);
		});
		descriptionDropdown.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			searchButton.setStyle("-fx-background-color: #b68bfc;");
			searchButton.setDepthLevel(DepthLevel.LEVEL1);
			hasSearched = false;
			editButton.setVisible(false);
			removeButton.setVisible(false);
		});
		timePeriodDropdown.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			searchButton.setStyle("-fx-background-color: #b68bfc;");
			searchButton.setDepthLevel(DepthLevel.LEVEL1);
			hasSearched = false;
			editButton.setVisible(false);
			removeButton.setVisible(false);
		});
	}

	@Override
	public void deselect() {
		if (!parentAnchorPane.isFocused()) {
			parentAnchorPane.requestFocus();
			parentAnchorPane.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
					true, true, true, true, true, true, true, true, true, true, null));
		}
	}

	public void exitScene() {
		((CustomEntryController) SceneChanger.getController(MyScenes.CUSTOMENTRY)).setEditMode(false, null);
		table.getItems().clear(); // lower memory usage
		SceneChanger.goToReturnScene();
	}

	/* For manually showing/hiding table scroll bar (MFXLegacyTableView bug) */
	private void fixTableScrollBar() {
		boolean visible = false;
		if (table.getItems().size() > 14) {
			visible = true;
		}
		for (Node currentNode : table.getChildrenUnmodifiable()) {
			if (currentNode instanceof ScrollBar) {
				ScrollBar currentScrollBar = (ScrollBar) currentNode;
				if (currentScrollBar.visibleProperty().isBound()) {
					currentScrollBar.visibleProperty().unbind();
				}
				if (currentScrollBar.getOrientation() == Orientation.VERTICAL) {
					currentScrollBar.setVisible(visible);
				} else {
					if (currentScrollBar.isVisible()) {
						currentScrollBar.setVisible(false); // always hide horizontal bar
					}
				}
			}
		}
	}
}
