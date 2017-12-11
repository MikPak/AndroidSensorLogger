package com.mikkopakkanen.androidsensorlogger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.mikkopakkanen.androidsensorlogger.DataLoggerContract.LogEntry;
import com.mikkopakkanen.androidsensorlogger.DataLoggerContract.LogEntry.DataLoggerDBHelper;

import static android.R.attr.key;

public class GraphActivity extends Activity {

    private SensorManager sensorManager;
    private Map<Integer, Sensor> sensors = new HashMap<Integer, Sensor>();
    private LinkedHashMap<Integer,Integer> sensorTypes = new LinkedHashMap<Integer,Integer>();
    private NavigableMap<Integer, LineChart> chartsMap = new TreeMap<Integer, LineChart>();
    private int chartCount;

    private LineChart currentChart;
    public LineChart getCurrentChart() { return currentChart; }
    public void setCurrentChart(LineChart chart) { currentChart = chart; }

    private int currentSensorType;
    public int getCurrentType() { return currentSensorType; }
    public void setCurrentType(int sensorType) { currentSensorType = sensorType; }

    private List<Entry> graphEntries = new ArrayList<>();
    private LinkedHashMap<Integer,ArrayList<Entry>> graphEntriesMap = new LinkedHashMap<Integer,ArrayList<Entry>>();

    private DataLoggerDBHelper DataLoggerDBHelper;

