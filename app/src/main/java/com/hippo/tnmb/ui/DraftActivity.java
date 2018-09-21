/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.tnmb.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.effect.ViewTransition;
import com.hippo.tnmb.R;
import com.hippo.tnmb.dao.DraftRaw;
import com.hippo.tnmb.util.DB;
import com.hippo.tnmb.util.ReadableTime;
import com.hippo.tnmb.util.Settings;
import com.hippo.tnmb.widget.FontTextView;
import com.hippo.util.DrawableManager;
import com.hippo.widget.Snackbar;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ResourcesUtils;

import de.greenrobot.dao.query.LazyList;

public final class DraftActivity extends TranslucentActivity implements EasyRecyclerView.OnItemClickListener {

    private LazyList<DraftRaw> mLazyList;

    private View mTip;
    private EasyRecyclerView mRecyclerView;
    private ViewTransition mViewTransition;

    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    @Override
    protected int getLightThemeResId() {
        return Settings.getColorStatusBar() ? R.style.NormalActivity : R.style.NormalActivity_NoStatus;
    }

    @Override
    protected int getDarkThemeResId() {
        return Settings.getColorStatusBar() ? R.style.NormalActivity_Dark : R.style.NormalActivity_Dark_NoStatus;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStatusBarColor(ResourcesUtils.getAttrColor(this, R.attr.colorPrimaryDark));
        ToolbarActivityHelper.setContentView(this, R.layout.activity_draft);
        setActionBarUpIndicator(DrawableManager.getDrawable(this, R.drawable.v_arrow_left_dark_x24));

        mTip = findViewById(R.id.tip);
        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);
        mViewTransition = new ViewTransition(mTip, mRecyclerView);

        // Layout Manager
        mLayoutManager = new LinearLayoutManager(this);

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);

        // swipe manager
        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();

        mAdapter = new DraftAdapter();
        mAdapter.setHasStableIds(true);
        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(mAdapter);      // wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mRecyclerView.hasFixedSize();
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator);
        mRecyclerView.setOnItemClickListener(this);

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewSwipeManager.attachRecyclerView(mRecyclerView);

        updateLazyList();
        checkEmpty(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRecyclerViewSwipeManager != null) {
            mRecyclerViewSwipeManager.release();
            mRecyclerViewSwipeManager = null;
        }

        if (mRecyclerViewTouchActionGuardManager != null) {
            mRecyclerViewTouchActionGuardManager.release();
            mRecyclerViewTouchActionGuardManager = null;
        }

        if (mRecyclerView != null) {
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter);
            mWrappedAdapter = null;
        }
        mAdapter = null;
        mLayoutManager = null;

        if (mLazyList != null) {
            mLazyList.close();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Remember to notify
    private void updateLazyList() {
        LazyList<DraftRaw> lazyList = DB.getDraftLazyList();
        if (mLazyList != null) {
            mLazyList.close();
        }
        mLazyList = lazyList;
    }

    private void checkEmpty(boolean animation) {
        if (mAdapter.getItemCount() == 0) {
            mViewTransition.showView(0, animation);
        } else {
            mViewTransition.showView(1, animation);
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        DraftRaw draftRaw = mLazyList.get(position);
        ClipboardManager cbm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cbm.setPrimaryClip(ClipData.newPlainText(null, draftRaw.getContent()));
        Toast.makeText(this, R.string.draft_copied_clipboard, Toast.LENGTH_SHORT).show();
        return false;
    }

    private class DraftHolder extends AbstractSwipeableItemViewHolder {

        private View swipable;
        public TextView time;
        public FontTextView content;

        public DraftHolder(View itemView) {
            super(itemView);

            swipable = itemView.findViewById(R.id.swipable);
            time = (TextView) itemView.findViewById(R.id.time);
            content = (FontTextView) itemView.findViewById(R.id.content);
        }

        @Override
        public View getSwipeableContainerView() {
            return swipable;
        }
    }

    private class DraftAdapter extends RecyclerView.Adapter<DraftHolder>
            implements SwipeableItemAdapter<DraftHolder> {

        @Override
        public DraftHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DraftHolder(DraftActivity.this.getLayoutInflater().inflate(R.layout.item_draft, parent, false));
        }

        @Override
        public void onBindViewHolder(DraftHolder holder, int position) {
            DraftRaw draftRaw = mLazyList.get(position);
            holder.time.setText(ReadableTime.getDisplayTime(draftRaw.getTime()));
            holder.content.setText(draftRaw.getContent());

            holder.content.setTextSize(Settings.getFontSize());
            holder.content.setLineSpacing(LayoutUtils.dp2pix(DraftActivity.this, Settings.getLineSpacing()), 1.0f);
            if (Settings.getFixEmojiDisplay()) {
                holder.content.useCustomTypeface();
            } else {
                holder.content.useOriginalTypeface();
            }
        }

        @Override
        public long getItemId(int position) {
            return mLazyList.get(position).getId();
        }

        @Override
        public int getItemCount() {
            return mLazyList.size();
        }

        @Override
        public int onGetSwipeReactionType(DraftHolder holder, int position, int x, int y) {
            return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H;
        }

        @Override
        public void onSetSwipeBackground(DraftHolder holder, int position, int type) {
            // Empty
        }

        @Override
        public void onSwipeItemStarted(DraftHolder holder, int position) {
            // Empty
        }

        @Override
        public SwipeResultAction onSwipeItem(DraftHolder holder, int position, int result) {
            switch (result) {
                // swipe right
                case SwipeableItemConstants.RESULT_SWIPED_RIGHT:
                case SwipeableItemConstants.RESULT_SWIPED_LEFT:
                    return new DeleteAction(position);
                case SwipeableItemConstants.RESULT_CANCELED:
                default:
                    return null;
            }
        }
    }

    private class DeleteAction extends SwipeResultActionRemoveItem {

        private int mPosition;

        public DeleteAction(int position) {
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();

            final int position = mPosition;
            final DraftRaw raw = mLazyList.get(position);
            if (raw != null) {
                DB.removeDraft(raw.getId());
                updateLazyList();
                mAdapter.notifyItemRemoved(position);
                checkEmpty(true);

                Snackbar snackbar = Snackbar.make(mRecyclerView, R.string.draft_deleted, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DB.addDraft(raw.getContent(), raw.getTime());
                        updateLazyList();
                        mAdapter.notifyDataSetChanged();
                        checkEmpty(true);
                    }
                });
                snackbar.show();
            }
        }
    }



}
