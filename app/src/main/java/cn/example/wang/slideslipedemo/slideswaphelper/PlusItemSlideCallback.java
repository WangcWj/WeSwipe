package cn.example.wang.slideslipedemo.slideswaphelper;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;

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

public class PlusItemSlideCallback extends WeItemTouchHelper.Callback {

    private int type;

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, WeItemTouchHelper.START|WeItemTouchHelper.ACTION_STATE_SWIPE);
    }

    @Override
    public int getItemSlideType() {
        return type;
    }

    /**
     * 拖动的时候才会触发。
     * @param recyclerView The RecyclerView to which WeItemTouchHelper is attached to.
     * @param viewHolder   The ViewHolder which is being dragged by the user.
     * @param target       The ViewHolder over which the currently active item is being
     *                     dragged.
     * @return
     */
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    /**
     * 当ItemView被滑动的时候调用。
     * @param viewHolder The ViewHolder which has been swiped by the user.
     * @param direction  The direction to which the ViewHolder is swiped. It is one of
     *                   {@link #UP}, {@link #DOWN},
     *                   {@link #LEFT} or {@link #RIGHT}. If your
     *                   {@link #getMovementFlags(RecyclerView, RecyclerView.ViewHolder)}
     *                   method
     *                   returned relative flags instead of {@link #LEFT} / {@link #RIGHT};
     *                   `direction` will be relative as well. ({@link #START} or {@link
     */
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

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
    }
}
