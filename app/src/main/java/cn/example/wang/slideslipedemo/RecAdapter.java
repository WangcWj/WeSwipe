package cn.example.wang.slideslipedemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.we.swipe.helper.WeSwipeHelper;

/**
 * Created by WANG on 18/4/24.
 */

public class RecAdapter extends RecyclerView.Adapter<RecAdapter.RecViewholder> {


    private Context context;
    private List<String> data = new ArrayList<>();
    private LayoutInflater layoutInflater;
    DeletedItemListener delectedItemListener;

    public void setDelectedItemListener(DeletedItemListener deletedItemListener) {
        this.delectedItemListener = deletedItemListener;
    }

    public RecAdapter(Context context) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    public void setList(List<String> list) {
        data.clear();
        data.addAll(list);
        notifyItemMoved(0, data.size() - 1);
    }

    public List<String> getData() {
        return data;
    }

    public void removeDataByPosition(int position) {
        if (position >= 0 && position < data.size()) {
            data.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public RecViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.layout_item_twotype, parent, false);
        return new RecViewholder(view);
    }

    @Override
    public void onBindViewHolder(final RecViewholder holder, int position) {
        holder.textView.setText(data.get(holder.getAdapterPosition()));
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "s  " + holder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
            }
        });

        holder.zhiding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "置顶" + holder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
            }
        });
        holder.yidu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "已读" + holder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
            }
        });
        holder.shanchu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != delectedItemListener) {
                    delectedItemListener.deleted(holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    /**
     * view.getWidth()获取的是屏幕中可以看到的大小.
     */
    public class RecViewholder extends RecyclerView.ViewHolder implements WeSwipeHelper.SwipeLayoutTypeCallBack {
        public TextView textView;
        public LinearLayout slide;
        public TextView zhiding, yidu, shanchu;
        public RelativeLayout slideItem;

        public RecViewholder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_text);
            zhiding = itemView.findViewById(R.id.zhiding);
            yidu = itemView.findViewById(R.id.yidu);
            shanchu = itemView.findViewById(R.id.shanchu);
            slide = itemView.findViewById(R.id.slide);
            slideItem = itemView.findViewById(R.id.slide_itemView);
        }

        @Override
        public float getSwipeWidth() {
            //布局隐藏超过父布局的范围的时候这里得不到宽度
            return dip2px(context, 240);
        }

        @Override
        public View needSwipeLayout() {
            return slideItem;
        }

        @Override
        public View onScreenView() {
            return textView;
        }
    }

    /**
     * 根据手机分辨率从DP转成PX
     *
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public interface DeletedItemListener {

        void deleted(int position);
    }

}
