package com.example.xyzreader.ui;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());
    // Use default locale format
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, ArticleDetailFragment.this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        //mPhotoView = mRootView.findViewById(R.id.photo);

        mStatusBarColorDrawable = new ColorDrawable(0);

        bindViews();
        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        WebView bodyView = mRootView.findViewById(R.id.article_body);

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            String title =  mCursor.getString(ArticleLoader.Query.TITLE);
            Date publishedDate = parsePublishedDate();
            String date;
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                date = DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR);

            } else {
                // If date is before 1902, just show the string
                date =  SimpleDateFormat.getDateTimeInstance().format(publishedDate) + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR);

            }
            bodyView.loadData(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />"),  "text/html", "UTF-8");
            String imageAddress = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            String thumbURL = mCursor.getString(ArticleLoader.Query.THUMB_URL);

            ((ArticleDetailActivity) requireActivity()).setTitleAppearance(imageAddress);
            final TextView articleTitleView = mRootView.findViewById(R.id.article_title);
            articleTitleView.setText(title);
            final TextView articleBylineView = mRootView.findViewById(R.id.article_byline);
            articleBylineView.setText(date);
            ImageView smallImage = mRootView.findViewById(R.id.article_thumb);
            Glide.with(this)
                    .asBitmap()
                    .load(thumbURL)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            if (resource != null) {
                                Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(@NonNull Palette palette) {
                                        mMutedColor = palette.getDominantColor(
                                                ContextCompat.getColor(requireContext(), R.color.colorSecondaryDark));
                                        mRootView.findViewById(R.id.meta_bar)
                                                .setBackgroundColor(mMutedColor);
                                        int mContrastColor = Color.rgb(255-Color.red(mMutedColor),
                                                255-Color.green(mMutedColor),
                                                255-Color.blue(mMutedColor));
                                        if (mMutedColor != mContrastColor) {
                                            articleTitleView.setTextColor(mContrastColor);
                                            articleBylineView.setTextColor(mContrastColor);
                                        }
                                    }
                                });
                                return false;
                            }
                            return false;
                        }
                    })
                    .into(smallImage);
        } else {
            mRootView.setVisibility(View.GONE);
            bodyView.loadData("N/A", null, null);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i("LOG", "loader is created");
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        } else {

            Log.i("LOG", "data is ready");
            bindViews();
        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
