package cn.we.swipe.helper;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Created to : 监听RecyclerView#Adapter的数据变化,变化的时候会自定回复布局。
 *
 * @author WANG
 * @date 2020/8/8
 */
public class WeSwipeAdapterDataObserver extends RecyclerView.AdapterDataObserver {

    private WeSwipe mWeSwipe;

    public WeSwipeAdapterDataObserver(WeSwipe weSwipe) {
        this.mWeSwipe = weSwipe;
    }

    /**
     * recAdapter.notifyDataSetChanged();
     */
    @Override
    public void onChanged() {
        recover();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        recover();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
        recover();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        recover();
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        recover();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        recover();
    }

    private void recover() {
        if (null != mWeSwipe) {
            mWeSwipe.recoverAll(null);
        }
    }
}
