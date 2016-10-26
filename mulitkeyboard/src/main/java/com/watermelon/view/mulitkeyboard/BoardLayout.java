package com.watermelon.view.mulitkeyboard;

import android.content.Context;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by mrq on 16/7/28.
 */
public class BoardLayout extends FrameLayout {

    private Map<Integer, BoardItem> mItemBoardMap = new HashMap<>();

    public BoardLayout(Context context) {
        super(context);
    }

    public BoardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof BoardItem) {
                int switchItemId = ((BoardItem) childAt).getRelevanceItemBtnId();
                if (switchItemId == 0) {
                    throw new RuntimeException("mast set view switchItemId");
                }
                mItemBoardMap.put(switchItemId, (BoardItem) childAt);
            }
        }
    }

    public void connect(BtnLayout btnLayout, BtnLayout.OnSwitchItemClickListener l){
        Set<Integer> switchItemIds = mItemBoardMap.keySet();
        for (Integer switchItemId : switchItemIds){
            btnLayout.setOnSwitchItemClickListener(switchItemId, l);
        }
    }

    /**
     * 展示或隐藏按钮对应的功能面板
     * @param btnId 按钮id
     * @param show true展示,false隐藏
     */
    public void show(@IdRes int btnId, boolean show) {
        boolean haveShowView = false;
        Set<Map.Entry<Integer, BoardItem>> entries = mItemBoardMap.entrySet();
        for (Map.Entry<Integer, BoardItem> entry : entries) {
            BoardItem boardItem = entry.getValue();
            if (entry.getKey() == btnId) {
                haveShowView = true;
                if (show) {//显示选中面板
                    boardItem.setVisibility(VISIBLE);
                    getLayoutParams().height = boardItem.getLayoutHeight();
                } else {//显示键盘
                    boardItem.setVisibility(GONE);
                }
            } else {
                boardItem.setVisibility(GONE);
            }
        }
        if (haveShowView && show){
            setVisibility(VISIBLE);
        } else {
            setVisibility(GONE);
        }
        mItemBoardMap.get(btnId);
    }

    /**
     * 获取按钮对应功能面板高度
     * @param btnId 按钮id
     * @return 返回功能面板高度
     */
    public int getItemLayoutHeight(@IdRes int btnId) {
        BoardItem boardItem = mItemBoardMap.get(btnId);
        return boardItem.getLayoutHeight();
    }
}