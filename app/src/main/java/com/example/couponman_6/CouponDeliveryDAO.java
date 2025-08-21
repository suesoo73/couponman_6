package com.example.couponman_6;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CouponDeliveryDAO {
    private static final String TAG = "CouponDeliveryDAO";
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private Context context;

    public CouponDeliveryDAO(Context context) {
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
     * 새로운 쿠폰 발송 기록 추가
     */
    public long insertDelivery(int couponId, String deliveryType, String recipientAddress, 
                              String subject, String message, String metadata) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DELIVERY_COUPON_ID, couponId);
        values.put(DatabaseHelper.COLUMN_DELIVERY_TYPE, deliveryType);
        values.put(DatabaseHelper.COLUMN_DELIVERY_STATUS, "PENDING");
        values.put(DatabaseHelper.COLUMN_DELIVERY_RECIPIENT_ADDRESS, recipientAddress);
        values.put(DatabaseHelper.COLUMN_DELIVERY_SUBJECT, subject);
        values.put(DatabaseHelper.COLUMN_DELIVERY_MESSAGE, message);
        values.put(DatabaseHelper.COLUMN_DELIVERY_METADATA, metadata);
        values.put(DatabaseHelper.COLUMN_DELIVERY_RETRY_COUNT, 0);

        try {
            long id = database.insert(DatabaseHelper.TABLE_COUPON_DELIVERY, null, values);
            if (id > 0) {
                Log.i(TAG, "Delivery record inserted with ID: " + id);
            }
            return id;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error inserting delivery record", e);
            return -1;
        }
    }

    /**
     * 발송 상태 업데이트
     */
    public boolean updateDeliveryStatus(long deliveryId, String status, String errorMessage) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DELIVERY_STATUS, status);
        values.put(DatabaseHelper.COLUMN_DELIVERY_UPDATED_AT, getCurrentTimestamp());

        if ("SENT".equals(status)) {
            values.put(DatabaseHelper.COLUMN_DELIVERY_SENT_AT, getCurrentTimestamp());
        } else if ("DELIVERED".equals(status)) {
            values.put(DatabaseHelper.COLUMN_DELIVERY_DELIVERED_AT, getCurrentTimestamp());
        } else if ("FAILED".equals(status)) {
            values.put(DatabaseHelper.COLUMN_DELIVERY_FAILED_AT, getCurrentTimestamp());
            if (errorMessage != null) {
                values.put(DatabaseHelper.COLUMN_DELIVERY_ERROR_MESSAGE, errorMessage);
            }
        }

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_COUPON_DELIVERY,
                values,
                DatabaseHelper.COLUMN_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)}
            );
            
            Log.i(TAG, "Delivery status updated, ID: " + deliveryId + ", Status: " + status);
            return rowsAffected > 0;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating delivery status", e);
            return false;
        }
    }

    /**
     * 재시도 횟수 증가
     */
    public boolean incrementRetryCount(long deliveryId) {
        try {
            // 현재 재시도 횟수 조회
            Cursor cursor = database.query(
                DatabaseHelper.TABLE_COUPON_DELIVERY,
                new String[]{DatabaseHelper.COLUMN_DELIVERY_RETRY_COUNT},
                DatabaseHelper.COLUMN_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)},
                null, null, null
            );

            int currentRetryCount = 0;
            if (cursor != null && cursor.moveToFirst()) {
                currentRetryCount = cursor.getInt(0);
            }
            if (cursor != null) {
                cursor.close();
            }

            // 재시도 횟수 증가
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_DELIVERY_RETRY_COUNT, currentRetryCount + 1);
            values.put(DatabaseHelper.COLUMN_DELIVERY_LAST_RETRY_AT, getCurrentTimestamp());
            values.put(DatabaseHelper.COLUMN_DELIVERY_UPDATED_AT, getCurrentTimestamp());

            int rowsAffected = database.update(
                DatabaseHelper.TABLE_COUPON_DELIVERY,
                values,
                DatabaseHelper.COLUMN_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)}
            );

            Log.i(TAG, "Retry count incremented for delivery ID: " + deliveryId);
            return rowsAffected > 0;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error incrementing retry count", e);
            return false;
        }
    }

    /**
     * 쿠폰별 발송 기록 조회
     */
    public List<CouponDelivery> getDeliveriesByCouponId(int couponId) {
        List<CouponDelivery> deliveries = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON_DELIVERY,
                null,
                DatabaseHelper.COLUMN_DELIVERY_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)},
                null, null,
                DatabaseHelper.COLUMN_DELIVERY_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CouponDelivery delivery = cursorToDelivery(cursor);
                    deliveries.add(delivery);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + deliveries.size() + " deliveries for coupon ID: " + couponId);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting deliveries by coupon ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return deliveries;
    }

    /**
     * 발송 유형별 기록 조회
     */
    public List<CouponDelivery> getDeliveriesByType(String deliveryType) {
        List<CouponDelivery> deliveries = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON_DELIVERY,
                null,
                DatabaseHelper.COLUMN_DELIVERY_TYPE + " = ?",
                new String[]{deliveryType},
                null, null,
                DatabaseHelper.COLUMN_DELIVERY_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CouponDelivery delivery = cursorToDelivery(cursor);
                    deliveries.add(delivery);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + deliveries.size() + " deliveries for type: " + deliveryType);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting deliveries by type", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return deliveries;
    }

    /**
     * 발송 상태별 기록 조회
     */
    public List<CouponDelivery> getDeliveriesByStatus(String status) {
        List<CouponDelivery> deliveries = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON_DELIVERY,
                null,
                DatabaseHelper.COLUMN_DELIVERY_STATUS + " = ?",
                new String[]{status},
                null, null,
                DatabaseHelper.COLUMN_DELIVERY_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CouponDelivery delivery = cursorToDelivery(cursor);
                    deliveries.add(delivery);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + deliveries.size() + " deliveries with status: " + status);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting deliveries by status", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return deliveries;
    }

    /**
     * Cursor를 CouponDelivery 객체로 변환
     */
    private CouponDelivery cursorToDelivery(Cursor cursor) {
        CouponDelivery delivery = new CouponDelivery();
        
        delivery.setDeliveryId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_ID)));
        delivery.setCouponId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_COUPON_ID)));
        delivery.setDeliveryType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_TYPE)));
        delivery.setDeliveryStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_STATUS)));
        delivery.setRecipientAddress(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_RECIPIENT_ADDRESS)));
        delivery.setSentAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_SENT_AT)));
        delivery.setDeliveredAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_DELIVERED_AT)));
        delivery.setFailedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_FAILED_AT)));
        delivery.setRetryCount(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_RETRY_COUNT)));
        delivery.setLastRetryAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_LAST_RETRY_AT)));
        delivery.setErrorMessage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_ERROR_MESSAGE)));
        delivery.setSubject(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_SUBJECT)));
        delivery.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_MESSAGE)));
        delivery.setMetadata(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_METADATA)));
        delivery.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_CREATED_AT)));
        delivery.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_UPDATED_AT)));
        
        return delivery;
    }

    /**
     * 모든 발송 기록 조회
     */
    public List<CouponDelivery> getAllDeliveries() {
        List<CouponDelivery> deliveries = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_COUPON_DELIVERY,
                null,
                null,
                null,
                null, null,
                DatabaseHelper.COLUMN_DELIVERY_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CouponDelivery delivery = cursorToDelivery(cursor);
                    deliveries.add(delivery);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved all deliveries: " + deliveries.size() + " records");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting all deliveries", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return deliveries;
    }
    
    /**
     * 현재 시간을 문자열로 반환
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}