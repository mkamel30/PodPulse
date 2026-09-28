package de.danoeh.antennapod.ui.screen.home.sections;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.net.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;
import de.danoeh.antennapod.ui.discovery.DiscoveryFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrendingSection extends Fragment {
    public static final String TAG = "TrendingSection";
    private static final int NUM_PODCASTS = 16;

    private TextView trendingTitle;
    private View trendingSeeAll;
    private ChipGroup categoryChipGroup;
    private RecyclerView trendingRecyclerView;
    private ProgressBar trendingProgressBar;
    private TextView trendingEmptyText;

    private TrendingPodcastAdapter adapter;
    private Disposable disposable;
    private String countryCode = "EG";
    private int selectedGenreId = 0;

    private static class CategoryInfo {
        final int genreId;
        final int titleRes;
        CategoryInfo(int genreId, int titleRes) {
            this.genreId = genreId;
            this.titleRes = titleRes;
        }
    }

    private final CategoryInfo[] categories = new CategoryInfo[] {
        new CategoryInfo(0, R.string.category_all),
        new CategoryInfo(1318, R.string.category_technology),
        new CategoryInfo(1321, R.string.category_business),
        new CategoryInfo(1324, R.string.category_society_culture),
        new CategoryInfo(1311, R.string.category_news),
        new CategoryInfo(1303, R.string.category_comedy),
        new CategoryInfo(1304, R.string.category_education),
        new CategoryInfo(1545, R.string.category_sports),
        new CategoryInfo(1314, R.string.category_religion_spirituality)
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_section_trending, container, false);

        trendingTitle = view.findViewById(R.id.trendingTitle);
        trendingSeeAll = view.findViewById(R.id.trendingSeeAll);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        trendingRecyclerView = view.findViewById(R.id.trendingRecyclerView);
        trendingProgressBar = view.findViewById(R.id.trendingProgressBar);
        trendingEmptyText = view.findViewById(R.id.trendingEmptyText);

        SharedPreferences prefs = requireContext().getSharedPreferences(ItunesTopListLoader.PREFS, Context.MODE_PRIVATE);
        countryCode = prefs.getString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE, Locale.getDefault().getCountry());

        updateTitle();

        trendingSeeAll.setOnClickListener(v -> {
            startActivity(new MainActivityStarter(getContext())
                    .withFragmentLoaded(DiscoveryFragment.TAG)
                    .getIntent());
        });

        adapter = new TrendingPodcastAdapter(requireContext());
        trendingRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        trendingRecyclerView.setAdapter(adapter);

        setupChips();
        loadTrending(countryCode, selectedGenreId);

        return view;
    }

    private void updateTitle() {
        if (trendingTitle == null || getContext() == null) {
            return;
        }
        Locale locale = new Locale("", countryCode);
        String displayCountry = locale.getDisplayCountry();
        if (displayCountry != null && !displayCountry.isEmpty()) {
            trendingTitle.setText(getString(R.string.trending_in_country, displayCountry));
        } else {
            trendingTitle.setText(R.string.home_trending_title);
        }
    }

    private void setupChips() {
        if (categoryChipGroup == null || getContext() == null) {
            return;
        }
        categoryChipGroup.removeAllViews();
        for (CategoryInfo cat : categories) {
            Chip chip = new Chip(getContext());
            chip.setText(cat.titleRes);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setId(View.generateViewId());
            if (cat.genreId == selectedGenreId) {
                chip.setChecked(true);
            }
            final int genreId = cat.genreId;
            chip.setOnClickListener(v -> {
                if (selectedGenreId != genreId) {
                    selectedGenreId = genreId;
                    loadTrending(countryCode, selectedGenreId);
                }
            });
            categoryChipGroup.addView(chip);
        }
    }

    private void loadTrending(String country, int genreId) {
        if (disposable != null) {
            disposable.dispose();
        }

        trendingRecyclerView.setVisibility(View.GONE);
        trendingEmptyText.setVisibility(View.GONE);
        trendingProgressBar.setVisibility(View.VISIBLE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        disposable = Observable.fromCallable(() ->
                        loader.loadToplist(country, genreId, NUM_PODCASTS, DBReader.getFeedList()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(podcasts -> {
                    trendingProgressBar.setVisibility(View.GONE);
                    if (podcasts == null || podcasts.isEmpty()) {
                        trendingEmptyText.setVisibility(View.VISIBLE);
                        trendingRecyclerView.setVisibility(View.GONE);
                    } else {
                        trendingEmptyText.setVisibility(View.GONE);
                        trendingRecyclerView.setVisibility(View.VISIBLE);
                        adapter.updateList(podcasts);
                    }
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    trendingProgressBar.setVisibility(View.GONE);
                    trendingEmptyText.setVisibility(View.VISIBLE);
                    trendingRecyclerView.setVisibility(View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private static class TrendingPodcastAdapter extends RecyclerView.Adapter<TrendingViewHolder> {
        private final Context context;
        private final List<PodcastSearchResult> items = new ArrayList<>();

        TrendingPodcastAdapter(Context context) {
            this.context = context;
        }

        void updateList(List<PodcastSearchResult> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.trending_podcast_item, parent, false);
            return new TrendingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
            PodcastSearchResult item = items.get(position);
            holder.podcastTitle.setText(item.title);
            holder.podcastAuthor.setText(item.author != null ? item.author : "");

            float radius = 16 * context.getResources().getDisplayMetrics().density;
            Glide.with(context)
                    .load(item.imageUrl)
                    .apply(new RequestOptions()
                            .placeholder(ImagePlaceholder.getDrawable(context, radius))
                            .transform(new CenterCrop(), new RoundedCorners((int) radius))
                            .dontAnimate())
                    .into(holder.podcastCover);

            holder.podcastCard.setOnClickListener(v -> {
                if (item.feedUrl != null) {
                    context.startActivity(new OnlineFeedviewActivityStarter(context, item.feedUrl).getIntent());
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    static class TrendingViewHolder extends RecyclerView.ViewHolder {
        final CardView podcastCard;
        final ImageView podcastCover;
        final TextView podcastTitle;
        final TextView podcastAuthor;

        TrendingViewHolder(@NonNull View itemView) {
            super(itemView);
            podcastCard = itemView.findViewById(R.id.podcastCard);
            podcastCover = itemView.findViewById(R.id.podcastCover);
            podcastTitle = itemView.findViewById(R.id.podcastTitle);
            podcastAuthor = itemView.findViewById(R.id.podcastAuthor);
        }
    }
}
