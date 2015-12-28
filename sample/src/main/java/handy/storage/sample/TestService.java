package handy.storage.sample;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import handy.storage.ColumnValuesTable;
import handy.storage.Database;
import handy.storage.ReadableTable;
import handy.storage.WritableTable;
import handy.storage.exception.OperationException;
import handy.storage.sample.model.Building;
import handy.storage.sample.model.Department;
import handy.storage.sample.model.Employee;
import handy.storage.sample.model.EmployeeLoad;
import handy.storage.sample.model.Gender;
import handy.storage.sample.model.Task;
import handy.storage.sample.model.TaskListEntry;

public class TestService extends IntentService {

	private static final String TAG = "sample";

	public TestService() {
		super("test");
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		try {
			Database database = ((DbUserApplication) getApplication()).getDatabase();
			fillDatabase(database);

			WritableTable<TaskListEntry> taskListEntryTable = database.getTable(TaskListEntry.class);
			List<TaskListEntry> tasksWithAssignment = taskListEntryTable.selectAll();
			for (TaskListEntry entry : tasksWithAssignment) {
				Log.i(TAG, entry.getEmployee().getName() + " will do: " + entry.getTask().getName());
			}

			WritableTable<Employee> employeeTable = database.getTable(Employee.class);
			ColumnValuesTable employeesWithTask = taskListEntryTable.distinctColumnValues(TaskListEntry.EMPLOYEE);
			List<Employee> employeesWithoutTasks = employeeTable.select()
				.where(employeeTable.expressions().exclude(employeesWithTask))
				.execute();
			for (Employee employee : employeesWithoutTasks) {
				Log.i(TAG, employee.getName() + " has no task");
			}

			ReadableTable<EmployeeLoad> employeeLoads = database.getTable(TaskListEntry.class)
				.aggregate()
				.groupBy(TaskListEntry.EMPLOYEE)
				.asReadableTable(EmployeeLoad.class);
			List<EmployeeLoad> loads = employeeLoads.selectAll();
			for (EmployeeLoad load : loads) {
				Log.i(TAG, load.toString());
			}
		} catch (Exception e) {
			Log.e(TAG, "exception", e);
		}
		Log.i(TAG, "end testing");
	}

	private void fillDatabase(Database database) throws Exception {
		List<Department> departments = initDepartments(database);
		List<Employee> employees = initEmployees(database, departments);
		initTasks(database, employees);
	}

	private List<Department> initDepartments(Database database) throws OperationException {
		Building mainBuilding = new Building("Main building");
		Building secondaryBuilding = new Building("Secondary building");
		WritableTable<Building> buildingsTable = database.getTable(Building.class);
		buildingsTable.insert(Arrays.asList(mainBuilding, secondaryBuilding));
		Department management = new Department(mainBuilding, "Management");
		Department accounts = new Department(mainBuilding, "Accounts department");
		Department delivery = new Department(secondaryBuilding, "Delivery");
		List<Department> departments = Arrays.asList(management, accounts, delivery);

		database.getTable(Department.class).insert(departments);
		return departments;
	}

	private List<Employee> initEmployees(Database database, List<Department> departments) throws OperationException {
		List<Employee> list = new ArrayList<>();
		list.add(new Employee(null, "Vasya", Gender.MALE, new GregorianCalendar(1988, 2, 17).getTime(), new GregorianCalendar(2012, 10, 17).getTime()));
		list.add(new Employee(null, "Vasya kosoy", Gender.MALE, new GregorianCalendar(1990, 2, 2).getTime(), new GregorianCalendar(2012, 10, 16).getTime()));
		list.add(new Employee(null, "Katya", Gender.FEMALE, new GregorianCalendar(1988, 2, 19).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		list.add(new Employee(null, "Lisa", Gender.FEMALE, new GregorianCalendar(1980, 2, 19).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		list.add(new Employee(null, "Stella", Gender.FEMALE, new GregorianCalendar(1987, 2, 19).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		list.add(new Employee(null, "Ann", Gender.FEMALE, new GregorianCalendar(1988, 2, 10).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		list.add(new Employee(null, "Sanya", Gender.MALE, new GregorianCalendar(1988, 2, 9).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		list.add(new Employee(null, "Vanya", Gender.MALE, new GregorianCalendar(1988, 1, 9).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		list.add(new Employee(null, "Peter", Gender.MALE, new GregorianCalendar(1988, 2, 9).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		list.add(new Employee(null, "Jack", Gender.MALE, new GregorianCalendar(1988, 2, 9).getTime(), new GregorianCalendar(2012, 11, 17).getTime()));
		Random r = new Random();
		for (Employee employee : list) {
			employee.setDepartment(departments.get(r.nextInt(departments.size())));
		}

		database.getTable(Employee.class).insert(list);
		return list;
	}

	private void initTasks(Database database, List<Employee> employees) throws Exception {
		List<Task> tasks = new ArrayList<>();
		tasks.add(new Task("Do nothing"));
		tasks.add(new Task("Clean rooms"));
		tasks.add(new Task("Feed trolls"));
		tasks.add(new Task("US123"));
		tasks.add(new Task("Deliver offers"));
		tasks.add(new Task("Sick leave"));
		tasks.add(new Task("Kill flies"));
		tasks.add(new Task("Open doors"));
		tasks.add(new Task("Pick flowers"));
		tasks.add(new Task("Water flowers"));
		tasks.add(new Task("Drag and drop"));
		tasks.add(new Task("Steal money"));
		tasks.add(new Task("Watch TV"));
		tasks.add(new Task("Internet browsing"));
		tasks.add(new Task("Yell"));
		database.getTable(Task.class).insert(tasks);
		Random r = new Random();
		List<TaskListEntry> taskAssignments = new ArrayList<>();
		for (Task task : tasks) {
			Employee assignment = employees.get(r.nextInt(employees.size()));
			taskAssignments.add(new TaskListEntry(task, assignment));
		}

		database.getTable(TaskListEntry.class).insert(taskAssignments);
	}

}
