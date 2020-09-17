package com.example.top10downloaded;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private String feedUrl ="http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit =10;
    private String SAVING_URL = "SAVING URL";
    private String SAVING_LIMIT = "SAVING LIMIT";
    private String feedCachedUrl = "INVALID";

    /**
     * Creating a ListView field for the listApp
     */
    private ListView listApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * Setting up the reference to the xmlListView
         */
        listApps = (ListView) findViewById(R.id.xmlListView);

        /**
         * We need to restore our data ain tha onCreate method rather then OnRestoreInstanceState coz
         * the downloading is taking place in the onCreate method and OnRestoreInstanceState is after the
         * onCreate method hence there will not be any reason to restore the state in the OnRestoreInstanceState
         * method
         */
        if( savedInstanceState != null){
            feedLimit= savedInstanceState.getInt(SAVING_LIMIT);
            feedUrl = savedInstanceState.getString(SAVING_URL);
        }
        downloadUrl(String.format(feedUrl, feedLimit));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(SAVING_URL, feedUrl);
        outState.putInt(SAVING_LIMIT,feedLimit);
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu, menu);
        if(feedLimit == 10) {
            menu.findItem(R.id.mnu10).setChecked(true);
        } else {
            menu.findItem(R.id.mnu25).setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.mnuFree:
                feedUrl="http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.mnuPaid:
                feedUrl="http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.mnuSongs:
                feedUrl="http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.mnu10:
            case R.id.mnu25:
                if(!item.isChecked()) {
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + " setting feedLimit to " + feedLimit);
                } else {
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + " feedLimit unchanged");
                }
                break;
            case R.id.mnuRefresh:
                feedCachedUrl="INVALID";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
       downloadUrl(String.format(feedUrl, feedLimit));
        return true;
    }

    private void downloadUrl(String url){
        if (!url.equalsIgnoreCase(feedCachedUrl)){
            Log.d(TAG, "downloadUrl: starting AsyncTask");
            DownloadData downloadData = new DownloadData();
            downloadData.execute(url);
            feedCachedUrl=url;
            Log.d(TAG, "downloadUrl: done");
        }else{
            Log.d(TAG, "downloadUrl: URL unchanged  hence not downloaded");
        }

    }

    /**
     RSS: its a way to present Web data in a standard format so that users can subscribe to RSS feed
     and receive updates automatically

     Creating a subclass of Async Class to do the work of background Download,
     so that the UI of the application do not crash and to notify when the
     background process is finished

     An asynchronous task is defined by 3 generic types, called Params, Progress and Result,
     and 4 steps, called onPreExecute, doInBackground, onProgressUpdate and onPostExecute.
     */

    private class DownloadData extends AsyncTask<String, Void, String> {
        private static final String TAG = "DownloadData";

        /**
         * this class will take the URL as the input and give us the XML of rss feed as the output
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
//            Log.d(TAG, "onPostExecute: parameter is " + s);
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);
/**
 * ArrayAdapter is a very basic and can only put data in a single text view Widget
 * Takes 3 parameters for the constructor (Context, resource File, list of items (to get the data from) )
 * Context: --NA--
 * resource file: resource containing the text view that it will use to put the data into and
 * letting the array adapter know that that's what we are going to use to put data into
 * list of item(Data source): from which the arrayAdapter will take the data and put in the Resource
 */
//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<>(
//                    MainActivity.this, R.layout.list_item, parseApplications.getApplications());
//            listApps.setAdapter(arrayAdapter);
            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record, parseApplications.getApplications());
            listApps.setAdapter(feedAdapter);
        }

        /**
         * calling the downloadXML method to return the String which will be stored in the
         * rssFeed variable of Data type String........
         * if rssFeed is null i.e. an error in the URl so it will log an error entry in the logcat
         */
        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: starts with " + strings[0]);
            String rssFeed = downloadXML(strings[0]);
            if(rssFeed == null) {
                Log.e(TAG, "doInBackground: Error downloading");
            }
            return rssFeed;
        }

        private String downloadXML(String urlPath) {

            /**
             * Reason we are using StringBuilder over String is that it is more efficient in
             * concatenation of two string objects...........
             * converting the urlPath to actual URl
             * Opening a http connection as a input stream and also catching the response code
             * opening a bufferedReader by passing inputStreamReader with the help of connection.getInputStream
             */
            StringBuilder xmlResult = new StringBuilder();

            try {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "downloadXML: The response code was " + response);
//                InputStream inputStream = connection.getInputStream();
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                BufferedReader reader = new BufferedReader(inputStreamReader);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                int charsRead;
                char[] inputBuffer = new char[500];
                while(true) {
                    charsRead = reader.read(inputBuffer);
                    if(charsRead < 0) {
                        break;
                    }
                    if(charsRead > 0) {
                        xmlResult.append(String.copyValueOf(inputBuffer, 0, charsRead));
                    }
                }
                reader.close();

                return xmlResult.toString();
            } catch(MalformedURLException e) {
                Log.e(TAG, "downloadXML: Invalid URL " + e.getMessage());
            } catch(IOException e) {
                Log.e(TAG, "downloadXML: IO Exception reading data: " + e.getMessage());
            } catch(SecurityException e) {
                Log.e(TAG, "downloadXML: Security Exception.  Needs permission ? " + e.getMessage());
//                e.printStackTrace();
            }
            return null;
        }
    }
}