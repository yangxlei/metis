package io.github.yangxlei;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.github.yangxiaolei.sub.TestAction;
import io.github.yangxlei.metis.loader.MetisLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (Class<TestAction> clazz : MetisLoader.load(TestAction.class)) {
            System.out.println("@@@ " + clazz);
        }
    }
}
