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
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public EmployeeDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * 데이터베이스 연결 열기 (쓰기용)
     */
    public void open() {
        try {
            database = dbHelper.getWritableDatabase();
            Log.d(TAG, "Database connection opened");
        } catch (SQLiteException e) {
            Log.e(TAG, "Error opening database", e);
            throw e;
        }
    }

    /**
     * 데이터베이스 연결 닫기
     */
    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
            Log.d(TAG, "Database connection closed");
        }
    }

    /**
     * 새로운 직원 추가
     */
    public long insertEmployee(Employee employee) {
        if (!employee.isValidForSave()) {
            Log.w(TAG, "Invalid employee data for insert");
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID, employee.getCorporateId());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_NAME, employee.getName());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_PHONE, employee.getPhone());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_EMAIL, employee.getEmail());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_DEPARTMENT, employee.getDepartment());

        try {
            long id = database.insert(DatabaseHelper.TABLE_EMPLOYEE, null, values);
            Log.i(TAG, "Employee inserted with ID: " + id + ", Name: " + employee.getName());
            return id;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error inserting employee", e);
            return -1;
        }
    }

    /**
     * 직원 정보 업데이트
     */
    public int updateEmployee(Employee employee) {
        if (!employee.isValidForSave()) {
            Log.w(TAG, "Invalid employee data for update");
            return 0;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID, employee.getCorporateId());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_NAME, employee.getName());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_PHONE, employee.getPhone());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_EMAIL, employee.getEmail());
        values.put(DatabaseHelper.COLUMN_EMPLOYEE_DEPARTMENT, employee.getDepartment());

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_EMPLOYEE,
                values,
                DatabaseHelper.COLUMN_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employee.getEmployeeId())}
            );
            Log.i(TAG, "Employee updated, ID: " + employee.getEmployeeId() + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating employee", e);
            return 0;
        }
    }

    /**
     * 직원 삭제
     */
    public int deleteEmployee(int employeeId) {
        try {
            int rowsAffected = database.delete(
                DatabaseHelper.TABLE_EMPLOYEE,
                DatabaseHelper.COLUMN_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employeeId)}
            );
            Log.i(TAG, "Employee deleted, ID: " + employeeId + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error deleting employee", e);
            return 0;
        }
    }

    /**
     * ID로 직원 조회
     */
    public Employee getEmployeeById(int employeeId) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_EMPLOYEE,
                null,
                DatabaseHelper.COLUMN_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employeeId)},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Employee employee = cursorToEmployee(cursor);
                Log.d(TAG, "Employee found by ID: " + employeeId);
                return employee;
            }
            
            Log.d(TAG, "No employee found with ID: " + employeeId);
            return null;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting employee by ID", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 핸드폰 번호로 직원 조회
     */
    public Employee getEmployeeByPhone(String phone) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_EMPLOYEE,
                null,
                DatabaseHelper.COLUMN_EMPLOYEE_PHONE + " = ?",
                new String[]{phone},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Employee employee = cursorToEmployee(cursor);
                Log.d(TAG, "Employee found by phone: " + phone);
                return employee;
            }
            
            Log.d(TAG, "No employee found with phone: " + phone);
            return null;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting employee by phone", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 거래처별 직원 목록 조회
     */
    public List<Employee> getEmployeesByCorporateId(int corporateId) {
        List<Employee> employees = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_EMPLOYEE,
                null,
                DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID + " = ?",
                new String[]{String.valueOf(corporateId)},
                null, null,
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Employee employee = cursorToEmployee(cursor);
                    employees.add(employee);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + employees.size() + " employees for corporate ID: " + corporateId);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting employees by corporate ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return employees;
    }

    /**
     * 모든 직원 조회
     */
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_EMPLOYEE,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Employee employee = cursorToEmployee(cursor);
                    employees.add(employee);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + employees.size() + " employees");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting all employees", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return employees;
    }

    /**
     * 이름으로 직원 검색 (부분 매치)
     */
    public List<Employee> searchEmployeesByName(String name) {
        List<Employee> employees = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_EMPLOYEE,
                null,
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                null, null,
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Employee employee = cursorToEmployee(cursor);
                    employees.add(employee);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Found " + employees.size() + " employees matching name: " + name);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error searching employees by name", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return employees;
    }

    /**
     * 부서별 직원 검색
     */
    public List<Employee> getEmployeesByDepartment(String department) {
        List<Employee> employees = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_EMPLOYEE,
                null,
                DatabaseHelper.COLUMN_EMPLOYEE_DEPARTMENT + " = ?",
                new String[]{department},
                null, null,
                DatabaseHelper.COLUMN_EMPLOYEE_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Employee employee = cursorToEmployee(cursor);
                    employees.add(employee);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Found " + employees.size() + " employees in department: " + department);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting employees by department", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return employees;
    }

    /**
     * 직원 총 개수 조회
     */
    public int getEmployeeCount() {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_EMPLOYEE, null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Total employee count: " + count);
                return count;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting employee count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 거래처별 직원 수 조회
     */
    public int getEmployeeCountByCorporateId(int corporateId) {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_EMPLOYEE + 
                " WHERE " + DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID + " = ?",
                new String[]{String.valueOf(corporateId)}
            );
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Employee count for corporate " + corporateId + ": " + count);
                return count;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting employee count by corporate ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 핸드폰 번호 중복 확인
     */
    public boolean isPhoneExists(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        Employee existing = getEmployeeByPhone(phone);
        return existing != null;
    }

    /**
     * Cursor를 Employee 객체로 변환
     */
    private Employee cursorToEmployee(Cursor cursor) {
        Employee employee = new Employee();
        
        employee.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_ID)));
        employee.setCorporateId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID)));
        employee.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_NAME)));
        employee.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_PHONE)));
        employee.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_EMAIL)));
        employee.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_DEPARTMENT)));
        employee.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMPLOYEE_CREATED_AT)));
        
        return employee;
    }
}