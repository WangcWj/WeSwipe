### WeSwipe：

`WeSwipe`是帮助`RecyclerView`实现侧滑菜单的功能类，其原理是根据系统帮助类`ItemTouchHelper`实现的。该类支持两种类型的侧滑，具体效果看下面的效果图：

**致谢：** 提出问题的优秀开发者们，同时你提出的问题会在第一时间进行回复，最终`WeSwipe`将会变成你想要的样子~~~

```java
type = SWIPE_ITEM_TYPE_FLOWING //表示侧滑菜单是跟随ItemView的。
type = SWIPE_ITEM_TYPE_DEFAULT //表示侧滑菜单是在ItemView的下面。   
```

**类型为SWIPE_ITEM_TYPE_FLOWING：**

![](https://raw.githubusercontent.com/WangcWj/image-folder/master/slide.gif)

**类型为SWIPE_ITEM_TYPE_DEFAULT：**

![](https://raw.githubusercontent.com/WangcWj/image-folder/master/videotogif_2018.05.02_19.02.16.gif)

#### 使用：[![](https://jitpack.io/v/WangcWj/WeSwipe.svg)](https://jitpack.io/#WangcWj/WeSwipe)

**V 1.0.1 ：** 项目改造完成，省去繁杂的操作，简化使用步骤。侧滑的流畅性提升，仿照`qq`侧滑的展开跟恢复效果改造。  

**敬请期待：**`v 1.0.2 `中将加入侧滑状态的获取；以及侧滑开始、侧滑打开、侧滑关闭各阶段的回调；支持每个`ViewHolder`自己选择是否支持侧滑，并不仅限于设置整个`RecyclerView`。

**一 ：**

项目的根`build.gradle`文件中加入：

```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
}
```

**二 ：**

那个`Module`中要使用`WeSwipe`，就在那个`Module`的`build.gradle`文件中加入：

```groovy
dependencies {
	        implementation 'com.github.WangcWj:WeSwipe:version'
}
```

**三 ：**

确保引入成功之后，在需要侧滑菜单的`RecyclerView`的`ViewHolder`中实现`SwipeLayoutTypeCallBack`接口，并实现特定的方法。这里就要区分滑动菜单的类型，如果类型是`SWIPE_ITEM_TYPE_DEFAULT`，也是默认的类型，你需要做的就是：

```java
public class RecViewHolder extends RecyclerView.ViewHolder 
implements WeSwipeHelper.SwipeLayoutTypeCallBack {

        public TextView textView;
        public TextView slide;

        public RecViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_text);
            slide = itemView.findViewById(R.id.item_slide);

        }
        //侧滑菜单的宽度，也是侧滑的距离。方法1.
        @Override
        public float getSwipeWidth() {
            return slide.getWidth();
        }
        //需要发生滑动的布局。方法2.
        @Override
        public View needSwipeLayout() {
            return textView;
        }
        //未滑动之前展现在屏幕中的布局。方法3.
        @Override
        public View onScreenView() {
            return textView;
        }
    }
```

你的`ViewHolder`必须继承`SwipeLayoutTypeCallBack`接口，并重写三个方法。目的是为了`WeSwipeHelper`可以准确的知道滑动布局的信息。布局文件：

![](https://raw.githubusercontent.com/WangcWj/image-folder/master/swipe2.png)

上面的**方法1**中返回的侧滑的宽度就是`xml`中**红色框1**所标记的`View`的宽度，也就是`删除`按钮的宽度。**方法2**中返回的`View`是图中**红色框2**所标记的，侧滑移动的就是该`View`。**方法3**中返回的`View`也是图中**红色框2**所标记的，在未发生侧滑的时候，屏幕中显示的是该布局。

因为`删除`所表示的侧滑菜单的布局，是被`id`为`item_text`的布局遮挡住的，所以需要将该布局平移一定的距离，才能显示出来侧滑菜单布局。

![](https://raw.githubusercontent.com/WangcWj/image-folder/master/swipe3.png)

以上只是对侧滑类型为`SWIPE_ITEM_TYPE_DEFAULT`的解释。对于类型为`SWIPE_ITEM_TYPE_FLOWING`的就需要自行理解了，或者查看`Demo`。

**四 关联RecyclerView：**

```java
//设置WeSwipe。
WeSwipe.attach(recyclerView);
```

支持设置侧滑类型、侧滑恢复动画的执行事件、是否支持侧滑（限于`RecyclerView`）、是否打开`Debug`。

**最后在强调一下`SwipeLayoutTypeCallBack`接口中的三个方法：**

* `getSwipeWidth()`

  也就是你侧滑平移多长的距离。

* `needSwipeLayout()`

  需要发生平移的布局，只有该布局平移走，才能显示出你的侧滑菜单布局。

* `onScreenView()`

  这个`View`其实是为了在侧滑菜单显示的时候，点击侧滑菜单布局响应点击事件，点击的`View`不是侧滑菜单的话恢复侧滑。所以该View的返回一定要根据现实的情况。

#### 注意：

当你使用侧滑删除的时候为了避免各种`item`错乱或者`position`错乱的问题，建议您选择这样的删除跟刷新方式，建议采用该种刷新方法：

* 采用`notifyItemRemoved(position)+ notifyItemRangeChanged(position,data.size()-1)`方法，`position`采用`holder.getAdapterPosition()`。

**可能出现的问题：**

* 使用`notifyDataSetChange()`方法的时候会导致侧滑的布局出现复用的问题。

* 当只使用`notifyItemRemoved(position)`刷新时会出现该`position`之后的`item`的位置未刷新，导致删除位置错乱。

#### 我们坚持：[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu)，好的身体才是革命的本钱！



