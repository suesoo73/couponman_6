package com.example.couponman_6;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private static final String TAG = "TransactionDAO";
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public TransactionDAO(Context context) {
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
     * 새로운 거래 추가
     */
    public long insertTransaction(Transaction transaction) {
        if (!transaction.isValidForSave()) {
            Log.w(TAG, "Invalid transaction data for insert");
            return -1;
        }

        // 거래 타입에 따른 금액 부호 정규화
        transaction.normalizeAmount();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TRANSACTION_COUPON_ID, transaction.getCouponId());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT, transaction.getAmount());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_TYPE, transaction.getTransactionType());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_TYPE, transaction.getBalanceType());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_BEFORE, transaction.getBalanceBefore());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_AFTER, transaction.getBalanceAfter());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_DESCRIPTION, transaction.getDescription());

        try {
            long id = database.insert(DatabaseHelper.TABLE_TRANSACTION, null, values);
            Log.i(TAG, "Transaction inserted with ID: " + id + ", Type: " + transaction.getTransactionType() + 
                      ", Amount: " + transaction.getAmount());
            return id;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error inserting transaction", e);
            return -1;
        }
    }

    /**
     * 거래 정보 업데이트
     */
    public int updateTransaction(Transaction transaction) {
        if (!transaction.isValidForSave()) {
            Log.w(TAG, "Invalid transaction data for update");
            return 0;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TRANSACTION_COUPON_ID, transaction.getCouponId());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT, transaction.getAmount());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_TYPE, transaction.getTransactionType());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_TYPE, transaction.getBalanceType());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_BEFORE, transaction.getBalanceBefore());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_AFTER, transaction.getBalanceAfter());
        values.put(DatabaseHelper.COLUMN_TRANSACTION_DESCRIPTION, transaction.getDescription());

        try {
            int rowsAffected = database.update(
                DatabaseHelper.TABLE_TRANSACTION,
                values,
                DatabaseHelper.COLUMN_TRANSACTION_ID + " = ?",
                new String[]{String.valueOf(transaction.getTransactionId())}
            );
            Log.i(TAG, "Transaction updated, ID: " + transaction.getTransactionId() + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating transaction", e);
            return 0;
        }
    }

    /**
     * 거래 삭제
     */
    public int deleteTransaction(int transactionId) {
        try {
            int rowsAffected = database.delete(
                DatabaseHelper.TABLE_TRANSACTION,
                DatabaseHelper.COLUMN_TRANSACTION_ID + " = ?",
                new String[]{String.valueOf(transactionId)}
            );
            Log.i(TAG, "Transaction deleted, ID: " + transactionId + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error deleting transaction", e);
            return 0;
        }
    }

    /**
     * ID로 거래 조회
     */
    public Transaction getTransactionById(int transactionId) {
        Cursor cursor = null;
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_TRANSACTION,
                null,
                DatabaseHelper.COLUMN_TRANSACTION_ID + " = ?",
                new String[]{String.valueOf(transactionId)},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Transaction transaction = cursorToTransaction(cursor);
                Log.d(TAG, "Transaction found by ID: " + transactionId);
                return transaction;
            }
            
            Log.d(TAG, "No transaction found with ID: " + transactionId);
            return null;
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting transaction by ID", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 쿠폰별 거래 내역 조회
     */
    public List<Transaction> getTransactionsByCouponId(int couponId) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_TRANSACTION,
                null,
                DatabaseHelper.COLUMN_TRANSACTION_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)},
                null, null,
                DatabaseHelper.COLUMN_TRANSACTION_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + transactions.size() + " transactions for coupon ID: " + couponId);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting transactions by coupon ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return transactions;
    }

    /**
     * 거래 유형별 거래 내역 조회
     */
    public List<Transaction> getTransactionsByType(String transactionType) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_TRANSACTION,
                null,
                DatabaseHelper.COLUMN_TRANSACTION_TYPE + " = ?",
                new String[]{transactionType},
                null, null,
                DatabaseHelper.COLUMN_TRANSACTION_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + transactions.size() + " transactions with type: " + transactionType);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting transactions by type", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return transactions;
    }

    /**
     * 기간별 거래 내역 조회
     */
    public List<Transaction> getTransactionsByDateRange(String startDate, String endDate) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_TRANSACTION,
                null,
                DatabaseHelper.COLUMN_TRANSACTION_DATE + " BETWEEN ? AND ?",
                new String[]{startDate, endDate},
                null, null,
                DatabaseHelper.COLUMN_TRANSACTION_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + transactions.size() + " transactions between " + startDate + " and " + endDate);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting transactions by date range", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return transactions;
    }

    /**
     * 모든 거래 내역 조회
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_TRANSACTION,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_TRANSACTION_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + transactions.size() + " transactions");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting all transactions", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return transactions;
    }

    /**
     * 페이지네이션을 지원하는 거래 내역 조회
     */
    public List<Transaction> getTransactionsPaginated(int limit, int offset) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            cursor = database.query(
                DatabaseHelper.TABLE_TRANSACTION,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_TRANSACTION_DATE + " DESC",
                offset + "," + limit
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            
            Log.i(TAG, "Retrieved " + transactions.size() + " transactions (limit: " + limit + ", offset: " + offset + ")");
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting paginated transactions", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return transactions;
    }

    /**
     * 쿠폰별 총 충전 금액 조회
     */
    public double getTotalChargeAmount(int couponId, String balanceType) {
        Cursor cursor = null;
        try {
            String sql = "SELECT SUM(" + DatabaseHelper.COLUMN_TRANSACTION_AMOUNT + ") FROM " + 
                        DatabaseHelper.TABLE_TRANSACTION +
                        " WHERE " + DatabaseHelper.COLUMN_TRANSACTION_COUPON_ID + " = ?" +
                        " AND " + DatabaseHelper.COLUMN_TRANSACTION_TYPE + " = ?" +
                        " AND " + DatabaseHelper.COLUMN_TRANSACTION_BALANCE_TYPE + " = ?";
            
            cursor = database.rawQuery(sql, new String[]{
                String.valueOf(couponId), 
                Transaction.TYPE_CHARGE,
                balanceType
            });
            
            if (cursor != null && cursor.moveToFirst()) {
                double total = cursor.getDouble(0);
                Log.d(TAG, "Total charge amount for coupon " + couponId + " (" + balanceType + "): " + total);
                return total;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting total charge amount", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0.0;
    }

    /**
     * 쿠폰별 총 사용 금액 조회
     */
    public double getTotalUseAmount(int couponId, String balanceType) {
        Cursor cursor = null;
        try {
            String sql = "SELECT SUM(ABS(" + DatabaseHelper.COLUMN_TRANSACTION_AMOUNT + ")) FROM " + 
                        DatabaseHelper.TABLE_TRANSACTION +
                        " WHERE " + DatabaseHelper.COLUMN_TRANSACTION_COUPON_ID + " = ?" +
                        " AND " + DatabaseHelper.COLUMN_TRANSACTION_TYPE + " = ?" +
                        " AND " + DatabaseHelper.COLUMN_TRANSACTION_BALANCE_TYPE + " = ?";
            
            cursor = database.rawQuery(sql, new String[]{
                String.valueOf(couponId), 
                Transaction.TYPE_USE,
                balanceType
            });
            
            if (cursor != null && cursor.moveToFirst()) {
                double total = cursor.getDouble(0);
                Log.d(TAG, "Total use amount for coupon " + couponId + " (" + balanceType + "): " + total);
                return total;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting total use amount", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0.0;
    }

    /**
     * 거래 통계 조회 (충전/사용 건수 및 금액)
     */
    public TransactionSummary getTransactionSummary(int couponId) {
        TransactionSummary summary = new TransactionSummary();
        Cursor cursor = null;
        
        try {
            String sql = "SELECT " +
                        "transaction_type, " +
                        "balance_type, " +
                        "COUNT(*) as count, " +
                        "SUM(ABS(amount)) as total_amount " +
                        "FROM " + DatabaseHelper.TABLE_TRANSACTION +
                        " WHERE coupon_id = ? " +
                        "GROUP BY transaction_type, balance_type";
            
            cursor = database.rawQuery(sql, new String[]{String.valueOf(couponId)});
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String type = cursor.getString(0);
                    String balanceType = cursor.getString(1);
                    int count = cursor.getInt(2);
                    double totalAmount = cursor.getDouble(3);
                    
                    summary.addRecord(type, balanceType, count, totalAmount);
                } while (cursor.moveToNext());
            }
            
            Log.d(TAG, "Retrieved transaction summary for coupon " + couponId);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting transaction summary", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return summary;
    }

    /**
     * 거래 총 개수 조회
     */
    public int getTransactionCount() {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTION, null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Total transaction count: " + count);
                return count;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting transaction count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 쿠폰별 거래 수 조회
     */
    public int getTransactionCountByCouponId(int couponId) {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTION + 
                " WHERE " + DatabaseHelper.COLUMN_TRANSACTION_COUPON_ID + " = ?",
                new String[]{String.valueOf(couponId)}
            );
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Transaction count for coupon " + couponId + ": " + count);
                return count;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting transaction count by coupon ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * Cursor를 Transaction 객체로 변환
     */
    private Transaction cursorToTransaction(Cursor cursor) {
        Transaction transaction = new Transaction();
        
        transaction.setTransactionId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_ID)));
        transaction.setCouponId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_COUPON_ID)));
        transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT)));
        transaction.setTransactionType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_TYPE)));
        transaction.setTransactionDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_DATE)));
        transaction.setBalanceType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_TYPE)));
        transaction.setBalanceBefore(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_BEFORE)));
        transaction.setBalanceAfter(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BALANCE_AFTER)));
        transaction.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_DESCRIPTION)));
        
        return transaction;
    }

    /**
     * 거래 요약 정보를 담는 내부 클래스
     */
    public static class TransactionSummary {
        private int chargeCount = 0;
        private int useCount = 0;
        private double totalChargeAmount = 0.0;
        private double totalUseAmount = 0.0;
        private double cashChargeAmount = 0.0;
        private double cashUseAmount = 0.0;
        private double pointChargeAmount = 0.0;
        private double pointUseAmount = 0.0;

        public void addRecord(String type, String balanceType, int count, double amount) {
            if (Transaction.TYPE_CHARGE.equals(type)) {
                chargeCount += count;
                totalChargeAmount += amount;
                if (Transaction.BALANCE_TYPE_CASH.equals(balanceType)) {
                    cashChargeAmount += amount;
                } else {
                    pointChargeAmount += amount;
                }
            } else if (Transaction.TYPE_USE.equals(type)) {
                useCount += count;
                totalUseAmount += amount;
                if (Transaction.BALANCE_TYPE_CASH.equals(balanceType)) {
                    cashUseAmount += amount;
                } else {
                    pointUseAmount += amount;
                }
            }
        }

        // Getters
        public int getChargeCount() { return chargeCount; }
        public int getUseCount() { return useCount; }
        public double getTotalChargeAmount() { return totalChargeAmount; }
        public double getTotalUseAmount() { return totalUseAmount; }
        public double getCashChargeAmount() { return cashChargeAmount; }
        public double getCashUseAmount() { return cashUseAmount; }
        public double getPointChargeAmount() { return pointChargeAmount; }
        public double getPointUseAmount() { return pointUseAmount; }
    }
}