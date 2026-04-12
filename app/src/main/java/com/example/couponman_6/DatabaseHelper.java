package com.example.couponman_6;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    
    // 데이터베이스 정보
    private static final String DATABASE_NAME = "couponman.db";
    private static final int DATABASE_VERSION = 7;
    
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
    public static final String COLUMN_EMPLOYEE_CODE = "employee_code";
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
    
    // Coupon Delivery 테이블 정보
    public static final String TABLE_COUPON_DELIVERY = "coupon_deliveries";
    public static final String COLUMN_DELIVERY_ID = "delivery_id";
    public static final String COLUMN_DELIVERY_COUPON_ID = "coupon_id";
    public static final String COLUMN_DELIVERY_TYPE = "delivery_type";
    public static final String COLUMN_DELIVERY_STATUS = "delivery_status";
    public static final String COLUMN_DELIVERY_RECIPIENT_ADDRESS = "recipient_address";
    public static final String COLUMN_DELIVERY_SENT_AT = "sent_at";
    public static final String COLUMN_DELIVERY_DELIVERED_AT = "delivered_at";
    public static final String COLUMN_DELIVERY_FAILED_AT = "failed_at";
    public static final String COLUMN_DELIVERY_RETRY_COUNT = "retry_count";
    public static final String COLUMN_DELIVERY_LAST_RETRY_AT = "last_retry_at";
    public static final String COLUMN_DELIVERY_ERROR_MESSAGE = "error_message";
    public static final String COLUMN_DELIVERY_SUBJECT = "sbj";
    public static final String COLUMN_DELIVERY_MESSAGE = "msg";
    public static final String COLUMN_DELIVERY_METADATA = "metadata";
    public static final String COLUMN_DELIVERY_CREATED_AT = "created_at";
    public static final String COLUMN_DELIVERY_UPDATED_AT = "updated_at";
    
    // System Settings 테이블 정보
    public static final String TABLE_SYSTEM_SETTINGS = "system_settings";
    public static final String COLUMN_SETTING_ID = "setting_id";
    public static final String COLUMN_SETTING_KEY = "setting_key";
    public static final String COLUMN_SETTING_VALUE = "setting_value";
    public static final String COLUMN_SETTING_DESCRIPTION = "description";
    public static final String COLUMN_SETTING_CREATED_AT = "created_at";
    public static final String COLUMN_SETTING_UPDATED_AT = "updated_at";
    
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
            COLUMN_EMPLOYEE_CODE + " TEXT, " +
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
    
    // Coupon Delivery 테이블 생성 SQL
    private static final String CREATE_TABLE_COUPON_DELIVERY = 
            "CREATE TABLE " + TABLE_COUPON_DELIVERY + " (" +
            COLUMN_DELIVERY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DELIVERY_COUPON_ID + " INTEGER NOT NULL, " +
            COLUMN_DELIVERY_TYPE + " VARCHAR(20) NOT NULL, " +
            COLUMN_DELIVERY_STATUS + " VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
            COLUMN_DELIVERY_RECIPIENT_ADDRESS + " TEXT NOT NULL, " +
            COLUMN_DELIVERY_SENT_AT + " DATETIME NULL, " +
            COLUMN_DELIVERY_DELIVERED_AT + " DATETIME NULL, " +
            COLUMN_DELIVERY_FAILED_AT + " DATETIME NULL, " +
            COLUMN_DELIVERY_RETRY_COUNT + " INTEGER DEFAULT 0, " +
            COLUMN_DELIVERY_LAST_RETRY_AT + " DATETIME NULL, " +
            COLUMN_DELIVERY_ERROR_MESSAGE + " TEXT NULL, " +
            COLUMN_DELIVERY_SUBJECT + " TEXT NULL, " +
            COLUMN_DELIVERY_MESSAGE + " TEXT NULL, " +
            COLUMN_DELIVERY_METADATA + " TEXT NULL, " +
            COLUMN_DELIVERY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_DELIVERY_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_DELIVERY_COUPON_ID + ") REFERENCES " + 
            TABLE_COUPON + "(" + COLUMN_COUPON_ID + ") ON DELETE CASCADE" +
            ");";
    
    // System Settings 테이블 생성 SQL
    private static final String CREATE_TABLE_SYSTEM_SETTINGS = 
            "CREATE TABLE " + TABLE_SYSTEM_SETTINGS + " (" +
            COLUMN_SETTING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_SETTING_KEY + " TEXT UNIQUE NOT NULL, " +
            COLUMN_SETTING_VALUE + " TEXT NOT NULL, " +
            COLUMN_SETTING_DESCRIPTION + " TEXT, " +
            COLUMN_SETTING_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_SETTING_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
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

    private static final String CREATE_INDEX_EMPLOYEE_CODE =
            "CREATE INDEX idx_employee_code ON " + TABLE_EMPLOYEE + "(" + COLUMN_EMPLOYEE_CODE + ");";
    
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
    
    // System Settings 인덱스 생성 SQL
    private static final String CREATE_INDEX_SETTING_KEY = 
            "CREATE INDEX idx_setting_key ON " + TABLE_SYSTEM_SETTINGS + "(" + COLUMN_SETTING_KEY + ");";

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
            
            // Coupon Delivery 테이블 생성
            db.execSQL(CREATE_TABLE_COUPON_DELIVERY);
            Log.i(TAG, "Coupon Delivery table created successfully");
            
            // System Settings 테이블 생성
            db.execSQL(CREATE_TABLE_SYSTEM_SETTINGS);
            Log.i(TAG, "System Settings table created successfully");
            
            // 인덱스 생성
            db.execSQL(CREATE_INDEX_BUSINESS_NUMBER);
            db.execSQL(CREATE_INDEX_NAME);
            db.execSQL(CREATE_INDEX_EMPLOYEE_CORPORATE_ID);
            db.execSQL(CREATE_INDEX_EMPLOYEE_PHONE);
            db.execSQL(CREATE_INDEX_EMPLOYEE_CODE);
            db.execSQL(CREATE_INDEX_COUPON_EMPLOYEE_ID);
            db.execSQL(CREATE_INDEX_COUPON_CODE);
            db.execSQL(CREATE_INDEX_COUPON_STATUS);
            db.execSQL(CREATE_INDEX_TRANSACTION_COUPON_ID);
            db.execSQL(CREATE_INDEX_TRANSACTION_DATE);
            db.execSQL(CREATE_INDEX_SETTING_KEY);
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
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SYSTEM_SETTINGS);
        } catch (Exception e) {
            Log.w(TAG, "Error dropping system settings table: " + e.getMessage());
        }
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUPON_DELIVERY);
        } catch (Exception e) {
            Log.w(TAG, "Error dropping coupon delivery table: " + e.getMessage());
        }
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
            // ── 샘플 거래처 데이터 (6개) ──────────────────────────────────────
            String[] sampleCorporateData = {
                "('(주)한국식품', '1234567891', '김대표', '031-123-4567', 'info@kfood.co.kr', '경기도 수원시 영통구 삼성로 129')",
                "('(주)서울테크', '2345678902', '이사장', '02-234-5678', 'contact@stech.co.kr', '서울특별시 강남구 테헤란로 521')",
                "('(주)부산물산', '3456789013', '박사장', '051-345-6789', 'info@bsmul.co.kr', '부산광역시 해운대구 센텀중앙로 55')",
                "('(주)대구기업', '4567890124', '최대표', '053-456-7890', 'admin@dgcorp.co.kr', '대구광역시 중구 달구벌대로 2077')",
                "('(주)인천건설', '5678901235', '정대표', '032-567-8901', 'info@iccon.co.kr', '인천광역시 남동구 인주대로 604')",
                "('(주)광주산업', '6789012346', '강대표', '062-678-9012', 'info@gjind.co.kr', '광주광역시 서구 상무중앙로 110')"
            };

            for (String data : sampleCorporateData) {
                db.execSQL("INSERT INTO " + TABLE_CORPORATE +
                        " (name, business_number, representative, phone, email, address) VALUES " + data);
            }
            Log.i(TAG, "Sample corporate data inserted: " + sampleCorporateData.length + " records");

            // ── 샘플 직원 데이터 (거래처당 5명, 총 30명) ──────────────────────
            String[] sampleEmployeeData = {
                // (주)한국식품 (corporate_id=1)
                "(1, '김민준', '010-1111-1001', 'minjun.kim@kfood.co.kr', '생산팀')",
                "(1, '이서연', '010-1111-1002', 'seoyeon.lee@kfood.co.kr', '영업팀')",
                "(1, '박지훈', '010-1111-1003', 'jihun.park@kfood.co.kr', '기획팀')",
                "(1, '최유나', '010-1111-1004', 'yuna.choi@kfood.co.kr', '총무팀')",
                "(1, '정민호', '010-1111-1005', 'minho.jung@kfood.co.kr', '개발팀')",
                // (주)서울테크 (corporate_id=2)
                "(2, '강지수', '010-2222-2001', 'jisu.kang@stech.co.kr', '개발팀')",
                "(2, '윤수현', '010-2222-2002', 'suhyun.yoon@stech.co.kr', '디자인팀')",
                "(2, '임재원', '010-2222-2003', 'jaewon.lim@stech.co.kr', '영업팀')",
                "(2, '한소희', '010-2222-2004', 'sohee.han@stech.co.kr', '인사팀')",
                "(2, '오동현', '010-2222-2005', 'donghyun.oh@stech.co.kr', '기획팀')",
                // (주)부산물산 (corporate_id=3)
                "(3, '신미래', '010-3333-3001', 'mirae.shin@bsmul.co.kr', '물류팀')",
                "(3, '류성민', '010-3333-3002', 'sungmin.ryu@bsmul.co.kr', '영업팀')",
                "(3, '권지영', '010-3333-3003', 'jiyoung.kwon@bsmul.co.kr', '총무팀')",
                "(3, '남재혁', '010-3333-3004', 'jaehyuk.nam@bsmul.co.kr', '기획팀')",
                "(3, '조아라', '010-3333-3005', 'ara.jo@bsmul.co.kr', '마케팅팀')",
                // (주)대구기업 (corporate_id=4)
                "(4, '황준서', '010-4444-4001', 'junseo.hwang@dgcorp.co.kr', '생산팀')",
                "(4, '문채원', '010-4444-4002', 'chaewon.moon@dgcorp.co.kr', '품질팀')",
                "(4, '배성준', '010-4444-4003', 'sungjun.bae@dgcorp.co.kr', '영업팀')",
                "(4, '노지민', '010-4444-4004', 'jimin.noh@dgcorp.co.kr', '인사팀')",
                "(4, '마승현', '010-4444-4005', 'seunghyun.ma@dgcorp.co.kr', '개발팀')",
                // (주)인천건설 (corporate_id=5)
                "(5, '구민성', '010-5555-5001', 'minsung.goo@iccon.co.kr', '시공팀')",
                "(5, '진하은', '010-5555-5002', 'haeun.jin@iccon.co.kr', '설계팀')",
                "(5, '방준혁', '010-5555-5003', 'junhyuk.bang@iccon.co.kr', '영업팀')",
                "(5, '엄소영', '010-5555-5004', 'soyoung.um@iccon.co.kr', '총무팀')",
                "(5, '석민재', '010-5555-5005', 'minjae.seok@iccon.co.kr', '안전팀')",
                // (주)광주산업 (corporate_id=6)
                "(6, '태현우', '010-6666-6001', 'hyunwoo.tae@gjind.co.kr', '생산팀')",
                "(6, '변수진', '010-6666-6002', 'sujin.byun@gjind.co.kr', '관리팀')",
                "(6, '도재현', '010-6666-6003', 'jaehyun.do@gjind.co.kr', '영업팀')",
                "(6, '표지현', '010-6666-6004', 'jihyun.pyo@gjind.co.kr', '마케팅팀')",
                "(6, '견미리', '010-6666-6005', 'miri.kyun@gjind.co.kr', '인사팀')"
            };

            for (String empData : sampleEmployeeData) {
                db.execSQL("INSERT INTO " + TABLE_EMPLOYEE +
                        " (corporate_id, name, phone, email, department) VALUES " + empData);
            }
            Log.i(TAG, "Sample employee data inserted: " + sampleEmployeeData.length + " records");
            
            // 시스템 설정 기본값 삽입
            insertDefaultSystemSettings(db);
            
        } catch (Exception e) {
            Log.e(TAG, "Error inserting sample data", e);
        }
    }
    
    /**
     * 시스템 설정 기본값 삽입
     */
    private void insertDefaultSystemSettings(SQLiteDatabase db) {
        Log.i(TAG, "Inserting default system settings");
        
        try {
            String[][] defaultSettings = {
                // 아침 시간대 설정 (06:00 - 10:59)
                {"breakfast_start_time", "06:00", "아침 식사 시간 시작"},
                {"breakfast_end_time", "10:59", "아침 식사 시간 종료"},
                {"breakfast_cash_deduction", "3000", "아침 식사 현금 차감액"},
                {"breakfast_point_deduction", "0", "아침 식사 포인트 차감액"},
                
                // 점심 시간대 설정 (11:00 - 14:59)
                {"lunch_start_time", "11:00", "점심 식사 시간 시작"},
                {"lunch_end_time", "14:59", "점심 식사 시간 종료"},
                {"lunch_cash_deduction", "5000", "점심 식사 현금 차감액"},
                {"lunch_point_deduction", "0", "점심 식사 포인트 차감액"},
                
                // 저녁 시간대 설정 (15:00 - 21:59)
                {"dinner_start_time", "15:00", "저녁 식사 시간 시작"},
                {"dinner_end_time", "21:59", "저녁 식사 시간 종료"},
                {"dinner_cash_deduction", "7000", "저녁 식사 현금 차감액"},
                {"dinner_point_deduction", "0", "저녁 식사 포인트 차감액"},
                
                // 기본 차감 설정 (시간대 외)
                {"default_cash_deduction", "1000", "기본 현금 차감액 (시간대 외)"},
                {"default_point_deduction", "0", "기본 포인트 차감액 (시간대 외)"},
                
                // 시스템 설정
                {"enable_time_based_deduction", "true", "시간대별 차감 활성화"},
                {"allow_negative_balance", "false", "마이너스 잔고 허용"}
            };
            
            for (String[] setting : defaultSettings) {
                String sql = "INSERT INTO " + TABLE_SYSTEM_SETTINGS + 
                           " (setting_key, setting_value, description) VALUES (?, ?, ?)";
                db.execSQL(sql, setting);
            }
            
            Log.i(TAG, "Default system settings inserted successfully: " + defaultSettings.length + " records");
            
        } catch (Exception e) {
            Log.e(TAG, "Error inserting default system settings", e);
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
