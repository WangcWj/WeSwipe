package cn.example.wang.slideslipedemo.slideswaphelper;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 *
 * @author WANG
 * @date 18/3/14
 *
 * 使用侧滑删除的话,删除item之后刷新列表的方法:
 * 1.采用notifyItemRemoved(position)+ notifyItemRangeChanged(position,data.size()-1)方法.
 * 2.采用notifyItemRemoved(position),position采用holder.getAdapterPosition().
 * 使用notifyDataSetChange()方法的时候会导致侧滑的布局出现复用的问题.当使用notifyItemRemoved(position)刷新时
 * 要注意RecyclerView.Adapter里面我们使用的position不要是onBindViewHolder()方法里面的参数,使用改参数的时候会导致item错乱.
 *
 */

public class PlusItemSlideCallback extends WItemTouchHelperPlus.Callback {

    private String type;

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }


    @Override
    int getSlideViewWidth() {
        return 0;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.START);
    }

    @Override
    public String getItemSlideType() {
        return type;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (viewHolder instanceof SlideSwapAction) {
            SlideSwapAction holder = (SlideSwapAction) viewHolder;
            float actionWidth = holder.getActionWidth();
            if (dX < -actionWidth) {
                dX = -actionWidth;
            }
            holder.ItemView().setTranslationX(dX);
        }
        return;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
    }
}
