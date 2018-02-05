package com.orbbec.threadpooltest;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button bt;
    private Button hf;
    private Button start;
    private Button btThread;
    private ThreadPoolUtils poolUtils;
    private Button tiao;
    private static String TAG = "PoolUtils";
    private Thread uiThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt = findViewById(R.id.bt);
        hf = findViewById(R.id.hf);
        btThread = findViewById(R.id.thread);
        start = findViewById(R.id.start);
        tiao = findViewById(R.id.tiao);
        tiao.setOnClickListener(this);
        start.setOnClickListener(this);
        bt.setOnClickListener(this);
        btThread.setOnClickListener(this);
        hf.setOnClickListener(this);
        ThreadPoolUtils.Builder builder = new ThreadPoolUtils.Builder()
                .corePoolSize(Runtime.getRuntime().availableProcessors()+1)
                .maximumPoolSize(Runtime.getRuntime().availableProcessors()*2+1)
                .keepAliveTime(60)
                .unit(TimeUnit.SECONDS)
                .workQueue(new LinkedBlockingQueue<Runnable>())
                .build();
        poolUtils = ThreadPoolUtils.getInstance();

    }
    class DownloadTask implements Runnable{
        private int num;
        public DownloadTask(int num) {
            super();
            this.num = num;
        }
        @Override
        public void run() {
            Log.d(TAG,"task1 - " + num + " 开始执行了...开始执行了...");
            for (int i = 0; i < 5; i++) {
                SystemClock.sleep(1000);
                Log.d(TAG,"stop1?         " + i +"    "+ num + "        " + Thread.currentThread().isInterrupted());
            }
            Log.d(TAG,"task1 - " + num + "   结束了...");
        }
    }

    class DownloadTask2 implements Runnable{
        private int num;
        public DownloadTask2(int num) {
            this.num = num;
        }
        @Override
        public void run() {
            Log.d(TAG,"task2 - " + num + " 开始执行了...开始执行了...");
            for (int i = 0; i < 5; i++) {
                SystemClock.sleep(1000);
                Log.d(TAG,"stop2?         " + num + "        " + Thread.currentThread().isInterrupted());
            }
            Log.d(TAG,"task2 - " + num + "   结束了...");
        }
    }

    class DownloadTask3 implements Runnable{
        private int num;
        public DownloadTask3(int num) {
            super();
            this.num = num;
        }
        @Override
        public void run() {
            Log.d(TAG,"task3 - " + num + " 开始执行了...开始执行了...");
            for (int i = 0; i < 5; i++) {
                SystemClock.sleep(1000);
                Log.d(TAG,"stop3?         " + num + "        " + Thread.currentThread().isInterrupted());
            }
            Log.d(TAG,"task3 - " + num + "   结束了...");
            poolUtils.wakeup(uiThread);
        }
    }

    @Override
    public void onClick(View v) {
        DownloadTask d1 = new DownloadTask(1);
        DownloadTask2 d2 = new DownloadTask2(2);
        DownloadTask3 d3 = new DownloadTask3(3);
        switch (v.getId()){
            case R.id.bt:
                poolUtils.serial(d1);
                poolUtils.serial(d2);
                break;
            case R.id.hf:
                poolUtils.pauseThreadPool();
                break;
            case R.id.start:
                poolUtils.resumeThreadPool();
                break;
            case R.id.tiao:
                //UI线程
                uiThread = Thread.currentThread();
                poolUtils.execute(d3);
                Log.d(TAG, "UIThread is running");
                for (int i = 0; i < 10; i++) {
                    Log.d(TAG, "onClick: UIThread is running  time : " + i);
                    SystemClock.sleep(1000);
                    if(i == 2){
                        poolUtils.join(d3);
                        Log.d(TAG, "UIThread is continue");
                    }
                }
                break;
            case R.id.thread:
                poolUtils.execute(d1);
                break;
            default:
                break;
        }
    }
}
