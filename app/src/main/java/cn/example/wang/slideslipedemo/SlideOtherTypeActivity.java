package cn.example.wang.slideslipedemo;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.example.wang.slideslipedemo.slideswaphelper.PlusItemSlideCallback;
import cn.example.wang.slideslipedemo.slideswaphelper.WeItemTouchHelper;

public class SlideOtherTypeActivity extends AppCompatActivity implements RecAdapter.DeletedItemListener{
    RecyclerView recyclerView;
    private RecOtherTypeAdapter recAdapter;

    public static void start(Context context){
        Intent intent = new Intent(context,SlideOtherTypeActivity.class);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_other_type);
        initView();
        initData();
    }

    private void initData() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            list.add("Item  " +i);
        }
        recAdapter.setList(list,true);
    }

    private void initView() {
        recyclerView = findViewById(R.id.recyclerViewTwo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recAdapter = new RecOtherTypeAdapter(this);
        recAdapter.setDeletedItemListener(this);
        recyclerView.setAdapter(recAdapter);

        PlusItemSlideCallback callback = new PlusItemSlideCallback();
        WeItemTouchHelper extension = new WeItemTouchHelper(callback);
        extension.attachToRecyclerView(recyclerView);
    }

    @Override
    public void deleted(int position) {
        recAdapter.removeDataByPosition(position);
    }
}
