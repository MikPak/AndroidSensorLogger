package com.mikkopakkanen.androidsensorlogger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class DataLoggerContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DataLoggerContract() {}

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LogEntry.TABLE_NAME + " (" +
                    LogEntry._ID + " INTEGER PRIMARY KEY," +
                    LogEntry.COLUMN_NAME_SENSOR_TYPE + " TEXT," +
                    LogEntry.COLUMN_NAME_X + " TEXT," +
                    LogEntry.COLUMN_NAME_Y + " TEXT," +
                    LogEntry.COLUMN_NAME_Z + " TEXT," +
                    LogEntry.COLUMN_NAME_TIMESTAMP + " TEXT," +
                    LogEntry.COLUMN_NAME_INSERT_DATE + " TEXT," +
                    LogEntry.COLUMN_NAME_DEVICE + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LogEntry.TABLE_NAME;

    /* Inner class that defines the table contents */
    public static class LogEntry implements BaseColumns {
        public static final String TABLE_NAME = "logs";
        public static final String COLUMN_NAME_SENSOR_TYPE = "type";
        public static final String COLUMN_NAME_X = "x";
        public static final String COLUMN_NAME_Y = "Y";
        public static final String COLUMN_NAME_Z = "Z";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_INSERT_DATE = "insertDate";
        public static final String COLUMN_NAME_DEVICE = "device";

        public static class DataLoggerDBHelper extends SQLiteOpenHelper {
            // If you change the database schema, you must increment the database version.
            public static final int DATABASE_VERSION = 12;
            public static final String DATABASE_NAME = "AndroidDataLogger.db";

            public DataLoggerDBHelper(Context context) {
                super(context, DATABASE_NAME, null, DATABASE_VERSION);
            }
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(SQL_CREATE_ENTRIES);
            }
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                // This database is only a cache for online data, so its upgrade policy is
                // to simply to discard the data and start over
                db.execSQL(SQL_DELETE_ENTRIES);
                onCreate(db);
            }
            public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                onUpgrade(db, oldVersion, newVersion);
            }
        }
    }

}
