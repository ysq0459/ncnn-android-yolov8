package com.tencent.yolov8ncnn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "DataDB";
    private static final int DATABASE_VERSION = 2; // 版本升级
    private static final String TABLE_NAME = "records";
    public static final String COL_ID = "_id";
    public static final String COL_LABEL = "label";
    // 新增字段
    public static final String COL_IMAGE_PATH = "image_path";
    public static final String COL_DETECTION_RESULT = "detection_result";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建新表结构（移除value字段）
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LABEL + " TEXT, " +
                COL_IMAGE_PATH + " TEXT, " +      // 图片路径
                COL_DETECTION_RESULT + " TEXT)";  // 检测结果
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级策略：删除旧表创建新表（简化处理）
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 插入数据方法（移除value参数）
    public long insertData(String label, String imagePath, String detectionResult) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_LABEL, label);
        contentValues.put(COL_IMAGE_PATH, imagePath);
        contentValues.put(COL_DETECTION_RESULT, detectionResult);
        return db.insert(TABLE_NAME, null, contentValues);
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME,
                new String[]{COL_ID, COL_LABEL, COL_IMAGE_PATH, COL_DETECTION_RESULT},
                null, null, null, null, null);
    }

    // 更新数据方法（移除value参数）
    public boolean updateData(long id, String label) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_LABEL, label);
        return db.update(TABLE_NAME, contentValues,
                COL_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // 删除单条数据（保持不变）
    public boolean deleteData(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME,
                COL_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // 批量删除数据（保持不变）
    public int deleteMultipleData(String ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME,
                COL_ID + " IN (" + ids + ")", null);
    }
}