package cn.example.wang.slideslipedemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.example.wang.slideslipedemo.slideswaphelper.SlideSwapAction;

/**
 * Created by WANG on 18/4/24.
 */

public class RecOtherTypeAdapter extends RecyclerView.Adapter<RecOtherTypeAdapter.RecViewholder> {


    private Context context;
    private List<String> data = new ArrayList<>();
    private LayoutInflater layoutInflater;
    private RecAdapter.DeletedItemListener deletedItemListener;

    public void setDeletedItemListener(RecAdapter.DeletedItemListener deletedItemListener) {
        this.deletedItemListener = deletedItemListener;
    }

    public RecOtherTypeAdapter(Context context) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    public void setList(List<String> list, boolean refresh) {
        if (refresh) {
            data.clear();
        }
        data.addAll(list);
        notifyDataSetChanged();
    }

    public void removeDataByPosition(int position) {
        if (position >= 0 && position < data.size()) {
            data.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, data.size() - 1);
        }
    }

    @Override
    public RecViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.layout_item, parent, false);
        return new RecViewholder(view);
    }

    @Override
    public void onBindViewHolder(final RecViewholder holder, final int position) {
        String s = data.get(holder.getAdapterPosition());
        holder.textView.setText(s);
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "s  " + holder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
            }
        });
        holder.slide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != deletedItemListener) {
                    deletedItemListener.deleted(holder.getAdapterPosition());
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
    public class RecViewholder extends RecyclerView.ViewHolder implements SlideSwapAction {
        public TextView textView;
        public TextView slide;

        public RecViewholder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_text);
            slide = itemView.findViewById(R.id.item_slide);

        }

        @Override
        public float getActionWidth() {
            return dip2px(slide.getContext(), 100);
        }

        @Override
        public View ItemView() {
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
}
