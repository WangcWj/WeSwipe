# WItemTouchHelperPlus

### 效果图   仿qq侧滑</br>

![](https://raw.githubusercontent.com/WangcWj/image-folder/master/slide.gif)

### 效果图   一般侧滑</br>

![](https://raw.githubusercontent.com/WangcWj/image-folder/master/videotogif_2018.05.02_19.02.16.gif)



该demo展示了如何高效的使用系统类ItemTouchHelper结合RecyclerView去实现侧滑删除的功能。考虑到布局多层嵌套跟列表滑动的流畅度，舍弃了自定义view去实现的想法。WItemTouchHelperPlus改进了系统类ItemTouchHelper的功能，实现了如下图所示的两种侧滑方案：

* 图一 侧滑的布局跟随Item滑动。
* 图二 侧滑的布局隐藏在Item布局的下面。

#### 原理：

侧滑就是将指定布局平移一定的距离之后显示你要显示的其它布局，之后将平移之后的布局恢复原位。

#### 使用：

1 在要移动的xml布局中设置tag。

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="50dp"
    android:layout_marginBottom="1dp"
    android:clipChildren="false">

    <RelativeLayout
        android:id="@+id/slide_itemView"
        android:clipChildren="false"
        android:tag="slide_flag"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <TextView
            android:id="@+id/item_text"
            android:layout_width="match_parent"
            android:layout_height="50dp"
            android:background="#e1e1e1"
            android:gravity="center"
            android:text="item"
            android:textColor="#333333"
            android:textSize="16sp" />
       //这个显示"删除" 按钮类似的布局

    </RelativeLayout>

</RelativeLayout>
```



2 在RecyclerView.Adapter中使用的ViewHolder里面或者是别的地方（只要能拿到我们要显示的侧滑布局的View就好），实现SlideSwapAction接口并返回指定的值。

```java

public  class RecViewholder extends RecyclerView.ViewHolder implements SlideSwapAction {
        public TextView textView;//item布局.
        public TextView slide;//显示"删除"布局.

        public RecViewholder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_text);
            slide = itemView.findViewById(R.id.item_slide);

        }
       
        @Override
        public float getActionWidth() {
            //返回侧滑移动的距离,必须写,切注意view.getWidth()得到是否为0.
            return  slide.getWidth();
        }

        @Override
        public View ItemView() {
            //这个是要移动的布局.
            return textView;
        }

    }
```

3 绑定RecyclerView。

需要注意的是，策划的类型不同callback.setType就不一样，如果是跟qq那样展示侧滑的布局跟在item布局的后面那就需要设置type为WItemTouchHelperPlus.SLIDE_ITEM_TYPE_ITEMVIEW。如果item布局是覆盖再侧滑布局的上面则无需设置。

```java
//侧滑布局跟随
 PlusItemSlideCallback callback = new PlusItemSlideCallback();
 callback.setType(WItemTouchHelperPlus.SLIDE_ITEM_TYPE_ITEMVIEW);
 WItemTouchHelperPlus extension = new WItemTouchHelperPlus(callback);
 extension.attachToRecyclerView(recyclerView);

 //侧滑布局被覆盖
 PlusItemSlideCallback callback = new PlusItemSlideCallback();
 WItemTouchHelperPlus extension = new WItemTouchHelperPlus(callback);
 extension.attachToRecyclerView(recyclerView);
```

#### 注意：

当你使用侧滑删除的时候为了避免各种item错乱或者position错乱的问题，建议您选择这样的删除跟刷新方式，建议采用第一种删除刷新方法：

* 采用notifyItemRemoved(position)+ notifyItemRangeChanged(position,data.size()-1)方法，position采用holder.getAdapterPosition()。

1.使用notifyDataSetChange()方法的时候会导致侧滑的布局出现复用的问题。

2.当只使用notifyItemRemoved(position)刷新时会出现该position之后的item的position未刷新，导致删除位置错乱。这是因为我们使用onBindViewHolder()方法里面的参数position，并且大多时候该参数还是final 的，这样就会导致点击事件里面的position位置未刷新。



[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu)



