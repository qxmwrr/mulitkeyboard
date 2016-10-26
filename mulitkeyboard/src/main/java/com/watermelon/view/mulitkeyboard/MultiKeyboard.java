package com.watermelon.view.mulitkeyboard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;


/**
 * <p>使用步骤</p>
 * <ol>
 *   <li>外层布局要用 {@link LinearLayout} 来嵌套内容布局和 {@link MultiKeyboard}</li>
 *   <li>添加{@link MultiKeyboard}的app:contentLayoutId属性,设置上方内容布局id</li>
 *   <li>添加{@link MultiKeyboard} 嵌套子Layout {@link BtnLayout} 和 {@link BoardLayout}</li>
 *   <li>Activity 的 dispatchKeyEvent 方法中截获返回键 执行 {@link MultiKeyboard#interceptBackPress()}
 *      返回true则拦截事件,否则放行事件
 *   </li>
 *   <li>为需要调起键盘的EditText设置 {@link MultiKeyboard#getOnEditTextTouchListener()}</li>
 *   <li>设置默认调起键盘的EditText设置 {@link MultiKeyboard#setFirstEditText(EditText)}</li>
 * </ol>
 *
 * Created by mrq on 16/7/28.
 */
public class MultiKeyboard extends LinearLayout {

    private static final String SHARE_PREFERENCE_NAME = "com.autohome.community";
    private static final String SHARE_PREFERENCE_TAG = "soft_input_height";
    public static int DEFAULT_KEYBOARD_HEIGHT = 826;

    public static final int ID_KEYBOARD = -1989;
    public static final int ID_NO_BOARD = 0;

    private Activity mActivity;
    private InputMethodManager mInputManager;
    private SharedPreferences sp;
    private int finalContentHeight;//键盘上方距离屏幕顶部区域高度
    private int finalKeyboardHeight;//键盘高度
    private int lastKeyboardHeight;

    private int currentBoardId;
    private int toBoardId;

    private OnBoardChangeListener mOnBoardChangeListener;
    /**
     * 用OnBoardChangeListener替代
     */
    @Deprecated
    private OnKeyboardChangeListener mOnKeyboardChangeListener;
    private OnCheckUserLoginCallback mOnCheckUserLoginCallback;

    private boolean isInit;
    private BtnLayout mBtnLayout;//按钮区域
    private BoardLayout mBoardLayout;//面板区域
    private EditText mEditText;//当前获得焦点的输入框
    private View mContentLayout;
    private int mContentLayoutId;

    private boolean switching;

    public MultiKeyboard(Context context) {
        super(context);
        init();
    }

