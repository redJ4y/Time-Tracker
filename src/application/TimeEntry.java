package application;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TimeEntry implements Serializable {
	private static final long serialVersionUID = 1L;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String job;
	private String descripton;

	public TimeEntry() {
		startTime = null;
		endTime = null;
		job = "NO INPUT";
		descripton = "NO INPUT";
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public String getDescripton() {
		return descripton;
	}

	public void setDescripton(String descripton) {
		this.descripton = descripton;
	}

	@Override
	public String toString() {
		return startTime.toString() + " - " + endTime.toString() + " (" + job + " - " + descripton + ")";
	}
}
