/*
 * Bitmovin Player Android SDK
 * Copyright (C) 2017, Bitmovin GmbH, All Rights Reserved
 *
 * This source code and its use and distribution, is subject to the terms
 * and conditions of the applicable license agreement.
 */

package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.drm.DRMConfiguration;
import com.bitmovin.player.config.drm.DRMSystems;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.dmk.drm_player.MainActivity;

import java.net.URLEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity1 extends AppCompatActivity {
    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinPlayer bitmovinPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        findViewById(R.id.btnExo).setOnClickListener(v -> {
            startExoPlayer();
        });
        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            play();
        });

        this.bitmovinPlayerView = (BitmovinPlayerView) this.findViewById(R.id.bitmovinPlayerView);
        this.bitmovinPlayer = this.bitmovinPlayerView.getPlayer();

        this.initializePlayer();
    }

    @Override
    protected void onStart() {
        this.bitmovinPlayerView.onStart();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.bitmovinPlayerView.onResume();
    }

    @Override
    protected void onPause() {
        this.bitmovinPlayerView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        this.bitmovinPlayerView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.bitmovinPlayerView.onDestroy();
        super.onDestroy();
    }

    public void play() {
        initializePlayer();
        bitmovinPlayer.play();
    }

    public void startExoPlayer() {
        startActivity(new Intent(this, MainActivity.class));
    }

    protected void initializePlayer() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();

        // Create a new source item
//        SourceItem sourceItem = new SourceItem("https://test.playready.microsoft.com/smoothstreaming/SSWSS720H264/SuperSpeedway_720.ism/manifest");
        SourceItem sourceItem = new SourceItem(Uri.parse("https://zavideoplatform.streaming.mediaservices.windows.net///2c856c2a-b469-4a0d-a6c1-984fee7017c9/03. Creating Numpy Arrays.ism/manifest").toString());
        // setup DRM handling
//        String drmLicenseUrl = "https://widevine-proxy.appspot.com/proxy";
//        UUID drmSchemeUuid = DRMSystems.WIDEVINE_UUID;
//        sourceItem.addDRMConfiguration(drmSchemeUuid, drmLicenseUrl);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2FjYWRlbXkuemVudmEuY29tIiwiaWF0IjoxNTU1MjE3NzcwLCJleHAiOjE1NTUyMzIyMDAsImF1ZCI6Imh0dHBzOi8vYWNhZGVteS56ZW52YS5jb20iLCJzdWIiOjAsInp2YWlwIjoiMjMuODkuMTUxLjk3IiwienZhcG9zdCI6MH0.O20JwOmS9n4dh_8wxznw6XbUFV_8Za_GQEeY6v_a1CU");
        // setup DRM handling
        String drmLicenseUrl = "https://zavideoplatform.keydelivery.eastus.media.azure.net/Widevine/?kid=29d061a3-82f0-4b19-8add-0a1395f17851";
        UUID drmSchemeUuid = DRMSystems.WIDEVINE_UUID;


        DRMConfiguration drmConf = null;

        try {
            drmConf = new DRMConfiguration.Builder()
                    .uuid(drmSchemeUuid)
                    .licenseUrl(drmLicenseUrl)
                    .build();
            drmConf.setHttpHeaders(headers);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sourceItem.addDRMConfiguration(drmConf);

        // Add source item including DRM configuration to source configuration
        sourceConfiguration.addSourceItem(sourceItem);

        // load source using the created source configuration
        this.bitmovinPlayer.load(sourceConfiguration);
    }
}