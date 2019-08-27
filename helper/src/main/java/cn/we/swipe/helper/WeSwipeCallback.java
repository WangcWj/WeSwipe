package cn.we.swipe.helper;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * @author WANG
 * @date 18/3/14
 * <p>
 * 使用侧滑删除的话,删除item之后刷新列表的方法:
 * 1.采用notifyItemRemoved(position)+ notifyItemRangeChanged(position,data.size()-1)方法.
 * 2.采用notifyItemRemoved(position),position采用holder.getAdapterPosition().
 * 使用notifyDataSetChange()方法的时候会导致侧滑的布局出现复用的问题.当使用notifyItemRemoved(position)刷新时
 * 要注意RecyclerView.Adapter里面我们使用的position不要是onBindViewHolder()方法里面的参数,使用改参数的时候会导致item错乱.
 */

public class WeSwipeCallback extends WeSwipeHelper.Callback {

    private int mRecoverAnimationDuration = 250;
    private boolean mSwipeEnable = true;

    public void setRecoverAnimationDuration(int mRecoverAnimationDuration) {
        this.mRecoverAnimationDuration = mRecoverAnimationDuration;
    }

    public void setSwipeEnable(boolean mSwipeEnable) {
        this.mSwipeEnable = mSwipeEnable;
    }

    /**
     * 该RecyclerView是否需要侧滑菜单。
     * @return
     */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return mSwipeEnable;
    }

    /**
     * 一般不需要修改该方法，除非有其他的需求。
     * @param recyclerView The RecyclerView to which WeSwipeHelper is attached.
     * @param viewHolder   The ViewHolder for which the movement information is necessary.
     * @return
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, WeSwipeHelper.START | WeSwipeHelper.ACTION_STATE_SWIPE);
    }

    /**
     * 拖动的时候才会触发，目前不支持拖动。不让使用者重写该方法。
     *
     * @param recyclerView The RecyclerView to which WeSwipeHelper is attached to.
     * @param viewHolder   The ViewHolder which is being dragged by the user.
     * @param target       The ViewHolder over which the currently active item is being
     *                     dragged.
     * @return
     */
    @Override
    public final boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    /**
     * 当ItemView被滑动的时候调用。
     *
     * @param viewHolder The ViewHolder which has been swiped by the user.
     */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

    }

    /**
     * 侧滑恢复时动画执行的时间。
     * @return
     */
    @Override
    public long getRecoveryAnimationDuration() {
        return mRecoverAnimationDuration;
    }

    /**
     * 这里进行平移动画。
     * @param swipeView         需要侧滑的View。
     * @param dX                The amount of horizontal displacement caused by user's action
     * @param dY                The amount of vertical displacement caused by user's action
     * @param swipeWidth        需要侧滑的距离。
     */
    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, View swipeView, float dX, float dY, int actionState, boolean isCurrentlyActive, float swipeWidth) {
        if(null == swipeView){
            return;
        }
        if (dX < -swipeWidth) {
            dX = -swipeWidth;
        }
        swipeView.setTranslationX(dX);
    }

    /**
     * 侧滑菜单恢复之后会调用，根据实际的情况重写。
     * @param recyclerView The RecyclerView which is controlled by the WeSwipeHelper.
     * @param viewHolder   The View that was interacted by the user.
     */
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
    }

    /**
     * 重新选择了另外一个ItemView。
     * @param viewHolder  The new ViewHolder that is being swiped or dragged. Might be null if
     *                    it is cleared.
     * @param actionState One of {@link WeSwipeHelper#ACTION_STATE_IDLE},
     *                    {@link WeSwipeHelper#ACTION_STATE_SWIPE} or
     *                    {@link WeSwipeHelper#ACTION_STATE_DRAG}.
     */
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
    }
}
