package com.example.couponman_6;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    
    // 데이터베이스 정보
    private static final String DATABASE_NAME = "couponman.db";
    private static final int DATABASE_VERSION = 4;
    
    // Corporate 테이블 정보
    public static final String TABLE_CORPORATE = "corporate";
    public static final String COLUMN_CUSTOMER_ID = "customer_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_BUSINESS_NUMBER = "business_number";
    public static final String COLUMN_REPRESENTATIVE = "representative";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_CREATED_AT = "created_at";
    
    // Employee 테이블 정보
    public static final String TABLE_EMPLOYEE = "employee";
    public static final String COLUMN_EMPLOYEE_ID = "employee_id";
    public static final String COLUMN_EMPLOYEE_CORPORATE_ID = "corporate_id";
    public static final String COLUMN_EMPLOYEE_NAME = "name";
    public static final String COLUMN_EMPLOYEE_PHONE = "phone";
    public static final String COLUMN_EMPLOYEE_EMAIL = "email";
    public static final String COLUMN_EMPLOYEE_DEPARTMENT = "department";
    public static final String COLUMN_EMPLOYEE_CREATED_AT = "created_at";
    
    // Coupon 테이블 정보
    public static final String TABLE_COUPON = "coupon";
    public static final String COLUMN_COUPON_ID = "coupon_id";
    public static final String COLUMN_COUPON_FULL_CODE = "full_coupon_code";
    public static final String COLUMN_COUPON_EMPLOYEE_ID = "employee_id";
    public static final String COLUMN_COUPON_CASH_BALANCE = "cash_balance";
    public static final String COLUMN_COUPON_POINT_BALANCE = "point_balance";
    public static final String COLUMN_COUPON_EXPIRE_DATE = "expire_date";
    public static final String COLUMN_COUPON_STATUS = "status";
    public static final String COLUMN_COUPON_PAYMENT_TYPE = "payment_type";
    public static final String COLUMN_COUPON_AVAILABLE_DAYS = "available_days";
    public static final String COLUMN_COUPON_CREATED_AT = "created_at";
    
    // Transaction 테이블 정보
    public static final String TABLE_TRANSACTION = "coupon_transaction";
    public static final String COLUMN_TRANSACTION_ID = "transaction_id";
    public static final String COLUMN_TRANSACTION_COUPON_ID = "coupon_id";
    public static final String COLUMN_TRANSACTION_AMOUNT = "amount";
    public static final String COLUMN_TRANSACTION_TYPE = "transaction_type";
    public static final String COLUMN_TRANSACTION_DATE = "transaction_date";
    public static final String COLUMN_TRANSACTION_BALANCE_TYPE = "balance_type";
    public static final String COLUMN_TRANSACTION_BALANCE_BEFORE = "balance_before";
    public static final String COLUMN_TRANSACTION_BALANCE_AFTER = "balance_after";
    public static final String COLUMN_TRANSACTION_DESCRIPTION = "description";
    
    // Corporate 테이블 생성 SQL
    private static final String CREATE_TABLE_CORPORATE = 
            "CREATE TABLE " + TABLE_CORPORATE + " (" +
            COLUMN_CUSTOMER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_BUSINESS_NUMBER + " TEXT UNIQUE, " +
            COLUMN_REPRESENTATIVE + " TEXT, " +
            COLUMN_PHONE + " TEXT, " +
            COLUMN_EMAIL + " TEXT, " +
            COLUMN_ADDRESS + " TEXT, " +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ");";
    
    // Employee 테이블 생성 SQL
    private static final String CREATE_TABLE_EMPLOYEE = 
            "CREATE TABLE " + TABLE_EMPLOYEE + " (" +
            COLUMN_EMPLOYEE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_EMPLOYEE_CORPORATE_ID + " INTEGER NOT NULL, " +
            COLUMN_EMPLOYEE_NAME + " TEXT NOT NULL, " +
            COLUMN_EMPLOYEE_PHONE + " TEXT NOT NULL, " +
            COLUMN_EMPLOYEE_EMAIL + " TEXT, " +
            COLUMN_EMPLOYEE_DEPARTMENT + " TEXT, " +
            COLUMN_EMPLOYEE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_EMPLOYEE_CORPORATE_ID + ") REFERENCES " + 
            TABLE_CORPORATE + "(" + COLUMN_CUSTOMER_ID + ") ON DELETE CASCADE" +
            ");";
    
    // Coupon 테이블 생성 SQL
    private static final String CREATE_TABLE_COUPON = 
            "CREATE TABLE " + TABLE_COUPON + " (" +
            COLUMN_COUPON_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_COUPON_FULL_CODE + " TEXT UNIQUE, " +
            COLUMN_COUPON_EMPLOYEE_ID + " INTEGER NOT NULL, " +
            COLUMN_COUPON_CASH_BALANCE + " REAL DEFAULT 0.0, " +
            COLUMN_COUPON_POINT_BALANCE + " REAL DEFAULT 0.0, " +
            COLUMN_COUPON_EXPIRE_DATE + " DATE NOT NULL, " +
            COLUMN_COUPON_STATUS + " TEXT NOT NULL, " +
            COLUMN_COUPON_PAYMENT_TYPE + " TEXT NOT NULL DEFAULT 'prepaid', " +
            COLUMN_COUPON_AVAILABLE_DAYS + " TEXT NOT NULL DEFAULT '1111111', " +
            COLUMN_COUPON_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_COUPON_EMPLOYEE_ID + ") REFERENCES " + 
            TABLE_EMPLOYEE + "(" + COLUMN_EMPLOYEE_ID + ") ON DELETE CASCADE" +
            ");";
    
    // Transaction 테이블 생성 SQL
    private static final String CREATE_TABLE_TRANSACTION = 
            "CREATE TABLE " + TABLE_TRANSACTION + " (" +
            COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TRANSACTION_COUPON_ID + " INTEGER NOT NULL, " +
            COLUMN_TRANSACTION_AMOUNT + " REAL NOT NULL, " +
            COLUMN_TRANSACTION_TYPE + " TEXT NOT NULL, " +
            COLUMN_TRANSACTION_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_TRANSACTION_BALANCE_TYPE + " TEXT NOT NULL DEFAULT 'cash', " +
            COLUMN_TRANSACTION_BALANCE_BEFORE + " REAL DEFAULT 0.0, " +
            COLUMN_TRANSACTION_BALANCE_AFTER + " REAL DEFAULT 0.0, " +
            COLUMN_TRANSACTION_DESCRIPTION + " TEXT, " +
            "FOREIGN KEY(" + COLUMN_TRANSACTION_COUPON_ID + ") REFERENCES " + 
            TABLE_COUPON + "(" + COLUMN_COUPON_ID + ") ON DELETE CASCADE" +
            ");";
    
    // 인덱스 생성 SQL (검색 성능 향상을 위해)
    private static final String CREATE_INDEX_BUSINESS_NUMBER = 
            "CREATE INDEX idx_business_number ON " + TABLE_CORPORATE + "(" + COLUMN_BUSINESS_NUMBER + ");";
    
    private static final String CREATE_INDEX_NAME = 
            "CREATE INDEX idx_name ON " + TABLE_CORPORATE + "(" + COLUMN_NAME + ");";
    
    // Employee 인덱스 생성 SQL
    private static final String CREATE_INDEX_EMPLOYEE_CORPORATE_ID = 
            "CREATE INDEX idx_employee_corporate_id ON " + TABLE_EMPLOYEE + "(" + COLUMN_EMPLOYEE_CORPORATE_ID + ");";
    
    private static final String CREATE_INDEX_EMPLOYEE_PHONE = 
            "CREATE INDEX idx_employee_phone ON " + TABLE_EMPLOYEE + "(" + COLUMN_EMPLOYEE_PHONE + ");";
    
    // Coupon 인덱스 생성 SQL
    private static final String CREATE_INDEX_COUPON_EMPLOYEE_ID = 
            "CREATE INDEX idx_coupon_employee_id ON " + TABLE_COUPON + "(" + COLUMN_COUPON_EMPLOYEE_ID + ");";
    
    private static final String CREATE_INDEX_COUPON_CODE = 
            "CREATE INDEX idx_coupon_code ON " + TABLE_COUPON + "(" + COLUMN_COUPON_FULL_CODE + ");";
    
    private static final String CREATE_INDEX_COUPON_STATUS = 
            "CREATE INDEX idx_coupon_status ON " + TABLE_COUPON + "(" + COLUMN_COUPON_STATUS + ");";
    
    // Transaction 인덱스 생성 SQL
    private static final String CREATE_INDEX_TRANSACTION_COUPON_ID = 
            "CREATE INDEX idx_transaction_coupon_id ON " + TABLE_TRANSACTION + "(" + COLUMN_TRANSACTION_COUPON_ID + ");";
    
    private static final String CREATE_INDEX_TRANSACTION_DATE = 
            "CREATE INDEX idx_transaction_date ON " + TABLE_TRANSACTION + "(" + COLUMN_TRANSACTION_DATE + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database tables");
        
        try {
            // Corporate 테이블 생성
            db.execSQL(CREATE_TABLE_CORPORATE);
            Log.i(TAG, "Corporate table created successfully");
            
            // Employee 테이블 생성
            db.execSQL(CREATE_TABLE_EMPLOYEE);
            Log.i(TAG, "Employee table created successfully");
            
            // Coupon 테이블 생성
            db.execSQL(CREATE_TABLE_COUPON);
            Log.i(TAG, "Coupon table created successfully");
            
            // Transaction 테이블 생성
            db.execSQL(CREATE_TABLE_TRANSACTION);
            Log.i(TAG, "Transaction table created successfully");
            
            // 인덱스 생성
            db.execSQL(CREATE_INDEX_BUSINESS_NUMBER);
            db.execSQL(CREATE_INDEX_NAME);
            db.execSQL(CREATE_INDEX_EMPLOYEE_CORPORATE_ID);
            db.execSQL(CREATE_INDEX_EMPLOYEE_PHONE);
            db.execSQL(CREATE_INDEX_COUPON_EMPLOYEE_ID);
            db.execSQL(CREATE_INDEX_COUPON_CODE);
            db.execSQL(CREATE_INDEX_COUPON_STATUS);
            db.execSQL(CREATE_INDEX_TRANSACTION_COUPON_ID);
            db.execSQL(CREATE_INDEX_TRANSACTION_DATE);
            Log.i(TAG, "Indexes created successfully");
            
            // 초기 샘플 데이터 삽입
            insertSampleData(db);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        // 간단한 업그레이드: 기존 테이블 삭제 후 재생성
        // 실제 운영 환경에서는 데이터 마이그레이션을 고려해야 함
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION);
        } catch (Exception e) {
            Log.w(TAG, "Error dropping transaction table: " + e.getMessage());
        }
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUPON);
        } catch (Exception e) {
            Log.w(TAG, "Error dropping coupon table: " + e.getMessage());
        }
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPLOYEE);
        } catch (Exception e) {
            Log.w(TAG, "Error dropping employee table: " + e.getMessage());
        }
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CORPORATE);
        } catch (Exception e) {
            Log.w(TAG, "Error dropping corporate table: " + e.getMessage());
        }
        onCreate(db);
    }
    
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        Log.i(TAG, "Database opened");
    }
    
    /**
     * 초기 샘플 데이터 삽입
     */
    private void insertSampleData(SQLiteDatabase db) {
        Log.i(TAG, "Inserting sample corporate data");
        
        try {
            // 샘플 거래처 데이터
            String[] sampleData = {
                "('삼성전자', '1208800767', '이재용', '02-2255-0114', 'contact@samsung.com', '서울특별시 서초구 서초대로74길 11')",
                "('LG전자', '1078600546', '조주완', '02-3777-1114', 'info@lge.com', '서울특별시 영등포구 여의대로 128')",
                "('현대자동차', '2014801002', '장재훈', '02-3464-1114', 'webmaster@hyundai.com', '서울특별시 서초구 헌릉로 12')",
                "('네이버', '2208800767', '최수연', '1588-3820', 'dl_naverhelp@navercorp.com', '경기도 성남시 분당구 정자일로 95')",
                "('카카오', '2208142253', '홍은택', '1566-3755', 'ir@kakaocorp.com', '제주특별자치도 제주시 첨단로 242')"
            };
            
            for (String data : sampleData) {
                String sql = "INSERT INTO " + TABLE_CORPORATE + 
                           " (name, business_number, representative, phone, email, address) VALUES " + data;
                db.execSQL(sql);
            }
            
            Log.i(TAG, "Sample corporate data inserted successfully: " + sampleData.length + " records");
            
            // 샘플 직원 데이터 (거래처 ID 1-5에 대응)
            String[] sampleEmployeeData = {
                "(1, '김철수', '010-1234-5678', 'kim@samsung.com', '개발팀')",
                "(1, '이영희', '010-2345-6789', 'lee@samsung.com', 'IT팀')",
                "(1, '박민수', '010-3456-7890', 'park@samsung.com', '영업팀')",
                "(2, '최준호', '010-4567-8901', 'choi@lge.com', '기획팀')",
                "(2, '정수진', '010-5678-9012', 'jung@lge.com', '마케팅팀')",
                "(3, '김영수', '010-6789-0123', 'kim@hyundai.com', '총무팀')",
                "(3, '서미경', '010-7890-1234', 'seo@hyundai.com', '인사팀')",
                "(4, '홍길동', '010-8901-2345', 'hong@navercorp.com', '개발팀')",
                "(4, '양지원', '010-9012-3456', 'yang@navercorp.com', 'UX팀')",
                "(5, '송하나', '010-0123-4567', 'song@kakaocorp.com', '서비스팀')"
            };
            
            for (String empData : sampleEmployeeData) {
                String sql = "INSERT INTO " + TABLE_EMPLOYEE + 
                           " (corporate_id, name, phone, email, department) VALUES " + empData;
                db.execSQL(sql);
            }
            
            Log.i(TAG, "Sample employee data inserted successfully: " + sampleEmployeeData.length + " records");
            
            // 샘플 쿠폰 데이터 (직원 ID 1-10에 대응, 각자 1개씩)
            String[] sampleCouponData = {
                "(1, 100000.0, 50000.0, '2024-12-31', '사용 가능', 'prepaid', '1111100')",
                "(2, 150000.0, 30000.0, '2024-12-31', '사용 가능', 'prepaid', '1111111')",
                "(3, 200000.0, 0.0, '2024-12-31', '사용 가능', 'postpaid', '1111100')",
                "(4, 80000.0, 20000.0, '2024-12-31', '사용 가능', 'prepaid', '1111111')",
                "(5, 120000.0, 60000.0, '2024-12-31', '사용 가능', 'prepaid', '1111100')",
                "(6, 90000.0, 10000.0, '2024-12-31', '사용 가능', 'prepaid', '1111111')",
                "(7, 110000.0, 40000.0, '2024-12-31', '사용 가능', 'prepaid', '1111100')",
                "(8, 180000.0, 70000.0, '2024-12-31', '사용 가능', 'prepaid', '1111111')",
                "(9, 95000.0, 25000.0, '2024-12-31', '사용 가능', 'prepaid', '1111100')",
                "(10, 160000.0, 80000.0, '2024-12-31', '사용 가능', 'prepaid', '1111111')"
            };
            
            for (String couponData : sampleCouponData) {
                String sql = "INSERT INTO " + TABLE_COUPON + 
                           " (employee_id, cash_balance, point_balance, expire_date, status, payment_type, available_days) VALUES " + couponData;
                db.execSQL(sql);
            }
            
            Log.i(TAG, "Sample coupon data inserted successfully: " + sampleCouponData.length + " records");
            
            // 샘플 거래내역 데이터 (쿠폰 ID 1-5에 대한 충전/사용 내역)
            String[] sampleTransactionData = {
                "(1, 100000.0, '충전', 'cash', 0.0, 100000.0, '초기 충전')",
                "(1, 50000.0, '충전', 'point', 0.0, 50000.0, '포인트 충전')",
                "(1, -15000.0, '사용', 'cash', 100000.0, 85000.0, '점심 식사')",
                "(2, 150000.0, '충전', 'cash', 0.0, 150000.0, '월급 충전')",
                "(2, 30000.0, '충전', 'point', 0.0, 30000.0, '이벤트 포인트')",
                "(2, -8000.0, '사용', 'cash', 150000.0, 142000.0, '커피 구매')",
                "(3, 200000.0, '충전', 'cash', 0.0, 200000.0, '대량 충전')",
                "(3, -25000.0, '사용', 'cash', 200000.0, 175000.0, '회식비')",
                "(4, 80000.0, '충전', 'cash', 0.0, 80000.0, '기본 충전')",
                "(4, 20000.0, '충전', 'point', 0.0, 20000.0, '보너스 포인트')",
                "(5, 120000.0, '충전', 'cash', 0.0, 120000.0, '충전')",
                "(5, 60000.0, '충전', 'point', 0.0, 60000.0, '적립 포인트')"
            };
            
            for (String transactionData : sampleTransactionData) {
                String sql = "INSERT INTO " + TABLE_TRANSACTION + 
                           " (coupon_id, amount, transaction_type, balance_type, balance_before, balance_after, description) VALUES " + transactionData;
                db.execSQL(sql);
            }
            
            Log.i(TAG, "Sample transaction data inserted successfully: " + sampleTransactionData.length + " records");
            
        } catch (Exception e) {
            Log.e(TAG, "Error inserting sample data", e);
        }
    }
    
    /**
     * 데이터베이스 초기화 (모든 테이블 재생성)
     */
    public void resetDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        onUpgrade(db, DATABASE_VERSION, DATABASE_VERSION);
    }
}