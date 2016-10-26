package com.watermelon.view.mulitkeyboard;

import android.content.Context;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by mrq on 16/7/28.
 */
public class BtnLayout extends RelativeLayout {

    private Map<Integer, View> mItemMap = new HashMap<>();
    private Map<Integer, View> mSwitchItemMap = new HashMap<>();

    private EditText mEditText;

    public BtnLayout(Context context) {
        super(context);

    }

    public BtnLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BtnLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        setViewToMap(this);
    }

    private void setViewToMap(ViewGroup viewGroup){
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt instanceof ViewGroup){
                setViewToMap((ViewGroup) childAt);
            } else {
                int id = childAt.getId();
                if (id > 0) {
                    mItemMap.put(id, childAt);
                }
            }
        }
    }

    public EditText getEditText() {
        return mEditText;
    }

    public void setOnSwitchItemClickListener(@IdRes int switchBtnId, final OnSwitchItemClickListener l){
        View view = mItemMap.get(switchBtnId);
        if (view != null){
            mSwitchItemMap.put(switchBtnId, view);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    l.onSwitchItemClick(v, !v.isSelected());
                }
            });
        }
    }


    public void select(@IdRes int id) {
        Set<Map.Entry<Integer, View>> entries = mSwitchItemMap.entrySet();
        for (Map.Entry<Integer, View> entry : entries) {
            if (entry.getKey() == id) {
                entry.getValue().setSelected(true);
            } else {
                entry.getValue().setSelected(false);
            }
        }
    }

    public interface OnSwitchItemClickListener {

        void onSwitchItemClick(View v, boolean selected);
    }
}
