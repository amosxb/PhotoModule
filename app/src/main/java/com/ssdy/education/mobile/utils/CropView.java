package com.ssdy.education.mobile.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;
import com.ssdy.education.mobile.R;

/**
 * 描述：绘制剪切图片
 * 作者：shaobing
 * 时间： 2017/3/21 13:39
 */
public class CropView  extends View{

    private Context mContext;
    //剪切的面积
    private RectF mRect;
    //触摸点大小
    private  int defaultTouch;
    //判断第一个手指当前的位置   1 左上角  2.右上角  3.左下角  4.右下角  5.中间 6.外面
    private int mFristFinger=-1;
    //第一个手指上一次的位置
    private float mLastFristX =-1;
    private float mLastFristY =-1;
    //采样率
    private int rate = 0;

    public CropView(Context context) {
        super(context);
        initView(context,null);
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context,attrs);
    }

    public CropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context,attrs);
    }
    //显示栅格
    private boolean isShowGrid;
    //剪切框为方形
    private boolean isSquare;
    //线条颜色
    private int isLineColor;
    //剪切框为方形
    private float isSquareRadius;
    //栅格高度
    private float gridHeight;
    /**
     * 初始化
     * @param context 上下文
     */
    private void initView(Context context, AttributeSet attrs){
        mContext =context;
        defaultTouch = DisplayMetricsUtil.dip2px(mContext,20);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CropView, 0, 0);
        isShowGrid = typedArray.getBoolean(R.styleable.CropView_show_grid, true);
        isSquare = typedArray.getBoolean(R.styleable.CropView_is_square, false);
        isLineColor = typedArray.getColor(R.styleable.CropView_is_line_color,0xFF5193f8);
        isSquareRadius = typedArray.getDimension(R.styleable.CropView_is_square_radius,DisplayMetricsUtil.dip2px(context,10));
        gridHeight = typedArray.getDimension(R.styleable.CropView_grid_height,DisplayMetricsUtil.dip2px(context,10));
        LogUtil.d("isShowGrid  :"+isShowGrid + "  isSquare :"+isSquare  +"   isLineColor:"+isLineColor+"   isSquareRadius:"+isSquareRadius+"   gridHeight:"+gridHeight);
        typedArray.recycle();
    }

    /**
     * 设置剪切是否为方的
     * @param isSquare true 代表方形
     */
    public void setIsSquare(boolean isSquare){
        this.isSquare =isSquare;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                RectF rectLeftTop= new RectF(mRect.left-defaultTouch,mRect.top-defaultTouch,mRect.left+ defaultTouch,mRect.top+ defaultTouch);
                RectF rectRightTop= new RectF(mRect.right-defaultTouch,mRect.top-defaultTouch,mRect.right+ defaultTouch,mRect.top+ defaultTouch);
                RectF rectLeftButton= new RectF(mRect.left-defaultTouch,mRect.bottom-defaultTouch,mRect.left+ defaultTouch,mRect.bottom+ defaultTouch);
                RectF rectRightButton= new RectF(mRect.right-defaultTouch,mRect.bottom-defaultTouch,mRect.right+ defaultTouch,mRect.bottom+ defaultTouch);
                if(rectLeftTop.contains((int)event.getX(),(int)event.getY())){
                    mFristFinger=1;
                } else if(rectRightTop.contains((int)event.getX(),(int)event.getY())){
                    mFristFinger=2;
                }else if(rectLeftButton.contains((int)event.getX(),(int)event.getY())){
                    mFristFinger=3;
                }else if(rectRightButton.contains((int)event.getX(),(int)event.getY())){
                    mFristFinger=4;
                }else if(mRect.contains((int)event.getX(),(int)event.getY())) {
                    mFristFinger=5;
                }
                else{
                    mFristFinger=6;
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:{
                rate++;
                if(rate==1){
                    rate=0;
                    handleFinger(event,0,mRect,defaultTouch);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                mFristFinger=-1;
                mLastFristX =-1;
                mLastFristY =-1;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
        }
        return  true;
    }


    /**
     * 获取剪切框
     * @return 获取截取的矩形
     */
    public RectF getCropRect(){
        LogUtil.d("getCropRect :"+new Gson().toJson(mRect) +"   "+(mRect.right-mRect.left) +"    "+(mRect.bottom-mRect.top) );
        return mRect;
    }

    /**
     * 手指点击事件处理
     * @param event 事件
     * @param index 0 代表只有一个手指
     * @param mRect 正方形
     * @param defaultTouch 触摸宽度
     */
    private  void handleFinger(MotionEvent event,int index,RectF mRect,int defaultTouch){
        if(mLastFristX==-1 && mLastFristY==-1){
            mLastFristX =(int)event.getX(index);
            mLastFristY =(int)event.getY(index);
            return;
        }
        //获取相对距离
        float dx = event.getX(index)-mLastFristX;
        float dy = event.getY(index)-mLastFristY;
        if(mFristFinger==1){
            if(isSquare){
                if((mRect.left+dy)>=0 &&
                        event.getY(index)+defaultTouch*3<=mRect.bottom &&  (mRect.top+dy)>=0){
                    mRect.left +=dy;
                    mRect.top += dy;
                }
            }else{
                if(event.getX(index)+defaultTouch*3<=mRect.right  && (mRect.left+dx)>=0 ){
                    mRect.left +=dx;
                }
                if(event.getY(index)+defaultTouch*3<=mRect.bottom &&  (mRect.top+dy)>=0){
                    mRect.top += dy;
                }
            }
            invalidate();
        } else if(mFristFinger==2){
            if(isSquare){
                if( (mRect.right-dy)<=DisplayMetricsUtil.getDisplayWidth(mContext) &&
                        event.getY(index)+defaultTouch*3<=mRect.bottom  &&(mRect.top+dy)>=0){
                    mRect.right -=dy;
                    mRect.top +=dy;
                }
            }else{
                if(event.getX(index)-defaultTouch*3>mRect.left  && (mRect.right+dx)<=DisplayMetricsUtil.getDisplayWidth(mContext)){
                    mRect.right +=dx;
                }
                if(event.getY(index)+defaultTouch*3<mRect.bottom  &&(mRect.top+dy)>=0){
                    mRect.top +=dy;
                }
            }

            invalidate();
        }else if(mFristFinger==3){
            if(isSquare){
                if((mRect.left-dy)>=0 &&
                        event.getY(index)-defaultTouch*3>mRect.top  &&(mRect.bottom+dy)<=(DisplayMetricsUtil.getDisplayHeight(mContext)-DisplayMetricsUtil.dip2px(mContext,120))){
                    mRect.left -=dy;
                    mRect.bottom +=dy;
                }
            }else{
                if(event.getX(index)+defaultTouch*3<mRect.right && (mRect.left+dx)>=0){
                    mRect.left +=dx;
                }
                if(event.getY(index)-defaultTouch*3>mRect.top  &&(mRect.bottom+dy)<=(DisplayMetricsUtil.getDisplayHeight(mContext)-DisplayMetricsUtil.dip2px(mContext,120))){
                    mRect.bottom +=dy;
                }
            }

            invalidate();
        }else if(mFristFinger==4){
            if(isSquare){
                if((mRect.right+dy)<=DisplayMetricsUtil.getDisplayWidth(mContext) &&
                        event.getY(index)-defaultTouch*3>mRect.top  &&(mRect.bottom+dy)<=(DisplayMetricsUtil.getDisplayHeight(mContext)-DisplayMetricsUtil.dip2px(mContext,120))){
                    mRect.right +=dy;
                    mRect.bottom += dy;
                }
            }else{
                if(event.getX(index)-defaultTouch*3>mRect.left  && (mRect.right+dx)<=DisplayMetricsUtil.getDisplayWidth(mContext)){
                    mRect.right +=dx;
                }
                if(event.getY(index)-defaultTouch*3>mRect.top  &&(mRect.bottom+dy)<=(DisplayMetricsUtil.getDisplayHeight(mContext)-DisplayMetricsUtil.dip2px(mContext,120))){
                    mRect.bottom += dy;
                }
            }

            invalidate();
        }else if(mFristFinger==5) {
            if(mLastFristX!=-1 && mLastFristY!=-1){

                if(mRect.left+dx>=0 && (mRect.right+dx<=DisplayMetricsUtil.getDisplayWidth(mContext))){
                    mRect.left   +=dx;
                    mRect.right  +=dx;
                }else if(mRect.left+dx<0){
                    mRect.right =mRect.right-mRect.left;
                    mRect.left = 0;
                }else if(mRect.right+dx>DisplayMetricsUtil.getDisplayWidth(mContext)){
                    mRect.left =DisplayMetricsUtil.getDisplayWidth(mContext) -(mRect.right - mRect.left);
                    mRect.right = DisplayMetricsUtil.getDisplayWidth(mContext);
                }
                if(mRect.top+dy>=0 && (mRect.bottom+dy<=(DisplayMetricsUtil.getDisplayHeight(mContext))-DisplayMetricsUtil.dip2px(mContext,120)) ){
                    mRect.top    +=dy;
                    mRect.bottom +=dy;
                }else if(mRect.top+dy<0){
                    mRect.bottom =mRect.bottom-mRect.top;
                    mRect.top = 0;
                }else if( (mRect.bottom+dy>(DisplayMetricsUtil.getDisplayHeight(mContext))-DisplayMetricsUtil.dip2px(mContext,120))){
                    mRect.top =DisplayMetricsUtil.getDisplayHeight(mContext)-DisplayMetricsUtil.dip2px(mContext,120)- (mRect.bottom -mRect.top);
                    mRect.bottom =DisplayMetricsUtil.getDisplayHeight(mContext)-DisplayMetricsUtil.dip2px(mContext,120);
                }
            }
            invalidate();
        }
        else{
            mFristFinger=6;
        }
        mLastFristX =event.getX(index);
        mLastFristY =event.getY(index);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDarkenedSurroundingArea(canvas,mRect);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(isSquare){
            //是方形的
            float x =((right-CropView.this.getPaddingRight())-(left+CropView.this.getPaddingLeft()))/2.0f;
            float y =((bottom-CropView.this.getPaddingBottom())-(top+CropView.this.getPaddingTop()))/2.0f;
            mRect = new RectF( x-isSquareRadius,y- isSquareRadius,x+isSquareRadius ,y+isSquareRadius);
        }else{
            float mTop = bottom/2.0f-(int)gridHeight/2.0f;
            float  mBottom = bottom/2.0f+(int)gridHeight/2.0f;
            mRect = new RectF( left+CropView.this.getPaddingLeft(),  mTop+CropView.this.getPaddingTop(), right-CropView.this.getPaddingRight(),  mBottom-CropView.this.getPaddingBottom());
        }
        LogUtil.d("mRect :"+new Gson().toJson(mRect));
    }

    /**
     * 绘制剪切矩形框
     * @param canvas 画布
     * @param mRect 绘制的矩形
     */
    private void drawDarkenedSurroundingArea( Canvas canvas,RectF mRect) {
        Paint paint = new Paint();
        paint.setColor(isLineColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(DisplayMetricsUtil.dip2px(mContext,1));
        //绘制外边框
        canvas.drawRect(mRect.left+DisplayMetricsUtil.dip2px(mContext,1),mRect.top,mRect.right-DisplayMetricsUtil.dip2px(mContext,1),mRect.bottom,paint);

        if(isShowGrid){
            //竖线
            canvas.drawLine((mRect.right-mRect.left)/3+mRect.left,mRect.top,(mRect.right-mRect.left)/3+mRect.left,mRect.bottom,paint);
            canvas.drawLine((mRect.right-mRect.left)*2/3+mRect.left,mRect.top,(mRect.right-mRect.left)*2/3+mRect.left,mRect.bottom,paint);
            //横线
            canvas.drawLine(mRect.left,mRect.top+(mRect.bottom-mRect.top)/3,mRect.right,mRect.top+(mRect.bottom-mRect.top)/3,paint);
            canvas.drawLine(mRect.left,mRect.top+(mRect.bottom-mRect.top)*2/3,mRect.right,mRect.top+(mRect.bottom-mRect.top)*2/3,paint);
        }
        //绘制四个角  左上角
        int defaultWidth =DisplayMetricsUtil.dip2px(mContext,4);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(mRect.left,mRect.top,mRect.left+defaultWidth,mRect.top+DisplayMetricsUtil.dip2px(mContext,20),paint);//左上角
        canvas.drawRect( mRect.left,mRect.top,mRect.left+DisplayMetricsUtil.dip2px(mContext,20),mRect.top+defaultWidth,paint);

        canvas.drawRect(mRect.left,mRect.bottom-DisplayMetricsUtil.dip2px(mContext,20),mRect.left+defaultWidth,mRect.bottom,paint);//左下角
        canvas.drawRect(mRect.left,mRect.bottom-defaultWidth,mRect.left+DisplayMetricsUtil.dip2px(mContext,20),mRect.bottom,paint);

        canvas.drawRect(mRect.right-defaultWidth,mRect.top,mRect.right,mRect.top+DisplayMetricsUtil.dip2px(mContext,20),paint);//右上角
        canvas.drawRect(mRect.right-DisplayMetricsUtil.dip2px(mContext,20),mRect.top,mRect.right,mRect.top+defaultWidth,paint);

        canvas.drawRect(mRect.right-defaultWidth,mRect.bottom-DisplayMetricsUtil.dip2px(mContext,20),mRect.right,mRect.bottom,paint);//右下角
        canvas.drawRect(mRect.right-DisplayMetricsUtil.dip2px(mContext,20),mRect.bottom-defaultWidth,mRect.right,mRect.bottom,paint);
    }
}
