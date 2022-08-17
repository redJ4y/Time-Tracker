package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class XMLKeeper {
	private File data;
	private File tempData;

	private HashSet<String> uniqueJobs;
	private HashSet<String> uniqueDescriptions;

	public XMLKeeper() {
		data = new File("data.xml");
		if (!data.exists()) {
			try {
				data.createNewFile();
				initializeDataFile();
			} catch (IOException e) {
				TTErrorLogger.log("Could not create data.xml");
			}
		}

		tempData = new File("temp/tempdata.xml");
		if (!tempData.exists()) {
			try {
				tempData.createNewFile();
			} catch (IOException e) {
				TTErrorLogger.log("Could not create tempdata.xml");
			}
		}

		if (data.length() == 0) {
			initializeDataFile(); // make sure data file is initialized
		}

		uniqueJobs = new HashSet<String>();
		uniqueDescriptions = new HashSet<String>();
		initializeUniqueSets();
	}

	public ArrayList<FileEntry> search(long startDateTime, String job, String description, boolean specificDate) {
		ArrayList<FileEntry> filteredEntries = new ArrayList<FileEntry>();

		QName entryQName = QName.valueOf("entry");
		QName timeQName = QName.valueOf("time");
		XMLEvent currentEvent = null;
		long currentTime = 0;
		String currentJob = null;
		String currentDescription = null;
		Duration currentDuration = null;
		String currentDateDisp = null;
		String currentStartTimeDisp = null;
		String currentDurationDisp = null;

		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
			XMLEventReader reader = inputFactory.createXMLEventReader(new FileInputStream(data));

			while (reader.hasNext()) {
				currentEvent = reader.nextEvent();
				if (currentEvent.getEventType() == XMLStreamConstants.START_ELEMENT) {
					if (currentEvent.asStartElement().getName().equals(entryQName)) {

						currentTime = Long
								.parseLong(currentEvent.asStartElement().getAttributeByName(timeQName).getValue());

						if (startDateTime == 0 || currentTime >= startDateTime) {
							if (!(specificDate && currentTime > startDateTime + 2400)) {

								reader.nextEvent(); // newline
								reader.nextEvent(); // job start event
								currentEvent = reader.nextEvent(); // job characters event
								if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
									currentJob = currentEvent.asCharacters().getData();
									reader.nextEvent(); // job end event
								} else {
									TTErrorLogger.log("data.xml incorrectly formatted");
									throw new Exception("data.xml incorrectly formatted");
								}

								if (job == null || currentJob.equals(job)) {

									reader.nextEvent(); // newline
									reader.nextEvent(); // description start event
									currentEvent = reader.nextEvent(); // description characters event
									if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
										currentDescription = currentEvent.asCharacters().getData();
										reader.nextEvent(); // description end event
									} else {
										TTErrorLogger.log("data.xml incorrectly formatted");
										throw new Exception("data.xml incorrectly formatted");
									}

									if (description == null || currentDescription.equals(description)) {

										reader.nextEvent(); // newline
										reader.nextEvent(); // duration start event
										currentEvent = reader.nextEvent(); // duration characters event
										if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
											currentDuration = Duration.parse(currentEvent.asCharacters().getData());
											reader.nextEvent(); // duration end event
										} else {
											TTErrorLogger.log("data.xml incorrectly formatted");
											throw new Exception("data.xml incorrectly formatted");
										}

										reader.nextEvent(); // newline
										reader.nextEvent(); // date display start event
										currentEvent = reader.nextEvent(); // date display characters event
										if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
											currentDateDisp = currentEvent.asCharacters().getData();
											reader.nextEvent(); // date display end event
										} else {
											TTErrorLogger.log("data.xml incorrectly formatted");
											throw new Exception("data.xml incorrectly formatted");
										}

										reader.nextEvent(); // newline
										reader.nextEvent(); // start time display start event
										currentEvent = reader.nextEvent(); // start time display characters event
										if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
											currentStartTimeDisp = currentEvent.asCharacters().getData();
											reader.nextEvent(); // start time display end event
										} else {
											TTErrorLogger.log("data.xml incorrectly formatted");
											throw new Exception("data.xml incorrectly formatted");
										}

										reader.nextEvent(); // newline
										reader.nextEvent(); // duration display start event
										currentEvent = reader.nextEvent(); // duration display characters event
										if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
											currentDurationDisp = currentEvent.asCharacters().getData();
											reader.nextEvent(); // duration display end event
										} else {
											TTErrorLogger.log("data.xml incorrectly formatted");
											throw new Exception("data.xml incorrectly formatted");
										}

										/* entry met criteria and values stored */
										FileEntry newFileEntry = new FileEntry();
										newFileEntry.setJob(currentJob);
										newFileEntry.setDescription(currentDescription);
										newFileEntry.setTime(currentTime);
										newFileEntry.setDuration(currentDuration);
										newFileEntry.setDateDisplay(currentDateDisp);
										newFileEntry.setStartTimeDisplay(currentStartTimeDisp);
										newFileEntry.setDurationDisplay(currentDurationDisp);
										filteredEntries.add(newFileEntry);
									} else {
										for (int i = 0; i < 17; i++) {
											reader.nextEvent();
										} // skip the rest of the entry
									}
								} else {
									for (int i = 0; i < 21; i++) {
										reader.nextEvent();
									} // skip the rest of the entry
								}
							} else {
								for (int i = 0; i < 25; i++) {
									reader.nextEvent();
								} // skip the rest of the entry
							}
						} else {
							break; // currentTime is now before startDateTime, done searching
						}
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			filteredEntries = new ArrayList<FileEntry>();
		}
		return filteredEntries;
	}

	public boolean remove(FileEntry entry) {
		boolean successful = false;

		if (!tempData.exists()) {
			try {
				tempData.createNewFile(); // make sure tempdata.xml was not deleted
			} catch (IOException e) {
				TTErrorLogger.log("Could not create tempdata.xml");
			}
		}

		QName entryQName = QName.valueOf("entry");
		QName timeQName = QName.valueOf("time");
		ArrayList<XMLEvent> singleEntryEvents = new ArrayList<XMLEvent>();
		XMLEvent currentEvent = null;
		long currentTime = 0;
		String currentJob = null;
		String currentDescription = null;
		Duration currentDuration = null;
		String currentDateDisp = null;
		String currentStartTimeDisp = null;
		String currentDurationDisp = null;

		try {
			XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(new FileOutputStream(tempData));
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent newLine = eventFactory.createDTD("\n");
			writer.add(eventFactory.createStartDocument()); // initialize tempdata.xml
			writer.add(newLine);

			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
			XMLEventReader reader = inputFactory.createXMLEventReader(new FileInputStream(data));
			reader.nextEvent(); // skip start document event

			while (reader.hasNext()) {
				currentEvent = reader.nextEvent();
				if (!successful) {
					if (currentEvent.getEventType() == XMLStreamConstants.START_ELEMENT) {
						if (currentEvent.asStartElement().getName().equals(entryQName)) {
							singleEntryEvents.clear(); // reset entry memory
							singleEntryEvents.add(currentEvent); // entry start event added

							currentTime = Long
									.parseLong(currentEvent.asStartElement().getAttributeByName(timeQName).getValue());
							if (currentTime != entry.getTime()) {
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
								continue;
							}

							singleEntryEvents.add(reader.nextEvent()); // newline added
							singleEntryEvents.add(reader.nextEvent()); // job start event added
							currentEvent = reader.nextEvent();
							if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
								currentJob = currentEvent.asCharacters().getData();
								singleEntryEvents.add(currentEvent); // job characters event added
								singleEntryEvents.add(reader.nextEvent()); // job end event added
							} else {
								TTErrorLogger.log("data.xml incorrectly formatted");
								throw new Exception("data.xml incorrectly formatted");
							}
							if (!currentJob.equals(entry.getJob())) {
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
								continue;
							}

							singleEntryEvents.add(reader.nextEvent()); // newline added
							singleEntryEvents.add(reader.nextEvent()); // description start event added
							currentEvent = reader.nextEvent();
							if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
								currentDescription = currentEvent.asCharacters().getData();
								singleEntryEvents.add(currentEvent); // description characters event added
								singleEntryEvents.add(reader.nextEvent()); // description end event added
							} else {
								TTErrorLogger.log("data.xml incorrectly formatted");
								throw new Exception("data.xml incorrectly formatted");
							}
							if (!currentDescription.equals(entry.getDescription())) {
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
								continue;
							}

							singleEntryEvents.add(reader.nextEvent()); // newline added
							singleEntryEvents.add(reader.nextEvent()); // duration start event added
							currentEvent = reader.nextEvent();
							if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
								currentDuration = Duration.parse(currentEvent.asCharacters().getData());
								singleEntryEvents.add(currentEvent); // duration characters event added
								singleEntryEvents.add(reader.nextEvent()); // duration end event added
							} else {
								TTErrorLogger.log("data.xml incorrectly formatted");
								throw new Exception("data.xml incorrectly formatted");
							}
							if (!currentDuration.equals(entry.getDuration())) {
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
								continue;
							}

							singleEntryEvents.add(reader.nextEvent()); // newline added
							singleEntryEvents.add(reader.nextEvent()); // date display start event added
							currentEvent = reader.nextEvent();
							if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
								currentDateDisp = currentEvent.asCharacters().getData();
								singleEntryEvents.add(currentEvent); // date display characters event added
								singleEntryEvents.add(reader.nextEvent()); // date display end event added
							} else {
								TTErrorLogger.log("data.xml incorrectly formatted");
								throw new Exception("data.xml incorrectly formatted");
							}
							if (!currentDateDisp.equals(entry.getDateDisplay())) {
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
								continue;
							}

							singleEntryEvents.add(reader.nextEvent()); // newline added
							singleEntryEvents.add(reader.nextEvent()); // start time display start event added
							currentEvent = reader.nextEvent();
							if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
								currentStartTimeDisp = currentEvent.asCharacters().getData();
								singleEntryEvents.add(currentEvent); // start time display characters event added
								singleEntryEvents.add(reader.nextEvent()); // start time display end event added
							} else {
								TTErrorLogger.log("data.xml incorrectly formatted");
								throw new Exception("data.xml incorrectly formatted");
							}
							if (!currentStartTimeDisp.equals(entry.getStartTimeDisplay())) {
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
								continue;
							}

							singleEntryEvents.add(reader.nextEvent()); // newline added
							singleEntryEvents.add(reader.nextEvent()); // duration display start event added
							currentEvent = reader.nextEvent();
							if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
								currentDurationDisp = currentEvent.asCharacters().getData();
								singleEntryEvents.add(currentEvent); // duration display characters event added
								singleEntryEvents.add(reader.nextEvent()); // duration display end event added
							} else {
								TTErrorLogger.log("data.xml incorrectly formatted");
								throw new Exception("data.xml incorrectly formatted");
							}
							if (!currentDurationDisp.equals(entry.getDurationDisplay())) {
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
								continue;
							}
							/* everything matches, entry will not be written */
							reader.nextEvent(); // newline skipped
							reader.nextEvent(); // entry end event skipped
							reader.nextEvent(); // newline skipped
							successful = true;
						} else {
							writer.add(currentEvent);
						}
					} else {
						writer.add(currentEvent);
					}
				} else {
					writer.add(currentEvent);
				}
			}

			writer.add(newLine);
			writer.add(eventFactory.createEndDocument());
			writer.flush();
			writer.close();
			reader.close();

			if (successful) {
				FileOutputStream dataFOS = new FileOutputStream(data);
				Files.copy(tempData.toPath(), dataFOS);
				dataFOS.flush();
				dataFOS.close();
			}
		} catch (Exception e) {
			successful = false;
		}
		clearTempDataFile();
		return successful;
	}

	public boolean add(FileEntry entry) {
		boolean successful = false;

		if (!tempData.exists()) {
			try {
				tempData.createNewFile(); // make sure tempdata.xml was not deleted
			} catch (IOException e) {
				TTErrorLogger.log("Could not create tempdata.xml");
			}
		}

		QName entryQName = QName.valueOf("entry");
		QName timeQName = QName.valueOf("time");
		boolean entryInserted = false;
		long currentTime = 0;
		XMLEvent currentEvent = null;
		StartElement eventAsStartElement = null;
		int eventType = -1;
		int depth = 0;

		try {
			XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(new FileOutputStream(tempData));
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent newLine = eventFactory.createDTD("\n");
			writer.add(eventFactory.createStartDocument()); // initialize tempdata.xml
			writer.add(newLine);

			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
			XMLEventReader reader = inputFactory.createXMLEventReader(new FileInputStream(data));
			reader.nextEvent(); // skip start document event

			while (reader.hasNext()) {
				currentEvent = reader.nextEvent();
				if (!entryInserted) {
					eventType = currentEvent.getEventType();
					if (eventType == XMLStreamConstants.START_ELEMENT) {
						depth++;
						eventAsStartElement = currentEvent.asStartElement();
						if (eventAsStartElement.getName().equals(entryQName)) {
							currentTime = Long.parseLong(eventAsStartElement.getAttributeByName(timeQName).getValue());
							if (entry.getTime() >= currentTime) {
								successful = entry.writeXML(writer);
								entryInserted = true;
								if (!successful) {
									break;
								}
							}
						}
					} else if (eventType == XMLStreamConstants.END_ELEMENT) {
						depth--;
						if (depth == 0) {
							successful = entry.writeXML(writer);
							entryInserted = true;
							if (!successful) {
								break;
							}
						}
					}
				}
				writer.add(currentEvent);
			}

			writer.add(newLine);
			writer.add(eventFactory.createEndDocument());
			writer.flush();
			writer.close();
			reader.close();

			if (successful) {
				FileOutputStream dataFOS = new FileOutputStream(data);
				Files.copy(tempData.toPath(), dataFOS);
				dataFOS.flush();
				dataFOS.close();
			}
		} catch (Exception e) {
			successful = false;
		}
		clearTempDataFile();
		uniqueJobs.add(entry.getJob()); // keep unique sets updated
		uniqueDescriptions.add(entry.getDescription());
		return successful;
	}

	public void purge(List<String> jobsToKeepList) {
		HashSet<String> jobsToKeep = new HashSet<String>(jobsToKeepList);
		boolean successful = false;

		if (!tempData.exists()) {
			try {
				tempData.createNewFile(); // make sure tempdata.xml was not deleted
			} catch (IOException e) {
				TTErrorLogger.log("Could not create tempdata.xml");
			}
		}

		if (backUpData()) {
			QName entryQName = QName.valueOf("entry");
			ArrayList<XMLEvent> singleEntryEvents = new ArrayList<XMLEvent>();
			XMLEvent currentEvent = null;
			String currentJob = null;

			try {
				XMLEventWriter writer = XMLOutputFactory.newInstance()
						.createXMLEventWriter(new FileOutputStream(tempData));
				XMLEventFactory eventFactory = XMLEventFactory.newInstance();
				XMLEvent newLine = eventFactory.createDTD("\n");
				writer.add(eventFactory.createStartDocument()); // initialize tempdata.xml
				writer.add(newLine);
				writer.add(eventFactory.createStartElement("", "", "entries"));
				writer.add(newLine);

				XMLInputFactory inputFactory = XMLInputFactory.newInstance();
				inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
				XMLEventReader reader = inputFactory.createXMLEventReader(new FileInputStream(data));
				reader.nextEvent(); // skip start document event

				while (reader.hasNext()) {
					currentEvent = reader.nextEvent();
					if (currentEvent.getEventType() == XMLStreamConstants.START_ELEMENT) {
						if (currentEvent.asStartElement().getName().equals(entryQName)) {
							singleEntryEvents.add(currentEvent); // entry start event added

							singleEntryEvents.add(reader.nextEvent()); // newline added
							singleEntryEvents.add(reader.nextEvent()); // job start event added
							currentEvent = reader.nextEvent();
							if (currentEvent.getEventType() == XMLStreamConstants.CHARACTERS) {
								currentJob = currentEvent.asCharacters().getData();
								singleEntryEvents.add(currentEvent); // job characters event added
								singleEntryEvents.add(reader.nextEvent()); // job end event added
							} else {
								TTErrorLogger.log("data.xml incorrectly formatted");
								throw new Exception("data.xml incorrectly formatted");
							}

							if (jobsToKeep.contains(currentJob)) {
								for (int i = 0; i < 21; i++) {
									singleEntryEvents.add(reader.nextEvent());
								} // read in the rest of the entry
								currentEvent = reader.nextEvent();
								/* verify that entry was of correct length */
								if (currentEvent.getEventType() == XMLStreamConstants.END_ELEMENT) {
									if (!currentEvent.asEndElement().getName().equals(entryQName)) {
										TTErrorLogger.log("data.xml incorrectly formatted");
										throw new Exception("data.xml incorrectly formatted");
									}
								} else {
									TTErrorLogger.log("data.xml incorrectly formatted");
									throw new Exception("data.xml incorrectly formatted");
								}
								singleEntryEvents.add(currentEvent); // entry end event added
								singleEntryEvents.add(reader.nextEvent()); // newline added

								/* entry is to be kept */
								for (XMLEvent currentEntryEvent : singleEntryEvents) {
									writer.add(currentEntryEvent);
								}
							} else {
								for (int i = 0; i < 21; i++) {
									reader.nextEvent();
								} // skip the rest of the entry
								successful = true; // at least one entry was purged
							}
							singleEntryEvents.clear();
						}
					}
				}

				writer.add(eventFactory.createEndElement("", "", "entries"));
				writer.add(newLine);
				writer.add(eventFactory.createEndDocument());
				writer.flush();
				writer.close();
				reader.close();

				if (successful) {
					FileOutputStream dataFOS = new FileOutputStream(data);
					Files.copy(tempData.toPath(), dataFOS);
					dataFOS.flush();
					dataFOS.close();
				}
			} catch (Exception e) {
				successful = false;
			}
			clearTempDataFile();
			if (successful) {
				uniqueJobs.clear();
				uniqueDescriptions.clear();
				initializeUniqueSets(); // keep unique sets updated
			}
		}
	}

	private boolean backUpData() {
		boolean backedUp = true;
		LocalDate today = LocalDate.now();
		String fileName = "backup " + today.getMonthValue() + "-" + today.getDayOfMonth() + "-" + today.getYear();
		File backupFile = new File("temp/" + fileName + ".xml");
		if (!backupFile.exists()) {
			try {
				backupFile.createNewFile();
				FileOutputStream backupFileFOS = new FileOutputStream(backupFile);
				Files.copy(data.toPath(), backupFileFOS);
				backupFileFOS.flush();
				backupFileFOS.close();
			} catch (IOException e) {
				backedUp = false;
				TTErrorLogger.log("Could not back up data.xml");
			}
		}
		return backedUp;
	}

	private void initializeUniqueSets() {
		QName jobQName = QName.valueOf("job");
		QName descQName = QName.valueOf("desc");
		XMLEvent currentEvent = null;
		QName currentEventName = null;
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
			XMLEventReader reader = inputFactory.createXMLEventReader(new FileInputStream(data));

			while (reader.hasNext()) {
				currentEvent = reader.nextEvent();
				if (currentEvent.getEventType() == XMLStreamConstants.START_ELEMENT) {
					currentEventName = currentEvent.asStartElement().getName();
					if (currentEventName.equals(jobQName)) {
						uniqueJobs.add(reader.nextEvent().asCharacters().getData());
					} else if (currentEventName.equals(descQName)) {
						uniqueDescriptions.add(reader.nextEvent().asCharacters().getData());
						for (int i = 0; i < 22; i++) {
							reader.nextEvent();
						} // skip to the next job start event
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			TTErrorLogger.log("Failed to search data.xml for unique jobs/descriptions");
		}
	}

	private void initializeDataFile() {
		try {
			XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(new FileOutputStream(data));
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent newLine = eventFactory.createDTD("\n");

			writer.add(eventFactory.createStartDocument());
			writer.add(newLine);
			writer.add(eventFactory.createStartElement("", "", "entries"));
			writer.add(newLine);
			writer.add(eventFactory.createEndElement("", "", "entries"));
			writer.add(newLine);
			writer.add(eventFactory.createEndDocument());
			writer.flush();
			writer.close();
		} catch (FileNotFoundException | XMLStreamException e) {
			TTErrorLogger.log("Could not initialize data.xml");
		}
	}

	private void clearTempDataFile() {
		try {
			PrintWriter writer = new PrintWriter(tempData);
			writer.print("");
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			TTErrorLogger.log("Could not clear tempdata.xml");
		}
	}

	public HashSet<String> getUniqueJobs() {
		return uniqueJobs;
	}

	public HashSet<String> getUniqueDescriptions() {
		return uniqueDescriptions;
	}
}
