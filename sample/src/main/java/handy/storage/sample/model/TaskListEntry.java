package handy.storage.sample.model;

import handy.storage.annotation.Column;
import handy.storage.annotation.Reference;
import handy.storage.annotation.TableName;
import handy.storage.api.UniqueObject;

@TableName(TaskListEntry.TABLE_NAME)
public class TaskListEntry extends UniqueObject {

	public static final String TABLE_NAME = "tasks_list";
	public static final String TASK = "task";
	public static final String EMPLOYEE = "employee";

	@Column(TASK)
	@Reference
	private Task task;

	@Column(EMPLOYEE)
	@Reference
	private Employee employee;

	public TaskListEntry() {
	}

	public TaskListEntry(Task task, Employee employee) {
		this.task = task;
		this.employee = employee;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Override
	public String toString() {
		return "TaskListEntry [task=" + task + ", employee=" + employee + "]";
	}

}
