package com.example.couponman_6;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    
    // 데이터베이스 정보
    private static final String DATABASE_NAME = "couponman.db";
    private static final int DATABASE_VERSION = 1;
    
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
    
    // 인덱스 생성 SQL (검색 성능 향상을 위해)
    private static final String CREATE_INDEX_BUSINESS_NUMBER = 
            "CREATE INDEX idx_business_number ON " + TABLE_CORPORATE + "(" + COLUMN_BUSINESS_NUMBER + ");";
    
    private static final String CREATE_INDEX_NAME = 
            "CREATE INDEX idx_name ON " + TABLE_CORPORATE + "(" + COLUMN_NAME + ");";

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
            
            // 인덱스 생성
            db.execSQL(CREATE_INDEX_BUSINESS_NUMBER);
            db.execSQL(CREATE_INDEX_NAME);
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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CORPORATE);
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
            
            Log.i(TAG, "Sample data inserted successfully: " + sampleData.length + " records");
            
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