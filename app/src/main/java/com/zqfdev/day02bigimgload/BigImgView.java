package com.zqfdev.day02bigimgload;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.Nullable;

/**
 * @author zhangqingfa
 * @createDate 2020/10/20 14:56
 * @description 自定义加载超级大图-长图
 */
public class BigImgView extends View implements GestureDetector.OnGestureListener
        , View.OnTouchListener, GestureDetector.OnDoubleTapListener {

    //图片的宽
    private int mImgWidth;
    //图片的高
    private int mImgHeight;
    //控件的宽
    private int mViewWidth;
    //控件的高
    private int mViewHeight;

    //复用的Bitmap
    private Bitmap bitmap;

    //用于分块加载，需要跟 BitmapRegionDecoder 配合使用
    private Rect mRect;

    //用于对Bitmap的加载方式进行配置,这是Bitmap实现高效（低内存）加载需要用的一个核心类
    private BitmapFactory.Options mOptions;

    //区域解码器
    private BitmapRegionDecoder mBitmapRegionDecoder;

    //得到缩放因子的范围值 就是原始图片相对控件大小的缩放
    private float mScale;


    //------------------------------------滑动必然需要手势---------------
    //滑动相关处理类
    private Scroller scroller;
    //手势支持处理类
    private GestureDetector mGestureDetector;
    //缩放手势
    private ScaleGestureDetector mScaleGestureDetector;

    private float mOriginalScale;


    public BigImgView(Context context) {
        this(context, null);
    }

    public BigImgView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigImgView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        /**
         * 第一步：初始化需要用的对象
         */
        //内存复用
        mOptions = new BitmapFactory.Options();
        //分块加载
        mRect = new Rect();

        mGestureDetector = new GestureDetector(context, this);

        scroller = new Scroller(context);

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGesture());

        setOnTouchListener(this);

    }

    //传递图片
    public void setImg(InputStream is) {
        //只获取属性，内存不加载实际图片
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, mOptions);
        mImgWidth = mOptions.outWidth;
        mImgHeight = mOptions.outHeight;
        Log.e("Tag", "图片的宽-----" + mImgWidth);
        Log.e("Tag", "图片的高-----" + mImgHeight);
        //加载入内存
        mOptions.inJustDecodeBounds = false;
        //开启复用
        mOptions.inMutable = true;
        //设置图片的解析格式
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        //初始化区域解码器，解码器有了Bitmap的流，待会儿就可以从解码器中，获取特定区域的图片了，这个特定区域便是由Rect指定的。
        try {
            mBitmapRegionDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //到这里，图片也有了，解码器也有了，mOptions也设置好了，万事俱备，只欠绘制

        // 当然，绘制前还得测量，然后进行绘制
        requestLayout();
    }

    /**
     * 第三步：测量
     * 用于获取控件宽高、缩放比，然后计算出取图片的区域Rect
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //首先获取到控件的宽高
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();
        Log.e("Tag", "控件的宽-----" + mViewWidth);
        Log.e("Tag", "控件的高-----" + mViewHeight);
        //缩放比例，这里得注意 把 mImgWidth 转为float类型，缩放小数位是需要的，不然不准
        mOriginalScale = mViewWidth / (float) mImgWidth;
        mScale = mOriginalScale;
//        mScale = mViewHeight / (float) mImgHeight;
        Log.e("Tag", "缩放比例-----" + mScale);
        //计算分块解析【图片】的范围
        mRect.top = 0;
        mRect.left = 0;
//        mRect.right = mImgWidth;
//        mRect.bottom = (int) (mViewHeight / mScale);
        mRect.right = Math.min(mImgWidth, mViewWidth);
        mRect.bottom = Math.min(mImgHeight, mViewHeight);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmapRegionDecoder == null) {
            return;
        }
        // 设置内存复用
        mOptions.inBitmap = bitmap;
        // 然后就是获取要显示的图片了
        bitmap = mBitmapRegionDecoder.decodeRegion(mRect, mOptions);
        // 在显示前，我们要进行一次缩放
        // 看，在这里设置了 对宽高进行一次缩放，因此啊，前面 mRect.bottom = (int) (mViewHeight / mScale);也就懂了。
        Matrix mMatrix = new Matrix();
        float tempScale = mViewWidth / (float) mRect.width();
        mMatrix.setScale(tempScale, tempScale);
        // 绘制图片
        canvas.drawBitmap(bitmap, mMatrix, null);
    }

    //第五步：处理touch事件
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //交给 mGestureDetector 进行处理
        mGestureDetector.onTouchEvent(motionEvent);
        mScaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    //第六步
    @Override
    public boolean onDown(MotionEvent motionEvent) {
        //点击事件 如果滑动中点击则滑动停止
        if (scroller.isFinished()) {
            scroller.forceFinished(true);
        }
        return true;
    }


    /**
     * 第七步：处理滑动事件
     * 滑动的本质是改变Rect的区域，然后重新绘制，也就达到了滑动的效果
     * 此View是要改变 mRect.top 和 mRect.bottom
     * <p>
     * 滑动的时候无非就是改变Rect分块加载的区域
     *
     * @param motionEvent  开始事件
     * @param motionEvent1 即时事件
     * @param distanceX
     * @param distanceY
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
        //上下移动时，改变Rect分块加载显示区域
        mRect.offset(0, (int) distanceY);
        if (mRect.bottom > mImgHeight) {
            mRect.bottom = mImgHeight;
            mRect.top = mImgHeight - (int) (mViewHeight / mScale);
        }
        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = (int) (mViewHeight / mScale);
        }

        if (mRect.right > mImgWidth) {
            mRect.right = mImgWidth;
            mRect.left = mImgWidth - (int) (mViewWidth / mScale);
        }
        if (mRect.left < 0) {
            mRect.left = 0;
            mRect.right = (int) (mViewWidth / mScale);
        }


        //开始绘制
        invalidate();
        return false;
    }

    /**
     * 第八步：处理惯性问题。呃，我怎么发现这个方法就没执行嘛。。。先放着吧，有空再分析下
     *
     * @param e1
     * @param e2
     * @param velocityX
     * @param velocityY
     * @return
     */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        scroller.fling(0, mRect.top
//                , 0, (int) -velocityY
//                , 0, 0
//                , 0, mImgHeight - (int) (mViewHeight / mScale));
        scroller.fling(mRect.left, mRect.top, (int) velocityX, (int) -velocityY, 0,
                mImgWidth - (int) (mViewWidth / mScale), 0,
                mImgHeight - (int) (mViewHeight / mScale));
        return false;
    }

    /**
     * 第九步：处理计算结果
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        //停止滑动了就不计算
        if (scroller.isFinished()) {
            return;
        }
        if (scroller.computeScrollOffset()) {
            mRect.top = scroller.getCurrY();
            mRect.bottom = mRect.top + (int) (mViewHeight / mScale);
            invalidate();
        }
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }


    // 处理缩放的回调事件
    class ScaleGesture extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = mScale;
            scale += detector.getScaleFactor() - 1;
            if (scale <= mOriginalScale) {
                scale = mOriginalScale;
            } else if (scale > mOriginalScale * 1.5) {
                scale = (float) (mOriginalScale * 1.5);
            }
            mRect.right = mRect.left + (int) (mViewWidth / scale);
            mRect.bottom = mRect.top + (int) (mViewHeight / scale);
            mScale = scale;
            invalidate();
            return super.onScale(detector);
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        //当处于正常情况  双击时 mScale < mOriginalScale * 1.5  此时1.5值可随意调整，看自己喜好
        if (mScale < mOriginalScale * 1.5) {
            mScale = (float) (mOriginalScale * 1.5);
        } else {
            //当处于放大情况  双击时 mScale = mOriginalScale
            mScale = mOriginalScale;
        }
        mRect.right = mRect.left + (int) (mViewWidth / mScale);
        mRect.bottom = mRect.top + (int) (mViewHeight / mScale);
        // 移动时，处理到达顶部和底部的情况
        if (mRect.bottom > mImgHeight) {
            mRect.bottom = mImgHeight;
            mRect.top = mImgHeight - (int) (mViewHeight / mScale);
        }
        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = (int) (mViewHeight / mScale);
        }
        if (mRect.right > mImgWidth) {
            mRect.right = mImgWidth;
            mRect.left = mImgWidth - (int) (mViewWidth / mScale);
        }
        if (mRect.left < 0) {
            mRect.left = 0;
            mRect.right = (int) (mViewWidth / mScale);
        }
        invalidate();
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }
}
