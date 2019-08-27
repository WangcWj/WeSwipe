package cn.we.swipe.helper;


import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * 该项目已在**信用卡上应用,采用的是{@link WeSwipeHelper#SWIPE_ITEM_TYPE_DEFAULT}
 * 类型的侧滑删除,目前并无用户反馈bug.
 * <p>
 * QQ的侧滑菜单沿用至今,说明该模式确实深受用户的喜爱,该项目目前是模仿qq的侧滑菜单效果,包括其关闭侧滑的方式.
 * 但有一个缺点就是该帮助类,将会拦截RecyclerView上的一切事件,也就是你的Item在侧滑菜单栏打开的状态下,只能通过特殊的方法去触发点击事件,
 * 不会触发MOVE事件或者是LongClick事件(有需求的话是可以添加的).该目的就是在侧滑菜单栏打开的时候,你应该做的仅仅是操作菜单栏,并不是
 * 去操作ItemView.当没有任何一个Item是处理菜单栏打开的状态下的话,不会拦截任何事件.
 * <p>
 * 项目地址: https://github.com/WangcWj/SideslippingDemo。
 * <p>
 * 使用:
 * <pre>
 *     WeSwipe.attach(recyclerView)
 *                 .isDebug(true)
 *                 .setType(WeSwipeHelper.SWIPE_ITEM_TYPE_FLOWING);
 *     </>
 *
 * 如有什么需求可以直接项目的Issues中提出,只要你提,项目就会变成你想要的样子~
 *
 * @author WANG
 * @date 2019.8.26
 */
public class WeSwipe {

    private final String TAG = "WeSwipe";

    private final int FLAG_MASK = 0x1;

    private int FLAG = 0x0;

    private int mPrivateFlag;

    private int mDuration;

    /**
     * 帮助类
     */
    private WeSwipeHelper mSwipeHelper;


    /**
     * 是否支持侧滑
     *
     * @return
     */
    public boolean swipeEnable() {
        haveInit();
        return mSwipeHelper.swipeEnable();
    }

    public boolean isDebug() {
        return (mPrivateFlag & FLAG_MASK << 3) != 0;
    }

    /**
     * {@link WeSwipeHelper}
     *
     * @param rec 目标RecyclerView.
     * @return
     */
    public static WeSwipe attach(RecyclerView rec) {
        if (null == rec) {
            throw new NullPointerException("WeSwipe : RecyclerView cannot be null !");
        }
        return new WeSwipe().init(rec);
    }

    /**
     * 设置该RecyclerView是否支持侧滑菜单栏.
     *
     * @param enable
     * @return
     */
    public WeSwipe setEnable(boolean enable) {
        if (!haveInit()) {
            return this;
        }
        if (enable) {
            mPrivateFlag |= FLAG_MASK << 2;
        }
        WeSwipeHelper.Callback callback = mSwipeHelper.getCallback();
        if (callback instanceof WeSwipeCallback) {
            ((WeSwipeCallback) callback).setSwipeEnable(enable);
        }
        return this;
    }

    /**
     * 设置侧滑恢复时,动画执行的时间.
     *
     * @param animationDuration
     * @return
     */
    public WeSwipe setRecoverAnimationDuration(int animationDuration) {
        if (!haveInit()) {
            return this;
        }
        mDuration = animationDuration;
        WeSwipeHelper.Callback callback = mSwipeHelper.getCallback();
        if (callback instanceof WeSwipeCallback) {
            ((WeSwipeCallback) callback).setRecoverAnimationDuration(animationDuration);
        }
        return this;
    }

    /**
     * 目前支持给RecyclerView设置两种侧滑菜单。
     * {@link WeSwipeHelper#SWIPE_ITEM_TYPE_FLOWING}。
     * {@link WeSwipeHelper#SWIPE_ITEM_TYPE_DEFAULT}。
     *
     * @param type
     * @return
     */
    public WeSwipe setType(int type) {
        if (!haveInit()) {
            return this;
        }
        if ((type & WeSwipeHelper.SWIPE_ITEM_TYPE) != 0) {
            mSwipeHelper.setItemSlideType(type);
        } else {
            if (isDebug()) {
                Log.e(TAG, "Please choose the right type: SWIPE_ITEM_TYPE_DEFAULT Or SWIPE_ITEM_TYPE_FLOWING .");
            }
        }
        return this;
    }

    /**
     * 是否开启Debug模式.在生产环境下面默认是不不打开的.
     *
     * @param use true 打开debug.
     * @return
     */
    public WeSwipe isDebug(boolean use) {
        boolean debug = BuildConfig.DEBUG;
        if (!debug) {
            use = false;
        }
        if (use) {
            mPrivateFlag |= FLAG_MASK << 3;
        } else {
            mPrivateFlag &= ~(FLAG_MASK << 3);
        }
        return this;
    }

    /**
     * 初始化.
     *
     * @param rec
     * @return
     */
    private WeSwipe init(RecyclerView rec) {
        mPrivateFlag = FLAG;
        WeSwipeCallback mCallback = new WeSwipeCallback();
        mSwipeHelper = new WeSwipeHelper(mCallback);
        mSwipeHelper.attachToRecyclerView(rec);
        mPrivateFlag |= FLAG_MASK << 1;
        return this;
    }

    private boolean haveInit() {
        if ((mPrivateFlag & (FLAG_MASK << 1)) == 0) {
            throw new NullPointerException("WeSwipe : An unexpected error occurred in the init process !");
        }
        if (null == mSwipeHelper) {
            return false;
        }
        return true;
    }
}
