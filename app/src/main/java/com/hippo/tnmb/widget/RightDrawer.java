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

package com.hippo.tnmb.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.drawerlayout.DrawerLayoutChild;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.tnmb.R;
import com.hippo.tnmb.client.data.Forum;
import com.hippo.ripple.Ripple;
import com.hippo.util.DrawableManager;
import com.hippo.yorozuya.ResourcesUtils;

import java.util.ArrayList;
import java.util.List;

public final class RightDrawer extends EasyRecyclerView
        implements EasyRecyclerView.OnItemClickListener, DrawerLayoutChild {

    private static final Object COMMOM_POSTS = new Object();

    private ForumAdapter mAdapter;

    private List<Object> mForums;

    private RightDrawerHelper mRightDrawerHelper;

    private Forum mActivatedForum;

    private int mFitPaddingTop = 0;
    private int mActionBarHeight = 0;

    public RightDrawer(Context context) {
        super(context);
        init(context);
    }

    public RightDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RightDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mForums = new ArrayList<>();
        mAdapter = new ForumAdapter();
        setAdapter(mAdapter);
        setLayoutManager(new LinearLayoutManager(context));
        setOnItemClickListener(this);
        setSelector(Ripple.generateRippleDrawable(
                context, ResourcesUtils.getAttrBoolean(context, R.attr.dark)));

        mActionBarHeight = ResourcesUtils.getAttrDimensionPixelOffset(context, R.attr.actionBarSize);
    }

    public void setForums(List<? extends Forum> forums) {
        mForums.clear();
        mForums.addAll(forums);
        mForums.add(COMMOM_POSTS);
        mAdapter.notifyDataSetChanged();
    }

    public void setRightDrawerHelper(RightDrawerHelper listener) {
        mRightDrawerHelper = listener;
    }

    public void setActivatedForum(Forum forum) {
        mActivatedForum = forum;
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (mRightDrawerHelper != null) {
            Object data = mForums.get(position);
            if (data instanceof Forum) {
                mRightDrawerHelper.onSelectForum((Forum) data);
            } else {
                mRightDrawerHelper.onClickCommonPosts();
            }
        }

        return true;
    }

    @Override
    public void setFitPadding(int top, int bottom) {
        mFitPaddingTop = top;
    }

    @Override
    public int getLayoutPaddingTop() {
        return mFitPaddingTop + mActionBarHeight;
    }

    @Override
    public int getLayoutPaddingBottom() {
        return 0;
    }

    private class ForumHolder extends RecyclerView.ViewHolder {

        public ForumHolder(View itemView) {
            super(itemView);
        }
    }

    private static final int TYPE_COMMOM_POSTS = 0;
    private static final int TYPE_FORUM = 1;

    private class ForumAdapter extends RecyclerView.Adapter<ForumHolder> {

        @Override
        public ForumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_COMMOM_POSTS) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_commom_posts, parent, false);
                Drawable tag = DrawableManager.getDrawable(getContext(), R.drawable.v_tag_x24);
                tag.setBounds(0, 0, tag.getIntrinsicWidth(), tag.getIntrinsicHeight());
                TextView tv = (TextView) view;
                tv.setText(R.string.common_posts);
                tv.setCompoundDrawables(tag, null, null, null);
            } else {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_forum_drawer, parent, false);
            }
            return new ForumHolder(view);
        }

        @Override
        public void onBindViewHolder(ForumHolder holder, int position) {
            Object data = mForums.get(position);
            if (data instanceof Forum && getItemViewType(position) == TYPE_FORUM) {
                ((TextView) holder.itemView).setText(((Forum) data).getNMBDisplayname());
                holder.itemView.setActivated(data == mActivatedForum);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) {
                return TYPE_COMMOM_POSTS;
            } else {
                return TYPE_FORUM;
            }
        }

        @Override
        public int getItemCount() {
            return mForums.size();
        }
    }

    public interface RightDrawerHelper {

        void onClickCommonPosts();

        void onSelectForum(Forum forum);
    }
}
