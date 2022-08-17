package application;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Model {
	private DataKeeper data;
	private String selectedJob;
	private String selectedDescription;

	public Model() {
		data = new DataKeeper();
		selectedJob = "N/A";
		selectedDescription = "N/A";
	}

	public void startTime() {
		if (data.getExistingEntry() == null) {
			TimeEntry newEntry = new TimeEntry();
			newEntry.setJob(selectedJob);
			newEntry.setDescripton(selectedDescription);
			newEntry.setStartTime(LocalDateTime.now());
			data.startEntry(newEntry);
		}
	}

	public void stopTime() {
		TimeEntry existingEntry = data.getExistingEntry();
		if (existingEntry != null) {
			existingEntry.setEndTime(LocalDateTime.now());
			if (data.saveEntry(existingEntry)) {
				((StartController) SceneChanger.getController(MyScenes.START)).showConfirmation();
			}
		}
	}

	public void stopTimeAt(LocalDateTime time) {
		TimeEntry existingEntry = data.getExistingEntry();
		if (existingEntry != null) {
			existingEntry.setEndTime(time);
			if (data.saveEntry(existingEntry)) {
				((StartController) SceneChanger.getController(MyScenes.START)).showConfirmation();
			}
		}
	}

	public boolean saveEntry(TimeEntry entry) {
		boolean saved = false;
		if (entry != null) {
			saved = data.saveEntry(entry);
		}
		return saved;
	}

	public boolean saveFileEntry(FileEntry entry) {
		boolean saved = false;
		if (entry != null) {
			saved = data.saveFileEntry(entry);
		}
		return saved;
	}

	public boolean removeEntry(FileEntry entry) {
		return data.removeEntry(entry);
	}

	public ArrayList<FileEntry> searchData(long startDateTime, String job, String description, boolean specificDate) {
		return data.searchData(startDateTime, job, description, specificDate);
	}

	public void purgeData() {
		data.purgeData(getJobs());
	}

	public List<String> getAllUniqueJobs() {
		return data.getAllUniqueJobs();
	}

	public List<String> getAllUniqueDescriptions() {
		return data.getAllUniqueDescriptions();
	}

	public boolean checkForExistingEntry() {
		boolean result = false;
		TimeEntry existingEntry = data.getExistingEntry();
		if (existingEntry != null) {
			result = true;
			selectedJob = existingEntry.getJob();
			selectedDescription = existingEntry.getDescripton();
		}
		return result;
	}

	public int getTimerStartingMinutes() {
		int timerStartingMinutes = -1;
		TimeEntry existingEntry = data.getExistingEntry();
		if (existingEntry != null) {
			timerStartingMinutes = (int) ChronoUnit.MINUTES.between(existingEntry.getStartTime(), LocalDateTime.now());
			timerStartingMinutes--;
		}
		return timerStartingMinutes;
	}

	public LocalDateTime getTimeClosed() {
		return data.getTimeClosed();
	}

	public void appClosed() {
		if (data.getExistingEntry() != null) {
			data.setTimeClosed(LocalDateTime.now());
		}
	}

	public ArrayList<String> getJobs() {
		return data.getJobs();
	}

	public void addJob(String newJob) {
		data.addJob(newJob);
	}

	public void removeJob(int index) {
		data.removeJob(index);
	}

	public ArrayList<String> getDescriptions() {
		return data.getDescriptions();
	}

	public void addDescription(String newDescription) {
		data.addDescription(newDescription);
	}

	public void removeDescription(int index) {
		data.removeDescription(index);
	}

	public String getSelectedJob() {
		return selectedJob;
	}

	public void setSelectedJob(String selectedJob) {
		this.selectedJob = selectedJob;
	}

	public String getSelectedDescription() {
		return selectedDescription;
	}

	public void setSelectedDescription(String selectedDescription) {
		this.selectedDescription = selectedDescription;
	}
}
