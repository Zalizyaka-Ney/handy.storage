package handy.storage.sample.model;

import handy.storage.annotation.Column;
import handy.storage.annotation.TableName;
import handy.storage.api.UniqueObject;

@TableName(Task.TABLE_NAME)
public class Task extends UniqueObject {

	public static final String TABLE_NAME = "tasks";
	public static final String NAME = "name";
	public static final String STATUS = "status";

	@Column(NAME)
	private String name;

	@Column(STATUS)
	private TaskStatus status;

	public Task(String name) {
		this.name = name;
		this.status = TaskStatus.PENDING;
	}

	public Task() {
	}

	@Override
	public String toString() {
		return "Task [name=" + name + ", status=" + status + "]";
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
