package com.mikkopakkanen.androidsensorlogger;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.github.mikephil.charting.data.Entry;

import com.mikkopakkanen.androidsensorlogger.DataLoggerContract.LogEntry;
import com.mikkopakkanen.androidsensorlogger.DataLoggerContract.LogEntry.DataLoggerDBHelper;

public class MainActivity extends Activity {

	private SensorManager sensorManager;
	private List<Sensor> sensor;
	private LinearLayout ll;

	private ArrayList<String> sensorsChecked = new ArrayList();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get available sensors on device and make a list of checkboxes
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		ll = (LinearLayout) findViewById (R.id.LinearLayoutMain);
		sensor = sensorManager.getSensorList(Sensor.TYPE_ALL);
		System.out.println(sensor);

		Sensor tmp;
		for (int i=0; i < sensor.size(); i++) {
			tmp = sensor.get(i);
			final CheckBox cb = new CheckBox(getApplicationContext());
			cb.setTextColor(Color.BLACK);
			cb.setTag(tmp.getType());
			cb.setText(tmp.getName());
			cb.setChecked(true);

			sensorsChecked.add(cb.getTag().toString()); // Add all sensors to checked array by default

			// Set onClickListeners
			cb.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					final boolean isChecked = cb.isChecked();
					if(cb.isChecked() == true)
					{
						sensorsChecked.add(cb.getTag().toString());
						System.out.println("toggle on " + cb.getTag());

					}
					else if(cb.isChecked() == false)
					{
						sensorsChecked.remove(cb.getTag().toString());
						System.out.println("toggle off " + cb.getTag());
					}
				}
			});
			ll.addView(cb);
		}

		// Register click listeners for buttons
		findViewById(R.id.toggle).setOnClickListener(clickListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private OnClickListener clickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.toggle:
				if (((ToggleButton) v).isChecked()) {
					Intent myIntent = new Intent(v.getContext(), GraphActivity.class);
					myIntent.putExtra("sensorsChecked", sensorsChecked);
					startActivityForResult(myIntent, 0);
				}
				break;
			}
		}

	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}