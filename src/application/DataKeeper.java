package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DataKeeper {
	private XMLKeeper dataKeeperXML;
	private File currentEntryFile;
	private File jobsFile;
	private File descriptionsFile;
	private File timeClosedFile;

	public DataKeeper() {
		File serDataDirectory = new File("serdata");
		if (!serDataDirectory.exists()) {
			serDataDirectory.mkdir();
		}

		File tempDirectory = new File("temp");
		if (!tempDirectory.exists()) {
			tempDirectory.mkdir();
		}

		dataKeeperXML = new XMLKeeper();

		currentEntryFile = new File("serdata/current.ser");
		if (!currentEntryFile.exists()) {
			try {
				currentEntryFile.createNewFile();
				writeObjectToFile(currentEntryFile, null); // initialize file
			} catch (IOException e) {
				TTErrorLogger.log("Could not create current.ser");
			}
		}

		jobsFile = new File("serdata/jobs.ser");
		if (!jobsFile.exists()) {
			try {
				jobsFile.createNewFile();
				writeObjectToFile(jobsFile, new ArrayList<String>()); // initialize file
			} catch (IOException e) {
				TTErrorLogger.log("Could not create jobs.ser");
			}
		}

		descriptionsFile = new File("serdata/descriptions.ser");
		if (!descriptionsFile.exists()) {
			try {
				descriptionsFile.createNewFile();
				writeObjectToFile(descriptionsFile, new ArrayList<String>()); // initialize file
			} catch (IOException e) {
				TTErrorLogger.log("Could not create descriptions.ser");
			}
		}

		timeClosedFile = new File("serdata/timeclosed.ser");
		if (!timeClosedFile.exists()) {
			try {
				timeClosedFile.createNewFile();
				writeObjectToFile(timeClosedFile, null); // initialize file
			} catch (IOException e) {
				TTErrorLogger.log("Could not create timeclosed.ser");
			}
		}
	}

	public void startEntry(TimeEntry timeEntry) {
		writeObjectToFile(currentEntryFile, timeEntry);
	}

	public TimeEntry getExistingEntry() {
		return (TimeEntry) readObjectFromFile(currentEntryFile);
	}

	/* Functions for working with XML data */
	public boolean saveEntry(TimeEntry timeEntry) {
		FileEntry entryToSave = new FileEntry(timeEntry);
		boolean saved = false;
		if (entryToSave.getDuration().compareTo(Duration.ofSeconds(30)) >= 0) {
			saved = dataKeeperXML.add(entryToSave);
			if (!saved) {
				logFailedFileEntry(entryToSave);
			}
		} // final validation - ignore entries of less than 30 seconds
		writeObjectToFile(currentEntryFile, null);
		return saved;
	}

	public boolean saveFileEntry(FileEntry entryToSave) {
		boolean saved = false;
		if (entryToSave.getDuration().compareTo(Duration.ofSeconds(30)) >= 0) {
			saved = dataKeeperXML.add(entryToSave);
			if (!saved) {
				logFailedFileEntry(entryToSave);
			}
		} // final validation - ignore entries of less than 30 seconds
		writeObjectToFile(currentEntryFile, null);
		return saved;
	}

	private void logFailedFileEntry(FileEntry entry) {
		try {
			Logger logger = Logger.getLogger("FailedFileEntryLogger");
			FileHandler fileHandler = new FileHandler("temp/logs.log", true);
			logger.addHandler(fileHandler);
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			logger.info("Failed to save entry\n" + entry.toString() + "\n");
			fileHandler.flush();
			fileHandler.close();
		} catch (Exception e) {
			TTErrorLogger.log("Failed to log failed entry");
		}
	}

	public ArrayList<FileEntry> searchData(long startDateTime, String job, String description, boolean specificDate) {
		return dataKeeperXML.search(startDateTime, job, description, specificDate);
	}

	public List<String> getAllUniqueJobs() {
		HashSet<String> uniqueJobs = dataKeeperXML.getUniqueJobs();
		uniqueJobs.addAll(getJobs());

		List<String> uniqueJobsList = new ArrayList<String>();
		uniqueJobsList.addAll(uniqueJobs);
		return uniqueJobsList;
	}

	public List<String> getAllUniqueDescriptions() {
		HashSet<String> uniqueDescriptions = dataKeeperXML.getUniqueDescriptions();
		uniqueDescriptions.addAll(getDescriptions());

		List<String> uniqueDescriptionsList = new ArrayList<String>();
		uniqueDescriptionsList.addAll(uniqueDescriptions);
		return uniqueDescriptionsList;
	}

	public boolean removeEntry(FileEntry entry) {
		return dataKeeperXML.remove(entry);
	}

	public void purgeData(List<String> jobsToKeepList) {
		dataKeeperXML.purge(jobsToKeepList);
	}
	/* End XML data functions */

	@SuppressWarnings("unchecked")
	public ArrayList<String> getJobs() {
		return (ArrayList<String>) readObjectFromFile(jobsFile);
	}

	public void addJob(String job) {
		ArrayList<String> jobs = getJobs();
		jobs.add(job);
		writeObjectToFile(jobsFile, jobs);
	}

	public void removeJob(int index) {
		ArrayList<String> jobs = getJobs();
		jobs.remove(index);
		writeObjectToFile(jobsFile, jobs);
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getDescriptions() {
		return (ArrayList<String>) readObjectFromFile(descriptionsFile);
	}

	public void addDescription(String description) {
		ArrayList<String> descriptions = getDescriptions();
		descriptions.add(description);
		writeObjectToFile(descriptionsFile, descriptions);
	}

	public void removeDescription(int index) {
		ArrayList<String> descriptions = getDescriptions();
		descriptions.remove(index);
		writeObjectToFile(descriptionsFile, descriptions);
	}

	public LocalDateTime getTimeClosed() {
		LocalDateTime timeClosed = (LocalDateTime) readObjectFromFile(timeClosedFile);
		writeObjectToFile(timeClosedFile, null); // reset after reading
		return timeClosed;
	}

	public void setTimeClosed(LocalDateTime time) {
		writeObjectToFile(timeClosedFile, time);
	}

	private void writeObjectToFile(File file, Object object) {
		ObjectOutputStream outputStream;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(file));
			outputStream.writeObject(object);
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			TTErrorLogger.log("Could not write to " + file.getName());
		}
	}

	private Object readObjectFromFile(File file) {
		Object result = null;
		ObjectInputStream inputStream;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(file));
			result = inputStream.readObject();
			inputStream.close();
		} catch (ClassNotFoundException | IOException e) {
			TTErrorLogger.log("Could not read from " + file.getName());
		}
		return result;
	}
}
