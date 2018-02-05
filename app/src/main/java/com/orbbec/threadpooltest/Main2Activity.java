package com.orbbec.threadpooltest;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * @author tanzhuohui
 */
public class Main2Activity extends AppCompatActivity implements View.OnClickListener {
    private Button test3;
    private ThreadPoolUtils instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        instance = ThreadPoolUtils.getInstance();
        test3 = findViewById(R.id.test3);
        test3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.test3:
                instance.addTask(new DownloadTask(3) , "test3");
                break;
            default:
                break;
        }
    }

    class DownloadTask implements Runnable{
        private int num;
        public DownloadTask(int num) {
            super();
            this.num = num;
        }
        @Override
        public void run() {
            Log.d("tzh","task - " + num + " 开始执行了...开始执行了...");
            for (int i = 0; i < 5; i++) {
                SystemClock.sleep(1000);
                Log.d("tzh","stop?         " + num + "        " + Thread.currentThread().isInterrupted());
            }
            Log.d("tzh","task - " + num + "   结束了...");
        }
    }
}
