package com.example.newsreaderudemy;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> newsFeed=new ArrayList<>();
    ArrayList<String> content=new ArrayList<>();
    SQLiteDatabase articleDB;
    ListView listView;
    ArrayAdapter<String> arrayAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        articleDB=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,content VARCHAR)");
        listView=(ListView)findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,newsFeed);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(MainActivity.this,WebActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });
        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
       updateListView();
    }

    public void updateListView() {
        Cursor c = articleDB.rawQuery("SELECT * FROM articles", null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if (c.moveToFirst()) {
            newsFeed.clear();
            content.clear();
            do {
                newsFeed.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }
    public  class DownloadTask extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems = 20;
                if (jsonArray.length() < 20) {
                    numberOfItems = jsonArray.length();
                }
                articleDB.execSQL("DELETE FROM articles");
                for (int i = 0; i < numberOfItems; i++) {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();
                    String articleInfo = "";
                    while (data != -1) {
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }
                    Log.i("articleunfo", articleInfo);
                    JSONObject jsonObject=new JSONObject(articleInfo);
                   if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        reader = new InputStreamReader(in);
                        String articleContent = "";
                        data = reader.read();
                        while (data != -1) {
                            char current = (char) data;
                            articleContent += current;
                            data = reader.read();
                        }
                        Log.i("HTML", articleContent);
                        Log.i("Title", articleTitle);
                        String sql = "INSERT INTO articles (articleId,title,content) VALUES (?, ?, ?)";
                        SQLiteStatement sqLiteStatement = articleDB.compileStatement(sql);
                        sqLiteStatement.bindString(1, articleId);
                        sqLiteStatement.bindString(2, articleTitle);
                        sqLiteStatement.bindString(3, articleContent);

                        sqLiteStatement.execute();
                    }
                    Log.i("sno",Integer.toString(i+1));
                }
                Log.i("URl content",result);
                return result;
            }catch (Exception e) {
                System.out.println("errrorr");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }


}
