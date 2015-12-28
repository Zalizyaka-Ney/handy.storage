package handy.storage.sample.model;

import handy.storage.annotation.Column;
import handy.storage.annotation.FunctionResult;
import handy.storage.annotation.TableName;
import handy.storage.api.Function;
import handy.storage.api.Model;

@TableName(TaskListEntry.TABLE_NAME)
public class EmployeeLoad implements Model {

	public static final String EMPLOYEE = TaskListEntry.EMPLOYEE;
	public static final String TASKS_NUMBER = "tasksNumber";

	@Column(EMPLOYEE)
	private Employee employee;

	@Column(TASKS_NUMBER)
	@FunctionResult(type = Function.COUNT)
	private int tasksNumber;

	@Override
	public String toString() {
		return employee.getName() + " has " + tasksNumber + " tasks";
	}

}
