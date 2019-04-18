package com.dmk.drm_player;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DownloadTask extends AsyncTask<String, Integer, String> {

        int count = 0;
        private Context context;
        private PowerManager.WakeLock mWakeLock;

    public String recordingStation="";
        public boolean isDownloading = false;

        public DownloadTask(Context context) {
            this.context = context;

        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v("Download-update","Download started");
            isDownloading = true;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;


            try {
                String token = sUrl[1];

             //   recordingStation = station_name;
                        URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization","Bearer "+token);
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                Log.v("Download-update", fileLength + " Bytes FILE FOUND Code :" + connection.getResponseCode() + " Response:" + connection.getResponseCode()
                        + " " + connection.getResponseMessage());


                // download the file
                input = connection.getInputStream();


                File root =   Environment.getExternalStorageDirectory();


                File dir = new File(root.getAbsolutePath() + "/exoplayer");
                if (dir.exists() == false) {
                    Log.v("Download-update","Directory doesn't exist trying to create it");

                    dir.mkdirs();


                }else
                    Log.v("Download-update","Directory EXISTS !");


                SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HHmm");
                String currentDateandTime = sdf.format(new Date());

                File file = new File(dir, "Manifest.mpd");
                file.createNewFile();

                output = new FileOutputStream(file.getAbsolutePath());


                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // dont allow canceling with back button
                    //                    if (isCancelled()) {
                    //                        input.close();
                    //                        return null;
                    //                    }

                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);

                    //stop writting file if asycn task has been cancelled!
                    if (isCancelled()) return  null;
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
          // isDownloading = true;

//            count++;
//            if (progressDialog != null)
//                if (count % 20 == 0)
//                    progressDialog.setProgress(values[0]);

            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
//            progressDialog.setMessage("Response:"+s);
            isDownloading =false;
            Log.v("Download-update","Download complete!"+s);

        }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        isDownloading =false;
    }
}