package cn.example.wang.slideslipedemo.slideswaphelper;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import cn.example.wang.slideslipedemo.RecAdapter;
import cn.example.wang.slideslipedemo.RecOtherTypeAdapter;


/**
 * Created by WANG on 18/3/14.
 */

public class PlusItemSlideCallback extends WItemTouchHelperPlus.Callback {
    String type;

    public PlusItemSlideCallback(String type) {
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
        if (viewHolder instanceof RecAdapter.RecViewholder) {
            RecAdapter.RecViewholder holder = (RecAdapter.RecViewholder) viewHolder;
            float actionWidth = holder.getActionWidth();
            if (dX < -actionWidth) {
                dX = -actionWidth;
            }
            holder.slideItem.setTranslationX(dX);
        }else if(viewHolder instanceof RecOtherTypeAdapter.RecViewholder){
            RecOtherTypeAdapter.RecViewholder holder = (RecOtherTypeAdapter.RecViewholder) viewHolder;
            float actionWidth = holder.getActionWidth();
            if (dX < -actionWidth) {
                dX = -actionWidth;
            }
            holder.textView.setTranslationX(dX);
        }
        return;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
    }
}
