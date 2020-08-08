package cn.we.swipe.helper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.*;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.事件的分发
 * 2.取消的时候自动滑动.
 *
 * @author WANG
 */
public class WeSwipeHelper extends RecyclerView.ItemDecoration
        implements RecyclerView.OnChildAttachStateChangeListener {
    /**
     * Sliding type.
     */
    public static final int SWIPE_ITEM_TYPE = 0x3;

    public static final int SWIPE_ITEM_TYPE_DEFAULT = 0x1;

    public static final int SWIPE_ITEM_TYPE_FLOWING = SWIPE_ITEM_TYPE_DEFAULT << 1;

    /**
     * Up direction, used for swipe & drag control.
     */
    public static final int UP = 1;

    /**
     * Down direction, used for swipe & drag control.
     */
    public static final int DOWN = 1 << 1;

    /**
     * Left direction, used for swipe & drag control.
     */
    public static final int LEFT = 1 << 2;

    /**
     * Right direction, used for swipe & drag control.
     */
    public static final int RIGHT = 1 << 3;

    // If you change these relative direction values, update Callback#convertToAbsoluteDirection,
    // Callback#convertToRelativeDirection.
    /**
     * Horizontal start direction. Resolved to LEFT or RIGHT depending on RecyclerView's layout
     * direction. Used for swipe & drag control.
     */
    public static final int START = LEFT << 2;

    /**
     * Horizontal end direction. Resolved to LEFT or RIGHT depending on RecyclerView's layout
     * direction. Used for swipe & drag control.
     */
    public static final int END = RIGHT << 2;

    /**
     * WeSwipeHelper is in idle state. At this state, either there is no related motion event by
     * the user or latest motion events have not yet triggered a swipe or drag.
     */
    public static final int ACTION_STATE_IDLE = 0;

    /**
     * A View is currently being swiped.
     */
    public static final int ACTION_STATE_SWIPE = 1;

    /**
     * A View is currently being dragged.
     */
    public static final int ACTION_STATE_DRAG = 2;

    /**
     * Animation type for views which are swiped successfully.
     */
    public static final int ANIMATION_TYPE_SWIPE_SUCCESS = 1 << 1;

    /**
     * Animation type for views which are not completely swiped thus will animate back to their
     * original position.
     */
    public static final int ANIMATION_TYPE_SWIPE_CANCEL = 1 << 2;

    /**
     * Animation type for views that were dragged and now will animate to their final position.
     */
    public static final int ANIMATION_TYPE_DRAG = 1 << 3;

    static final String TAG = "WeSwipeHelper";

    static final boolean DEBUG = false;

    static final int ACTIVE_POINTER_ID_NONE = -1;

    static final int DIRECTION_FLAG_COUNT = 8;

    private static final int ACTION_MODE_IDLE_MASK = (1 << DIRECTION_FLAG_COUNT) - 1;

    static final int ACTION_MODE_SWIPE_MASK = ACTION_MODE_IDLE_MASK << DIRECTION_FLAG_COUNT;

    static final int ACTION_MODE_DRAG_MASK = ACTION_MODE_SWIPE_MASK << DIRECTION_FLAG_COUNT;

    /**
     * The unit we are using to track velocity
     */
    private static final int PIXELS_PER_SECOND = 1000;

    private final int SWIPE_FLAG_MASK = 0x1;

    private int mSwipeType;

    /**
     * Views, whose state should be cleared after they are detached from RecyclerView.
     * This is necessary after swipe dismissing an item. We wait until animator finishes its job
     * to clean these views.
     */
    final List<View> mPendingCleanup = new ArrayList<View>();

    /**
     * Re-use array to calculate dx dy for a ViewHolder
     */
    private final float[] mTmpPosition = new float[2];

    /**
     * Currently selected view holder
     */
    RecyclerView.ViewHolder mSelected = null;

    /**
     * The reference coordinates for the action start. For drag & drop, this is the time long
     * press is completed vs for swipe, this is the initial touch point.
     */
    float mInitialTouchX;

    float mInitialTouchY;

    /**
     * Set when WeSwipeHelper is assigned to a RecyclerView.
     */
    float mSwipeEscapeVelocity;

    /**
     * Set when WeSwipeHelper is assigned to a RecyclerView.
     */
    float mMaxSwipeVelocity;

    /**
     * The diff between the last event and initial touch.
     */
    float mDx;

    float mDy;

    /**
     * The coordinates of the selected view at the time it is selected. We record these values
     * when action starts so that we can consistently position it even if LayoutManager moves the
     * View.
     */
    float mSelectedStartX;

    float mSelectedStartY;

    /**
     * The pointer we are tracking.
     */
    int mActivePointerId = ACTIVE_POINTER_ID_NONE;

    /**
     * Developer callback which controls the behavior of WeSwipeHelper.
     */
    WeSwipeHelper.Callback mCallback;

    /**
     * Current mode.
     */
    int mActionState = ACTION_STATE_IDLE;

    /**
     * The direction flags obtained from unmasking
     * {@link WeSwipeHelper.Callback#getAbsoluteMovementFlags(RecyclerView, RecyclerView.ViewHolder)} for the current
     * action state.
     */
    int mSelectedFlags;

    /**
     * When a View is dragged or swiped and needs to go back to where it was, we create a Recover
     * Animation and animate it to its location using this custom Animator, instead of using
     * framework Animators.
     * Using framework animators has the side effect of clashing with ItemAnimator, creating
     * jumpy UIs.
     */
    private List<RecoverAnimation> mRecoverAnimations = new ArrayList<>();

    private int mSlop;

    RecyclerView mRecyclerView;

    /**
     * When user drags a view to the edge, we start scrolling the LayoutManager as long as View
     * is partially out of bounds.
     */
    final Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSelected != null && scrollIfNecessary()) {
                //it might be lost during scrolling
                if (mSelected != null) {
                    moveIfNecessary(mSelected);
                }
                mRecyclerView.removeCallbacks(mScrollRunnable);
                ViewCompat.postOnAnimation(mRecyclerView, this);
            }
        }
    };

    /**
     * Used for detecting fling swipe.
     */
    VelocityTracker mVelocityTracker;

    /**
     * re-used list for selecting a swap target.
     */
    private List<RecyclerView.ViewHolder> mSwapTargets;

    /**
     * re used for for sorting swap targets.
     */
    private List<Integer> mDistances;

    /**
     * If drag & drop is supported, we use child drawing order to bring them to front.
     */
    private RecyclerView.ChildDrawingOrderCallback mChildDrawingOrderCallback = null;

    /**
     * This keeps a reference to the child dragged by the user. Even after user stops dragging,
     * until view reaches its final position (end of recover animation), we keep a reference so
     * that it can be drawn above other children.
     */
    View mOverdrawChild = null;

    /**
     * We cache the position of the overdraw child to avoid recalculating it each time child
     * position callback is called. This value is invalidated whenever a child is attached or
     * detached.
     */
    int mOverdrawChildPosition = -1;

    /**
     * Is the last open item closed
     */
    private boolean mCloseAnimaIsFinish = true;

    /**
     * Whether the current entry has been clicked
     */
    private boolean mClick;

    /**
     * Previous selected view holder
     */
    private RecyclerView.ViewHolder mPreOpened = null;

    private View mClickOtherView;


    float mLastX = 0;

    private boolean swipeTypeIsFollowing() {
        return ((mSwipeType & SWIPE_ITEM_TYPE_FLOWING) != 0);
    }

    /**
     * 目前支持给RecyclerView设置两种侧滑菜单。
     * {@link WeSwipeHelper#SWIPE_ITEM_TYPE_FLOWING}。
     * {@link WeSwipeHelper#SWIPE_ITEM_TYPE_DEFAULT}。
     * 具体可以查看该连接：https://github.com/WangcWj/SideslippingDemo。
     *
     * @return
     */
    public void setItemSlideType(int type) {
        mSwipeType = type;
    }

    /**
     * 使用功能的RecyclerView，将拦截一些事件。想让ItemView接收到事件的话.
     */
    private final RecyclerView.OnItemTouchListener mOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            if (DEBUG) {
                Log.d(TAG, "intercept: x:" + event.getX() + ",y:" + event.getY() + ", " + event);
            }
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mClick = true;
                mLastX = event.getX();
                mActivePointerId = event.getPointerId(0);
                mInitialTouchX = event.getX();
                mInitialTouchY = event.getY();
                obtainVelocityTracker();
                if (null != mPreOpened) {
                    boolean swipeViewBounds = checkSwipeViewBounds(event);
                    boolean isMe = false;
                    mClickOtherView = findChildView(event);
                    if (null == mClickOtherView) {
                        isMe = false;
                    } else if (mClickOtherView != mPreOpened.itemView) {
                        isMe = true;
                    }
                    mCloseAnimaIsFinish = false;
                    if (swipeViewBounds || isMe) {
                        mClick = false;
                        recoveryOpenedPreItem(mPreOpened);
                        return true;
                    }
                }
            } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                //
                if (mClick && null != mPreOpened && mSwipeType == SWIPE_ITEM_TYPE_FLOWING) {
                    doChildClickEvent(event.getRawX(), event.getRawY());
                }
                mActivePointerId = ACTIVE_POINTER_ID_NONE;
                select(null, ACTION_STATE_IDLE, false);
                //查找可以滑动的ItemView之前,先判断界面中是否有打开侧滑的Item.
            } else if (mActivePointerId != ACTIVE_POINTER_ID_NONE && mCloseAnimaIsFinish) {
                final int index = event.findPointerIndex(mActivePointerId);
                if (index >= 0) {
                    checkSelectForSwipe(action, event, index);
                }
            }
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
            }

            Log.e("WANG", "WeSwipeHelper.onInterceptTouchEvent" + mSelected);
            return mSelected != null;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
            }
            if (mActivePointerId == ACTIVE_POINTER_ID_NONE || !mCloseAnimaIsFinish) {
                return;
            }
            final int action = event.getActionMasked();
            final int activePointerIndex = event.findPointerIndex(mActivePointerId);
            if (activePointerIndex >= 0) {
                checkSelectForSwipe(action, event, activePointerIndex);
            }
            RecyclerView.ViewHolder viewHolder = mSelected;
            if (viewHolder == null) {
                return;
            }
            switch (action) {
                case MotionEvent.ACTION_MOVE: {
                    // Find the index of the active pointer and fetch its position
                    if (activePointerIndex >= 0) {
                        if (Math.abs(event.getX() - mLastX) > mSlop) {
                            //为了往下分发点击事件
                            mClick = false;
                        }
                        mLastX = event.getX();
                        updateDxDy(event, mSelectedFlags, activePointerIndex);
                        moveIfNecessary(viewHolder);
                        mRecyclerView.removeCallbacks(mScrollRunnable);
                        mScrollRunnable.run();
                        mRecyclerView.invalidate();
                    }
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                    if (mVelocityTracker != null) {
                        mVelocityTracker.clear();
                    }
                    // fall through
                case MotionEvent.ACTION_UP:
                    if (mClick) {
                        // doChildClickEvent(event.getRawX(), event.getRawY());
                        //you can do something
                    }
                    boolean needRecovery = false;
                    int swiped = swipeIfNecessary(viewHolder);
                    if (swiped <= 0) {
                        View swipeView = getNeedSwipeLayout(viewHolder);
                        if (null != swipeView) {
                            float swipeWidth = getSwipeWidth(viewHolder) / 2;
                            float translationX = swipeView.getTranslationX();
                            if (Math.abs(translationX) >= swipeWidth) {
                                needRecovery = true;
                            }
                        }
                    }
                    select(null, ACTION_STATE_IDLE, needRecovery);
                    mActivePointerId = ACTIVE_POINTER_ID_NONE;
                    break;
                case MotionEvent.ACTION_POINTER_UP: {
                    mClick = false;
                    final int pointerIndex = event.getActionIndex();
                    final int pointerId = event.getPointerId(pointerIndex);
                    if (pointerId == mActivePointerId) {
                        // This was our active pointer going up. Choose a new
                        // active pointer and adjust accordingly.
                        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        mActivePointerId = event.getPointerId(newPointerIndex);
                        updateDxDy(event, mSelectedFlags, pointerIndex);
                    }
                    break;
                }
                default:
                    mClick = false;
                    break;
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (!disallowIntercept) {
                return;
            }
            select(null, ACTION_STATE_IDLE, false);
        }
    };

    private boolean checkSwipeViewBounds(MotionEvent event) {
        View view = getViewOnScreen(mPreOpened);
        if (null == view) {
            return false;
        }
        int[] location = new int[2];
        Rect screen = getViewRectForScreen(view, location);
        return screen.contains((int) event.getRawX(), (int) event.getRawY());
    }

    /**
     * Temporary rect instance that is used when we need to lookup Item decorations.
     */
    private Rect mTmpRect;

    /**
     * When user started to drag scroll. Reset when we don't scroll
     */
    private long mDragScrollStartTimeInMs;

    /**
     * Creates an WeSwipeHelper that will work with the given Callback.
     * <p>
     * You can attach WeSwipeHelper to a RecyclerView via
     * {@link #attachToRecyclerView(RecyclerView)}. Upon attaching, it will add an item decoration,
     * an onItemTouchListener and a Child attach / detach listener to the RecyclerView.
     *
     * @param callback The Callback which controls the behavior of this touch helper.
     */
    public WeSwipeHelper(WeSwipeHelper.Callback callback) {
        mCallback = callback;
    }

    public Callback getCallback() {
        return mCallback;
    }

    public boolean swipeEnable() {
        return mCallback.isItemViewSwipeEnabled();
    }

    private boolean hitTest(View child, float x, float y, float left, float top, RecyclerView.ViewHolder vh) {
        return x >= left
                && x <= left + child.getWidth()
                && y >= top
                && y <= top + child.getHeight();
    }

    /**
     * Attaches the WeSwipeHelper to the provided RecyclerView. If TouchHelper is already
     * attached to a RecyclerView, it will first detach from the previous one. You can call this
     * method with {@code null} to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     *                     {@code null} if you want to remove WeSwipeHelper from the current
     *                     RecyclerView.
     */
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return; // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks();
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView != null) {
            final Resources resources = recyclerView.getResources();
            mSwipeEscapeVelocity = resources
                    .getDimension(android.support.v7.recyclerview.R.dimen.item_touch_helper_swipe_escape_velocity);
            mMaxSwipeVelocity = resources
                    .getDimension(android.support.v7.recyclerview.R.dimen.item_touch_helper_swipe_escape_max_velocity);
            setupCallbacks();
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING && mPreOpened != null) {
                        recoveryOpenedPreItem(mPreOpened);
                    }
                }
            });
        }
    }


    /**
     * 判断当前点击的位置是否为view并且该View实现了OnClickListener事件
     *
     * @param x
     * @param y
     */
    private void doChildClickEvent(float x, float y) {
        RecyclerView.ViewHolder viewHolder;
        if (null == mSelected && null == mPreOpened) {
            return;
        }
        if (null != mPreOpened) {
            viewHolder = mPreOpened;
        } else {
            viewHolder = mSelected;
        }
        View consumeEventView = viewHolder.itemView;
        if (consumeEventView instanceof ViewGroup) {
            consumeEventView = findConsumeView((ViewGroup) consumeEventView, x, y);
        }
        if (consumeEventView != null) {
            Log.e("WANG", "WeSwipeHelper.doChildClickEvent");
            consumeEventView.performClick();
            mClick = false;
            recoveryOpenedPreItem(viewHolder);
        }
    }

    /**
     * 查找view
     *
     * @param parent 父容器
     * @param x      点击事件的x坐标
     * @param y      点击事件的y坐标
     * @return
     */
    private View findConsumeView(ViewGroup parent, float x, float y) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup && child.getVisibility() == View.VISIBLE) {
                View view = findConsumeView((ViewGroup) child, x, y);
                if (view != null) {
                    return view;
                }
            } else if (child.getVisibility() == View.VISIBLE
                    && child.isClickable()
                    && child.isEnabled()) {
                if (isInBoundsClickable(x, y, child)) {
                    return child;
                }
            }
        }
        if (isInBoundsClickable(x, y, parent)) {
            return parent;
        }
        return null;
    }

    /**
     * 边界判断
     *
     * @param x
     * @param y
     * @param child 再点击事件下找到的view
     * @return
     */
    private boolean isInBoundsClickable(float x, float y, View child) {
        int[] location = new int[2];
        Rect screen = getViewRectForScreen(child, location);
        if (screen.contains((int) x, (int) y)
                && child.isClickable()
                && child.getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }

    private Rect getViewRectForScreen(View view, int[] location) {
        view.getLocationOnScreen(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    /**
     * 找到需要执行侧滑的那个View,自己设置的.
     *
     * @param viewHolder
     * @return
     */
    public View getItemFrontView(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof SwipeLayoutTypeCallBack) {
            SwipeLayoutTypeCallBack callBack = (SwipeLayoutTypeCallBack) viewHolder;
            return callBack.needSwipeLayout();
        }
        return null;
    }

    private void setupCallbacks() {
        ViewConfiguration vc = ViewConfiguration.get(mRecyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mRecyclerView.addItemDecoration(this);
        mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);
        mRecyclerView.addOnChildAttachStateChangeListener(this);
    }

    private void destroyCallbacks() {
        mRecyclerView.removeItemDecoration(this);
        mRecyclerView.removeOnItemTouchListener(mOnItemTouchListener);
        mRecyclerView.removeOnChildAttachStateChangeListener(this);
        // clean all attached
        final int recoverAnimSize = mRecoverAnimations.size();
        for (int i = recoverAnimSize - 1; i >= 0; i--) {
            final WeSwipeHelper.RecoverAnimation recoverAnimation = mRecoverAnimations.get(0);
            mCallback.clearView(mRecyclerView, recoverAnimation.mViewHolder);
        }
        mRecoverAnimations.clear();
        mOverdrawChild = null;
        mOverdrawChildPosition = -1;
        releaseVelocityTracker();
    }

    private void getSelectedDxDy(float[] outPosition) {
        if ((mSelectedFlags & (LEFT | RIGHT)) != 0) {
            outPosition[0] = mSelectedStartX + mDx - mSelected.itemView.getLeft();
        } else {
            outPosition[0] = mSelected.itemView.getTranslationX();
        }
        if ((mSelectedFlags & (UP | DOWN)) != 0) {
            outPosition[1] = mSelectedStartY + mDy - mSelected.itemView.getTop();
        } else {
            outPosition[1] = mSelected.itemView.getTranslationY();
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        float dx = 0, dy = 0;
        if (mSelected != null) {
            getSelectedDxDy(mTmpPosition);
            dx = mTmpPosition[0];
            dy = mTmpPosition[1];
        }
        mCallback.onDrawOver(c, parent, mSelected,
                mRecoverAnimations, mActionState, dx, dy);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        // we don't know if RV changed something so we should invalidate this index.
        mOverdrawChildPosition = -1;
        float dx = 0, dy = 0;
        if (mSelected != null) {
            getSelectedDxDy(mTmpPosition);
            dx = mTmpPosition[0];
            dy = mTmpPosition[1];
        }
        mCallback.onDraw(c, parent, mSelected,
                mRecoverAnimations, mActionState, dx, dy);
    }

    private static float getSwipeWidth(RecyclerView.ViewHolder viewHolder) {
        if (!(viewHolder instanceof SwipeLayoutTypeCallBack)) {
            return 0;
        }
        return ((SwipeLayoutTypeCallBack) viewHolder).getSwipeWidth();
    }

    private static View getNeedSwipeLayout(RecyclerView.ViewHolder viewHolder) {
        if (!(viewHolder instanceof SwipeLayoutTypeCallBack)) {
            return null;
        }
        return ((SwipeLayoutTypeCallBack) viewHolder).needSwipeLayout();
    }

    private static View getViewOnScreen(RecyclerView.ViewHolder viewHolder) {
        if (!(viewHolder instanceof SwipeLayoutTypeCallBack)) {
            return null;
        }
        return ((SwipeLayoutTypeCallBack) viewHolder).onScreenView();
    }

    public void recoverPre(RecoverCallback callback, long duration) {
        if (null != mPreOpened) {
            Log.e("WANG", "WeSwipeHelper.recoverPre" + mPreOpened.getAdapterPosition() + "    " + mPreOpened.getLayoutPosition() + "    " + mPreOpened.getOldPosition());
            recoveryOpenedPreItem(mPreOpened, duration, callback);
        }
    }

    public boolean haveRecoverItem() {
        return mPreOpened != null;
    }

    private void recoveryOpenedPreItem(RecyclerView.ViewHolder viewHolder) {
        recoveryOpenedPreItem(viewHolder, -1, null);
    }

    /**
     * 关闭一个打开的Item
     *
     * @param viewHolder 要关闭的Item的ViewHolder
     */
    private void recoveryOpenedPreItem(RecyclerView.ViewHolder viewHolder, long duration, final RecoverCallback callback) {
        if (viewHolder == null) {
            return;
        }
        final View view = getItemFrontView(viewHolder);
        if (view == null) {
            return;
        }
        float translationX = view.getTranslationX();
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "translationX", translationX, 0f);
        objectAnimator.clone();
        objectAnimator.setDuration(-1 == duration ? mCallback.getRecoveryAnimationDuration() : duration);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                //在每次接收到新的点击事件的时候都需要将上一个选择的ItemVIew从mRecoverAnimations中删除。
                endRecoverAnimation(mPreOpened, true);
                if (mPendingCleanup.remove(mPreOpened)) {
                    mCallback.clearView(mRecyclerView, mPreOpened);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mPreOpened = null;
                mCloseAnimaIsFinish = true;
                if (null != callback) {
                    callback.recoverEnd();
                }
            }
        });
        objectAnimator.start();
    }

    /**
     * Starts dragging or swiping the given View. Call with null if you want to clear it.
     *
     * @param selected    The ViewHolder to drag or swipe. Can be null if you want to cancel the
     *                    current action
     * @param actionState The type of action
     */
    void select(RecyclerView.ViewHolder selected, int actionState, boolean needOpen) {
        if (selected == mSelected && actionState == mActionState) {
            return;
        }
        mDragScrollStartTimeInMs = Long.MIN_VALUE;
        final int prevActionState = mActionState;
        // prevent duplicate animations
        endRecoverAnimation(selected, true);
        mActionState = actionState;
        int actionStateMask = (1 << (DIRECTION_FLAG_COUNT + DIRECTION_FLAG_COUNT * actionState))
                - 1;
        boolean preventLayout = false;

        if (mSelected != null) {
            final RecyclerView.ViewHolder prevSelected = mSelected;
            if (prevSelected.itemView.getParent() != null) {
                final int swipeDir;
                if (needOpen) {
                    swipeDir = 16;
                } else {
                    swipeDir = prevActionState == ACTION_STATE_DRAG ? 0
                            : swipeIfNecessary(prevSelected);
                }
                releaseVelocityTracker();
                // find where we should animate to
                final float targetTranslateX, targetTranslateY;
                final int animationType;
                switch (swipeDir) {
                    case LEFT:
                    case RIGHT:
                    case START:
                    case END:
                        targetTranslateY = 0;
                        float swipeWidth = mRecyclerView.getWidth();
                        if (swipeTypeIsFollowing()) {
                            swipeWidth += getSwipeWidth(mSelected);
                        }
                        targetTranslateX = Math.signum(mDx) * swipeWidth;
                        break;
                    case UP:
                    case DOWN:
                        targetTranslateX = 0;
                        targetTranslateY = Math.signum(mDy) * mRecyclerView.getHeight();
                        break;
                    default:
                        targetTranslateX = 0;
                        targetTranslateY = 0;
                }
                if (prevActionState == ACTION_STATE_DRAG) {
                    animationType = ANIMATION_TYPE_DRAG;
                } else if (swipeDir > 0) {
                    animationType = ANIMATION_TYPE_SWIPE_SUCCESS;
                } else {
                    animationType = ANIMATION_TYPE_SWIPE_CANCEL;
                }
                getSelectedDxDy(mTmpPosition);
                final float currentTranslateX = mTmpPosition[0];
                final float currentTranslateY = mTmpPosition[1];
                final WeSwipeHelper.RecoverAnimation rv = new WeSwipeHelper.RecoverAnimation(prevSelected, animationType,
                        prevActionState, currentTranslateX, currentTranslateY,
                        targetTranslateX, targetTranslateY) {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (this.mOverridden) {
                            return;
                        }
                        if (swipeDir <= 0) {
                            // this is a drag or failed swipe. recover immediately.
                            mPreOpened = null;
                            mCallback.clearView(mRecyclerView, prevSelected);
                            // full cleanup will happen on onDrawOver.
                        } else {
                            //successful sliding.
                            mPreOpened = prevSelected;
                            Log.e("WANG", "WeSwipeHelper.select" + mPreOpened.getAdapterPosition());
                            // wait until remove animation is complete.
                            mPendingCleanup.add(prevSelected.itemView);
                            mIsPendingCleanup = true;
                            if (swipeDir > 0) {
                                // Animation might be ended by other animators during a layout.
                                // We defer callback to avoid editing adapter during a layout.
                                postDispatchSwipe(this, swipeDir);
                            }
                        }
                        // removed from the list after it is drawn for the last time
                        if (mOverdrawChild == prevSelected.itemView) {
                            removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView);
                        }
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                    }
                };
                final long duration = mCallback.getAnimationDuration(mRecyclerView, animationType,
                        targetTranslateX - currentTranslateX, targetTranslateY - currentTranslateY);
                rv.setDuration(duration);
                mRecoverAnimations.add(rv);
                rv.start();
                preventLayout = true;
            } else {
                removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView);
                mCallback.clearView(mRecyclerView, prevSelected);
            }
            mSelected = null;
        }
        if (selected != null) {
            mSelectedFlags =
                    (mCallback.getAbsoluteMovementFlags(mRecyclerView, selected) & actionStateMask)
                            >> (mActionState * DIRECTION_FLAG_COUNT);
            mSelectedStartX = selected.itemView.getLeft();
            mSelectedStartY = selected.itemView.getTop();
            mSelected = selected;

            if (actionState == ACTION_STATE_DRAG) {
                mSelected.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        }
        final ViewParent rvParent = mRecyclerView.getParent();
        if (rvParent != null) {
            rvParent.requestDisallowInterceptTouchEvent(mSelected != null);
        }
        if (!preventLayout) {
            mRecyclerView.getLayoutManager().requestSimpleAnimationsInNextLayout();
        }
        mCallback.onSelectedChanged(mSelected, mActionState);
        mRecyclerView.invalidate();
    }

    void postDispatchSwipe(final WeSwipeHelper.RecoverAnimation anim, final int swipeDir) {
        // wait until animations are complete.
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (mRecyclerView != null && mRecyclerView.isAttachedToWindow()
                        && !anim.mOverridden
                        && anim.mViewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    final RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
                    // if animator is running or we have other active recover animations, we try
                    // not to call onSwiped because DefaultItemAnimator is not good at merging
                    // animations. Instead, we wait and batch.
                    if ((animator == null || !animator.isRunning(null))
                            && !hasRunningRecoverAnim()) {
                        mCallback.onSwiped(anim.mViewHolder, swipeDir);
                    } else {
                        mRecyclerView.post(this);
                    }
                }
            }
        });
    }

    boolean hasRunningRecoverAnim() {
        final int size = mRecoverAnimations.size();
        for (int i = 0; i < size; i++) {
            if (!mRecoverAnimations.get(i).mEnded) {
                return true;
            }
        }
        return false;
    }

    /**
     * If user drags the view to the edge, trigger a scroll if necessary.
     */
    boolean scrollIfNecessary() {
        if (mSelected == null) {
            mDragScrollStartTimeInMs = Long.MIN_VALUE;
            return false;
        }
        final long now = System.currentTimeMillis();
        final long scrollDuration = mDragScrollStartTimeInMs
                == Long.MIN_VALUE ? 0 : now - mDragScrollStartTimeInMs;
        RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
        if (mTmpRect == null) {
            mTmpRect = new Rect();
        }
        int scrollX = 0;
        int scrollY = 0;
        lm.calculateItemDecorationsForChild(mSelected.itemView, mTmpRect);
        if (lm.canScrollHorizontally()) {
            int curX = (int) (mSelectedStartX + mDx);
            final int leftDiff = curX - mTmpRect.left - mRecyclerView.getPaddingLeft();
            if (mDx < 0 && leftDiff < 0) {
                scrollX = leftDiff;
            } else if (mDx > 0) {
                final int rightDiff =
                        curX + mSelected.itemView.getWidth() + mTmpRect.right
                                - (mRecyclerView.getWidth() - mRecyclerView.getPaddingRight());
                if (rightDiff > 0) {
                    scrollX = rightDiff;
                }
            }
        }
        if (lm.canScrollVertically()) {
            int curY = (int) (mSelectedStartY + mDy);
            final int topDiff = curY - mTmpRect.top - mRecyclerView.getPaddingTop();
            if (mDy < 0 && topDiff < 0) {
                scrollY = topDiff;
            } else if (mDy > 0) {
                final int bottomDiff = curY + mSelected.itemView.getHeight() + mTmpRect.bottom
                        - (mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom());
                if (bottomDiff > 0) {
                    scrollY = bottomDiff;
                }
            }
        }
        if (scrollX != 0) {
            scrollX = mCallback.interpolateOutOfBoundsScroll(mRecyclerView,
                    mSelected.itemView.getWidth(), scrollX,
                    mRecyclerView.getWidth(), scrollDuration);
        }
        if (scrollY != 0) {
            scrollY = mCallback.interpolateOutOfBoundsScroll(mRecyclerView,
                    mSelected.itemView.getHeight(), scrollY,
                    mRecyclerView.getHeight(), scrollDuration);
        }
        if (scrollX != 0 || scrollY != 0) {
            if (mDragScrollStartTimeInMs == Long.MIN_VALUE) {
                mDragScrollStartTimeInMs = now;
            }
            mRecyclerView.scrollBy(scrollX, scrollY);
            return true;
        }
        mDragScrollStartTimeInMs = Long.MIN_VALUE;
        return false;
    }

    private List<RecyclerView.ViewHolder> findSwapTargets(RecyclerView.ViewHolder viewHolder) {
        if (mSwapTargets == null) {
            mSwapTargets = new ArrayList<RecyclerView.ViewHolder>();
            mDistances = new ArrayList<Integer>();
        } else {
            mSwapTargets.clear();
            mDistances.clear();
        }
        final int margin = mCallback.getBoundingBoxMargin();
        final int left = Math.round(mSelectedStartX + mDx) - margin;
        final int top = Math.round(mSelectedStartY + mDy) - margin;
        final int right = left + viewHolder.itemView.getWidth() + 2 * margin;
        final int bottom = top + viewHolder.itemView.getHeight() + 2 * margin;
        final int centerX = (left + right) / 2;
        final int centerY = (top + bottom) / 2;
        final RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
        final int childCount = lm.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View other = lm.getChildAt(i);
            if (other == viewHolder.itemView) {
                continue; //myself!
            }
            if (other.getBottom() < top || other.getTop() > bottom
                    || other.getRight() < left || other.getLeft() > right) {
                continue;
            }
            final RecyclerView.ViewHolder otherVh = mRecyclerView.getChildViewHolder(other);
            if (mCallback.canDropOver(mRecyclerView, mSelected, otherVh)) {
                // find the index to add
                final int dx = Math.abs(centerX - (other.getLeft() + other.getRight()) / 2);
                final int dy = Math.abs(centerY - (other.getTop() + other.getBottom()) / 2);
                final int dist = dx * dx + dy * dy;

                int pos = 0;
                final int cnt = mSwapTargets.size();
                for (int j = 0; j < cnt; j++) {
                    if (dist > mDistances.get(j)) {
                        pos++;
                    } else {
                        break;
                    }
                }
                mSwapTargets.add(pos, otherVh);
                mDistances.add(pos, dist);
            }
        }
        return mSwapTargets;
    }

    /**
     * Checks if we should swap w/ another view holder.
     */
    void moveIfNecessary(RecyclerView.ViewHolder viewHolder) {
        if (mRecyclerView.isLayoutRequested()) {
            return;
        }
        if (mActionState != ACTION_STATE_DRAG) {
            return;
        }

        final float threshold = mCallback.getMoveThreshold(viewHolder);
        final int x = (int) (mSelectedStartX + mDx);
        final int y = (int) (mSelectedStartY + mDy);
        if (Math.abs(y - viewHolder.itemView.getTop()) < viewHolder.itemView.getHeight() * threshold
                && Math.abs(x - viewHolder.itemView.getLeft())
                < viewHolder.itemView.getWidth() * threshold) {
            return;
        }
        List<RecyclerView.ViewHolder> swapTargets = findSwapTargets(viewHolder);
        if (swapTargets.size() == 0) {
            return;
        }
        // may swap.
        RecyclerView.ViewHolder target = mCallback.chooseDropTarget(viewHolder, swapTargets, x, y);
        if (target == null) {
            mSwapTargets.clear();
            mDistances.clear();
            return;
        }
        final int toPosition = target.getAdapterPosition();
        final int fromPosition = viewHolder.getAdapterPosition();
        if (mCallback.onMove(mRecyclerView, viewHolder, target)) {
            // keep target visible
            mCallback.onMoved(mRecyclerView, viewHolder, fromPosition,
                    target, toPosition, x, y);
        }
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        removeChildDrawingOrderCallbackIfNecessary(view);
        final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }
        if (mSelected != null && holder == mSelected) {
            select(null, ACTION_STATE_IDLE, false);
        } else {
            //this may push it into pending cleanup list.
            endRecoverAnimation(holder, false);
            if (mPendingCleanup.remove(holder.itemView)) {
                mCallback.clearView(mRecyclerView, holder);
            }
        }
    }

    /**
     * Returns the animation type or 0 if cannot be found.
     */
    int endRecoverAnimation(RecyclerView.ViewHolder viewHolder, boolean override) {
        final int recoverAnimSize = mRecoverAnimations.size();
        for (int i = recoverAnimSize - 1; i >= 0; i--) {
            final WeSwipeHelper.RecoverAnimation anim = mRecoverAnimations.get(i);
            if (anim.mViewHolder == viewHolder) {
                anim.mOverridden |= override;
                if (!anim.mEnded) {
                    anim.cancel();
                }
                mRecoverAnimations.remove(i);
                return anim.mAnimationType;
            }
        }
        return 0;
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.setEmpty();
    }

    void obtainVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
        mVelocityTracker = VelocityTracker.obtain();
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private RecyclerView.ViewHolder findSwipedView(MotionEvent motionEvent) {
        final RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
        if (mActivePointerId == ACTIVE_POINTER_ID_NONE) {
            return null;
        }
        final int pointerIndex = motionEvent.findPointerIndex(mActivePointerId);
        final float dx = motionEvent.getX(pointerIndex) - mInitialTouchX;
        final float dy = motionEvent.getY(pointerIndex) - mInitialTouchY;
        final float absDx = Math.abs(dx);
        final float absDy = Math.abs(dy);

        if (absDx < mSlop && absDy < mSlop) {
            return null;
        }
        if (absDx > absDy && lm.canScrollHorizontally()) {
            return null;
        } else if (absDy > absDx && lm.canScrollVertically()) {
            return null;
        }
        View child = findChildView(motionEvent);
        if (child == null) {
            return null;
        }
        return mRecyclerView.getChildViewHolder(child);
    }

    /**
     * Checks whether we should select a View for swiping.
     */
    boolean checkSelectForSwipe(int action, MotionEvent motionEvent, int pointerIndex) {
        if (mSelected != null || action != MotionEvent.ACTION_MOVE
                || mActionState == ACTION_STATE_DRAG || !mCallback.isItemViewSwipeEnabled()) {
            return false;
        }
        if (mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
            return false;
        }
        final RecyclerView.ViewHolder vh = findSwipedView(motionEvent);
        if (vh == null) {
            return false;
        }
        final int movementFlags = mCallback.getAbsoluteMovementFlags(mRecyclerView, vh);

        final int swipeFlags = (movementFlags & ACTION_MODE_SWIPE_MASK)
                >> (DIRECTION_FLAG_COUNT * ACTION_STATE_SWIPE);

        if (swipeFlags == 0) {
            return false;
        }

        // mDx and mDy are only set in allowed directions. We use custom x/y here instead of
        // updateDxDy to avoid swiping if user moves more in the other direction
        final float x = motionEvent.getX(pointerIndex);
        final float y = motionEvent.getY(pointerIndex);

        // Calculate the distance moved
        final float dx = x - mInitialTouchX;
        final float dy = y - mInitialTouchY;
        // swipe target is chose w/o applying flags so it does not really check if swiping in that
        // direction is allowed. This why here, we use mDx mDy to check slope value again.
        final float absDx = Math.abs(dx);
        final float absDy = Math.abs(dy);

        if (absDx < mSlop && absDy < mSlop) {
            return false;
        }
        if (absDx > absDy) {
            if (dx < 0 && (swipeFlags & LEFT) == 0) {
                return false;
            }
            if (dx > 0 && (swipeFlags & RIGHT) == 0) {
                return false;
            }
        } else {
            if (dy < 0 && (swipeFlags & UP) == 0) {
                return false;
            }
            if (dy > 0 && (swipeFlags & DOWN) == 0) {
                return false;
            }
        }
        mDx = mDy = 0f;
        mActivePointerId = motionEvent.getPointerId(0);
        select(vh, ACTION_STATE_SWIPE, false);
        return true;
    }

    View findChildView(MotionEvent event) {
        // first check elevated views, if none, then call RV
        final float x = event.getX();
        final float y = event.getY();
        if (mSelected != null) {
            final View selectedView = mSelected.itemView;
            if (hitTest(selectedView, x, y, mSelectedStartX + mDx, mSelectedStartY + mDy, mSelected)) {
                return selectedView;
            }
        }
        for (int i = mRecoverAnimations.size() - 1; i >= 0; i--) {
            final WeSwipeHelper.RecoverAnimation anim = mRecoverAnimations.get(i);
            final View view = anim.mViewHolder.itemView;
            boolean hitTest = hitTest(view, x, y, anim.mX, anim.mY, anim.mViewHolder);
            if (hitTest) {
                return view;
            }
        }
        View childViewUnder = mRecyclerView.findChildViewUnder(x, y);
        return childViewUnder;
    }

    /**
     * Starts dragging the provided ViewHolder. By default, WeSwipeHelper starts a drag when a
     * View is long pressed. You can disable that behavior by overriding
     * {@link WeSwipeHelper.Callback#isLongPressDragEnabled()}.
     * <p>
     * For this method to work:
     * <ul>
     * <li>The provided ViewHolder must be a child of the RecyclerView to which this
     * WeSwipeHelper
     * is attached.</li>
     * <li>{@link WeSwipeHelper.Callback} must have dragging enabled.</li>
     * <li>There must be a previous touch event that was reported to the WeSwipeHelper
     * through RecyclerView's ItemTouchListener mechanism. As long as no other ItemTouchListener
     * grabs previous events, this should work as expected.</li>
     * </ul>
     * <p>
     * For example, if you would like to let your user to be able to drag an Item by touching one
     * of its descendants, you may implement it as follows:
     * <pre>
     *     viewHolder.dragButton.setOnTouchListener(new View.OnTouchListener() {
     *         public boolean onTouch(View v, MotionEvent event) {
     *             if (MotionEvent.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
     *                 mWItemTouchHelperPlus.startDrag(viewHolder);
     *             }
     *             return false;
     *         }
     *     });
     * </pre>
     * <p>
     *
     * @param viewHolder The ViewHolder to start dragging. It must be a direct child of
     *                   RecyclerView.
     * @see WeSwipeHelper.Callback#isItemViewSwipeEnabled()
     */
    public void startDrag(RecyclerView.ViewHolder viewHolder) {
        if (!mCallback.hasDragFlag(mRecyclerView, viewHolder)) {
            Log.e(TAG, "Start drag has been called but dragging is not enabled");
            return;
        }
        if (viewHolder.itemView.getParent() != mRecyclerView) {
            Log.e(TAG, "Start drag has been called with a view holder which is not a child of "
                    + "the RecyclerView which is controlled by this WeSwipeHelper.");
            return;
        }
        obtainVelocityTracker();
        mDx = mDy = 0f;
        select(viewHolder, ACTION_STATE_DRAG, false);
    }

    /**
     * Starts swiping the provided ViewHolder. By default, WeSwipeHelper starts swiping a View
     * when user swipes their finger (or mouse pointer) over the View. You can disable this
     * behavior
     * by overriding {@link WeSwipeHelper.Callback}
     * <p>
     * For this method to work:
     * <ul>
     * <li>The provided ViewHolder must be a child of the RecyclerView to which this
     * WeSwipeHelper is attached.</li>
     * <li>{@link WeSwipeHelper.Callback} must have swiping enabled.</li>
     * <li>There must be a previous touch event that was reported to the WeSwipeHelper
     * through RecyclerView's ItemTouchListener mechanism. As long as no other ItemTouchListener
     * grabs previous events, this should work as expected.</li>
     * </ul>
     * <p>
     * For example, if you would like to let your user to be able to swipe an Item by touching one
     * of its descendants, you may implement it as follows:
     * <pre>
     *     viewHolder.dragButton.setOnTouchListener(new View.OnTouchListener() {
     *         public boolean onTouch(View v, MotionEvent event) {
     *             if (MotionEvent.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
     *                 mWItemTouchHelperPlus.startSwipe(viewHolder);
     *             }
     *             return false;
     *         }
     *     });
     * </pre>
     *
     * @param viewHolder The ViewHolder to start swiping. It must be a direct child of
     *                   RecyclerView.
     */
    public void startSwipe(RecyclerView.ViewHolder viewHolder) {
        if (!mCallback.hasSwipeFlag(mRecyclerView, viewHolder)) {
            Log.e(TAG, "Start swipe has been called but swiping is not enabled");
            return;
        }
        if (viewHolder.itemView.getParent() != mRecyclerView) {
            Log.e(TAG, "Start swipe has been called with a view holder which is not a child of "
                    + "the RecyclerView controlled by this WeSwipeHelper.");
            return;
        }
        obtainVelocityTracker();
        mDx = mDy = 0f;
        select(viewHolder, ACTION_STATE_SWIPE, false);
    }

    void updateDxDy(MotionEvent ev, int directionFlags, int pointerIndex) {
        final float x = ev.getX(pointerIndex);
        final float y = ev.getY(pointerIndex);

        // Calculate the distance moved
        mDx = x - mInitialTouchX;
        mDy = y - mInitialTouchY;
        if ((directionFlags & LEFT) == 0) {
            mDx = Math.max(0, mDx);
        }
        if ((directionFlags & RIGHT) == 0) {
            mDx = Math.min(0, mDx);
        }
        if ((directionFlags & UP) == 0) {
            mDy = Math.max(0, mDy);
        }
        if ((directionFlags & DOWN) == 0) {
            mDy = Math.min(0, mDy);
        }
    }

    private int swipeIfNecessary(RecyclerView.ViewHolder viewHolder) {
        if (mActionState == ACTION_STATE_DRAG) {
            return 0;
        }
        final int originalMovementFlags = mCallback.getMovementFlags(mRecyclerView, viewHolder);
        final int absoluteMovementFlags = mCallback.convertToAbsoluteDirection(
                originalMovementFlags,
                ViewCompat.getLayoutDirection(mRecyclerView));
        final int flags = (absoluteMovementFlags
                & ACTION_MODE_SWIPE_MASK) >> (ACTION_STATE_SWIPE * DIRECTION_FLAG_COUNT);
        if (flags == 0) {
            return 0;
        }
        final int originalFlags = (originalMovementFlags
                & ACTION_MODE_SWIPE_MASK) >> (ACTION_STATE_SWIPE * DIRECTION_FLAG_COUNT);
        int swipeDir;
        //start slipping.
        if (Math.abs(mDx) > Math.abs(mDy)) {
            if ((swipeDir = checkHorizontalSwipe(viewHolder, flags)) > 0) {
                // if swipe dir is not in original flags, it should be the relative direction
                if ((originalFlags & swipeDir) == 0) {
                    // convert to relative
                    return WeSwipeHelper.Callback.convertToRelativeDirection(swipeDir,
                            ViewCompat.getLayoutDirection(mRecyclerView));
                }
                return swipeDir;
            }
            if ((swipeDir = checkVerticalSwipe(viewHolder, flags)) > 0) {
                return swipeDir;
            }
        } else {
            if ((swipeDir = checkVerticalSwipe(viewHolder, flags)) > 0) {
                return swipeDir;
            }
            if ((swipeDir = checkHorizontalSwipe(viewHolder, flags)) > 0) {
                // if swipe dir is not in original flags, it should be the relative direction
                if ((originalFlags & swipeDir) == 0) {
                    // convert to relative
                    return WeSwipeHelper.Callback.convertToRelativeDirection(swipeDir,
                            ViewCompat.getLayoutDirection(mRecyclerView));
                }
                return swipeDir;
            }
        }
        return 0;
    }

    private int checkHorizontalSwipe(RecyclerView.ViewHolder viewHolder, int flags) {
        if ((flags & (LEFT | RIGHT)) != 0) {
            final int dirFlag = mDx > 0 ? RIGHT : LEFT;
            if (mVelocityTracker != null && mActivePointerId > -1) {
                mVelocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND,
                        mCallback.getSwipeVelocityThreshold(mMaxSwipeVelocity));
                final float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
                final float yVelocity = mVelocityTracker.getYVelocity(mActivePointerId);
                final int velDirFlag = xVelocity > 0f ? RIGHT : LEFT;
                final float absXVelocity = Math.abs(xVelocity);
                if ((velDirFlag & flags) != 0 && dirFlag == velDirFlag
                        && absXVelocity >= mCallback.getSwipeEscapeVelocity(mSwipeEscapeVelocity)
                        && absXVelocity > Math.abs(yVelocity)) {
                    return velDirFlag;
                }
            }
            int width = mRecyclerView.getWidth();
            //If the target layout follows the item view, the RecyclerView is wider than itself.
            if (swipeTypeIsFollowing()) {
                float swipeWidth = getSwipeWidth(viewHolder);
                width += (int) swipeWidth;
            }
            final float threshold = width * mCallback
                    .getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(mDx) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }

    private int checkVerticalSwipe(RecyclerView.ViewHolder viewHolder, int flags) {
        if ((flags & (UP | DOWN)) != 0) {
            final int dirFlag = mDy > 0 ? DOWN : UP;
            if (mVelocityTracker != null && mActivePointerId > -1) {
                mVelocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND,
                        mCallback.getSwipeVelocityThreshold(mMaxSwipeVelocity));
                final float xVelocity = mVelocityTracker.getXVelocity(mActivePointerId);
                final float yVelocity = mVelocityTracker.getYVelocity(mActivePointerId);
                final int velDirFlag = yVelocity > 0f ? DOWN : UP;
                final float absYVelocity = Math.abs(yVelocity);
                if ((velDirFlag & flags) != 0 && velDirFlag == dirFlag
                        && absYVelocity >= mCallback.getSwipeEscapeVelocity(mSwipeEscapeVelocity)
                        && absYVelocity > Math.abs(xVelocity)) {
                    return velDirFlag;
                }
            }

            final float threshold = mRecyclerView.getHeight() * mCallback
                    .getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(mDy) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }

    void removeChildDrawingOrderCallbackIfNecessary(View view) {
        if (view == mOverdrawChild) {
            mOverdrawChild = null;
            // only remove if we've added
            if (mChildDrawingOrderCallback != null) {
                mRecyclerView.setChildDrawingOrderCallback(null);
            }
        }
    }

    /**
     * Two types of sliding are provided
     */
    public interface SwipeLayoutTypeCallBack {


        float getSwipeWidth();

        /**
         * View that needs to slide in RecyclerView.
         *
         * @return View that needs to slide.
         */
        View needSwipeLayout();

        /**
         * When an item is sliding and has not yet been restored,
         * you need to determine whether the current click event requires subview
         * processing or restoring the sliding state.Click events are handled only
         * by clicking on the Operations View
         *
         * @return null: Consumer click events and recovery animation,otherwise only recovery animation.
         */
        View onScreenView();

    }

    /**
     * An interface which can be implemented by LayoutManager for better integration with
     * {@link WeSwipeHelper}.
     */
    public interface ViewDropHandler {

        /**
         * Called by the {@link WeSwipeHelper} after a View is dropped over another View.
         * <p>
         * A LayoutManager should implement this interface to get ready for the upcoming move
         * operation.
         * <p>
         * For example, LinearLayoutManager sets up a "scrollToPositionWithOffset" calls so that
         * the View under drag will be used as an anchor View while calculating the next layout,
         * making layout stay consistent.
         *
         * @param view   The View which is being dragged. It is very likely that user is still
         *               dragging this View so there might be other
         *               {@link #prepareForDrop(View, View, int, int)} after this one.
         * @param target The target view which is being dropped on.
         * @param x      The <code>left</code> offset of the View that is being dragged. This value
         *               includes the movement caused by the user.
         * @param y      The <code>top</code> offset of the View that is being dragged. This value
         *               includes the movement caused by the user.
         */
        void prepareForDrop(View view, View target, int x, int y);
    }

    /**
     * This class is the contract between WeSwipeHelper and your application. It lets you control
     * which touch behaviors are enabled per each ViewHolder and also receive callbacks when user
     * performs these actions.
     * <p>
     * To control which actions user can take on each view, you should override
     * {@link #getMovementFlags(RecyclerView, RecyclerView.ViewHolder)} and return appropriate set
     * of direction flags. ({@link #LEFT}, {@link #RIGHT}, {@link #START}, {@link #END},
     * {@link #UP}, {@link #DOWN}). You can use
     * {@link #makeMovementFlags(int, int)} to easily construct it. Alternatively, you can use
     * {@link WeSwipeHelper.SimpleCallback}.
     * <p>
     * If user drags an item, WeSwipeHelper will call
     * {@link WeSwipeHelper.Callback#onMove(RecyclerView, RecyclerView.ViewHolder, RecyclerView.ViewHolder)
     * onMove(recyclerView, dragged, target)}.
     * Upon receiving this callback, you should move the item from the old position
     * ({@code dragged.getAdapterPosition()}) to new position ({@code target.getAdapterPosition()})
     * in your adapter and also call {@link RecyclerView.Adapter#notifyItemMoved(int, int)}.
     * To control where a View can be dropped, you can override
     * {@link #canDropOver(RecyclerView, RecyclerView.ViewHolder, RecyclerView.ViewHolder)}. When a
     * dragging View overlaps multiple other views, Callback chooses the closest View with which
     * dragged View might have changed positions. Although this approach works for many use cases,
     * if you have a custom LayoutManager, you can override
     * {@link #chooseDropTarget(RecyclerView.ViewHolder, java.util.List, int, int)} to select a
     * custom drop target.
     * <p>
     * When a View is swiped, WeSwipeHelper animates it until it goes out of bounds, then calls
     * {@link #onSwiped(RecyclerView.ViewHolder, int)}. At this point, you should update your
     * adapter (e.g. remove the item) and call related Adapter#notify event.
     */
    @SuppressWarnings("UnusedParameters")
    public abstract static class Callback {

        public static final int DEFAULT_DRAG_ANIMATION_DURATION = 200;

        public static final int DEFAULT_SWIPE_ANIMATION_DURATION = 250;

        static final int RELATIVE_DIR_FLAGS = START | END
                | ((START | END) << DIRECTION_FLAG_COUNT)
                | ((START | END) << (2 * DIRECTION_FLAG_COUNT));

        private static final ItemTouchUIUtil sUICallback;

        private static final int ABS_HORIZONTAL_DIR_FLAGS = LEFT | RIGHT
                | ((LEFT | RIGHT) << DIRECTION_FLAG_COUNT)
                | ((LEFT | RIGHT) << (2 * DIRECTION_FLAG_COUNT));

        private static final Interpolator sDragScrollInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                return t * t * t * t * t;
            }
        };

        private static final Interpolator sDragViewScrollCapInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                t -= 1.0f;
                return t * t * t * t * t + 1.0f;
            }
        };

        /**
         * Drag scroll speed keeps accelerating until this many milliseconds before being capped.
         */
        private static final long DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS = 2000;

        private int mCachedMaxScrollSpeed = -1;

        static {
            if (Build.VERSION.SDK_INT >= 21) {
                sUICallback = new ItemTouchUIUtilImpl.Lollipop();
            } else {
                sUICallback = new ItemTouchUIUtilImpl.Honeycomb();
            }
        }

        /**
         * Returns the {@link ItemTouchUIUtil} that is used by the {@link WeSwipeHelper.Callback} class for
         * visual
         * changes on Views in response to user interactions. {@link ItemTouchUIUtil} has different
         * implementations for different platform versions.
         * <p>
         * By default, {@link WeSwipeHelper.Callback} applies these changes on
         * {@link RecyclerView.ViewHolder#itemView}.
         * <p>
         * For example, if you have a use case where you only want the text to move when user
         * swipes over the view, you can do the following:
         * <pre>
         *     public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder){
         *         getDefaultUIUtil().clearView(((ItemTouchViewHolder) viewHolder).textView);
         *     }
         *     public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
         *         if (viewHolder != null){
         *             getDefaultUIUtil().onSelected(((ItemTouchViewHolder) viewHolder).textView);
         *         }
         *     }
         *     public void onChildDraw(Canvas c, RecyclerView recyclerView,
         *             RecyclerView.ViewHolder viewHolder, View swipeView,float dX, float dY, int actionState,
         *             boolean isCurrentlyActive,,float swipeWidth) {
         *         getDefaultUIUtil().onDraw(c, recyclerView,
         *                 ((ItemTouchViewHolder) viewHolder).textView, dX, dY,
         *                 actionState, isCurrentlyActive);
         *         return true;
         *     }
         *     public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
         *             RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
         *             boolean isCurrentlyActive) {
         *         getDefaultUIUtil().onDrawOver(c, recyclerView,
         *                 ((ItemTouchViewHolder) viewHolder).textView, dX, dY,
         *                 actionState, isCurrentlyActive);
         *         return true;
         *     }
         * </pre>
         *
         * @return The {@link ItemTouchUIUtil} instance that is used by the {@link WeSwipeHelper.Callback}
         */
        public static ItemTouchUIUtil getDefaultUIUtil() {
            return sUICallback;
        }

        /**
         * Replaces a movement direction with its relative version by taking layout direction into
         * account.
         *
         * @param flags           The flag value that include any number of movement flags.
         * @param layoutDirection The layout direction of the View. Can be obtained from
         *                        {@link ViewCompat#getLayoutDirection(android.view.View)}.
         * @return Updated flags which uses relative flags ({@link #START}, {@link #END}) instead
         * of {@link #LEFT}, {@link #RIGHT}.
         * @see #convertToAbsoluteDirection(int, int)
         */
        public static int convertToRelativeDirection(int flags, int layoutDirection) {
            int masked = flags & ABS_HORIZONTAL_DIR_FLAGS;
            if (masked == 0) {
                return flags; // does not have any abs flags, good.
            }
            flags &= ~masked; //remove left / right.
            if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                // no change. just OR with 2 bits shifted mask and return
                flags |= masked << 2; // START is 2 bits after LEFT, END is 2 bits after RIGHT.
                return flags;
            } else {
                // add RIGHT flag as START
                flags |= ((masked << 1) & ~ABS_HORIZONTAL_DIR_FLAGS);
                // first clean RIGHT bit then add LEFT flag as END
                flags |= ((masked << 1) & ABS_HORIZONTAL_DIR_FLAGS) << 2;
            }
            return flags;
        }

        /**
         * Convenience method to create movement flags.
         * <p>
         * For instance, if you want to let your items be drag & dropped vertically and swiped
         * left to be dismissed, you can call this method with:
         * <code>makeMovementFlags(UP | DOWN, LEFT);</code>
         *
         * @param dragFlags  The directions in which the item can be dragged.
         * @param swipeFlags The directions in which the item can be swiped.
         * @return Returns an integer composed of the given drag and swipe flags.
         */
        public static int makeMovementFlags(int dragFlags, int swipeFlags) {
            return makeFlag(ACTION_STATE_IDLE, swipeFlags | dragFlags)
                    | makeFlag(ACTION_STATE_SWIPE, swipeFlags)
                    | makeFlag(ACTION_STATE_DRAG, dragFlags);
        }

        /**
         * Shifts the given direction flags to the offset of the given action state.
         *
         * @param actionState The action state you want to get flags in. Should be one of
         *                    {@link #ACTION_STATE_IDLE}, {@link #ACTION_STATE_SWIPE} or
         *                    {@link #ACTION_STATE_DRAG}.
         * @param directions  The direction flags. Can be composed from {@link #UP}, {@link #DOWN},
         *                    {@link #RIGHT}, {@link #LEFT} {@link #START} and {@link #END}.
         * @return And integer that represents the given directions in the provided actionState.
         */
        public static int makeFlag(int actionState, int directions) {
            return directions << (actionState * DIRECTION_FLAG_COUNT);
        }

        /**
         * Should return a composite flag which defines the enabled move directions in each state
         * (idle, swiping, dragging).
         * <p>
         * Instead of composing this flag manually, you can use {@link #makeMovementFlags(int,
         * int)}
         * or {@link #makeFlag(int, int)}.
         * <p>
         * This flag is composed of 3 sets of 8 bits, where first 8 bits are for IDLE state, next
         * 8 bits are for SWIPE state and third 8 bits are for DRAG state.
         * Each 8 bit sections can be constructed by simply OR'ing direction flags defined in
         * {@link WeSwipeHelper}.
         * <p>
         * For example, if you want it to allow swiping LEFT and RIGHT but only allow starting to
         * swipe by swiping RIGHT, you can return:
         * <pre>
         *      makeFlag(ACTION_STATE_IDLE, RIGHT) | makeFlag(ACTION_STATE_SWIPE, LEFT | RIGHT);
         * </pre>
         * This means, allow right movement while IDLE and allow right and left movement while
         * swiping.
         *
         * @param recyclerView The RecyclerView to which WeSwipeHelper is attached.
         * @param viewHolder   The ViewHolder for which the movement information is necessary.
         * @return flags specifying which movements are allowed on this ViewHolder.
         * @see #makeMovementFlags(int, int)
         * @see #makeFlag(int, int)
         */
        public abstract int getMovementFlags(RecyclerView recyclerView,
                                             RecyclerView.ViewHolder viewHolder);

        /**
         * Converts a given set of flags to absolution direction which means {@link #START} and
         * {@link #END} are replaced with {@link #LEFT} and {@link #RIGHT} depending on the layout
         * direction.
         *
         * @param flags           The flag value that include any number of movement flags.
         * @param layoutDirection The layout direction of the RecyclerView.
         * @return Updated flags which includes only absolute direction values.
         */
        public int convertToAbsoluteDirection(int flags, int layoutDirection) {
            int masked = flags & RELATIVE_DIR_FLAGS;
            if (masked == 0) {
                return flags; // does not have any relative flags, good.
            }
            flags &= ~masked; //remove start / end
            if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                // no change. just OR with 2 bits shifted mask and return
                flags |= masked >> 2; // START is 2 bits after LEFT, END is 2 bits after RIGHT.
                return flags;
            } else {
                // add START flag as RIGHT
                flags |= ((masked >> 1) & ~RELATIVE_DIR_FLAGS);
                // first clean start bit then add END flag as LEFT
                flags |= ((masked >> 1) & RELATIVE_DIR_FLAGS) >> 2;
            }
            return flags;
        }

        final int getAbsoluteMovementFlags(RecyclerView recyclerView,
                                           RecyclerView.ViewHolder viewHolder) {
            final int flags = getMovementFlags(recyclerView, viewHolder);
            return convertToAbsoluteDirection(flags, ViewCompat.getLayoutDirection(recyclerView));
        }

        boolean hasDragFlag(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            final int flags = getAbsoluteMovementFlags(recyclerView, viewHolder);
            return (flags & ACTION_MODE_DRAG_MASK) != 0;
        }

        boolean hasSwipeFlag(RecyclerView recyclerView,
                             RecyclerView.ViewHolder viewHolder) {
            final int flags = getAbsoluteMovementFlags(recyclerView, viewHolder);
            return (flags & ACTION_MODE_SWIPE_MASK) != 0;
        }

        /**
         * Return true if the current ViewHolder can be dropped over the the target ViewHolder.
         * <p>
         * This method is used when selecting drop target for the dragged View. After Views are
         * eliminated either via bounds check or via this method, resulting set of views will be
         * passed to {@link #chooseDropTarget(RecyclerView.ViewHolder, java.util.List, int, int)}.
         * <p>
         * Default implementation returns true.
         *
         * @param recyclerView The RecyclerView to which WeSwipeHelper is attached to.
         * @param current      The ViewHolder that user is dragging.
         * @param target       The ViewHolder which is below the dragged ViewHolder.
         * @return True if the dragged ViewHolder can be replaced with the target ViewHolder, false
         * otherwise.
         */
        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current,
                                   RecyclerView.ViewHolder target) {
            return true;
        }

        /**
         * Called when WeSwipeHelper wants to move the dragged item from its old position to
         * the new position.
         * <p>
         * If this method returns true, WeSwipeHelper assumes {@code viewHolder} has been moved
         * to the adapter position of {@code target} ViewHolder
         * ({@link RecyclerView.ViewHolder#getAdapterPosition()
         * ViewHolder#getAdapterPosition()}).
         * <p>
         * If you don't support drag & drop, this method will never be called.
         *
         * @param recyclerView The RecyclerView to which WeSwipeHelper is attached to.
         * @param viewHolder   The ViewHolder which is being dragged by the user.
         * @param target       The ViewHolder over which the currently active item is being
         *                     dragged.
         * @return True if the {@code viewHolder} has been moved to the adapter position of
         * {@code target}.
         * @see #onMoved(RecyclerView, RecyclerView.ViewHolder, int, RecyclerView.ViewHolder, int, int, int)
         */
        public abstract boolean onMove(RecyclerView recyclerView,
                                       RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target);

        /**
         * Returns whether WeSwipeHelper should start a drag and drop operation if an item is
         * long pressed.
         * <p>
         * Default value returns true but you may want to disable this if you want to start
         * dragging on a custom view touch using {@link #startDrag(RecyclerView.ViewHolder)}.
         *
         * @return True if WeSwipeHelper should start dragging an item when it is long pressed,
         * false otherwise. Default value is <code>true</code>.
         * @see #startDrag(RecyclerView.ViewHolder)
         */
        public boolean isLongPressDragEnabled() {
            return true;
        }

        /**
         * Returns whether WeSwipeHelper should start a swipe operation if a pointer is swiped
         * over the View.
         * <p>
         * Default value returns true but you may want to disable this if you want to start
         * swiping on a custom view touch using {@link #startSwipe(RecyclerView.ViewHolder)}.
         *
         * @return True if WeSwipeHelper should start swiping an item when user swipes a pointer
         * over the View, false otherwise. Default value is <code>true</code>.
         * @see #startSwipe(RecyclerView.ViewHolder)
         */
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        /**
         * When finding views under a dragged view, by default, WeSwipeHelper searches for views
         * that overlap with the dragged View. By overriding this method, you can extend or shrink
         * the search box.
         *
         * @return The extra margin to be added to the hit box of the dragged View.
         */
        public int getBoundingBoxMargin() {
            return 0;
        }

        /**
         * Returns the fraction that the user should move the View to be considered as swiped.
         * The fraction is calculated with respect to RecyclerView's bounds.
         * <p>
         * Default value is .5f, which means, to swipe a View, user must move the View at least
         * half of RecyclerView's width or height, depending on the swipe direction.
         *
         * @param viewHolder The ViewHolder that is being dragged.
         * @return A float value that denotes the fraction of the View size. Default value
         * is .5f .
         */
        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return .5f;
        }

        /**
         * Returns the fraction that the user should move the View to be considered as it is
         * dragged. After a view is moved this amount, WeSwipeHelper starts checking for Views
         * below it for a possible drop.
         *
         * @param viewHolder The ViewHolder that is being dragged.
         * @return A float value that denotes the fraction of the View size. Default value is
         * .5f .
         */
        public float getMoveThreshold(RecyclerView.ViewHolder viewHolder) {
            return .5f;
        }

        /**
         * Defines the minimum velocity which will be considered as a swipe action by the user.
         * <p>
         * You can increase this value to make it harder to swipe or decrease it to make it easier.
         * Keep in mind that WeSwipeHelper also checks the perpendicular velocity and makes sure
         * current direction velocity is larger then the perpendicular one. Otherwise, user's
         * movement is ambiguous. You can change the threshold by overriding
         * {@link #getSwipeVelocityThreshold(float)}.
         * <p>
         * The velocity is calculated in pixels per second.
         * <p>
         * The default framework value is passed as a parameter so that you can modify it with a
         * multiplier.
         *
         * @param defaultValue The default value (in pixels per second) used by the
         *                     WeSwipeHelper.
         * @return The minimum swipe velocity. The default implementation returns the
         * <code>defaultValue</code> parameter.
         * @see #getSwipeVelocityThreshold(float)
         * @see #getSwipeThreshold(RecyclerView.ViewHolder)
         */
        public float getSwipeEscapeVelocity(float defaultValue) {
            return defaultValue;
        }

        /**
         * Defines the maximum velocity WeSwipeHelper will ever calculate for pointer movements.
         * <p>
         * To consider a movement as swipe, WeSwipeHelper requires it to be larger than the
         * perpendicular movement. If both directions reach to the max threshold, none of them will
         * be considered as a swipe because it is usually an indication that user rather tried to
         * scroll then swipe.
         * <p>
         * The velocity is calculated in pixels per second.
         * <p>
         * You can customize this behavior by changing this method. If you increase the value, it
         * will be easier for the user to swipe diagonally and if you decrease the value, user will
         * need to make a rather straight finger movement to trigger a swipe.
         *
         * @param defaultValue The default value(in pixels per second) used by the WeSwipeHelper.
         * @return The velocity cap for pointer movements. The default implementation returns the
         * <code>defaultValue</code> parameter.
         * @see #getSwipeEscapeVelocity(float)
         */
        public float getSwipeVelocityThreshold(float defaultValue) {
            return defaultValue;
        }

        /**
         * Called by WeSwipeHelper to select a drop target from the list of ViewHolders that
         * are under the dragged View.
         * <p>
         * Default implementation filters the View with which dragged item have changed position
         * in the drag direction. For instance, if the view is dragged UP, it compares the
         * <code>view.getTop()</code> of the two views before and after drag started. If that value
         * is different, the target view passes the filter.
         * <p>
         * Among these Views which pass the test, the one closest to the dragged view is chosen.
         * <p>
         * This method is called on the main thread every time user moves the View. If you want to
         * override it, make sure it does not do any expensive operations.
         *
         * @param selected    The ViewHolder being dragged by the user.
         * @param dropTargets The list of ViewHolder that are under the dragged View and
         *                    candidate as a drop.
         * @param curX        The updated left value of the dragged View after drag translations
         *                    are applied. This value does not include margins added by
         *                    {@link RecyclerView.ItemDecoration}s.
         * @param curY        The updated top value of the dragged View after drag translations
         *                    are applied. This value does not include margins added by
         *                    {@link RecyclerView.ItemDecoration}s.
         * @return A ViewHolder to whose position the dragged ViewHolder should be
         * moved to.
         */
        public RecyclerView.ViewHolder chooseDropTarget(RecyclerView.ViewHolder selected,
                                                        List<RecyclerView.ViewHolder> dropTargets, int curX, int curY) {
            int right = curX + selected.itemView.getWidth();
            int bottom = curY + selected.itemView.getHeight();
            RecyclerView.ViewHolder winner = null;
            int winnerScore = -1;
            final int dx = curX - selected.itemView.getLeft();
            final int dy = curY - selected.itemView.getTop();
            final int targetsSize = dropTargets.size();
            for (int i = 0; i < targetsSize; i++) {
                final RecyclerView.ViewHolder target = dropTargets.get(i);
                if (dx > 0) {
                    int diff = target.itemView.getRight() - right;
                    if (diff < 0 && target.itemView.getRight() > selected.itemView.getRight()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dx < 0) {
                    int diff = target.itemView.getLeft() - curX;
                    if (diff > 0 && target.itemView.getLeft() < selected.itemView.getLeft()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dy < 0) {
                    int diff = target.itemView.getTop() - curY;
                    if (diff > 0 && target.itemView.getTop() < selected.itemView.getTop()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }

                if (dy > 0) {
                    int diff = target.itemView.getBottom() - bottom;
                    if (diff < 0 && target.itemView.getBottom() > selected.itemView.getBottom()) {
                        final int score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
            }
            return winner;
        }

        /**
         * Called when a ViewHolder is swiped by the user.
         * <p>
         * If you are returning relative directions ({@link #START} , {@link #END}) from the
         * {@link #getMovementFlags(RecyclerView, RecyclerView.ViewHolder)} method, this method
         * will also use relative directions. Otherwise, it will use absolute directions.
         * <p>
         * If you don't support swiping, this method will never be called.
         * <p>
         * WeSwipeHelper will keep a reference to the View until it is detached from
         * RecyclerView.
         * As soon as it is detached, WeSwipeHelper will call
         * {@link #clearView(RecyclerView, RecyclerView.ViewHolder)}.
         *
         * @param viewHolder The ViewHolder which has been swiped by the user.
         * @param direction  The direction to which the ViewHolder is swiped. It is one of
         *                   {@link #UP}, {@link #DOWN},
         *                   {@link #LEFT} or {@link #RIGHT}. If your
         *                   {@link #getMovementFlags(RecyclerView, RecyclerView.ViewHolder)}
         *                   method
         *                   returned relative flags instead of {@link #LEFT} / {@link #RIGHT};
         *                   `direction` will be relative as well. ({@link #START} or {@link
         *                   #END}).
         */
        public abstract void onSwiped(RecyclerView.ViewHolder viewHolder, int direction);

        /**
         * Called when the ViewHolder swiped or dragged by the WeSwipeHelper is changed.
         * <p/>
         * If you override this method, you should call super.
         *
         * @param viewHolder  The new ViewHolder that is being swiped or dragged. Might be null if
         *                    it is cleared.
         * @param actionState One of {@link WeSwipeHelper#ACTION_STATE_IDLE},
         *                    {@link WeSwipeHelper#ACTION_STATE_SWIPE} or
         *                    {@link WeSwipeHelper#ACTION_STATE_DRAG}.
         * @see #clearView(RecyclerView, RecyclerView.ViewHolder)
         */
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                sUICallback.onSelected(viewHolder.itemView);
            }
        }

        private int getMaxDragScroll(RecyclerView recyclerView) {
            if (mCachedMaxScrollSpeed == -1) {
                mCachedMaxScrollSpeed = recyclerView.getResources().getDimensionPixelSize(
                        android.support.v7.recyclerview.R.dimen.item_touch_helper_max_drag_scroll_per_frame);
            }
            return mCachedMaxScrollSpeed;
        }

        /**
         * Called when {@link #onMove(RecyclerView, RecyclerView.ViewHolder, RecyclerView.ViewHolder)} returns true.
         * <p>
         * WeSwipeHelper does not create an extra Bitmap or View while dragging, instead, it
         * modifies the existing View. Because of this reason, it is important that the View is
         * still part of the layout after it is moved. This may not work as intended when swapped
         * Views are close to RecyclerView bounds or there are gaps between them (e.g. other Views
         * which were not eligible for dropping over).
         * <p>
         * This method is responsible to give necessary hint to the LayoutManager so that it will
         * keep the View in visible area. For example, for LinearLayoutManager, this is as simple
         * as calling {@link LinearLayoutManager#scrollToPositionWithOffset(int, int)}.
         * <p>
         * Default implementation calls {@link RecyclerView#scrollToPosition(int)} if the View's
         * new position is likely to be out of bounds.
         * <p>
         * It is important to ensure the ViewHolder will stay visible as otherwise, it might be
         * removed by the LayoutManager if the move causes the View to go out of bounds. In that
         * case, drag will end prematurely.
         *
         * @param recyclerView The RecyclerView controlled by the WeSwipeHelper.
         * @param viewHolder   The ViewHolder under user's control.
         * @param fromPos      The previous adapter position of the dragged item (before it was
         *                     moved).
         * @param target       The ViewHolder on which the currently active item has been dropped.
         * @param toPos        The new adapter position of the dragged item.
         * @param x            The updated left value of the dragged View after drag translations
         *                     are applied. This value does not include margins added by
         *                     {@link RecyclerView.ItemDecoration}s.
         * @param y            The updated top value of the dragged View after drag translations
         *                     are applied. This value does not include margins added by
         *                     {@link RecyclerView.ItemDecoration}s.
         */
        public void onMoved(final RecyclerView recyclerView,
                            final RecyclerView.ViewHolder viewHolder, int fromPos, final RecyclerView.ViewHolder target, int toPos, int x,
                            int y) {
            final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof WeSwipeHelper.ViewDropHandler) {
                ((WeSwipeHelper.ViewDropHandler) layoutManager).prepareForDrop(viewHolder.itemView,
                        target.itemView, x, y);
                return;
            }

            // if layout manager cannot handle it, do some guesswork
            if (layoutManager.canScrollHorizontally()) {
                final int minLeft = layoutManager.getDecoratedLeft(target.itemView);
                if (minLeft <= recyclerView.getPaddingLeft()) {
                    recyclerView.scrollToPosition(toPos);
                }
                final int maxRight = layoutManager.getDecoratedRight(target.itemView);
                if (maxRight >= recyclerView.getWidth() - recyclerView.getPaddingRight()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }

            if (layoutManager.canScrollVertically()) {
                final int minTop = layoutManager.getDecoratedTop(target.itemView);
                if (minTop <= recyclerView.getPaddingTop()) {
                    recyclerView.scrollToPosition(toPos);
                }
                final int maxBottom = layoutManager.getDecoratedBottom(target.itemView);
                if (maxBottom >= recyclerView.getHeight() - recyclerView.getPaddingBottom()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }
        }

        void onDraw(Canvas c, RecyclerView parent, RecyclerView.ViewHolder selected,
                    List<RecoverAnimation> recoverAnimationList,
                    int actionState, float dX, float dY) {
            final int recoverAnimSize = recoverAnimationList.size();
            for (int i = 0; i < recoverAnimSize; i++) {
                final WeSwipeHelper.RecoverAnimation anim = recoverAnimationList.get(i);
                anim.update();
                final int count = c.save();
                float swipeWidth = getSwipeWidth(anim.mViewHolder);
                View swipeView = getNeedSwipeLayout(anim.mViewHolder);
                onChildDraw(c, parent, anim.mViewHolder, swipeView, anim.mX, anim.mY, anim.mActionState,
                        false, swipeWidth);
                c.restoreToCount(count);
            }
            if (selected != null) {
                final int count = c.save();
                float swipeWidth = getSwipeWidth(selected);
                View swipeView = getNeedSwipeLayout(selected);
                onChildDraw(c, parent, selected, swipeView, dX, dY, actionState, true, swipeWidth);
                c.restoreToCount(count);
            }
        }

        void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.ViewHolder selected,
                        List<RecoverAnimation> recoverAnimationList,
                        int actionState, float dX, float dY) {
            final int recoverAnimSize = recoverAnimationList.size();
            for (int i = 0; i < recoverAnimSize; i++) {
                final WeSwipeHelper.RecoverAnimation anim = recoverAnimationList.get(i);
                final int count = c.save();
                onChildDrawOver(c, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState,
                        false);
                c.restoreToCount(count);
            }
            if (selected != null) {
                final int count = c.save();
                onChildDrawOver(c, parent, selected, dX, dY, actionState, true);
                c.restoreToCount(count);
            }
            boolean hasRunningAnimation = false;
            for (int i = recoverAnimSize - 1; i >= 0; i--) {
                final WeSwipeHelper.RecoverAnimation anim = recoverAnimationList.get(i);
                if (anim.mEnded && !anim.mIsPendingCleanup) {
                    recoverAnimationList.remove(i);
                } else if (!anim.mEnded) {
                    hasRunningAnimation = true;
                }
            }
            if (hasRunningAnimation) {
                parent.invalidate();
            }
        }

        /**
         * Called by the WeSwipeHelper when the user interaction with an element is over and it
         * also completed its animation.
         * <p>
         * This is a good place to clear all changes on the View that was done in
         * {@link #onSelectedChanged(RecyclerView.ViewHolder, int)},
         * {@link #onChildDraw(Canvas, RecyclerView, RecyclerView.ViewHolder, View, float, float, int,
         * boolean, float)} or
         * {@link #onChildDrawOver(Canvas, RecyclerView, RecyclerView.ViewHolder, float, float, int, boolean)}.
         *
         * @param recyclerView The RecyclerView which is controlled by the WeSwipeHelper.
         * @param viewHolder   The View that was interacted by the user.
         */
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            sUICallback.clearView(viewHolder.itemView);
        }

        /**
         * Called by WeSwipeHelper on RecyclerView's onDraw callback.
         * <p>
         * If you would like to customize how your View's respond to user interactions, this is
         * a good place to override.
         * <p>
         * Default implementation translates the child by the given <code>dX</code>,
         * <code>dY</code>.
         * WeSwipeHelper also takes care of drawing the child after other children if it is being
         * dragged. This is done using child re-ordering mechanism. On platforms prior to L, this
         * is
         * achieved via {@link android.view.ViewGroup#getChildDrawingOrder(int, int)} and on L
         * and after, it changes View's elevation value to be greater than all other children.)
         *
         * @param c                 The canvas which RecyclerView is drawing its children
         * @param recyclerView      The RecyclerView to which WeSwipeHelper is attached to
         * @param viewHolder        The ViewHolder which is being interacted by the User or it was
         *                          interacted and simply animating to its original position
         * @param dX                The amount of horizontal displacement caused by user's action
         * @param dY                The amount of vertical displacement caused by user's action
         * @param actionState       The type of interaction on the View. Is either {@link
         *                          #ACTION_STATE_DRAG} or {@link #ACTION_STATE_SWIPE}.
         * @param isCurrentlyActive True if this view is currently being controlled by the user or
         *                          false it is simply animating back to its original state.
         * @see #onChildDrawOver(Canvas, RecyclerView, RecyclerView.ViewHolder, float, float, int,
         * boolean)
         */
        public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, View swipeView,
                                float dX, float dY, int actionState, boolean isCurrentlyActive, float swipeWidth) {
            sUICallback.onDraw(c, recyclerView, viewHolder.itemView, dX, dY, actionState,
                    isCurrentlyActive);
        }

        /**
         * Called by WeSwipeHelper on RecyclerView's onDraw callback.
         * <p>
         * If you would like to customize how your View's respond to user interactions, this is
         * a good place to override.
         * <p>
         * Default implementation translates the child by the given <code>dX</code>,
         * <code>dY</code>.
         * WeSwipeHelper also takes care of drawing the child after other children if it is being
         * dragged. This is done using child re-ordering mechanism. On platforms prior to L, this
         * is
         * achieved via {@link android.view.ViewGroup#getChildDrawingOrder(int, int)} and on L
         * and after, it changes View's elevation value to be greater than all other children.)
         *
         * @param c                 The canvas which RecyclerView is drawing its children
         * @param recyclerView      The RecyclerView to which WeSwipeHelper is attached to
         * @param viewHolder        The ViewHolder which is being interacted by the User or it was
         *                          interacted and simply animating to its original position
         * @param dX                The amount of horizontal displacement caused by user's action
         * @param dY                The amount of vertical displacement caused by user's action
         * @param actionState       The type of interaction on the View. Is either {@link
         *                          #ACTION_STATE_DRAG} or {@link #ACTION_STATE_SWIPE}.
         * @param isCurrentlyActive True if this view is currently being controlled by the user or
         *                          false it is simply animating back to its original state.
         * @see #onChildDrawOver(Canvas, RecyclerView, RecyclerView.ViewHolder, float, float, int,
         * boolean)
         */
        public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
            sUICallback.onDrawOver(c, recyclerView, viewHolder.itemView, dX, dY, actionState,
                    isCurrentlyActive);
        }

        /**
         * Called by the WeSwipeHelper when user action finished on a ViewHolder and now the View
         * will be animated to its final position.
         * <p>
         * Default implementation uses ItemAnimator's duration values. If
         * <code>animationType</code> is {@link #ANIMATION_TYPE_DRAG}, it returns
         * {@link RecyclerView.ItemAnimator#getMoveDuration()}, otherwise, it returns
         * {@link RecyclerView.ItemAnimator#getRemoveDuration()}. If RecyclerView does not have
         * any {@link RecyclerView.ItemAnimator} attached, this method returns
         * {@code DEFAULT_DRAG_ANIMATION_DURATION} or {@code DEFAULT_SWIPE_ANIMATION_DURATION}
         * depending on the animation type.
         *
         * @param recyclerView  The RecyclerView to which the WeSwipeHelper is attached to.
         * @param animationType The type of animation. Is one of {@link #ANIMATION_TYPE_DRAG},
         *                      {@link #ANIMATION_TYPE_SWIPE_CANCEL} or
         *                      {@link #ANIMATION_TYPE_SWIPE_SUCCESS}.
         * @param animateDx     The horizontal distance that the animation will offset
         * @param animateDy     The vertical distance that the animation will offset
         * @return The duration for the animation
         */
        public long getAnimationDuration(RecyclerView recyclerView, int animationType,
                                         float animateDx, float animateDy) {
            final RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return animationType == ANIMATION_TYPE_DRAG ? DEFAULT_DRAG_ANIMATION_DURATION
                        : DEFAULT_SWIPE_ANIMATION_DURATION;
            } else {
                return animationType == ANIMATION_TYPE_DRAG ? itemAnimator.getMoveDuration()
                        : itemAnimator.getRemoveDuration();
            }
        }

        /**
         * The duration of animation
         *
         * @return
         */
        public long getRecoveryAnimationDuration() {
            return 250;
        }

        /**
         * Called by the WeSwipeHelper when user is dragging a view out of bounds.
         * <p>
         * You can override this method to decide how much RecyclerView should scroll in response
         * to this action. Default implementation calculates a value based on the amount of View
         * out of bounds and the time it spent there. The longer user keeps the View out of bounds,
         * the faster the list will scroll. Similarly, the larger portion of the View is out of
         * bounds, the faster the RecyclerView will scroll.
         *
         * @param recyclerView        The RecyclerView instance to which WeSwipeHelper is
         *                            attached to.
         * @param viewSize            The total size of the View in scroll direction, excluding
         *                            item decorations.
         * @param viewSizeOutOfBounds The total size of the View that is out of bounds. This value
         *                            is negative if the View is dragged towards left or top edge.
         * @param totalSize           The total size of RecyclerView in the scroll direction.
         * @param msSinceStartScroll  The time passed since View is kept out of bounds.
         * @return The amount that RecyclerView should scroll. Keep in mind that this value will
         * be passed to {@link RecyclerView#scrollBy(int, int)} method.
         */
        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView,
                                                int viewSize, int viewSizeOutOfBounds,
                                                int totalSize, long msSinceStartScroll) {
            final int maxScroll = getMaxDragScroll(recyclerView);
            final int absOutOfBounds = Math.abs(viewSizeOutOfBounds);
            final int direction = (int) Math.signum(viewSizeOutOfBounds);
            // might be negative if other direction
            float outOfBoundsRatio = Math.min(1f, 1f * absOutOfBounds / viewSize);
            final int cappedScroll = (int) (direction * maxScroll
                    * sDragViewScrollCapInterpolator.getInterpolation(outOfBoundsRatio));
            final float timeRatio;
            if (msSinceStartScroll > DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS) {
                timeRatio = 1f;
            } else {
                timeRatio = (float) msSinceStartScroll / DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS;
            }
            final int value = (int) (cappedScroll * sDragScrollInterpolator
                    .getInterpolation(timeRatio));
            if (value == 0) {
                return viewSizeOutOfBounds > 0 ? 1 : -1;
            }
            return value;
        }
    }

    /**
     * A simple wrapper to the default Callback which you can construct with drag and swipe
     * directions and this class will handle the flag callbacks. You should still override onMove
     * or
     * onSwiped depending on your use case.
     * <p>
     * <pre>
     * WeSwipeHelper mIth = new WeSwipeHelper(
     *     new WeSwipeHelper.SimpleCallback(WeSwipeHelper.UP | WeSwipeHelper.DOWN,
     *         WeSwipeHelper.LEFT) {
     *         public abstract boolean onMove(RecyclerView recyclerView,
     *             ViewHolder viewHolder, ViewHolder target) {
     *             final int fromPos = viewHolder.getAdapterPosition();
     *             final int toPos = target.getAdapterPosition();
     *             // move item in `fromPos` to `toPos` in adapter.
     *             return true;// true if moved, false otherwise
     *         }
     *         public void onSwiped(ViewHolder viewHolder, int direction) {
     *             // remove from adapter
     *         }
     * });
     * </pre>
     */
    public abstract static class SimpleCallback extends WeSwipeHelper.Callback {

        private int mDefaultSwipeDirs;

        private int mDefaultDragDirs;

        /**
         * Creates a Callback for the given drag and swipe allowance. These values serve as
         * defaults
         * and if you want to customize behavior per ViewHolder, you can override
         * {@link #getSwipeDirs(RecyclerView, RecyclerView.ViewHolder)}
         * and / or {@link #getDragDirs(RecyclerView, RecyclerView.ViewHolder)}.
         *
         * @param dragDirs  Binary OR of direction flags in which the Views can be dragged. Must be
         *                  composed of {@link #LEFT}, {@link #RIGHT}, {@link #START}, {@link
         *                  #END},
         *                  {@link #UP} and {@link #DOWN}.
         * @param swipeDirs Binary OR of direction flags in which the Views can be swiped. Must be
         *                  composed of {@link #LEFT}, {@link #RIGHT}, {@link #START}, {@link
         *                  #END},
         *                  {@link #UP} and {@link #DOWN}.
         */
        public SimpleCallback(int dragDirs, int swipeDirs) {
            mDefaultSwipeDirs = swipeDirs;
            mDefaultDragDirs = dragDirs;
        }

        /**
         * Updates the default swipe directions. For example, you can use this method to toggle
         * certain directions depending on your use case.
         *
         * @param defaultSwipeDirs Binary OR of directions in which the ViewHolders can be swiped.
         */
        public void setDefaultSwipeDirs(int defaultSwipeDirs) {
            mDefaultSwipeDirs = defaultSwipeDirs;
        }

        /**
         * Updates the default drag directions. For example, you can use this method to toggle
         * certain directions depending on your use case.
         *
         * @param defaultDragDirs Binary OR of directions in which the ViewHolders can be dragged.
         */
        public void setDefaultDragDirs(int defaultDragDirs) {
            mDefaultDragDirs = defaultDragDirs;
        }

        /**
         * Returns the swipe directions for the provided ViewHolder.
         * Default implementation returns the swipe directions that was set via constructor or
         * {@link #setDefaultSwipeDirs(int)}.
         *
         * @param recyclerView The RecyclerView to which the WeSwipeHelper is attached to.
         * @param viewHolder   The RecyclerView for which the swipe direction is queried.
         * @return A binary OR of direction flags.
         */
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return mDefaultSwipeDirs;
        }

        /**
         * Returns the drag directions for the provided ViewHolder.
         * Default implementation returns the drag directions that was set via constructor or
         * {@link #setDefaultDragDirs(int)}.
         *
         * @param recyclerView The RecyclerView to which the WeSwipeHelper is attached to.
         * @param viewHolder   The RecyclerView for which the swipe direction is queried.
         * @return A binary OR of direction flags.
         */
        public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return mDefaultDragDirs;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(getDragDirs(recyclerView, viewHolder),
                    getSwipeDirs(recyclerView, viewHolder));
        }
    }

    private static class RecoverAnimation implements Animator.AnimatorListener {

        final float mStartDx;

        final float mStartDy;

        final float mTargetX;

        final float mTargetY;

        final RecyclerView.ViewHolder mViewHolder;

        final int mActionState;

        private final ValueAnimator mValueAnimator;

        final int mAnimationType;

        public boolean mIsPendingCleanup;

        float mX;

        float mY;

        // if user starts touching a recovering view, we put it into interaction mode again,
        // instantly.
        boolean mOverridden = false;

        boolean mEnded = false;

        private float mFraction;

        RecoverAnimation(RecyclerView.ViewHolder viewHolder, int animationType,
                         int actionState, float startDx, float startDy, float targetX, float targetY) {
            mActionState = actionState;
            mAnimationType = animationType;
            mViewHolder = viewHolder;
            mStartDx = startDx;
            mStartDy = startDy;
            mTargetX = targetX;
            mTargetY = targetY;
            mValueAnimator = ValueAnimator.ofFloat(0f, 1f);
            mValueAnimator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            setFraction(animation.getAnimatedFraction());
                        }
                    });
            mValueAnimator.setTarget(viewHolder.itemView);
            mValueAnimator.addListener(this);
            setFraction(0f);
        }

        public void setDuration(long duration) {
            mValueAnimator.setDuration(duration);
        }

        public void start() {
            mViewHolder.setIsRecyclable(false);
            mValueAnimator.start();
        }

        public void cancel() {
            mValueAnimator.cancel();
        }

        public void setFraction(float fraction) {
            mFraction = fraction;
        }

        /**
         * We run updates on onDraw method but use the fraction from animator callback.
         * This way, we can sync translate x/y values w/ the animators to avoid one-off frames.
         */
        public void update() {
            if (mStartDx == mTargetX) {
                mX = mViewHolder.itemView.getTranslationX();
            } else {
                mX = mStartDx + mFraction * (mTargetX - mStartDx);
            }
            if (mStartDy == mTargetY) {
                mY = mViewHolder.itemView.getTranslationY();
            } else {
                mY = mStartDy + mFraction * (mTargetY - mStartDy);
            }
        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!mEnded) {
                mViewHolder.setIsRecyclable(true);
            }
            mEnded = true;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            setFraction(1f); //make sure we recover the view's state.
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

}
