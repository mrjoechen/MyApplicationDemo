package com.chenqiao.okhttp_demo;

import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;



/*

发送一个get请求的步骤:
1.首先构造一个Request对象，参数最起码有个url，当然你可以通过Request.Builder设置更多的参数比如：header、method等。

2.然后通过request的对象去构造得到一个Call对象，类似于将你的请求封装成了任务，既然是任务，就会有execute()和cancel()等方法。

3.最后，我们希望以异步的方式去执行请求，所以我们调用的是call.enqueue，将call加入调度队列，然后等待任务执行完成，我们在Callback中即可得到结果。
 */


public class MainActivity extends AppCompatActivity {

    private Button bt_loadimg;
    private ImageView img;
    private OkHttpClient okHttpClient;
    private TextView tv_api;


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            GitHubApi githubApi = (GitHubApi) msg.obj;
//            tv_api.setText(githubApi.toString());
            byte[] bytes = (byte[]) msg.obj;
            img.setImageBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt_loadimg = (Button) findViewById(R.id.bt_loadimg);

        img = (ImageView) findViewById(R.id.img);

        tv_api = (TextView) findViewById(R.id.tv_api);


        bt_loadimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //创建okHttpClient对象
                okHttpClient = new OkHttpClient();
                //创建一个Request
//                Request request = new Request.Builder().url("https://api.github.com/users/mrjoechen").build();
                Request request = new Request.Builder().url("http://jctech.cc/img/girl.jpg").build();

                Call call = okHttpClient.newCall(request);
                //请求加入调度，异步调度
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //拿到响应体，异步调用
//                        Gson gson = new Gson();
//                        GitHubApi gitHubApi = gson.fromJson(response.body().string(), GitHubApi.class);
//                        Log.i("gitHubApi",gitHubApi.toString());
//                        Message message = handler.obtainMessage();
//                        message.obj = gitHubApi;
//                        handler.sendMessage(message);


                        Message message = handler.obtainMessage();
                        byte[] bytes = response.body().bytes();
                        message.obj = bytes;
                        handler.sendMessage(message);

                    }
                });

            }
        });

    }



}
