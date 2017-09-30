package com.sctek.smartglasses.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import com.cn.zhongdun110.camlog.R;

/**
 * 输入文本框 右边有自带的删除按钮 当有输入时，显示删除按钮，当无输入时，不显示删除按钮。
 * 
 * 
 */
public class RemoteLiveEdittext extends EditText implements
		OnFocusChangeListener, TextWatcher {
	/**
	 * 删除按钮的引用
	 */
	private Drawable mClearDrawable;

	private Drawable mScanDrawable;
	/**
	 * 控件是否有焦点
	 */
	private boolean hasFoucs;

	private OnScanDrawableClickListener mOnScanDrawableClickListener;

	public RemoteLiveEdittext(Context context) {
		this(context, null);
	}

	public RemoteLiveEdittext(Context context, AttributeSet attrs) {
		// 这里构造方法也很重要，不加这个很多属性不能再XML里面定义
		this(context, attrs, android.R.attr.editTextStyle);
	}

	public RemoteLiveEdittext(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		// 获取EditText的DrawableRight,假如没有设置我们就使用默认的图片
		mScanDrawable = getCompoundDrawables()[2];
		mClearDrawable = getResources().getDrawable(
				R.drawable.button_login_delete);
		mScanDrawable = getResources().getDrawable(R.drawable.ic_qr_code);
		mClearDrawable.setBounds(0, 0, mClearDrawable.getIntrinsicWidth(),
				mClearDrawable.getIntrinsicHeight());
		mScanDrawable.setBounds(0, 0, mScanDrawable.getIntrinsicWidth(),
				mScanDrawable.getIntrinsicHeight());
		setCursorVisible(false);
		// 默认设置隐藏图标
		setClearIconVisible();
		// 设置焦点改变的监听
		setOnFocusChangeListener(this);
		// 设置输入框里面内容发生改变的监听
		addTextChangedListener(this);
	}

	/**
	 * 因为我们不能直接给EditText设置点击事件，所以我们用记住我们按下的位置来模拟点击事件 当我们按下的位置 在 EditText的宽度 -
	 * 图标到控件右边的间距 - 图标的宽度 和 EditText的宽度 - 图标到控件右边的间距之间我们就算点击了图标，竖直方向就没有考虑
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			setCursorVisible(true);
			if (getCompoundDrawables()[2] != null) {

				boolean touchable = event.getX() > (getWidth() - getTotalPaddingRight())
						&& (event.getX() < ((getWidth() - getPaddingRight())));

				if (touchable) {
					if (getText().length() > 0) {
						this.setText("");
					} else {
						if (mOnScanDrawableClickListener != null) {
							mOnScanDrawableClickListener
									.onScanDrawableClick(this);
						}
						return true;
					}
				}
			}
		}
		return super.onTouchEvent(event);
	}

	/**
	 * 当ClearEditText焦点发生变化的时候，判断里面字符串长度设置清除图标的显示与隐藏
	 */
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		this.hasFoucs = hasFocus;
		if (hasFocus) {
			setClearIconVisible();
		} else {
			setCompoundDrawables(getCompoundDrawables()[0],
					getCompoundDrawables()[1], null, getCompoundDrawables()[3]);
		}
	}

	/**
	 * 设置清除图标的显示与隐藏，调用setCompoundDrawables为EditText绘制上去
	 * 
	 * @param visible
	 */
	private void setClearIconVisible() {
		Drawable right = getText().length() > 0 ? mClearDrawable
				: mScanDrawable;
		setCompoundDrawables(getCompoundDrawables()[0],
				getCompoundDrawables()[1], right, getCompoundDrawables()[3]);
	}

	/**
	 * 当输入框里面内容发生变化的时候回调的方法
	 */
	@Override
	public void onTextChanged(CharSequence s, int start, int count, int after) {
		if (hasFoucs) {
			setClearIconVisible();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	@Override
	public void afterTextChanged(Editable s) {

	}

	public interface OnScanDrawableClickListener {
		public void onScanDrawableClick(View view);
	}

	public void setOnLeftDrawableClickListner(
			OnScanDrawableClickListener listener) {
		mOnScanDrawableClickListener = listener;
	}

	public void setScanIconEnable(boolean enable) {
		mScanDrawable = enable ? getResources().getDrawable(R.drawable.ic_qr_code)
				: null;
		setClearIconVisible();
	}

}
