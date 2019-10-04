package com.zhihu.matisse.internal.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter;
import com.zhihu.matisse.internal.ui.widget.CheckRadioView;
import com.zhihu.matisse.internal.ui.widget.CheckView;
import com.zhihu.matisse.internal.ui.widget.IncapableDialog;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;
import com.zhihu.matisse.ui.MatisseActivity;

import java.util.List;

/**
 * Created by Andrei.Aiftimoaie on 8/24/2018.
 */

public class BasePreviewFragment extends Fragment implements ViewPager.OnPageChangeListener {

    public static final String EXTRA_DEFAULT_BUNDLE = "extra_default_bundle";
    public static final String EXTRA_RESULT_BUNDLE = "extra_result_bundle";
    public static final String EXTRA_RESULT_APPLY = "extra_result_apply";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    public static final String CHECK_STATE = "checkState";

    protected final SelectedItemCollection mSelectedCollection = new SelectedItemCollection(getContext());
    protected SelectionSpec mSpec;
    protected ViewPager mPager;

    protected PreviewPagerAdapter mAdapter;

    protected CheckView mCheckView;
    protected TextView mButtonBack;
    protected TextView mButtonApply;
    protected TextView mSize;

    protected int mPreviousPos = -1;

    private LinearLayout mOriginalLayout;
    private CheckRadioView mOriginal;
    protected boolean mOriginalEnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_media_preview, null);

        if (savedInstanceState == null) {
            mSelectedCollection.onCreate(getActivity().getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
        } else {
            mSelectedCollection.onCreate(savedInstanceState);
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }

        mPager = (ViewPager) result.findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);
        mAdapter = new PreviewPagerAdapter(getActivity().getSupportFragmentManager(), null);
        mPager.setAdapter(mAdapter);
        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mSelectedCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
        super.onSaveInstanceState(outState);
    }

    public void update(){
        Bundle bundle = null;
        OUTER:
        if (((MatisseActivity) getActivity()) != null) {
            bundle = ((MatisseActivity) getActivity()).mSelectedCollection.getDataWithBundle();
            List<Item> selected = bundle.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION);
            if(selected.size() == 0)
                break OUTER;
            mSelectedCollection.onCreate(bundle);
            mAdapter = new PreviewPagerAdapter(getActivity().getSupportFragmentManager(), null);
            mPager.setAdapter(mAdapter);
            ((MatisseActivity) getActivity()).updateBottomToolbar();
//            for(int i = 0; i < selected.size(); i++){
//                Log.d("JS_d", selected.get(i).getContentUri().toString());
//            }
            mAdapter.update(selected);
        }
        mAdapter.notifyDataSetChanged();
        mPreviousPos = 0;
//        mPager.setCurrentItem(0);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
        if (mPreviousPos != -1 && mPreviousPos != position) {
            ((PreviewItemFragment) adapter.instantiateItem(mPager, mPreviousPos)).resetView();

//            Item item = adapter.getMediaItem(position);
//            if (mSpec.countable) {
//                int checkedNum = mSelectedCollection.checkedNumOf(item);
//                mCheckView.setCheckedNum(checkedNum);
//                if (checkedNum > 0) {
//                    mCheckView.setEnabled(true);
//                } else {
//                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
//                }
//            } else {
//                boolean checked = mSelectedCollection.isSelected(item);
//                mCheckView.setChecked(checked);
//                if (checked) {
//                    mCheckView.setEnabled(true);
//                } else {
//                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
//                }
//            }
        }
        mPreviousPos = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void updateApplyButton() {
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonApply.setText(R.string.button_sure_default);
            mButtonApply.setEnabled(false);
        } else if (selectedCount == 1 && ((MatisseActivity) getActivity()).mSpec.singleSelectionModeEnabled()) {
            mButtonApply.setText(R.string.button_sure_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.button_sure, selectedCount));
        }

        if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mOriginalLayout.setVisibility(View.GONE);
        }
    }


    private void updateOriginalState() {
        mOriginal.setChecked(mOriginalEnable);
        if (!mOriginalEnable) {
            mOriginal.setColor(Color.WHITE);
        }

        if (countOverMaxSize() > 0) {

            if (mOriginalEnable) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec.originalMaxSize));
                incapableDialog.show(((MatisseActivity) getContext()).getSupportFragmentManager(),
                        IncapableDialog.class.getName());

                mOriginal.setChecked(false);
                mOriginal.setColor(Color.WHITE);
                mOriginalEnable = false;
            }
        }
    }


    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            Item item = mSelectedCollection.asList().get(i);
            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    protected void sendBackResult(boolean apply) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EXTRA_RESULT_APPLY, apply);
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
//        setResult(Activity.RESULT_OK, intent);
    }
}
