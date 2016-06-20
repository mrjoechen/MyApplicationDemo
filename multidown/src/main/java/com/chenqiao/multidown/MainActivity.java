package com.chenqiao.multidown;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText et_fileurl;
    private ProgressBar progressBar;
    private Button btn_multidown;
    private int total;
    private File file;
    private URL url;
    private boolean downloading;
    private List<HashMap<String, Integer>> threadList;
    private Handler handler;
    private int length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_fileurl = (EditText) findViewById(R.id.et_fileurl);
        btn_multidown = (Button) findViewById(R.id.btn_multidown);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        threadList = new ArrayList<>();
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    progressBar.setProgress(msg.arg1);
                    if (msg.arg1 == length) {
                        Toast.makeText(MainActivity.this, "下载完成！", Toast.LENGTH_SHORT).show();
                        total = 0;
                        btn_multidown.setText("下载");
                        Log.d("multidown","下载完成");
                    }
                }
            }
        };

        btn_multidown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (downloading) {
                    downloading = false;
                    btn_multidown.setText("继续下载");
                    return;
                }
                downloading = true;
                btn_multidown.setText("暂停");

                if (threadList.size() == 0){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                url = new URL(et_fileurl.getText().toString());
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");
                                connection.setConnectTimeout(5000);
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
                                length = connection.getContentLength();
                                Log.i("multidown",length+"");
                                progressBar.setMax(length);
                                progressBar.setProgress(0);

                                if (length < 0) {
                                    Toast.makeText(MainActivity.this, "File not found!", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                file = new File(Environment.getExternalStorageDirectory(), getFileName(et_fileurl.getText().toString()));
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                                randomAccessFile.setLength(length);

                                int blocksize = length / 3;
                                for (int i = 0; i < 3; i++) {
                                    int begin = i * blocksize;
                                    int end = (i + 1) * blocksize-1;
                                    if (i == 2) {
                                        end = length;
                                    }

                                    HashMap<String, Integer> map = new HashMap<>();
                                    map.put("begin",begin);
                                    map.put("end",end);
                                    map.put("finished",0);
                                    threadList.add(map);
                                    //创建新的线程，下载文件
                                    new Thread(new DownloadRunnable(url, begin, end, file, i)).start();
                                }


                            } catch (MalformedURLException e) {
                                Toast.makeText(MainActivity.this, "URL Exception", Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }else {
                    //恢复下载
                    for (int i =0;i<threadList.size();i++){
                        HashMap<String, Integer> hashMap = threadList.get(i);
                        int  begin = hashMap.get("begin");
                        int end = hashMap.get("end");
                        int finished = hashMap.get("finished");
                        new Thread(new DownloadRunnable(url,begin+finished,end,file,i)).start();
                    }

                }
            }
        });
    }

    private String getFileName(String url) {

        int i = url.lastIndexOf("/") + 1;
        return url.substring(i);

    }

    class DownloadRunnable implements Runnable {


        private URL url;
        private int begin;
        private int end;
        private File file;
        private int id;

        public DownloadRunnable(URL url, int begin, int end, File file, int id) {
            this.url = url;
            this.begin = begin;
            this.end = end;
            this.file = file;
            this.id = id;
        }

        @Override
        public void run() {

            if (begin > end) {
                return;
            }
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
                conn.setRequestProperty("Range", "bytes=" + begin + "-" + end);
                InputStream inputStream = conn.getInputStream();
                byte[] bytes = new byte[1024 * 1024];
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                randomAccessFile.seek(begin);
                int length = 0;
                while ((length = inputStream.read(bytes)) != -1 && downloading) {
                    randomAccessFile.write(bytes, 0, length);
                    updateProgress(length);
                    HashMap<String, Integer> hashMap = threadList.get(id);
                    hashMap.put("finished",hashMap.get("finished")+length);
                    Log.d("multidown", "Download size:" + total);
                }
                inputStream.close();
                randomAccessFile.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    synchronized private void updateProgress(int progress) {
        total += progress;
        handler.obtainMessage(0, total, 0).sendToTarget();
    }
}
