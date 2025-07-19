package com.tencent.yolov8ncnn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "DataDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "records";
    public static final String COL_ID = "_id";
    public static final String COL_VALUE = "value";
    public static final String COL_LABEL = "label";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LABEL + " TEXT, " +
                COL_VALUE + " REAL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertData(String label, double value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_LABEL, label);
        contentValues.put(COL_VALUE, value);
        return db.insert(TABLE_NAME, null, contentValues);
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME,
                new String[]{COL_ID, COL_LABEL, COL_VALUE},
                null, null, null, null, null);
    }

    // 更新数据方法
    public boolean updateData(long id, String label, double value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_LABEL, label);
        contentValues.put(COL_VALUE, value);
        return db.update(TABLE_NAME, contentValues,
                COL_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // 删除单条数据
    public boolean deleteData(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME,
                COL_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // 批量删除数据
    public int deleteMultipleData(String ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME,
                COL_ID + " IN (" + ids + ")", null);
    }
}
