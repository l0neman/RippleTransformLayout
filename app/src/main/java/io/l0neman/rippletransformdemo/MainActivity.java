package io.l0neman.rippletransformdemo;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import io.l0neman.widget.RippleTransformLayout;

public class MainActivity extends AppCompatActivity {

  private static final class Node {
    int color;
    Node next;

    Node(int color, Node next) {
      this.color = color;
      this.next = next;
    }
  }

  // 为了 Demo 效果，使用循环链表保存颜色，无限切换。
  private Node mColorThemeList = new Node(0xFFF44336, null);

  {
    mColorThemeList.next =
        new Node(0xFF3F51B5,
            new Node(0xFF4CAF50,
                new Node(0xFFFF9800,
                    new Node(0xFF2196F3,
                        new Node(0xFF009688,
                            mColorThemeList)))));
  }

  private RippleTransformLayout mRtl;
  private RelativeLayout mContent;
  private Toolbar mToolbar;

  private float mxy = 0F;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mContent = findViewById(R.id.rl_content);

    mToolbar = findViewById(R.id.tb_demo);
    setSupportActionBar(mToolbar);

    mRtl = findViewById(R.id.rtl_demo);
    mRtl.setDuration(400);
    mRtl.setStartDelay(0);
  }

  public void transform(final View view) {
    if ((mxy += 0.1F) >= 1F) { mxy = 0F; }
    // 为了 Demo 效果，动态改变圆心。
    mRtl.setRippleCenter(mxy, mxy);

    mRtl.transform(new RippleTransformLayout.TransformAction() {
      @Override public void viewChange() {
        // 设置主题的任意操作（甚至改变布局结构）。
        mToolbar.setTitleTextColor(Color.WHITE);
        ((TextView) view).setTextColor(Color.WHITE);
        mToolbar.setBackgroundColor(mColorThemeList.next.color);
        mContent.setBackgroundColor(mColorThemeList.next.next.next.color);
        mColorThemeList = mColorThemeList.next;
      }
    }, null);
  }
}