    public MultiKeyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiKeyboard, defStyleAttr, 0);
        mContentLayoutId = a.getResourceId(R.styleable.MultiKeyboard_contentLayoutId, -1);
        a.recycle();
        init();
    }

    /**
     * 得到EditText的OnTouchListener监听事件,
     * @return
     */
    public OnTouchListener getOnEditTextTouchListener(){
        return onTouchListener;
    }

    /**
     * 对于进入页面直接调用{@link MultiKeyboard#showKeyboard()}的页面,要优先调用本方法
     * @param editText 关联的输入框
     */
    public void setFirstEditText(EditText editText){
        this.mEditText = editText;
    }

    public EditText getCurrentEditText(){
        return mEditText;
    }

    public void setOnBoardChangeListener(OnBoardChangeListener l){
        this.mOnBoardChangeListener = l;
    }

    /**
     * 设置键盘打开关闭监听事件
     * @param l 键盘监听事件
     */
    public void setOnKeyboardChangeListener(OnKeyboardChangeListener l){
        this.mOnKeyboardChangeListener = l;
    }

    /**
     * 设置用户登录判断回调
     * @param l 回调方法
     */
    public void setOnCheckUserLoginCallback(OnCheckUserLoginCallback l){
        this.mOnCheckUserLoginCallback = l;
    }

    /**
     * 打开输入法键盘,确定在调用本方法前调用了{@link MultiKeyboard#setFirstEditText(EditText)}
     */
    public void showKeyboard(){
        if (isInit && !switching){
            switching = true;
            getSupportSoftInputHeight();
            mBtnLayout.select(0);
            lockContentHeight(finalKeyboardHeight);
            mBoardLayout.show(0, false);
            if (mEditText != null){
                showSoftInput(mEditText);
            }
            toBoardId = ID_KEYBOARD;
            unlockContentHeightDelayed();
        }
    }

    private void sendBoardChangeListener() {
        if (mOnBoardChangeListener != null){
            mOnBoardChangeListener.onBoardChange(currentBoardId, toBoardId);
        }
        currentBoardId = toBoardId;
    }

    /**
     * 拦截物理返回键
     * @return 键盘打开状态时关闭键盘并返回true,否则返回false
     */
    public boolean interceptBackPress() {
        if (!isInit){
            return false;
        }
        mBtnLayout.select(0);

        if (isMultiBoardShown()) {
            mBoardLayout.show(0, false);
            toBoardId = ID_NO_BOARD;
            sendBoardChangeListener();
            return true;
        }
        if (isSoftInputShown()) {
            hideSoftInput();
            toBoardId = ID_NO_BOARD;
            sendBoardChangeListener();
            return true;
        }
        if (isShown()){
            toBoardId = ID_NO_BOARD;
            sendBoardChangeListener();
        }
        return false;
    }

    public boolean isSoftInputShown() {
        return isInit && getSupportSoftInputHeight() > 0;
    }

    public boolean isMultiBoardShown() {
        return mBoardLayout.isShown();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInit){
            if (mBtnLayout.getEditText() != null) {
                mBtnLayout.getEditText().setOnTouchListener(onTouchListener);
            }
            if (!isInEditMode()){
                mContentLayout = mActivity.findViewById(mContentLayoutId);
                if (mContentLayout == null){
                    throw new RuntimeException("you mast set contentLayoutId");
                }
            }
            hideSoftInput();

            isInit = true;
        }
    }



    private void init() {
        setOrientation(VERTICAL);
        initBase();
    }

    private void initBase() {
        DEFAULT_KEYBOARD_HEIGHT = (int) (getResources().getDisplayMetrics().widthPixels * 0.765625);
        sp = getContext().getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        finalKeyboardHeight = sp.getInt(SHARE_PREFERENCE_TAG, DEFAULT_KEYBOARD_HEIGHT);

        if (!isInEditMode()){
            if (!(getContext() instanceof Activity)) {
                throw new RuntimeException("mast use Activity context");
            }
            mActivity = (Activity) getContext();
            mInputManager = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void initViews() {
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new RuntimeException("child count mast 2");
        }
        View childAt0 = getChildAt(0);
        View childAt1 = getChildAt(1);
        if (!(childAt0 instanceof BtnLayout)) {
            throw new RuntimeException("child 0 mast BtnLayout");
        }
        if (!(childAt1 instanceof BoardLayout)) {
            throw new RuntimeException("child 0 mast BoardLayout");
        }

        mBtnLayout = (BtnLayout) childAt0;
        mBoardLayout = (BoardLayout) childAt1;
        mBoardLayout.connect(mBtnLayout, switchItemClickListener);
        getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    //锁定键盘外部区域布局高度
    private void lockContentHeight(int boardLayoutHeight) {
        LayoutParams params = (LayoutParams) mContentLayout.getLayoutParams();
        params.height = finalContentHeight - (boardLayoutHeight - finalKeyboardHeight);
        params.weight = 0.0F;
    }

    //解除键盘外部区域布局高度
    private void unlockContentHeightDelayed() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                LayoutParams layoutParams = (LayoutParams) mContentLayout.getLayoutParams();
                layoutParams.weight = 1.0F;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                sendBoardChangeListener();
                switching = false;
            }
        }, 200L);
    }

    //获取键盘高度
    private int getSupportSoftInputHeight() {
        if (isInEditMode()){
            return DEFAULT_KEYBOARD_HEIGHT;//TODO  这个应该是动态的
        }
        Rect r = new Rect();
        mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        int screenHeight = mActivity.getWindow().getDecorView().getRootView().getHeight();
        int softInputHeight = screenHeight - r.bottom;
        if (Build.VERSION.SDK_INT >= 20) {
            // When SDK Level >= 20 (Android L), the softInputHeight will contain the height of softButtonsBar (if has)
            softInputHeight = softInputHeight - getSoftButtonsBarHeight();
        }
        if (softInputHeight < 0) {
            softInputHeight = 0;
        }
        if (softInputHeight > 0) {
            sp.edit().putInt(SHARE_PREFERENCE_TAG, softInputHeight).apply();
            finalKeyboardHeight = softInputHeight;
            finalContentHeight = mContentLayout.getHeight();
        } else {//初次进入页面直接打开表情键盘
            finalKeyboardHeight = sp.getInt(SHARE_PREFERENCE_TAG, DEFAULT_KEYBOARD_HEIGHT);
            if (finalContentHeight == 0){
                finalContentHeight = mContentLayout.getHeight() - finalKeyboardHeight;
            }
        }
        return softInputHeight;
    }

    //获取虚拟按键高度(Nexus手机的底部虚拟按键的高度)
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        if (isInEditMode()){
            return 0;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }

    private void showSoftInput(final View editText) {
        if (isInEditMode()){
            return;
        }
        editText.requestFocus();
        post(new Runnable() {
            @Override
            public void run() {
                mInputManager.showSoftInput(editText, 0);
            }
        });
    }

    private void hideSoftInput() {
        if (isInEditMode()){
            return;
        }
        mInputManager.hideSoftInputFromWindow(MultiKeyboard.this.getWindowToken(), 0);
    }

    private OnTouchListener onTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v instanceof EditText){
                mEditText = (EditText) v;
            } else {
                throw new RuntimeException("you mast set this onTouchListener to an EditText");
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                boolean isLogin = UserInfoDomain.getInstance().checkIsLoginOrGoLogin(mActivity);
//                    if (onEditTextActionDownListener != null) {
//                        onEditTextActionDownListener.run();
//                    }
//                return !isLogin;
                if (mOnCheckUserLoginCallback != null){
                    return !mOnCheckUserLoginCallback.isUserLogin();
                }
                return false;
            } else if (event.getAction() == MotionEvent.ACTION_UP && isMultiBoardShown()) {
                if (!switching){
                    switching = true;
                    lockContentHeight(finalKeyboardHeight);
                    mBtnLayout.select(0);
                    mBoardLayout.show(0, false);
                    showSoftInput(v);
                    toBoardId = ID_KEYBOARD;
                    unlockContentHeightDelayed();
                }
            }
            return false;
        }
    };

    //表情等需要底部自定义键盘区域的按钮点击事件
    private BtnLayout.OnSwitchItemClickListener switchItemClickListener = new BtnLayout.OnSwitchItemClickListener() {

        @Override
        public void onSwitchItemClick(View v, boolean selected) {
            int softInputHeight = getSupportSoftInputHeight();
//            if (softInputHeight == 0) {
//                softInputHeight = sp.getInt(SHARE_PREFERENCE_TAG, DEFAULT_KEYBOARD_HEIGHT);
//            }
            if (!switching){
                switching = true;
                if (selected) {
                    mBtnLayout.select(v.getId());
                    lockContentHeight(mBoardLayout.getItemLayoutHeight(v.getId()));
                    hideSoftInput();
                    mBoardLayout.show(v.getId(), true);
                    toBoardId = v.getId();
                    unlockContentHeightDelayed();
                } else {
                    mBtnLayout.select(0);
                    lockContentHeight(finalKeyboardHeight);
                    mBoardLayout.show(0, false);
                    if (mEditText != null){
                        showSoftInput(mEditText);
                    }
                    toBoardId = ID_KEYBOARD;
                    unlockContentHeightDelayed();
                }
            }
        }
    };

    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (isInEditMode()){
                return;
            }
            Rect r = new Rect();
            //获取当前界面可视部分
            mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
            //获取屏幕的高度
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int height = mActivity.getWindow().getDecorView().getRootView().getHeight();
            screenHeight = Math.min(screenHeight, height);
            //此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数

            int keyboardHeight = screenHeight - r.bottom;

            if (lastKeyboardHeight != keyboardHeight) {
                lastKeyboardHeight = keyboardHeight;
                if (!switching){
                    if (keyboardHeight <= 0){
                        toBoardId = ID_NO_BOARD;
                        sendBoardChangeListener();
                    } else {
                        toBoardId = ID_KEYBOARD;
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendBoardChangeListener();
                            }
                        }, 200L);
                    }
                }

                if (mOnKeyboardChangeListener != null) {
                    mOnKeyboardChangeListener.onKeyboardChange(keyboardHeight > 0);
                }
            }
        }
    };

    public interface OnKeyboardChangeListener {
        void onKeyboardChange(boolean keyboardShown);
    }

    /**
     * 键盘模式切换时调用
     */
    public interface OnBoardChangeListener {
        /**
         * 其他面板未打开时直接调起键盘不执行,键盘调起时返回键关闭键盘不执行
         * @param fromBoardId 关闭的面板
         * @param toBoardId 打开的面板
         */
        void onBoardChange(int fromBoardId, int toBoardId);
    }

    public interface OnCheckUserLoginCallback {
        boolean isUserLogin();
    }
}