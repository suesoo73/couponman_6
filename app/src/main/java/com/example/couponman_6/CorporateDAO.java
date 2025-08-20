package com.example.couponman_6;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CorporateDAO {
    private static final String TAG = "CorporateDAO";
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public CorporateDAO(Context context) {
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
     * 새로운 거래처 추가
     */
    public long insertCorporate(Corporate corporate) {
        if (!corporate.isValidForSave()) {
            Log.w(TAG, "Invalid corporate data for insert");
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, corporate.getName());
        values.put(DatabaseHelper.COLUMN_BUSINESS_NUMBER, corporate.getBusinessNumber());
        values.put(DatabaseHelper.COLUMN_REPRESENTATIVE, corporate.getRepresentative());
        values.put(DatabaseHelper.COLUMN_PHONE, corporate.getPhone());
        values.put(DatabaseHelper.COLUMN_EMAIL, corporate.getEmail());
        values.put(DatabaseHelper.COLUMN_ADDRESS, corporate.getAddress());

        try {
            long id = database.insert(DatabaseHelper.TABLE_CORPORATE, null, values);
            Log.i(TAG, "Corporate inserted with ID: " + id + ", Name: " + corporate.getName());
            return id;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error inserting corporate", e);
            return -1;
        }
    }

    /**
     * 거래처 정보 업데이트
     */
    public int updateCorporate(Corporate corporate) {
        if (!corporate.isValidForSave()) {
            Log.w(TAG, "Invalid corporate data for update");
            return 0;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, corporate.getName());
        values.put(DatabaseHelper.COLUMN_BUSINESS_NUMBER, corporate.getBusinessNumber());
        values.put(DatabaseHelper.COLUMN_REPRESENTATIVE, corporate.getRepresentative());
        values.put(DatabaseHelper.COLUMN_PHONE, corporate.getPhone());
        values.put(DatabaseHelper.COLUMN_EMAIL, corporate.getEmail());
        values.put(DatabaseHelper.COLUMN_ADDRESS, corporate.getAddress());

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_CORPORATE,
                values,
                DatabaseHelper.COLUMN_CUSTOMER_ID + " = ?",
                new String[]{String.valueOf(corporate.getCustomerId())}
            );
            Log.i(TAG, "Corporate updated, ID: " + corporate.getCustomerId() + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating corporate", e);
            return 0;
        }
    }

    /**
     * 거래처 삭제
     */
    public int deleteCorporate(int customerId) {
        try {
            int rowsAffected = database.delete(
                DatabaseHelper.TABLE_CORPORATE,
                DatabaseHelper.COLUMN_CUSTOMER_ID + " = ?",
                new String[]{String.valueOf(customerId)}
            );
            Log.i(TAG, "Corporate deleted, ID: " + customerId + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error deleting corporate", e);
            return 0;
        }
    }

    /**
     * ID로 거래처 조회
     */
    public Corporate getCorporateById(int customerId) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_CORPORATE,
                null,
                DatabaseHelper.COLUMN_CUSTOMER_ID + " = ?",
                new String[]{String.valueOf(customerId)},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Corporate corporate = cursorToCorporate(cursor);
                Log.d(TAG, "Corporate found by ID: " + customerId);
                return corporate;
            }
            
            Log.d(TAG, "No corporate found with ID: " + customerId);
            return null;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting corporate by ID", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 사업자등록번호로 거래처 조회
     */
    public Corporate getCorporateByBusinessNumber(String businessNumber) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_CORPORATE,
                null,
                DatabaseHelper.COLUMN_BUSINESS_NUMBER + " = ?",
                new String[]{businessNumber},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Corporate corporate = cursorToCorporate(cursor);
                Log.d(TAG, "Corporate found by business number: " + businessNumber);
                return corporate;
            }
            
            Log.d(TAG, "No corporate found with business number: " + businessNumber);
            return null;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting corporate by business number", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 모든 거래처 조회
     */
    public List<Corporate> getAllCorporates() {
        List<Corporate> corporates = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_CORPORATE,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Corporate corporate = cursorToCorporate(cursor);
                    corporates.add(corporate);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + corporates.size() + " corporates");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting all corporates", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return corporates;
    }

    /**
     * 이름으로 거래처 검색 (부분 매치)
     */
    public List<Corporate> searchCorporatesByName(String name) {
        List<Corporate> corporates = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_CORPORATE,
                null,
                DatabaseHelper.COLUMN_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                null, null,
                DatabaseHelper.COLUMN_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Corporate corporate = cursorToCorporate(cursor);
                    corporates.add(corporate);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Found " + corporates.size() + " corporates matching name: " + name);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error searching corporates by name", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return corporates;
    }

    /**
     * 페이지네이션을 지원하는 거래처 조회
     */
    public List<Corporate> getCorporatesPaginated(int limit, int offset) {
        List<Corporate> corporates = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_CORPORATE,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_NAME + " ASC",
                offset + "," + limit
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Corporate corporate = cursorToCorporate(cursor);
                    corporates.add(corporate);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + corporates.size() + " corporates (limit: " + limit + ", offset: " + offset + ")");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting paginated corporates", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return corporates;
    }

    /**
     * 거래처 총 개수 조회
     */
    public int getCorporateCount() {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_CORPORATE, null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Total corporate count: " + count);
                return count;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting corporate count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 사업자등록번호 중복 확인
     */
    public boolean isBusinessNumberExists(String businessNumber) {
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            return false;
        }
        
        Corporate existing = getCorporateByBusinessNumber(businessNumber);
        return existing != null;
    }

    /**
     * Cursor를 Corporate 객체로 변환
     */
    private Corporate cursorToCorporate(Cursor cursor) {
        Corporate corporate = new Corporate();
        
        corporate.setCustomerId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_ID)));
        corporate.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
        corporate.setBusinessNumber(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BUSINESS_NUMBER)));
        corporate.setRepresentative(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPRESENTATIVE)));
        corporate.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE)));
        corporate.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL)));
        corporate.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS)));
        corporate.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));
        
        return corporate;
    }
}