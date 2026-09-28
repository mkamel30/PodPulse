package de.danoeh.antennapod.ui.screen.episode;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.AiSummaryDialogBinding;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AISummaryDialog extends BottomSheetDialogFragment {
    public static final String TAG = "AISummaryDialog";
    private static final String ARG_FEED_ITEM = "feedItem";

    private FeedItem item;
    private AiSummaryDialogBinding viewBinding;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public static AISummaryDialog newInstance(FeedItem item) {
        AISummaryDialog dialog = new AISummaryDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FEED_ITEM, item);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            item = (FeedItem) getArguments().getSerializable(ARG_FEED_ITEM);
        }
        viewBinding = AiSummaryDialogBinding.inflate(inflater, container, false);

        viewBinding.btnShareSummary.setEnabled(false);
        if (item != null) {
            viewBinding.episodeTitle.setText(item.getTitle());
            viewBinding.summaryText.setText(R.string.ai_summary_loading);
            viewBinding.loadingProgress.setVisibility(View.VISIBLE);
            loadOrGenerateSummary();
        }

        viewBinding.btnClose.setOnClickListener(v -> dismiss());
        viewBinding.btnShareSummary.setOnClickListener(v -> shareSummary());

        return viewBinding.getRoot();
    }

    private void loadOrGenerateSummary() {
        if (item == null) {
            return;
        }

        disposables.add(io.reactivex.rxjava3.core.Single.fromCallable(() -> {
            String episodeUrl = getEpisodeIdentifier();
            String cachedSummary = fetchFromRemote(episodeUrl);

            if (!TextUtils.isEmpty(cachedSummary)) {
                return cachedSummary;
            } else {
                String generatedSummary = extractSummary(item);
                uploadToRemote(episodeUrl, item.getTitle(), generatedSummary);
                return generatedSummary;
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::displaySummary, error -> {
            Log.e(TAG, "Error generating summary", error);
            if (item != null) {
                displaySummary(extractSummary(item));
            }
        }));
    }

    private String getEpisodeIdentifier() {
        if (item.getMedia() != null && !TextUtils.isEmpty(item.getMedia().getDownloadUrl())) {
            return item.getMedia().getDownloadUrl();
        }
        if (!TextUtils.isEmpty(item.getLink())) {
            return item.getLink();
        }
        return "episode_" + item.getId();
    }

    private String getBaseServerUrl() {
        String host = SynchronizationCredentials.getHosturl();
        if (!TextUtils.isEmpty(host)) {
            return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        }
        return null;
    }

    private String fetchFromRemote(String episodeUrl) {
        String baseUrl = getBaseServerUrl();
        if (baseUrl == null) {
            return null;
        }
        try {
            String encoded = URLEncoder.encode(episodeUrl, StandardCharsets.UTF_8.name());
            String url = baseUrl + "/api/podpulse/summaries?episode_url=" + encoded;
            Request request = new Request.Builder().url(url).build();
            try (Response response = AntennapodHttpClient.getHttpClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JSONObject obj = new JSONObject(jsonStr);
                    StringBuilder sb = new StringBuilder();
                    if (obj.has("summary") && !obj.isNull("summary")) {
                        sb.append(obj.getString("summary")).append("\n\n");
                    }
                    if (obj.has("key_takeaways") && !obj.isNull("key_takeaways")) {
                        JSONArray arr = obj.getJSONArray("key_takeaways");
                        for (int i = 0; i < arr.length(); i++) {
                            sb.append("• ").append(arr.getString(i)).append("\n");
                        }
                    }
                    return sb.toString().trim();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Remote summary fetch error: " + e.getMessage());
        }
        return null;
    }

    private void uploadToRemote(String episodeUrl, String title, String summary) {
        String baseUrl = getBaseServerUrl();
        if (baseUrl == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("episode_url", episodeUrl);
            json.put("title", title != null ? title : "");
            json.put("summary", summary);
            json.put("key_takeaways", new JSONArray());

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/podpulse/summaries")
                    .post(body)
                    .build();

            AntennapodHttpClient.getHttpClient().newCall(request).execute().close();
        } catch (Exception e) {
            Log.d(TAG, "Remote summary upload error: " + e.getMessage());
        }
    }

    private String extractSummary(FeedItem feedItem) {
        String description = feedItem.getDescription();
        if (TextUtils.isEmpty(description)) {
            return isAdded() ? getString(R.string.no_media_label) : "";
        }

        String plainText = Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        String[] sentences = plainText.split("(?<=[.!?])\\s+");

        List<String> keyPoints = new ArrayList<>();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 25 && trimmed.length() < 220) {
                keyPoints.add("• " + trimmed);
            }
            if (keyPoints.size() >= 4) {
                break;
            }
        }

        if (keyPoints.isEmpty()) {
            return plainText.length() > 300 ? plainText.substring(0, 300) + "…" : plainText;
        }

        return TextUtils.join("\n\n", keyPoints);
    }

    private void displaySummary(String summary) {
        if (viewBinding != null) {
            viewBinding.loadingProgress.setVisibility(View.GONE);
            viewBinding.summaryText.setText(summary);
            viewBinding.btnShareSummary.setEnabled(!TextUtils.isEmpty(summary));
        }
    }

    private void shareSummary() {
        if (item == null || viewBinding == null || !isAdded()) {
            return;
        }
        String textToShare = item.getTitle() + "\n\n"
                + getString(R.string.ai_summary_label) + ":\n"
                + viewBinding.summaryText.getText().toString();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
        intent.putExtra(Intent.EXTRA_TEXT, textToShare);
        startActivity(Intent.createChooser(intent, getString(R.string.share_label)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        viewBinding = null;
    }
}
