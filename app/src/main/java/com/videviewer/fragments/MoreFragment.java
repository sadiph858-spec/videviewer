package com.videviewer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.videviewer.R;
import com.videviewer.activities.*;

/**
 * MoreFragment - Quick-access hub for all extra features
 */
public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Private Vault
        view.findViewById(R.id.card_vault).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), VaultActivity.class)));

        // Playlists
        view.findViewById(R.id.card_playlists).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), PlaylistActivity.class)));

        // Settings
        view.findViewById(R.id.card_settings).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), SettingsActivity.class)));

        // Privacy Policy
        view.findViewById(R.id.card_privacy).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), PrivacyPolicyActivity.class)));

        // Terms
        view.findViewById(R.id.card_terms).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), TermsActivity.class)));

        // About
        view.findViewById(R.id.card_about).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), AboutActivity.class)));

        // Contact
        view.findViewById(R.id.card_contact).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), ContactActivity.class)));

        // Disclaimer
        view.findViewById(R.id.card_disclaimer).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), DisclaimerActivity.class)));
    }
}
