package com.example.horizontalscrollview.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

//支持横向滑动的自定义布局，处理了滑动冲突
public class HorizontalScrollview extends ViewGroup {
    //记录上次的坐标
    int lastX,lastY;

    Scroller scroller;
    VelocityTracker tracker;

    //子view内容的最大宽度，高度
    int maxHeight,maxWidth;

    //子view开始布局的位置
    int left = getPaddingLeft();

    //储存每个子view的位置信息
    private List<ViewLocation> viewLocationList = new ArrayList<>();

    public HorizontalScrollview(Context context) {
        super(context);
        init();
    }

    public HorizontalScrollview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HorizontalScrollview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    //初始化
    private void init(){
        scroller = new Scroller(getContext());
        tracker = VelocityTracker.obtain();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        measureChildren(widthMeasureSpec,heightMeasureSpec);
        int childCount = getChildCount();
        maxHeight=0;
        maxWidth=0;
        for(int i = 0;i<childCount;i++){
            View v = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            //记录子view信息，方便布局
            setLocation(v,lp);
        }
        maxHeight += getPaddingBottom()+getPaddingTop();
        maxWidth  += getPaddingRight();
        //使wrap_content属性起作用
        if(getLayoutParams().width == LayoutParams.WRAP_CONTENT && getLayoutParams().height == LayoutParams.WRAP_CONTENT){
            int mWidth = Math.min(maxWidth,widthSize);
            setMeasuredDimension(mWidth,maxHeight);
        }else if(getLayoutParams().height == LayoutParams.WRAP_CONTENT){
            setMeasuredDimension(widthSize,maxHeight);
        }else if(getLayoutParams().width == LayoutParams.WRAP_CONTENT){
            int mWidth = Math.min(maxWidth,widthSize);
            setMeasuredDimension(mWidth,heightSize);
        }else {
            setMeasuredDimension(widthSize,heightSize);
        }
    }

    //保存各view的位置参数（处理margin）
    private void setLocation(View v,MarginLayoutParams lp){
        ViewLocation mLocation = new ViewLocation();
        mLocation.setLeft(left+lp.leftMargin);
        mLocation.setRight(mLocation.getLeft()+v.getMeasuredWidth());
        mLocation.setTop(getPaddingTop()+lp.topMargin);
        mLocation.setBottom(mLocation.getTop()+v.getMeasuredHeight());
        maxWidth += mLocation.getRight()+lp.rightMargin-mLocation.getLeft()+lp.leftMargin;
        left += mLocation.getRight()+lp.rightMargin-mLocation.getLeft()+lp.leftMargin;
        maxHeight = (mLocation.getBottom()-mLocation.getTop()+lp.bottomMargin+lp.topMargin)>=maxHeight?mLocation.getBottom()-mLocation.getTop()+lp.bottomMargin+lp.topMargin:maxHeight;
        viewLocationList.add(mLocation);
    }

    //处理滑动冲突，判断是否拦截事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Boolean intercept = false;
        int x = (int)ev.getX();
        int y = (int)ev.getY();
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                intercept = false;
                if(!scroller.isFinished()){
                    scroller.abortAnimation();
                    intercept =true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = x-lastX;
                int deltaY = y-lastY;
                if(Math.abs(deltaX)>Math.abs(deltaY)){
                    intercept = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercept = false;
                break;
            default:
                break;
        }
        lastX = x;
        lastY = y;
        return intercept;

    }

    //滑动事件处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        tracker.addMovement(event);
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(!scroller.isFinished()){
                    scroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = x-lastX;
                int deltaY = y-lastY;
                //每次进行滑动限制
                scrollBy(-scrollLimit(deltaX),0);
                break;
            case MotionEvent.ACTION_UP:
                int dx=0;
                //处理快速滑动
                tracker.computeCurrentVelocity(1000);
                float xVelocity = tracker.getXVelocity();
                if(Math.abs(xVelocity)>=50){
                       dx = 0-scrollLimit((int)xVelocity);
                }
                //使用Scroller
                smoothScrollBy(dx,0);
                tracker.clear();
                break;
        }
        lastX = x;
        lastY = y;
        return true;
    }

    //对子view进行布局
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        for(int i = 0;i < childCount;i++){
            View v = getChildAt(i);
            v.layout(viewLocationList.get(i).getLeft(),viewLocationList.get(i).getTop(),viewLocationList.get(i).getRight(),viewLocationList.get(i).getBottom());
        }
    }

    private void smoothScrollBy(int dx,int dy){
        scroller.startScroll(getScrollX(),0,dx,0,500);
        invalidate();
    }

    //限制滑动距离
    private int scrollLimit(int delta){
        //子view总长度小于布局宽度，禁止滑动
        if(maxWidth-getWidth()<=0){
            return 0;
        }else {
            if (delta <= 0) {
                //左滑
                if (getScrollX() == maxWidth - getWidth()) {
                    //处于最右边，右边缘可见 ，禁止继续左滑
                    return 0;
                }else{
                    //限制滑动距离，使左滑不超过子view内容的右边缘
                    int dx = Math.min(maxWidth - getWidth() - getScrollX(), Math.abs(delta));
                    return 0 - dx;
                }

            } else {
                //右滑
                if (getScrollX() == 0) {
                    //处于开始状态，左边缘可见，禁止继续右滑
                    return 0;
                }else{
                    //限制滑动距离，使右滑不超过子view内容的左边缘
                    return Math.min(Math.abs(getScrollX()),delta);
                }
            }

        }
    }

    @Override
    public void computeScroll() {
        if(scroller.computeScrollOffset()){
            scrollTo(scroller.getCurrX(),scroller.getCurrY());
            postInvalidate();
        }
    }

    //资源回收
    @Override
    protected void onDetachedFromWindow() {
        tracker.recycle();
        super.onDetachedFromWindow();
    }

    //为获取子view margin属性，重写方法（否则会报错）
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(),attrs);
    }
}
