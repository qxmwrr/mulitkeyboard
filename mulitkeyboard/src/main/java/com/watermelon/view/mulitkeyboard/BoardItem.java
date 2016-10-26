package com.watermelon.view.mulitkeyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 *
 * Created by mrq on 16/7/28.
 */
public class BoardItem extends LinearLayout {

    private int mRelevanceItemBtnId;
    private int mBoardHeight;

    public BoardItem(Context context) {
        super(context);
    }

    public BoardItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoardItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BoardItem, defStyleAttr, 0);
        mRelevanceItemBtnId = a.getResourceId(R.styleable.BoardItem_switchItemId, 0);
        mBoardHeight = a.getDimensionPixelSize(R.styleable.BoardItem_boardHeight, 0);
        if (mRelevanceItemBtnId == 0){
            throw new RuntimeException("you mast set app:btn_id");
        }
        if (mBoardHeight == 0){
            mBoardHeight = MultiKeyboard.DEFAULT_KEYBOARD_HEIGHT;
        }
        a.recycle();
        setVisibility(GONE);
    }

    public int getRelevanceItemBtnId(){
        return mRelevanceItemBtnId;
    }

    public int getLayoutHeight(){
        return mBoardHeight;
    }
}
