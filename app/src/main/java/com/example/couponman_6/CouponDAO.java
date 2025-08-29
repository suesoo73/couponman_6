package com.example.couponman_6;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CouponDAO {
    private static final String TAG = "CouponDAO";
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private Context context;

    public CouponDAO(Context context) {
        this.context = context;
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
     * 새로운 쿠폰 추가
     */
    public long insertCoupon(Coupon coupon) {
        if (!coupon.isValidForSave()) {
            Log.w(TAG, "Invalid coupon data for insert");
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID, coupon.getEmployeeId());
        values.put(DatabaseHelper.COLUMN_COUPON_CASH_BALANCE, coupon.getCashBalance());
        values.put(DatabaseHelper.COLUMN_COUPON_POINT_BALANCE, coupon.getPointBalance());
        values.put(DatabaseHelper.COLUMN_COUPON_EXPIRE_DATE, coupon.getExpireDate());
        values.put(DatabaseHelper.COLUMN_COUPON_STATUS, coupon.getStatus());
        values.put(DatabaseHelper.COLUMN_COUPON_PAYMENT_TYPE, coupon.getPaymentType());
        values.put(DatabaseHelper.COLUMN_COUPON_AVAILABLE_DAYS, coupon.getAvailableDays());

        try {
            long id = database.insert(DatabaseHelper.TABLE_COUPON, null, values);
            if (id > 0) {
                // 쿠폰 ID 설정 후 쿠폰 코드 생성
                coupon.setCouponId((int) id);
                String fullCouponCode = coupon.generateFullCouponCode(context);
                
                // 쿠폰 코드 업데이트
                ContentValues updateValues = new ContentValues();
                updateValues.put(DatabaseHelper.COLUMN_COUPON_FULL_CODE, fullCouponCode);
                
                database.update(DatabaseHelper.TABLE_COUPON, updateValues,
                    DatabaseHelper.COLUMN_COUPON_ID + " = ?",
                    new String[]{String.valueOf(id)});
                
                coupon.setFullCouponCode(fullCouponCode);
                
                Log.i(TAG, "Coupon inserted with ID: " + id + ", Code: " + fullCouponCode);
            }
            return id;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error inserting coupon", e);
            return -1;
        }
    }

    /**
     * 쿠폰 코드 업데이트
     */
    public boolean updateCouponCode(int couponId, String fullCouponCode) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_COUPON_FULL_CODE, fullCouponCode);

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_COUPON,
                values,
                DatabaseHelper.COLUMN_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)}
            );
            
            Log.i(TAG, "Coupon code updated for ID: " + couponId + ", Code: " + fullCouponCode);
            return rowsAffected > 0;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating coupon code", e);
            return false;
        }
    }

    /**
     * 쿠폰 정보 업데이트
     */
    public int updateCoupon(Coupon coupon) {
        if (!coupon.isValidForSave()) {
            Log.w(TAG, "Invalid coupon data for update");
            return 0;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID, coupon.getEmployeeId());
        values.put(DatabaseHelper.COLUMN_COUPON_CASH_BALANCE, coupon.getCashBalance());
        values.put(DatabaseHelper.COLUMN_COUPON_POINT_BALANCE, coupon.getPointBalance());
        values.put(DatabaseHelper.COLUMN_COUPON_EXPIRE_DATE, coupon.getExpireDate());
        values.put(DatabaseHelper.COLUMN_COUPON_STATUS, coupon.getStatus());
        values.put(DatabaseHelper.COLUMN_COUPON_PAYMENT_TYPE, coupon.getPaymentType());
        values.put(DatabaseHelper.COLUMN_COUPON_AVAILABLE_DAYS, coupon.getAvailableDays());

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_COUPON,
                values,
                DatabaseHelper.COLUMN_COUPON_ID + " = ?",
                new String[]{String.valueOf(coupon.getCouponId())}
            );
            Log.i(TAG, "Coupon updated, ID: " + coupon.getCouponId() + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating coupon", e);
            return 0;
        }
    }

    /**
     * 쿠폰 삭제
     */
    public int deleteCoupon(int couponId) {
        try {
            int rowsAffected = database.delete(
                DatabaseHelper.TABLE_COUPON,
                DatabaseHelper.COLUMN_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)}
            );
            Log.i(TAG, "Coupon deleted, ID: " + couponId + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error deleting coupon", e);
            return 0;
        }
    }

    /**
     * ID로 쿠폰 조회
     */
    public Coupon getCouponById(int couponId) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON,
                null,
                DatabaseHelper.COLUMN_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Coupon coupon = cursorToCoupon(cursor);
                Log.d(TAG, "Coupon found by ID: " + couponId);
                return coupon;
            }
            
            Log.d(TAG, "No coupon found with ID: " + couponId);
            return null;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting coupon by ID", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 쿠폰 코드로 쿠폰 조회
     */
    public Coupon getCouponByCode(String couponCode) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON,
                null,
                DatabaseHelper.COLUMN_COUPON_FULL_CODE + " = ?",
                new String[]{couponCode},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Coupon coupon = cursorToCoupon(cursor);
                Log.d(TAG, "Coupon found by code: " + couponCode);
                return coupon;
            }
            
            Log.d(TAG, "No coupon found with code: " + couponCode);
            return null;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting coupon by code", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 직원별 쿠폰 목록 조회
     */
    public List<Coupon> getCouponsByEmployeeId(int employeeId) {
        List<Coupon> coupons = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON,
                null,
                DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employeeId)},
                null, null,
                DatabaseHelper.COLUMN_COUPON_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Coupon coupon = cursorToCoupon(cursor);
                    coupons.add(coupon);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + coupons.size() + " coupons for employee ID: " + employeeId);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting coupons by employee ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return coupons;
    }

    /**
     * 상태별 쿠폰 조회
     */
    public List<Coupon> getCouponsByStatus(String status) {
        List<Coupon> coupons = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON,
                null,
                DatabaseHelper.COLUMN_COUPON_STATUS + " = ?",
                new String[]{status},
                null, null,
                DatabaseHelper.COLUMN_COUPON_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Coupon coupon = cursorToCoupon(cursor);
                    coupons.add(coupon);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + coupons.size() + " coupons with status: " + status);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting coupons by status", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return coupons;
    }

    /**
     * 모든 쿠폰 조회 (직원 및 거래처 정보 포함)
     */
    public List<Coupon> getAllCoupons() {
        List<Coupon> coupons = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            // 쿠폰, 직원, 거래처 테이블을 JOIN하여 포괄적인 정보 조회
            String sql = "SELECT " +
                        "c." + DatabaseHelper.COLUMN_COUPON_ID + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_FULL_CODE + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_CASH_BALANCE + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_POINT_BALANCE + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_EXPIRE_DATE + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_STATUS + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_PAYMENT_TYPE + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_AVAILABLE_DAYS + ", " +
                        "c." + DatabaseHelper.COLUMN_COUPON_CREATED_AT + ", " +
                        "e." + DatabaseHelper.COLUMN_EMPLOYEE_NAME + " AS employee_name, " +
                        "e." + DatabaseHelper.COLUMN_EMPLOYEE_PHONE + " AS employee_phone, " +
                        "e." + DatabaseHelper.COLUMN_EMPLOYEE_EMAIL + " AS employee_email, " +
                        "e." + DatabaseHelper.COLUMN_EMPLOYEE_DEPARTMENT + " AS employee_department, " +
                        "corp." + DatabaseHelper.COLUMN_NAME + " AS corporate_name, " +
                        "corp." + DatabaseHelper.COLUMN_BUSINESS_NUMBER + " AS corporate_business_number, " +
                        "corp." + DatabaseHelper.COLUMN_REPRESENTATIVE + " AS corporate_representative, " +
                        "corp." + DatabaseHelper.COLUMN_PHONE + " AS corporate_phone " +
                        "FROM " + DatabaseHelper.TABLE_COUPON + " c " +
                        "LEFT JOIN " + DatabaseHelper.TABLE_EMPLOYEE + " e ON c." + DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID + " = e." + DatabaseHelper.COLUMN_EMPLOYEE_ID + " " +
                        "LEFT JOIN " + DatabaseHelper.TABLE_CORPORATE + " corp ON e." + DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID + " = corp." + DatabaseHelper.COLUMN_CUSTOMER_ID + " " +
                        "ORDER BY c." + DatabaseHelper.COLUMN_COUPON_CREATED_AT + " DESC";
            
            cursor = database.rawQuery(sql, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Coupon coupon = cursorToCouponWithJoinedData(cursor);
                    coupons.add(coupon);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + coupons.size() + " coupons with employee and corporate information");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting all coupons with JOIN", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return coupons;
    }

    /**
     * 거래처별 쿠폰 조회
     */
    public List<Coupon> getCouponsByCorporateId(int corporateId) {
        List<Coupon> coupons = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            // 쿠폰 테이블과 직원 테이블을 조인하여 거래처 ID로 필터링
            String sql = "SELECT c.* FROM " + DatabaseHelper.TABLE_COUPON + " c " +
                        "JOIN " + DatabaseHelper.TABLE_EMPLOYEE + " e ON c." + DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID + " = e." + DatabaseHelper.COLUMN_EMPLOYEE_ID + " " +
                        "WHERE e." + DatabaseHelper.COLUMN_EMPLOYEE_CORPORATE_ID + " = ? " +
                        "ORDER BY c." + DatabaseHelper.COLUMN_COUPON_CREATED_AT + " DESC";
            
            cursor = database.rawQuery(sql, new String[]{String.valueOf(corporateId)});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Coupon coupon = cursorToCoupon(cursor);
                    coupons.add(coupon);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + coupons.size() + " coupons for corporate ID: " + corporateId);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting coupons by corporate ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return coupons;
    }

    /**
     * 만료 예정 쿠폰 조회 (N일 이내)
     */
    public List<Coupon> getExpiringCoupons(int daysFromNow) {
        List<Coupon> coupons = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            String sql = "SELECT * FROM " + DatabaseHelper.TABLE_COUPON + 
                        " WHERE " + DatabaseHelper.COLUMN_COUPON_EXPIRE_DATE + 
                        " <= date('now', '+" + daysFromNow + " days')" +
                        " AND " + DatabaseHelper.COLUMN_COUPON_STATUS + " = ?" +
                        " ORDER BY " + DatabaseHelper.COLUMN_COUPON_EXPIRE_DATE + " ASC";
            
            cursor = database.rawQuery(sql, new String[]{Coupon.STATUS_ACTIVE});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Coupon coupon = cursorToCoupon(cursor);
                    coupons.add(coupon);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + coupons.size() + " coupons expiring within " + daysFromNow + " days");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting expiring coupons", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return coupons;
    }

    /**
     * 잔액 업데이트
     */
    public boolean updateCouponBalance(int couponId, double cashBalance, double pointBalance) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_COUPON_CASH_BALANCE, cashBalance);
        values.put(DatabaseHelper.COLUMN_COUPON_POINT_BALANCE, pointBalance);

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_COUPON,
                values,
                DatabaseHelper.COLUMN_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)}
            );
            
            Log.i(TAG, "Coupon balance updated, ID: " + couponId + 
                      ", Cash: " + cashBalance + ", Point: " + pointBalance);
            
            return rowsAffected > 0;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating coupon balance", e);
            return false;
        }
    }

    /**
     * 쿠폰 상태 업데이트
     */
    public boolean updateCouponStatus(int couponId, String status) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_COUPON_STATUS, status);

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_COUPON,
                values,
                DatabaseHelper.COLUMN_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)}
            );
            
            Log.i(TAG, "Coupon status updated, ID: " + couponId + ", Status: " + status);
            return rowsAffected > 0;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating coupon status", e);
            return false;
        }
    }

    /**
     * 쿠폰 총 개수 조회
     */
    public int getCouponCount() {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_COUPON, null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Total coupon count: " + count);
                return count;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting coupon count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 직원별 쿠폰 수 조회
     */
    public int getCouponCountByEmployeeId(int employeeId) {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_COUPON + 
                " WHERE " + DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID + " = ?",
                new String[]{String.valueOf(employeeId)}
            );
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Coupon count for employee " + employeeId + ": " + count);
                return count;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting coupon count by employee ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * Cursor를 Coupon 객체로 변환
     */
    private Coupon cursorToCoupon(Cursor cursor) {
        Coupon coupon = new Coupon();
        
        coupon.setCouponId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_ID)));
        coupon.setFullCouponCode(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_FULL_CODE)));
        coupon.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID)));
        coupon.setCashBalance(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_CASH_BALANCE)));
        coupon.setPointBalance(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_POINT_BALANCE)));
        coupon.setExpireDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_EXPIRE_DATE)));
        coupon.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_STATUS)));
        coupon.setPaymentType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_PAYMENT_TYPE)));
        coupon.setAvailableDays(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_AVAILABLE_DAYS)));
        coupon.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_CREATED_AT)));
        
        return coupon;
    }

    /**
     * Cursor를 Coupon 객체로 변환 (JOIN된 직원 및 거래처 정보 포함)
     */
    private Coupon cursorToCouponWithJoinedData(Cursor cursor) {
        Coupon coupon = new Coupon();
        
        // 기본 쿠폰 정보
        coupon.setCouponId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_ID)));
        coupon.setFullCouponCode(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_FULL_CODE)));
        coupon.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_EMPLOYEE_ID)));
        coupon.setCashBalance(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_CASH_BALANCE)));
        coupon.setPointBalance(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_POINT_BALANCE)));
        coupon.setExpireDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_EXPIRE_DATE)));
        coupon.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_STATUS)));
        coupon.setPaymentType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_PAYMENT_TYPE)));
        coupon.setAvailableDays(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_AVAILABLE_DAYS)));
        coupon.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COUPON_CREATED_AT)));
        
        // 직원 정보 설정 (JOIN된 데이터에서)
        try {
            String employeeName = cursor.getString(cursor.getColumnIndex("employee_name"));
            String employeePhone = cursor.getString(cursor.getColumnIndex("employee_phone"));
            String employeeEmail = cursor.getString(cursor.getColumnIndex("employee_email"));
            String employeeDepartment = cursor.getString(cursor.getColumnIndex("employee_department"));
            
            // 직원 정보를 Coupon 객체에 저장 (수신자 정보 필드 활용)
            if (employeeName != null) {
                coupon.setRecipientName(employeeName);
                coupon.setRecipientPhone(employeePhone);
                coupon.setRecipientEmail(employeeEmail);
                coupon.setEmployeeDepartment(employeeDepartment);
                Log.d(TAG, "Coupon ID " + coupon.getCouponId() + " - Employee: " + employeeName + 
                          " (" + employeeDepartment + "), Phone: " + employeePhone);
            }
        } catch (Exception e) {
            Log.w(TAG, "Employee data not available for coupon " + coupon.getCouponId());
        }
        
        // 거래처 정보 설정 (JOIN된 데이터에서)
        try {
            String corporateName = cursor.getString(cursor.getColumnIndex("corporate_name"));
            String corporateBusinessNumber = cursor.getString(cursor.getColumnIndex("corporate_business_number"));
            String corporateRepresentative = cursor.getString(cursor.getColumnIndex("corporate_representative"));
            String corporatePhone = cursor.getString(cursor.getColumnIndex("corporate_phone"));
            
            // 거래처 정보를 Coupon 객체에 저장
            if (corporateName != null) {
                coupon.setCorporateName(corporateName);
                coupon.setCorporateBusinessNumber(corporateBusinessNumber);
                coupon.setCorporateRepresentative(corporateRepresentative);
                coupon.setCorporatePhone(corporatePhone);
                Log.d(TAG, "Coupon ID " + coupon.getCouponId() + " - Corporate: " + corporateName + 
                          " (Business No: " + corporateBusinessNumber + "), Representative: " + corporateRepresentative);
            }
        } catch (Exception e) {
            Log.w(TAG, "Corporate data not available for coupon " + coupon.getCouponId());
        }
        
        return coupon;
    }
}