package io.l0neman.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Created by l0neman on 2020/03/13.
 * <p>
 * 原理：
 * <p>
 * 1. 首先将当前布局内的所有子 View 生成一张快照（通过将 View 内容绘制在 Bitmap 上进行截图）。
 * <p>
 * 2. 添加一个 View，通过 FrameLayout 叠加特性，使此 View 置于所有子 View 的上方。
 * <p>
 * 3. 将快照绘制在刚添加的 View 上，那么此时用户所见画面的内容将无任何变化，就可以在被遮挡的
 * View 上执行换主题的操作了（即设置颜色，大小等属性，随意变化均可）。
 * <p>
 * 4. 换主题操作过后，在添加的 View 上执行水波纹动画，原理是首先绘制一个圆形，给定一个圆心，一个半径变量，
 * 半径由 0 变化至圆心与最远的角的距离，绘制圆形时，给 Paint 指定一个 Xfermode，使圆形与快照图片交叉的部
 * 分为透明，即可看到下面被遮挡的 View，那么整个动画过程就能展示出新界面把旧的界面使用水波纹替换的效果了。
 * <p>
 * 2. 初始化 View：<code>RippleTransformLayout rtl = findViewById(R.id.rtl_test);</code>
 * <p>
 * 3. 设置动画时长，动画开始延迟时间，设置波纹圆心。
 *
 * <pre><code>
 *   rtl.setDuration(1000);  // 持续 1s。
 *   rtl.setStartDelay(200); // 延迟 300ms 后开始。
 *   rtl.setRippleCenter(0.3F, 0.3F); // 处于 rtl 布局左上角。
 * </code></pre>
 * <p>
 * 4. 开始进行变换。
 *
 * <pre><code>
 *   rtl.transform();
 * </code></pre>
 */
public class RippleTransformLayout extends FrameLayout {

  private int mW;
  private int mH;

  // for ripple anim
  private ValueAnimator mTransformAnimator;
  private float mRippleRadius = 0;
  private Bitmap mMagicMask;

  // for draw
  private PorterDuffXfermode mSrcOutMode;
  private Paint mTransformPaint;
  private View mTransformView;

  // for config
  private int mDuration = 400;
  private int mStartDelay = 0;
  private float mXFraction;
  private float mYFraction;
  private float mCenterX;
  private float mCenterY;

  private AnimatorCallback mAnimatorCallback;

  public RippleTransformLayout(@NonNull Context context) {
    super(context);
    init();
  }

  public RippleTransformLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public RippleTransformLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public RippleTransformLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  private void init() {
    mTransformPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mSrcOutMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    if (getChildCount() == 0) {
      throw new IllegalStateException("must have children.");
    }
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    mW = getMeasuredWidth();
    mH = getMeasuredHeight();

    initTransformView();
  }

  public class TransformView extends View {

    public TransformView(Context context) {
      super(context);
    }

    @Override protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      if (mMagicMask == null) { return; }

      int sc = canvas.saveLayer(0, 0, mW, mH, mTransformPaint, Canvas.ALL_SAVE_FLAG);
      drawMask(canvas);
      mTransformPaint.setXfermode(mSrcOutMode);
      canvas.drawBitmap(mMagicMask, 0, 0, mTransformPaint);
      mTransformPaint.setXfermode(null);
      canvas.restoreToCount(sc);
    }

