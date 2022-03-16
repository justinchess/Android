package com.example.tsensors;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

/* Main Activity for the main screen */
public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener, AdapterView.OnItemLongClickListener, ListAdapter.CheckBoxClickListener {
    private SensorManager sensorManager;
    private Sensor mAcc, mLAcc, mGyr;                           // Sensors: Accelerometer, Linear Acceleration, Gyroscope
    private Button btnStart, btnSave;                // Buttons
    static public ArrayList<ArrayList<Float>> values;                 // Sensor values to save
    private float Tax, Tay, Taz, Tlx, Tly, Tlz, Tgx, Tgy, Tgz;  // Temp sensor value
    private SensorEvent Aevn, Levn, Gevn;                       // Temp sensor event
    private int gap;                                            // time gap between each sensor value
    static public ListView listView;                                  // main list
    private boolean selectState;                                // state for long clicked(showing the checkbox_
    private boolean startState;                                 // state for Start and Stop button
    static public ArrayList<ListItem> savedFiles;                                // all the save fileNames
    private int width, height;                                  // screen size
    static public ListAdapter adapter;                                // main adapter for listview
    static public String fileName, filePath;
    private boolean allSelState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeValues();

        configureSensors();

        initializeGUI();

        Toast.makeText(this, "Press Start button to get sensor values", Toast.LENGTH_LONG).show();
    }

    protected void initializeValues() {

        /* Get the screen size*/
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;

        /* Initialize the values */
        values = new ArrayList<ArrayList<Float>>();
        Aevn = Levn = Gevn = null;
        Tax = Tay = Taz = Tlx = Tly = Tlz = Tgx = Tgy = Tgz = 0;
        gap = 5;
        startState = selectState = false;
        allSelState = true;
        fileName = "";
        filePath = "";

        /* Get the saved file names */
        String path = Environment.getExternalStorageDirectory().toString()+"/tsensor";
        File directory = new File(path);
        File[] files = directory.listFiles();
        savedFiles = new ArrayList<ListItem>();
        for (int i = 0; i < files.length; i++)
            savedFiles.add(new ListItem(files[i].getName()));

        /* Set the adapter */
        adapter = new ListAdapter(this, savedFiles);
        adapter.setNotifyOnChange(true);
        adapter.notifyDataSetChanged();
        adapter.setCheckBoxClickListener(this);
        adapter.sort(new Comparator<ListItem>() {
            @Override
            public int compare(ListItem listItem, ListItem t1) {
                return listItem.getTime() < t1.getTime() ? 1 : -1;
            }
        });
        oneMinRunnable.run();
    }

    /*initialize the GUI(Get the GUI items and Add the listener)*/
    protected void initializeGUI() {
        /* Configure the buttons */
        btnStart = findViewById(R.id.btnStart);
        btnSave = findViewById(R.id.btnSave);

        btnStart.setOnClickListener(this);
        btnSave.setOnClickListener(this);


        /* Configure the listview */
        try {
            listView = (ListView) findViewById(R.id.listView);
            listView.setAdapter(adapter);
            listView.setOnItemLongClickListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void configureSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyr = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    /* Create the menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.info, menu);
        return true;
    }

    /* Change the option menu */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuSelect).setVisible(!selectState);
        menu.findItem(R.id.menuSelectAll).setVisible(allSelState && selectState);
        menu.findItem(R.id.menuDeselectAll).setVisible(!allSelState);
        menu.findItem(R.id.menuRemove).setVisible(selectState);
        menu.findItem(R.id.menuUndo).setVisible(selectState);
        return true;
    }

    /* Implement the menu event */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuSelect:
                //showCheckBox(View.VISIBLE);
                showCheckBox(true);
                return true;
            case R.id.menuRemove:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure to remove selected items?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return true;
            case R.id.menuSelectAll:
                selectAllItems(true);
                allSelState = false;
                return true;
            case R.id.menuDeselectAll:
                selectAllItems(false);
                allSelState = true;
                return true;
            case R.id.menuUndo:
                undo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Select or deselect all items */
    static public void selectAllItems(boolean state) {
        try {
            for (int i = 0; i < adapter.getCount(); i++) {
                adapter.getItem(i).setSelect(state);
            }
        } catch(Exception e) {
            Log.e("Exception", e.getMessage());
        }
        adapter.notifyDataSetChanged();
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    /* Show or hide the checkbox when long click or choose the select menu */
    protected void showCheckBox(boolean state) {
        adapter.isCheckboxVisible = state;
        selectState = state;
        adapter.notifyDataSetChanged();
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    /* Handle the sensor event */
    @Override
    public final void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Aevn = event;
            Tax = event.values[0];
            Tay = event.values[1];
            Taz = event.values[2];
        }
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            Levn = event;
            Tlx = event.values[0];
            Tly = event.values[1];
            Tlz = event.values[2];
        }
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            Gevn = event;
            Tgx = event.values[0];
            Tgy = event.values[1];
            Tgz = event.values[2];
        }
    }

    /* Handler for getting sensors */
    private final Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            if (Aevn != null && Levn != null && Gevn != null) {
                ArrayList<Float> array = new ArrayList<Float>();
                array.add(Tax);
                array.add(Tay);
                array.add(Taz);
                array.add(Tlx);
                array.add(Tly);
                array.add(Tlz);
                array.add(Tgx);
                array.add(Tgy);
                array.add(Tgz);
                values.add(array);
            }
            handler.postDelayed(this, gap);
        }
    };

    /* Handler for the invalidating the time */
    private final Handler oneMinHandler = new Handler();
    private Runnable oneMinRunnable = new Runnable() {
        public void run() {
            try {
                adapter.notifyDataSetChanged();
            } catch(Exception e) {
                e.printStackTrace();
            }
            handler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /* Set the sensor listener and run the handler */
    protected void startListen() {
        if(mAcc != null)
            sensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_FASTEST);
        else
            Toast.makeText(this, "This device doesn't support Accelerometer.", Toast.LENGTH_LONG).show();
        if(mLAcc != null)
            sensorManager.registerListener(this, mLAcc, SensorManager.SENSOR_DELAY_FASTEST);
        else
            Toast.makeText(this, "This device doesn't support Linear Acceleration.", Toast.LENGTH_LONG).show();
        if(mGyr != null)
            sensorManager.registerListener(this, mGyr, SensorManager.SENSOR_DELAY_FASTEST);
        else
            Toast.makeText(this, "This device doesn't support Gyroscope.", Toast.LENGTH_LONG).show();
        runnable.run();
    }

    /* Handle the button events */
    @Override
    public void onClick(View view) {
        /* When Start button clicked */
        if(view.getId() == R.id.btnStart) {
            /* When the current text of button is "Start"*/
            if (!startState) {
                for (int i = 0; i < values.size(); i++)
                    values.get(i).clear();
                values.clear();
                startListen();
                btnStart.setText("Stop");
                Toast.makeText(this, "Press the Stop button to pause getting sensor values.", Toast.LENGTH_LONG).show();
            }
            /* When the current text of button is "Stop"*/
            else {
                /* Unregister the listener and Stop the handler */
                sensorManager.unregisterListener(this);
                handler.removeCallbacks(runnable);

                btnStart.setText("Start");
                Toast.makeText(this, "Press the Start button to resume or Save button to save sensor values.", Toast.LENGTH_LONG).show();
            }
            /* Change the startState */
            startState = !startState;
        }
        /* When the Save button clicked */
        if(view.getId() == R.id.btnSave) {
            try {
                if (isExternalStorageWritable())
                    showSaveDialog();
                else
                    Toast.makeText(this, "Can't find external storage!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e("Write", e.getMessage());
            }
        }
        selectAllItems(false);
        showCheckBox(false);
    }

    /* Remove selected items */
    protected void removeItems() {
        int count = 0;
        for ( int i = listView.getAdapter().getCount() - 1; i >= 0; i--) {
            View v = getViewByPosition(i, listView);
            //View v = listView.getChildAt(i);
            ListAdapter.ViewHolder view = (ListAdapter.ViewHolder) v.getTag();
            if(adapter.getItem(i).getSelect()) {
                count++;
                File fileDelete = new File(android.os.Environment.
                        getExternalStorageDirectory().getAbsolutePath() + "/tsensor/"
                        + adapter.getItem(i).getItem());
                if (fileDelete.exists()) {
                    fileDelete.delete();
                }
                savedFiles.remove(adapter.getItem(i));
            }
        }
        selectAllItems(false);
        allSelState = true;
        Toast.makeText(this, count + " items deleted", Toast.LENGTH_SHORT).show();
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }

    /* Handle the ItemLongClick in listview */
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        //showCheckBox(View.VISIBLE);
        showCheckBox(true);
        View v = getViewByPosition(i, listView);
        ListAdapter.ViewHolder tag = (ListAdapter.ViewHolder) v.getTag();
        adapter.getItem(i).setSelect(true);
        adapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(selectState) {
            undo();
        }
        else {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            MainActivity.this.finish();
                            System.exit(0);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure to finish the program?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }
    }

    protected void undo() {
        //showCheckBox(View.GONE);
        showCheckBox(false);
        selectAllItems(false);
    }

    /* Show the Input dialog for saving */
    protected void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input file name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        Date date = Calendar.getInstance().getTime();
        input.setText(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fileName = input.getText().toString();
                filePath = Long.toString(System.currentTimeMillis());
                try {
                    /* Notify the listview */
                    savedFiles.add(new ListItem(fileName + utils.TEMPPATH + filePath));
                    adapter.sort(new Comparator<ListItem>() {
                        @Override
                        public int compare(ListItem listItem, ListItem t1) {
                            return listItem.getTime() < t1.getTime() ? 1 : -1;
                        }
                    });
                    selectAllItems(false);

                    new FileSaveProgress(MainActivity.this).execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    /* Yes/No Alert dialog when choose the remove option */
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    try {
                        removeItems();
                    //    showCheckBox(View.GONE);
                        showCheckBox(false);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    @Override
    public void onCheckBoxClickListener(int position, String value) {
        try {
            adapter.getItem(position).setSelect(
                    !adapter.getItem(position).getSelect());
            adapter.notifyDataSetChanged();
            View v = getViewByPosition(position, listView);
            //View v = listView.getChildAt(position);
            ListAdapter.ViewHolder view = (ListAdapter.ViewHolder) v.getTag();
            if (isSeletedAll(view.checkBox.isChecked()) == 1) {
                allSelState = false;
            } else {
                allSelState = true;
            }
        } catch (Exception e) {
            Toast.makeText(this, position + ", " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected int isSeletedAll(boolean state) {
        int st = state ? 1 : 2;
        for (int i = 0; i < adapter.getCount(); i++) {
            View v = getViewByPosition(i, listView);
            //View v = listView.getChildAt(i);
            ListAdapter.ViewHolder view = (ListAdapter.ViewHolder) v.getTag();
            if(state != view.checkBox.isChecked()) {
                st = 0;
                break;
            }
        }
        return st;
    }

    public class FileSaveProgress extends AsyncTask<Void, Void, Void> {

        private ProgressDialog processingDialog;
        Context context = null;

        FileSaveProgress(Context context)
        {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            processingDialog = ProgressDialog.show( this.context, "Saving Files", "Please wait ...", true, false);

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            //    createPDF();
            try {
                writeToSDFile();
            } catch (Exception e) {
                //    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            processingDialog.dismiss();
            Toast.makeText(this.context, "File saved: /tsensor/" + MainActivity.fileName, Toast.LENGTH_LONG).show();
        }

        /* Save the sensor value into file */
        private void writeToSDFile(){
            /* get the root directory*/
            File root = android.os.Environment.getExternalStorageDirectory();

            /* create the "tsensor" folder */
            File dir = new File (root.getAbsolutePath() + "/tsensor");
            dir.mkdirs();

            /* get the current time and set it as fileName */
            Date curTime = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00")).getTime();
            filePath = Long.toString(System.currentTimeMillis());

            /* Create the file */
            File file = new File(dir, fileName + utils.TEMPPATH + filePath);

            /* Write sensor values to file*/
            try {
                FileOutputStream f = new FileOutputStream(file);
                String str = "";
                for(int i = 0; i < values.size(); i++) {
                    for(int j = 0; j < 9; j++) {
                        str += values.get(i).get(j);
                        if(j < 8)   str += ", ";
                        else        str += "\n";
                    }
                }
                f.write(str.getBytes());
                f.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            /* Clear the current sensor values */
            for (int i = 0; i < values.size(); i++)
                values.get(i).clear();
            values.clear();
        }
    }
}