package io.github.yangxlei;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.github.yangxiaolei.sub.TestAction;
import io.github.yangxlei.metis.loader.MetisLoader;

/**
 * Created by yanglei on 2017/7/22.
 */

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);


        for (Class<TestAction> clazz : MetisLoader.load(TestAction.class)) {
            System.out.println("@@@ " + clazz);
        }

    }
}
