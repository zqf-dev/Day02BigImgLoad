package com.zqfdev.day02bigimgload;

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

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class MNBigView extends View implements GestureDetector.OnGestureListener,
        View.OnTouchListener, GestureDetector.OnDoubleTapListener {

    private BitmapFactory.Options mOptions;
    private GestureDetector mGestureDetector;
    private Scroller mScroller;
    private ScaleGestureDetector mScaleGestureDetector;
    private Rect mRect;
    private int mImageWidth;
    private int mImageHeight;
    private BitmapRegionDecoder mDecoder;
    private int mViewWidth;
    private int mViewHeight;
    private float mScale;
    private Bitmap mBitmap;
    private float originalScale;

    public MNBigView(Context context) {
        this(context, null);
    }

    public MNBigView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MNBigView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 设置bigView的成员变量
        mRect = new Rect();
        mOptions = new BitmapFactory.Options();
        mGestureDetector = new GestureDetector(context, this);
        mScroller = new Scroller(context);
        // 添加缩放手势识别
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGesture());
        setOnTouchListener(this);
    }

    public void setImage(InputStream is) {
        // 不传path mipmap url Bitmap
        // 获取图片的宽和高；
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, mOptions);
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;
        mOptions.inJustDecodeBounds = false;
        // 开启复用
        mOptions.inMutable = true;
        // 设置图片格式
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        // 图片由像素点组成；像素点由什么组成；argb 透明通道 红色 绿色 蓝色；
        // 创建区域解码器
        try {
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();
        // 确定图片的加载区域；
//        mRect.left = 0;
//        mRect.top = 0;
//        mRect.right = mImageWidth;
//        mScale = mViewWidth/(float)mImageWidth;
//        mRect.bottom = (int)(mViewHeight/mScale);

        // 加入缩放因子之后的逻辑
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = Math.min(mImageWidth, mViewWidth);
        Log.e("xxxxxxx", mImageWidth + "  " + mViewWidth);
        mRect.bottom = Math.min(mImageHeight, mViewHeight);

        // 再定义一个缩放因子
        originalScale = mViewWidth / (float) mImageWidth;
        mScale = originalScale;
    }

    private float tempScale;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDecoder == null) {
            return;
        }
        // 复用内存
        mOptions.inBitmap = mBitmap;
        mBitmap = mDecoder.decodeRegion(mRect, mOptions);
        Matrix matrix = new Matrix();
        // 缩放因子之前是固定的，现在不是；随时改变；
        tempScale = mViewWidth / (float) mRect.width();
        matrix.setScale(tempScale, tempScale);
        canvas.drawBitmap(mBitmap, matrix, null);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mRect.offset((int) distanceX, (int) distanceY);
        // 移动时，处理到达顶部和底部的情况
        if (mRect.bottom > mImageHeight) {
            mRect.bottom = mImageHeight;
            mRect.top = mImageHeight - (int) (mViewHeight / mScale);
        }
        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = (int) (mViewHeight / mScale);
        }
        if (mRect.right > mImageWidth) {
            mRect.right = mImageWidth;
            mRect.left = mImageWidth - (int) (mViewWidth / mScale);
        }
        if (mRect.left < 0) {
            mRect.left = 0;
            mRect.right = (int) (mViewWidth / mScale);
        }
        invalidate();
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        mScroller.fling(0,mRect.top,0,(int)-velocityY,0,0,0,
//                mImageHeight-(int)(mViewHeight/mScale));
        mScroller.fling(mRect.left, mRect.top, (int) velocityX, (int) -velocityY, 0,
                mImageWidth - (int) (mViewWidth / mScale), 0,
                mImageHeight - (int) (mViewHeight / mScale));
        return false;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.isFinished()) {
            return;
        }
        if (mScroller.computeScrollOffset()) {
            mRect.top = mScroller.getCurrY();
            mRect.bottom = mRect.top + (int) (mViewHeight / mScale);
            invalidate();
        }
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // 双击放大图片；
        if (mScale < originalScale * 2) {
            mScale = originalScale * 2;
        } else {
            mScale = originalScale;
        }

        Log.e("xxxxx mScale ", "mScale   = " + mScale + "   originalScale = " + originalScale);
        mRect.right = mRect.left + (int) (mViewWidth / mScale);
        mRect.bottom = mRect.top + (int) (mViewHeight / mScale);


        // 移动时，处理到达顶部和底部的情况
        if (mRect.bottom > mImageHeight) {
            mRect.bottom = mImageHeight;
            mRect.top = mImageHeight - (int) (mViewHeight / mScale);
        }
        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = (int) (mViewHeight / mScale);
        }
        if (mRect.right > mImageWidth) {
            mRect.right = mImageWidth;
            mRect.left = mImageWidth - (int) (mViewWidth / mScale);
        }
        if (mRect.left < 0) {
            mRect.left = 0;
            mRect.right = (int) (mViewWidth / mScale);
        }
        Log.e("xxxxx Rect ", "mRect.Left   = " + mRect.left +
                "   mRect.right = " + mRect.right +
                "   mRect.top = " + mRect.top +
                "   mRect.bottom = " + mRect.bottom);
        invalidate();
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    // 处理缩放的回调事件
    class ScaleGesture extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = mScale;
            scale += detector.getScaleFactor() - 1;// getScaleFactor()跟上一次事件相比得到的事件因子；
            // getScaleFactor初始值=1；
            // 以下是日志打印结果，mScale它是在变化的，而getScaleFactor()的值也是实时变化的，还需要将变化的值即时赋给mScale；
//            detector = 1.0
//            detector = 1.0241059
//            detector = 1.0649118
//            detector = 1.0915182
//            detector = 1.1195749
            Log.e("xxxxxx", "detector = " + detector.getScaleFactor());
            if (scale <= originalScale) {
                scale = originalScale;
            } else if (scale > originalScale * 2) {
                scale = originalScale * 2;
            }
            mRect.right = mRect.left + (int) (mViewWidth / scale);
            mRect.bottom = mRect.top + (int) (mViewHeight / scale);
            mScale = scale;
            invalidate();
            return super.onScale(detector);
        }
    }

}
