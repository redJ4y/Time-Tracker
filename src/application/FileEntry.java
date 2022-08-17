package application;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class FileEntry {
	private String job;
	private String description;
	private long time;
	private Duration duration;

	private String dateDisplay;
	private String startTimeDisplay;
	private String durationDisplay;

	public FileEntry() {
		job = "Error";
		description = "Error";
		time = 0;
		duration = Duration.ofHours(0);
		dateDisplay = "Error";
		startTimeDisplay = "Error";
		durationDisplay = "Error";
	}

	public FileEntry(TimeEntry entry) {
		job = entry.getJob();
		description = entry.getDescripton();
		time = CustomDateTime.convertDateTime(entry.getStartTime());
		duration = Duration.between(entry.getStartTime(), entry.getEndTime());

		dateDisplay = entry.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("M/d/yy"));
		startTimeDisplay = entry.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"));
		if (duration.toMinutes() > 0) {
			durationDisplay = duration.toHours() + ":" + String.format("%02d", duration.toMinutes() % 60);
		} else {
			durationDisplay = "<1m";
		}
	}

	@SuppressWarnings("exports")
	public boolean writeXML(XMLEventWriter writer) {
		boolean written = false;
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		XMLEvent newLine = eventFactory.createDTD("\n");
		XMLEvent indent = eventFactory.createDTD("\t");

		try {
			writer.add(eventFactory.createStartElement("", "", "entry"));
			writer.add(eventFactory.createAttribute("time", Long.toString(time)));
			writer.add(newLine);

			writer.add(indent);
			writer.add(eventFactory.createStartElement("", "", "job"));
			writer.add(eventFactory.createCharacters(job));
			writer.add(eventFactory.createEndElement("", "", "job"));
			writer.add(newLine);

			writer.add(indent);
			writer.add(eventFactory.createStartElement("", "", "desc"));
			writer.add(eventFactory.createCharacters(description));
			writer.add(eventFactory.createEndElement("", "", "desc"));
			writer.add(newLine);

			writer.add(indent);
			writer.add(eventFactory.createStartElement("", "", "dur"));
			writer.add(eventFactory.createCharacters(duration.toString()));
			writer.add(eventFactory.createEndElement("", "", "dur"));
			writer.add(newLine);

			writer.add(indent);
			writer.add(eventFactory.createStartElement("", "", "dateDisp"));
			writer.add(eventFactory.createCharacters(dateDisplay));
			writer.add(eventFactory.createEndElement("", "", "dateDisp"));
			writer.add(newLine);

			writer.add(indent);
			writer.add(eventFactory.createStartElement("", "", "timeDisp"));
			writer.add(eventFactory.createCharacters(startTimeDisplay));
			writer.add(eventFactory.createEndElement("", "", "timeDisp"));
			writer.add(newLine);

			writer.add(indent);
			writer.add(eventFactory.createStartElement("", "", "durDisp"));
			writer.add(eventFactory.createCharacters(durationDisplay));
			writer.add(eventFactory.createEndElement("", "", "durDisp"));
			writer.add(newLine);

			writer.add(eventFactory.createEndElement("", "", "entry"));
			writer.add(newLine);

			written = true;
		} catch (XMLStreamException e) {
			written = false;
		}
		return written;
	}

	public String getJob() {
		return job;
	}

	public String getDescription() {
		return description;
	}

	public long getTime() {
		return time;
	}

	public Duration getDuration() {
		return duration;
	}

	public String getDateDisplay() {
		return dateDisplay;
	}

	public String getStartTimeDisplay() {
		return startTimeDisplay;
	}

	public String getDurationDisplay() {
		return durationDisplay;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public void setDateDisplay(String dateDisplay) {
		this.dateDisplay = dateDisplay;
	}

	public void setStartTimeDisplay(String startTimeDisplay) {
		this.startTimeDisplay = startTimeDisplay;
	}

	public void setDurationDisplay(String durationDisplay) {
		this.durationDisplay = durationDisplay;
	}

	@Override
	public String toString() {
		String toReturn = job + " (" + description + "):\n";
		toReturn += startTimeDisplay + " On " + dateDisplay + " For " + durationDisplay + "\n";
		toReturn += "Time: " + Long.toString(time) + " Duration: " + duration.toString();
		return toReturn;
	}
}
