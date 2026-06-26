package com.videviewer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.videviewer.R;
import com.videviewer.activities.LegalActivity;
import com.videviewer.activities.VaultActivity;
import com.videviewer.activities.PlaylistActivity;
import com.videviewer.activities.SettingsActivity;

public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        safe(view, R.id.card_vault, () ->
            startActivity(new Intent(requireContext(), VaultActivity.class)));

        safe(view, R.id.card_playlists, () ->
            startActivity(new Intent(requireContext(), PlaylistActivity.class)));

        safe(view, R.id.card_settings, () ->
            startActivity(new Intent(requireContext(), SettingsActivity.class)));

        safe(view, R.id.card_about, () ->
            openLegal("about"));

        safe(view, R.id.card_privacy, () ->
            openLegal("privacy"));

        safe(view, R.id.card_terms, () ->
            openLegal("terms"));

        safe(view, R.id.card_contact, () ->
            openLegal("contact"));

        safe(view, R.id.card_disclaimer, () ->
            openLegal("disclaimer"));
    }

    private void openLegal(String pageType) {
        Intent i = new Intent(requireContext(), LegalActivity.class);
        i.putExtra(LegalActivity.EXTRA_PAGE_TYPE, pageType);
        startActivity(i);
    }

    private void safe(View root, int id, Runnable action) {
        View v = root.findViewById(id);
        if (v != null) v.setOnClickListener(ignored -> {
            try { action.run(); } catch (Exception e) { e.printStackTrace(); }
        });
    }
}
