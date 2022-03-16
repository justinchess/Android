package com.example.tsensors;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

/* Show the progress while saving files */
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
        try {
            processingDialog.dismiss();
        //    Toast.makeText(this.context, "File saved: /tsensor/" + MainActivity.fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            //    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        String filePath = Long.toString(System.currentTimeMillis());

        /* Create the file */
        File file = new File(dir, MainActivity.fileName + utils.TEMPPATH + filePath);

        /* Write sensor values to file*/
        try {
            FileOutputStream f = new FileOutputStream(file);
            String str = "";
            for(int i = 0; i < MainActivity.values.size(); i++) {
                for(int j = 0; j < 9; j++) {
                    str += MainActivity.values.get(i).get(j);
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

        /* Notify the listview */
        MainActivity.savedFiles.add(new ListItem(MainActivity.fileName + utils.TEMPPATH + filePath));
        MainActivity.adapter.sort(new Comparator<ListItem>() {
            @Override
            public int compare(ListItem listItem, ListItem t1) {
                return listItem.getTime() < t1.getTime() ? 1 : -1;
            }
        });
        MainActivity.selectAllItems(false);
        /* Clear the current sensor values */
        for (int i = 0; i < MainActivity.values.size(); i++)
            MainActivity.values.get(i).clear();
        MainActivity.values.clear();
    }
}