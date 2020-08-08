package cn.we.swipe.helper;

import android.support.v7.widget.RecyclerView;

/**
 * Created to :
 *
 * @author WANG
 * @date 2020/8/8
 */
public abstract class WeSwipeProxyAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private long recoverDuration = 100;

    public void setRecoverDuration(long recoverDuration) {
        this.recoverDuration = recoverDuration;
    }

    private WeSwipe mWeSwipe;

    public void setWeSwipe(WeSwipe mWeSwipe) {
        this.mWeSwipe = mWeSwipe;
    }

    private void checkItem(RecoverCallback callback) {
        if (null != mWeSwipe) {
            if (mWeSwipe.haveRecoverItem()) {
                mWeSwipe.recoverAll(callback,recoverDuration);
            }else {
                callback.recoverEnd();
            }
        }else {
            callback.recoverEnd();
        }
    }

    public void proxyNotifyDataSetChanged() {
       checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyDataSetChanged();
            }
        });
    }

    public void proxyNotifyItemChanged(final int position) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeChanged(position, 1);
            }
        });
    }

    public void proxyNotifyItemChanged(final int position,final Object payload) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeChanged(position, 1,payload);
            }
        });
    }

    public void proxyNotifyItemRangeChanged(final int positionStart,final int itemCount) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeChanged(positionStart, itemCount);
            }
        });
    }

    public void proxyNotifyItemRangeChanged(final int positionStart, final int itemCount, final Object payload) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeChanged(positionStart, itemCount, payload);
            }
        });
    }

    public void proxyNotifyItemInserted(final int position) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeInserted(position, 1);
            }
        });
    }

    public void proxyNotifyItemMoved(final int fromPosition, final int toPosition) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemMoved(fromPosition, toPosition);
            }
        });
    }

    public void proxyNotifyItemRangeInserted(final int positionStart, final int itemCount) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeInserted(positionStart, itemCount);
            }
        });
    }

    public void proxyNotifyItemRemoved(final int position) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeRemoved(position, 1);
            }
        });
    }

    public void proxyNotifyItemRangeRemoved(final int positionStart, final int itemCount) {
        checkItem(new RecoverCallback() {
            @Override
            public void recoverEnd() {
                notifyItemRangeRemoved(positionStart, itemCount);
            }
        });
    }


}
