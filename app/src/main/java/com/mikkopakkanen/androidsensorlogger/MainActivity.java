package com.mikkopakkanen.androidsensorlogger;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.mikkopakkanen.androidsensorlogger.DataLoggerContract.LogEntry;
import com.mikkopakkanen.androidsensorlogger.DataLoggerContract.LogEntry.DataLoggerDBHelper;

import static android.R.attr.data;
import static android.R.attr.entries;
import static com.mikkopakkanen.androidsensorlogger.R.id.chart;
import static com.mikkopakkanen.androidsensorlogger.R.id.log;

public class MainActivity extends Activity {

	private SensorManager sensorManager;
	private BufferedWriter file;
	private LocationManager locationManager;
	private Map<Integer, String> sensorTypes = new HashMap<Integer, String>();
	private Map<Integer, Sensor> sensors = new HashMap<Integer, Sensor>();
	private TextView filenameDisplay;
	private TextView logDisplay;
	private DataLoggerDBHelper DataLoggerDBHelper;
	private List<Entry> entries = new ArrayList<Entry>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get sensors to be captured
		sensorTypes.put(Sensor.TYPE_ACCELEROMETER, "ACCEL");
		sensorTypes.put(Sensor.TYPE_GYROSCOPE, "GYRO");
		sensorTypes.put(Sensor.TYPE_LINEAR_ACCELERATION, "LINEAR");
		sensorTypes.put(Sensor.TYPE_MAGNETIC_FIELD, "MAG");
		sensorTypes.put(Sensor.TYPE_GRAVITY, "GRAV");
		sensorTypes.put(Sensor.TYPE_ROTATION_VECTOR, "ROTATION");

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		for (Integer type : sensorTypes.keySet()) {
			sensors.put(type, sensorManager.getDefaultSensor(type));
		}

		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Register click listeners for buttons
		findViewById(R.id.toggle).setOnClickListener(clickListener);
		findViewById(R.id.btnUpload).setOnClickListener(clickListener);
		findViewById(R.id.btnGetDataFromDB).setOnClickListener(clickListener);
		findViewById(R.id.btnEnter).setOnClickListener(clickListener);
		findViewById(R.id.btnExit).setOnClickListener(clickListener);

		filenameDisplay = (TextView) findViewById(R.id.filename);
		logDisplay = (TextView) findViewById(log);

		// Initialize DB
		DataLoggerDBHelper = new LogEntry.DataLoggerDBHelper(getApplicationContext());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopRecording();
	}

	private void startRecording() {
		// Prepare data storage
		File directory = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		String name = "AllData_" + System.currentTimeMillis() + ".csv";
		File filename = new File(directory, name);
		try {
			file = new BufferedWriter(new FileWriter(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
		filenameDisplay.setText(name);

		// Register sensor listeners
		for (Sensor sensor : sensors.values()) {
			sensorManager.registerListener(sensorListener, sensor,
					SensorManager.SENSOR_DELAY_NORMAL);
		}
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);

	}

	private void stopRecording() {
		sensorManager.unregisterListener(sensorListener);
		locationManager.removeUpdates(locationListener);
		filenameDisplay.setText("");
		try {
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private OnClickListener clickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.toggle:
				if (((ToggleButton) v).isChecked()) {
					startRecording();
				} else {
					stopRecording();
				}
				break;
			case R.id.btnEnter:
				write("ENTER");
				break;
			case R.id.btnExit:
				write("EXIT");
				break;
			case R.id.btnUpload:
				//upload();
				break;
			case R.id.btnGetDataFromDB:
				readFromDB();
				break;
			}
		}

	};

	private SensorEventListener sensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			write(sensorTypes.get(event.sensor.getType()), event.values);
		}

	};

	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			write("GPS",
					new double[] { location.getLatitude(),
							location.getLongitude() });
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void upload(String sensorType, String sensorX, String sensorY, String sensorZ) {
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

		// Retrieve LineChart from XML
		LineChart chart = (LineChart) findViewById(R.id.chart);

		// Sort entries
		Collections.sort(entries, new EntryXComparator());

		LineDataSet dataSet = new LineDataSet(entries, "ACCEL"); // add entries to dataset
		dataSet.setColor(R.color.red);
		dataSet.setValueTextColor(R.color.red); // styling, ...

		LineData lineData = new LineData(dataSet);
		chart.setData(lineData);
		chart.invalidate(); // refresh

		cursor.close();


/*		for (Object s : itemIds){
			Log.d("ID: ", s.toString());
		}*/
	}

	private void write(String tag, String[] values) {
		if (file == null) {
			return;
		}

		String line = "";
		if (values != null) {
			for (String value : values) {
				line += "," + value;
			}
		}
		line = Long.toString(System.currentTimeMillis()) + "," + tag + line
				+ "\n";


		try {
			file.write(line);
		} catch (IOException e) {
			e.printStackTrace();
		}

		upload(tag,values[0], values[1], values[2]);

			if(tag.equals("ACCEL")) {
				if(values[0] != null && values[1] != null) {
					entries.add(new Entry(Float.parseFloat(values[0]), Float.parseFloat(values[1])));

					// Retrieve LineChart from XML
					LineChart chart = (LineChart) findViewById(R.id.chart);

					// Sort entries
					Collections.sort(entries, new EntryXComparator());

					LineDataSet dataSet = new LineDataSet(entries, "ACCEL"); // add entries to dataset
					dataSet.setColor(R.color.red);
					dataSet.setValueTextColor(R.color.red); // styling, ...

					LineData lineData = new LineData(dataSet);
					chart.setData(lineData);
					chart.invalidate(); // refresh
				}
			}


		logDisplay.setText(line);
	}

	private void write(String tag, float[] values) {
		String[] array = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			array[i] = Float.toString(values[i]);
		}
		write(tag, array);
	}

	private void write(String tag, double[] values) {
		String[] array = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			array[i] = Double.toString(values[i]);
		}
		write(tag, array);
	}

	private void write(String tag) {
		write(tag, (String[]) null);
	}

	public String getPhoneName()
	{
		BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
		String deviceName = myDevice.getName();
		return deviceName;
	}

}