package com.example.couponman_6;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {
    private static final String TAG = "EmployeeDAO";

    private final DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public EmployeeDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        if (database != null && database.isOpen()) return;
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        // no-op: SQLiteOpenHelper 연결 유지. shutdown()으로만 실제 닫기.
    }

    public void shutdown() {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

    public long insertEmployee(Employee employee) {
        if (!employee.isValidForSave()) {
            return -1;
        }
        return database.insert(DatabaseHelper.TABLE_EMPLOYEE, null, toValues(employee));
    }

    public int updateEmployee(Employee employee) {
        if (!employee.isValidForSave()) {
            return 0;
        }
        return database.update(
                DatabaseHelper.TABLE_EMPLOYEE,
                toValues(employee),
                DatabaseHelper.COLUMN_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employee.getEmployeeId())}
        );
    }

    public int deleteEmployee(int employeeId) {
        return database.delete(
                DatabaseHelper.TABLE_EMPLOYEE,
                DatabaseHelper.COLUMN_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employeeId)}
        );
    }

    public Employee getEmployeeById(int employeeId) {
        return querySingle(
                DatabaseHelper.COLUMN_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employeeId)},
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
        );
    }

    public Employee getEmployeeByCorporateAndCode(int corporateId, String employeeCode) {
        if (employeeCode == null || employeeCode.trim().isEmpty()) {
            return null;
        }
        return querySingle(
                DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID + " = ? AND " +
                        DatabaseHelper.COLUMN_EMPLOYEE_CODE + " = ?",
                new String[]{String.valueOf(corporateId), employeeCode.trim()},
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
        );
    }

    public Employee getEmployeeByPhone(String phone) {
        return null;
    }

    public List<Employee> getEmployeesByCorporateId(int corporateId) {
        return queryList(
                DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID + " = ?",
                new String[]{String.valueOf(corporateId)},
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
        );
    }

    public List<Employee> getAllEmployees() {
        return queryList(null, null, DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC");
    }

    public List<Employee> searchEmployeesByName(String name) {
        return queryList(
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
        );
    }

    public boolean isPhoneExists(String phone) {
        return false;
    }

    public int getEmployeeCount() {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_EMPLOYEE, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    private ContentValues toValues(Employee employee) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID, employee.getCorporateId());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_NAME, employee.getName());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_CODE, employee.getEmployeeCode());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_PHONE, employee.getPhone() != null ? employee.getPhone() : "");
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_EMAIL, employee.getEmail());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_DEPARTMENT, employee.getDepartment());
        return values;
    }

    private Employee querySingle(String selection, String[] args, String orderBy) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                    DatabaseHelper.TABLE_EMPLOYEE,
                    null,
                    selection,
                    args,
                    null,
                    null,
                    orderBy
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToEmployee(cursor);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error querying single employee", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private List<Employee> queryList(String selection, String[] args, String orderBy) {
        List<Employee> employees = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(
                    DatabaseHelper.TABLE_EMPLOYEE,
                    null,
                    selection,
                    args,
                    null,
                    null,
                    orderBy
            );
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    employees.add(cursorToEmployee(cursor));
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error querying employees", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return employees;
    }

    private Employee cursorToEmployee(Cursor cursor) {
        Employee employee = new Employee();
        employee.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_ID)));
        employee.setCorporateId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID)));
        employee.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_NAME)));
        int codeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_EMPLOYEE_CODE);
        if (codeIndex >= 0) {
            employee.setEmployeeCode(cursor.getString(codeIndex));
        }
        employee.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_DEPARTMENT)));
        employee.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_CREATED_AT)));
        return employee;
    }
}
