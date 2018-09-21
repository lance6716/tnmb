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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.hippo.conaco.DataContainer;
import com.hippo.conaco.ProgressNotify;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.easyrecyclerview.RawMarginItemDecoration;
import com.hippo.effect.ViewTransition;
import com.hippo.io.FileInputStreamPipe;
import com.hippo.tnmb.Constants;
import com.hippo.tnmb.R;
import com.hippo.tnmb.client.data.ACSite;
import com.hippo.tnmb.dao.ACRecordRaw;
import com.hippo.tnmb.util.DB;
import com.hippo.tnmb.util.ReadableTime;
import com.hippo.tnmb.util.Settings;
import com.hippo.tnmb.widget.FontTextView;
import com.hippo.tnmb.widget.LoadImageView;
import com.hippo.ripple.Ripple;
import com.hippo.util.DrawableManager;
import com.hippo.widget.Snackbar;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.Messenger;
import com.hippo.yorozuya.ResourcesUtils;
import com.hippo.yorozuya.io.InputStreamPipe;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import de.greenrobot.dao.query.LazyList;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public final class RecordActivity extends TranslucentActivity
        implements EasyRecyclerView.OnItemClickListener {

    private LazyList<ACRecordRaw> mLazyList;

    private EasyRecyclerView mRecyclerView;
    private ViewTransition mViewTransition;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    private Set<WeakReference<LoadImageView>> mLoadImageViewSet = new HashSet<>();

    @Override
    protected int getLightThemeResId() {
        return Settings.getColorStatusBar() ? R.style.NormalActivity : R.style.NormalActivity_NoStatus;
    }

    @Override
    protected int getDarkThemeResId() {
        return Settings.getColorStatusBar() ? R.style.NormalActivity_Dark : R.style.NormalActivity_Dark_NoStatus;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStatusBarColor(ResourcesUtils.getAttrColor(this, R.attr.colorPrimaryDark));
        ToolbarActivityHelper.setContentView(this, R.layout.activity_record);
        setActionBarUpIndicator(DrawableManager.getDrawable(this, R.drawable.v_arrow_left_dark_x24));

        View tip = findViewById(R.id.tip);
        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);
        mViewTransition = new ViewTransition(tip, mRecyclerView);

        // Layout Manager
        int interval = getResources().getDimensionPixelOffset(R.dimen.card_interval);
        if (getResources().getBoolean(R.bool.two_way)) {
            mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            MarginItemDecoration decoration = new MarginItemDecoration(interval, interval, interval, interval, interval);
            mRecyclerView.addItemDecoration(decoration);
            decoration.applyPaddings(mRecyclerView);
            mRecyclerView.setItemAnimator(new SlideInUpAnimator());
        } else {
            int halfInterval = interval / 2;
            mRecyclerView.addItemDecoration(new RawMarginItemDecoration(0, halfInterval, 0, halfInterval));
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setPadding(0, halfInterval, 0, halfInterval);
        }

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);

        // swipe manager
        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();

        mAdapter = new RecordAdapter();
        mAdapter.setHasStableIds(true);
        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(mAdapter);      // wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mRecyclerView.hasFixedSize();
        mRecyclerView.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setSelector(Ripple.generateRippleDrawable(this, ResourcesUtils.getAttrBoolean(this, R.attr.dark)));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setClipChildren(false);

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewSwipeManager.attachRecyclerView(mRecyclerView);

        updateLazyList();
        checkEmpty(false);

        Messenger.getInstance().register(Constants.MESSENGER_ID_UPDATE_RECORD, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Messenger.getInstance().unregister(Constants.MESSENGER_ID_UPDATE_RECORD, this);

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

        if (mLazyList != null) {
            mLazyList.close();
        }

        for (WeakReference<LoadImageView> ref : mLoadImageViewSet) {
            LoadImageView liv = ref.get();
            if (liv != null) {
                liv.unload();
            }
        }
        mLoadImageViewSet.clear();
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

    @Override
    public void onReceive(final int id, final Object obj) {
        if (id == Constants.MESSENGER_ID_UPDATE_RECORD) {
            updateLazyList();
            mAdapter.notifyDataSetChanged();
            checkEmpty(true);
        } else {
            super.onReceive(id, obj);
        }
    }

    // Remember to notify
    private void updateLazyList() {
        LazyList<ACRecordRaw> lazyList = DB.getACRecordLazyList();
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
        ACRecordRaw raw = mLazyList.get(position);
        String postId = raw.getPostid();
        if (!TextUtils.isEmpty(postId)) {
            Intent intent = new Intent(this, PostActivity.class);
            intent.setAction(PostActivity.ACTION_SITE_ID);
            intent.putExtra(PostActivity.KEY_SITE, ACSite.getInstance().getId());
            intent.putExtra(PostActivity.KEY_ID, postId);
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    private class RecordHolder extends AbstractSwipeableItemViewHolder {

        public View swipable;
        public TextView leftText;
        public TextView rightText;
        private FontTextView content;
        private LoadImageView thumb;

        public RecordHolder(View itemView) {
            super(itemView);

            swipable = itemView.findViewById(R.id.swipable);
            leftText = (TextView) itemView.findViewById(R.id.left_text);
            rightText = (TextView) itemView.findViewById(R.id.right_text);
            content = (FontTextView) itemView.findViewById(R.id.content);
            thumb = (LoadImageView) itemView.findViewById(R.id.thumb);
        }

        @Override
        public View getSwipeableContainerView() {
            return swipable;
        }
    }

    private class RecordAdapter extends RecyclerView.Adapter<RecordHolder>
            implements SwipeableItemAdapter<RecordHolder> {

        @Override
        public RecordHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecordHolder holder = new RecordHolder(getLayoutInflater().inflate(R.layout.item_record, parent, false));
            mLoadImageViewSet.add(new WeakReference<>(holder.thumb));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecordHolder holder, int position) {
            ACRecordRaw raw = mLazyList.get(position);
            String leftText = null;
            switch (raw.getType()) {
                case DB.AC_RECORD_POST:
                    leftText = getString(R.string.create_post);
                    break;
                case DB.AC_RECORD_REPLY:
                    leftText = getString(R.string.reply);
            }
            holder.leftText.setText(leftText);
            holder.rightText.setText(ReadableTime.getDisplayTime(raw.getTime()));
            holder.content.setText(raw.getContent());

            String image = raw.getImage();
            if (!TextUtils.isEmpty(image)) {
                holder.thumb.setVisibility(View.VISIBLE);
                holder.thumb.unload();
                holder.thumb.load(image, "dump", new LocalPathDataContain(image), false);
            } else {
                holder.thumb.setVisibility(View.GONE);
                holder.thumb.unload();
            }

            holder.content.setTextSize(Settings.getFontSize());
            holder.content.setLineSpacing(LayoutUtils.dp2pix(RecordActivity.this, Settings.getLineSpacing()), 1.0f);
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
        public int onGetSwipeReactionType(RecordHolder holder, int position, int x, int y) {
            return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H;
        }

        @Override
        public void onSetSwipeBackground(RecordHolder holder, int position, int type) {
            // Empty
        }

        @Override
        public void onSwipeItemStarted(RecordHolder holder, int position) {
            // Empty
        }

        @Override
        public SwipeResultAction onSwipeItem(RecordHolder holder, int position, int result) {
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
            final ACRecordRaw raw = mLazyList.get(position);
            if (raw != null) {
                DB.removeACRecord(mLazyList.get(position));
                updateLazyList();
                mAdapter.notifyItemRemoved(position);
                checkEmpty(true);

                Snackbar snackbar = Snackbar.make(mRecyclerView, R.string.record_deleted, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DB.addACRecord(raw.getType(), raw.getRecordid(), raw.getPostid(),
                                raw.getContent(), raw.getImage(), raw.getTime());
                        updateLazyList();
                        mAdapter.notifyDataSetChanged();
                        checkEmpty(true);
                    }
                });
                snackbar.setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            String image = raw.getImage();
                            if (image != null) {
                                new File(image).delete();
                            }
                        }
                    }
                });
                snackbar.show();
            }
        }
    }

    private static class LocalPathDataContain implements DataContainer {

        private File mFile;

        public LocalPathDataContain(String path) {
            File file = new File(path);
            if (file.isFile()) {
                mFile = file;
            }
        }

        @Override
        public void onUrlMoved(String requestUrl, String responseUrl) {
        }

        @Override
        public boolean save(InputStream is, long length, String mediaType, ProgressNotify notify) {
            return false;
        }

        @Override
        public InputStreamPipe get() {
            if (mFile != null) {
                return new FileInputStreamPipe(mFile);
            } else {
                return null;
            }
        }

        @Override
        public void remove() {
            // Empty
        }
    }
}
