package cn.example.wang.slideslipedemo;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import cn.example.wang.slideslipedemo.slideswaphelper.PlusItemSlideCallback;
import cn.example.wang.slideslipedemo.slideswaphelper.WItemTouchHelperPlus;

/**
 * @author WANG
 */
public class SmartRefreshLayoutActivity extends AppCompatActivity implements RecAdapter.DeletedItemListener {
    SmartRefreshLayout smartRefreshLayout;
    RecyclerView recyclerView;
    private RecOtherTypeAdapter recAdapter;
    List<String> list;

    public static void start(Context context){
        Intent intent = new Intent(context,SmartRefreshLayoutActivity.class);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ptr);
        recyclerView = findViewById(R.id.smart_recyclerview);
        smartRefreshLayout = findViewById(R.id.refreshlayout);
        smartRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                initData(false);
                smartRefreshLayout.finishRefresh(2000);
            }
        });
        initView();
        initData(true);
    }

    private void initData(boolean refresh) {
        if(list == null) {
            list = new ArrayList<>();
        }
        for (int i = 0; i < 4; i++) {
            list.add("Item  " +i);
        }
        recAdapter.setList(list,refresh);
    }

    private void initView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recAdapter = new RecOtherTypeAdapter(this);
        recAdapter.setDeletedItemListener(this);
        recyclerView.setAdapter(recAdapter);

        PlusItemSlideCallback callback = new PlusItemSlideCallback();
        WItemTouchHelperPlus extension = new WItemTouchHelperPlus(callback);
        extension.attachToRecyclerView(recyclerView);
    }

    @Override
    public void deleted(int position) {
        recAdapter.removeDataByPosition(position);
    }
}