    private void drawMask(Canvas canvas) {
      canvas.drawCircle(mCenterX, mCenterY, mRippleRadius, mTransformPaint);
    }
  }

  private void initTransformView() {
    if (mTransformView != null) {
      if (getChildAt(getChildCount() - 1) != mTransformView) {
        removeView(mTransformView);
        addView(mTransformView);
      }

      return;
    }

    mTransformView = new TransformView(getContext());
    mTransformView.setVisibility(GONE);
    final int height;
    // fixed for Android KitKat.
    height = mH + getNavigationBarHeight();

    mTransformView.setLayoutParams(new LayoutParams(mW, height));
    addView(mTransformView);
  }

  private int getNavigationBarHeight() {
    Resources resources = getResources();
    int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    return resources.getDimensionPixelSize(resourceId);
  }

  /**
   * 设置动画时间。
   *
   * @param duration ms
   */
  public void setDuration(int duration) {
    this.mDuration = duration;
  }

  /**
   * 动画开始延时。
   *
   * @param startDelay ms
   */
  public void setStartDelay(int startDelay) {
    this.mStartDelay = startDelay;
  }

  /**
   * 波纹中心点，占自身布局的 X、Y 轴尺寸的比例。
   *
   * @param xFraction x 轴尺寸比例。
   * @param yFraction y 轴尺寸比例。
   */
  public void setRippleCenter(float xFraction, float yFraction) {
    this.mXFraction = xFraction;
    this.mYFraction = yFraction;

    if (mW != 0) {
      mCenterX = mXFraction * mW;
      mCenterY = mYFraction * mH;
    }
  }

  /** 执行回调 */
  public interface TransformAction {
    /** 在此方法中执行切换主题或改变界面的逻辑 */
    void viewChange();
  }

  public interface AnimatorCallback {
    /** 波纹动画结束回调 */
    void onAnimatorEnded();
  }

  private void ensureSelfSize() {
    if (mW == 0 || mH == 0) {
      MeasureSpec.makeMeasureSpec(getLayoutParams().width, MeasureSpec.EXACTLY);
      MeasureSpec.makeMeasureSpec(getLayoutParams().height, MeasureSpec.EXACTLY);
      measure(getMeasuredWidth(), getMeasuredHeight());
    }

    mCenterX = mXFraction * mW;
    mCenterY = mYFraction * mH;
  }

  /**
   * 开始执行切换动画。
   *
   * @param action   切换逻辑回调。
   * @param callback 动画结束回调，可为 null。
   */
  public void transform(final TransformAction action, AnimatorCallback callback) {
    this.mAnimatorCallback = callback;

    ensureSelfSize();
    mTransformView.setVisibility(VISIBLE);
    mMagicMask = drawToBitmap(this);
    invalidate();
    action.viewChange();
    startTransform();
  }

  private void startTransform() {
    if (mTransformAnimator != null && mTransformAnimator.isRunning()) {
      mTransformAnimator.cancel();
      return;
    }

    float maxRadius = calculateRippleMaxRadius();
    mTransformAnimator = ValueAnimator.ofFloat(0, maxRadius);
    mTransformAnimator.setDuration(mDuration);
    mTransformAnimator.setStartDelay(mStartDelay);
    mTransformAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override public void onAnimationUpdate(ValueAnimator animation) {
        mRippleRadius = (float) animation.getAnimatedValue();
        mTransformView.invalidate();
      }
    });

    mTransformAnimator.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animation) {}

      @Override public void onAnimationEnd(Animator animation) {
        mTransformView.setVisibility(GONE);
        mMagicMask.recycle();
        mMagicMask = null;
        mRippleRadius = 0;

        if (mAnimatorCallback != null) {
          mAnimatorCallback.onAnimatorEnded();
        }
      }

      @Override public void onAnimationCancel(Animator animation) {}

      @Override public void onAnimationRepeat(Animator animation) {}
    });
    mTransformAnimator.start();
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (mTransformAnimator != null && mTransformAnimator.isRunning()) {
      mTransformAnimator.cancel();
    }
  }

  // utils

  private float calculateRippleMaxRadius() {
    final float w = mW * Math.max(mXFraction, 1 - mXFraction);
    final float h = mH * Math.max(mYFraction, 1 - mYFraction);
    return (float) Math.sqrt(w * w + h * h);
  }

  private Bitmap drawToBitmap(View view) {
    Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    view.draw(canvas);
    return bitmap;
  }
}