    private long startTime, timeElapsed;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graphs);

        DataLoggerDBHelper = new DataLoggerDBHelper(getBaseContext());
        chartCount = 0;

        Button back = (Button) findViewById(R.id.ButtonBack);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                stopRecording();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Button previous = (Button) findViewById(R.id.ButtonPrevious);
        previous.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                LineChart chart;
                if(chartCount != 0) {
                    chartCount--;
                }

                for (Map.Entry<Integer, LineChart> e : chartsMap.entrySet()) {
                    Map.Entry<Integer, LineChart> next = chartsMap.higherEntry(e.getKey()); // next

                    if(e.getKey() == chartCount ) {
                        System.out.println(chartCount);
                        getCurrentChart().setVisibility(view.GONE);
                        int sensorType = (new ArrayList<Integer>(sensorTypes.values())).get(chartCount);
                        setCurrentType(sensorType);
                        System.out.println(e.getKey() + " / " + e.getValue());

                        chart = e.getValue();
                        setCurrentChart(chart);
                        getCurrentChart().setVisibility(view.VISIBLE);
                        System.out.println(getCurrentChart());
                    }
                }
            }
        });

        Button next = (Button) findViewById(R.id.ButtonNext);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                System.out.println(chartCount);
                LineChart chart;
                if(chartCount < sensors.size()) {
                    chartCount++;
                }

                for (Map.Entry<Integer, LineChart> e : chartsMap.entrySet()) {
                    Map.Entry<Integer, LineChart> next = chartsMap.higherEntry(e.getKey()); // next

                    if(e.getKey() == chartCount) {
                        System.out.println(chartCount);
                        getCurrentChart().setVisibility(view.GONE);
                        int sensorType = (new ArrayList<Integer>(sensorTypes.values())).get(chartCount);
                        setCurrentType(sensorType);
                        System.out.println(e.getKey() + " / " + e.getValue());

                        chart = e.getValue();
                        setCurrentChart(chart);
                        getCurrentChart().setVisibility(view.VISIBLE);
                        System.out.println(getCurrentChart());
                    }
                }
            }
        });

        ArrayList<String> sensorsChecked = (ArrayList<String>) getIntent().getSerializableExtra("sensorsChecked");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //System.out.println("Selected sensor types: ");
        int i = 0;
        for (String sensor : sensorsChecked) {
            System.out.println(sensor);
            int type = Integer.parseInt(sensor);
            sensors.put(type, sensorManager.getDefaultSensor(type));
            sensorTypes.put(i, type);
            graphEntriesMap.put(type, new ArrayList<Entry>());

            // Create charts of selected sensors and set visibility to hidden by default
            LineChart chart = new LineChart(getApplicationContext());
            chart.setVisibility(View.GONE);
            RelativeLayout rl = (RelativeLayout) findViewById(R.id.relativeLayoutChart);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            this.timeElapsed = (System.currentTimeMillis() - this.startTime) / 1000;
            ArrayList<Entry> entries = new ArrayList<Entry>();
            entries.add(new Entry(timeElapsed, 0));

            Collections.sort(entries, new EntryXComparator()); // Sort entries
            LineDataSet dataSet = new LineDataSet(entries, sensorManager.getDefaultSensor(type).getName()); // add entries to dataset
            LineData lineData = new LineData(dataSet);

            chart.setData(lineData);
            chart.notifyDataSetChanged(); // let the chart know it's data changed
            chart.setVisibleXRangeMaximum(10);
            //this.currentChart.invalidate(); // refresh
            chart.moveViewToX(lineData.getEntryCount());
            //params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

            if(i == 0) {
                chart.setVisibility(View.VISIBLE);
                this.currentChart = chart;
                this.currentSensorType = type;
            }

            chartsMap.put(i, chart);
            rl.addView(chart, params);
            i++;
        }
        startRecording();
    }

    public void startRecording() {
        this.startTime = System.currentTimeMillis();

        // Register sensor listeners
        for (Sensor sensor : sensors.values()) {
            try {
                System.out.println("Registering: " + sensor.getName());
                sensorManager.registerListener(sensorListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void stopRecording() {
        sensorManager.unregisterListener(sensorListener);
    }

    public void graphDraw(int tag, String name, float[] values) {
        //System.out.println(name);
        String[] array = new String[values.length];

        for (int i = 0; i < values.length; i++) {
            array[i] = Float.toString(values[i]);
        }

        if (array[0] != null && array[1] != null && array[2] != null) {
            upload(tag, array[0], array[1], array[2]);
            this.timeElapsed = (System.currentTimeMillis() - this.startTime) / 1000;
            ArrayList<Entry> entries;

            if (this.currentSensorType == tag) {
                //System.out.println(name);
                this.currentChart.invalidate();
                this.currentChart.clear();
                for (Map.Entry<Integer, ArrayList<Entry>> entry : graphEntriesMap.entrySet()) {
                    int key = entry.getKey();
                    if (key == tag) {
                        //System.out.println(key);
                        //System.out.println(value);
                        entries = entry.getValue();
                        entries.add(new Entry(timeElapsed, Float.parseFloat(array[0])));
                        graphEntriesMap.put(key, entries);

                        Collections.sort(entries, new EntryXComparator()); // Sort entries
                        LineDataSet dataSet = new LineDataSet(entries, name); // add entries to dataset
                        LineData lineData = new LineData(dataSet);

                        this.currentChart.setData(lineData);
                        this.currentChart.notifyDataSetChanged(); // let the chart know it's data changed
                        this.currentChart.setVisibleXRangeMaximum(10);
                        this.currentChart.invalidate(); // refresh
                        this.currentChart.moveViewToX(lineData.getEntryCount());
                    }
                }
            } else {
                for (Map.Entry<Integer, ArrayList<Entry>> entry : graphEntriesMap.entrySet()) {
                    int key = entry.getKey();
                    if (key == tag) {
                        //System.out.println("Pushing entry data to other graph: " + name);
                        entries = entry.getValue();
                        entries.add(new Entry(timeElapsed, Float.parseFloat(array[0])));
                        graphEntriesMap.put(key, entries);
                    }
                }
            }
        }
    }

    private SensorEventListener sensorListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //System.out.println(event.sensor.getType() + " - " + event.sensor.getName());
            //System.out.println(event.sensor.getName());
            graphDraw(event.sensor.getType(), event.sensor.getName(), event.values);
        }
    };

    private void upload(int sensorType, String sensorX, String sensorY, String sensorZ) {
        // Gets the data repository in write mode
        SQLiteDatabase db = DataLoggerDBHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(LogEntry.COLUMN_NAME_SENSOR_TYPE, sensorType);
        values.put(LogEntry.COLUMN_NAME_X, sensorX);
        values.put(LogEntry.COLUMN_NAME_Y, sensorY);
        values.put(LogEntry.COLUMN_NAME_Z, sensorZ);
        values.put(LogEntry.COLUMN_NAME_TIMESTAMP, Long.toString(System.currentTimeMillis()));
        values.put(LogEntry.COLUMN_NAME_INSERT_DATE, Calendar.getInstance().getTime().toString());
        values.put(LogEntry.COLUMN_NAME_DEVICE, getPhoneName());

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(LogEntry.TABLE_NAME, null, values);
        System.out.println("Added new sensor record to DB with the ID: #" + newRowId);
    }

    private void readFromDB() {
        SQLiteDatabase db = DataLoggerDBHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                LogEntry._ID,
                LogEntry.COLUMN_NAME_SENSOR_TYPE ,
                LogEntry.COLUMN_NAME_X,
                LogEntry.COLUMN_NAME_Y,
                LogEntry.COLUMN_NAME_Z,
                LogEntry.COLUMN_NAME_TIMESTAMP,
                LogEntry.COLUMN_NAME_INSERT_DATE,
                LogEntry.COLUMN_NAME_DEVICE
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = LogEntry.COLUMN_NAME_SENSOR_TYPE + " = ?";
        String[] selectionArgs = { "ACCEL" };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                LogEntry._ID + " ASC";

        Cursor cursor = db.query(
                LogEntry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        List itemIds = new ArrayList<>();
        List<Entry> entries = new ArrayList<Entry>();

        while(cursor.moveToNext()) {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(LogEntry._ID));
            itemIds.add(itemId);

            Log.d("ID: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry._ID)));
            Log.d("Sensor: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_SENSOR_TYPE)));
            Log.d("X: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_X)));
            Log.d("Y: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_Y)));
            Log.d("Z: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_Z)));
            Log.d("Timestamp: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_TIMESTAMP)));
            Log.d("Insert Date: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_INSERT_DATE)));
            Log.d("Device: ", cursor.getString(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_DEVICE)));

            entries.add(new Entry(cursor.getFloat(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_X)), cursor.getFloat(cursor.getColumnIndexOrThrow(LogEntry.COLUMN_NAME_Y))));
        }

        cursor.close();
    }

    public String getPhoneName()
    {
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceName = myDevice.getName();
        return deviceName;
    }
}