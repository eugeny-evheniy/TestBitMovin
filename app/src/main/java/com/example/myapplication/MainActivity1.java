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
import android.util.Log;
import android.view.View;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.drm.DRMConfiguration;
import com.bitmovin.player.config.drm.DRMSystems;
import com.bitmovin.player.config.drm.WidevineConfiguration;
import com.bitmovin.player.config.media.AdaptiveSource;
import com.bitmovin.player.config.media.DASHSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.config.track.MimeTypes;
import com.bitmovin.player.config.track.SubtitleTrack;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

public class MainActivity1 extends AppCompatActivity {
    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinPlayer bitmovinPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        this.bitmovinPlayerView = (BitmovinPlayerView) this.findViewById(R.id.bitmovinPlayerView);
        this.bitmovinPlayer = this.bitmovinPlayerView.getPlayer();

        getPlayerInfo();
    }

    private void getPlayerInfo() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("https://academy.zenva.com/wp-json/zva-mobile-app/v1/demoWidevine", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);

                Log.v("JSON_R","Response: "+response);
                try {
                    JSONObject jsonObject = new JSONObject(response);

                    String streamingLocatorUrlWidevine = jsonObject.getString("streamingLocatorUrlWidevine");
                    String tokenWidevine = jsonObject.getString("tokenWidevine");
                    String ccUrl = jsonObject.getString("ccUrl");

                    initializePlayer(streamingLocatorUrlWidevine,tokenWidevine,ccUrl);

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }



            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
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

    protected void initializePlayer(String streamingUrl, String token, String cUrl) {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();

        AdaptiveSource adaptiveSource = new DASHSource(streamingUrl);
        SourceItem sourceItem = new SourceItem(adaptiveSource);

        // setup DRM handling
        String drmLicenseUrl = "https://zavideoplatform.keydelivery.eastus.media.azure.net/Widevine/?kid=29d061a3-82f0-4b19-8add-0a1395f17851";

        // Create a new source item
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);

        DRMConfiguration config = new WidevineConfiguration(drmLicenseUrl);
        config.setHttpHeaders(headers);
        sourceItem.addDRMConfiguration(config);

        sourceItem.addSubtitleTrack(cUrl, MimeTypes.TYPE_VTT ,"", null, true, "hi");
        // Add source item including DRM configuration to source configuration
        sourceConfiguration.addSourceItem(sourceItem);

        // load source using the created source configuration
        this.bitmovinPlayer.load(sourceConfiguration);
    }
}