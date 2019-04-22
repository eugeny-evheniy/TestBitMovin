/*
 * Bitmovin Player Android SDK
 * Copyright (C) 2017, Bitmovin GmbH, All Rights Reserved
 *
 * This source code and its use and distribution, is subject to the terms
 * and conditions of the applicable license agreement.
 */

package com.example.myapplication;

import com.bitmovin.player.offline.options.OfflineContentOptions;
import com.bitmovin.player.offline.options.OfflineOptionEntry;
import com.bitmovin.player.offline.options.ThumbnailOfflineOptionEntry;

import java.util.ArrayList;
import java.util.List;

public class Util
{
    public  static  String WIDEVINE_DRM_LICENSE = "https://zavideoplatform.keydelivery.eastus.media.azure.net/Widevine/?kid=29d061a3-82f0-4b19-8add-0a1395f17851";
    private Util()
    {
    }

    /**
     * Returns the video, audio and text options of the {@link OfflineContentOptions} in one list
     *
     * @param offlineContentOptions
     * @return
     */
    public static List<OfflineOptionEntry> getAsOneList(OfflineContentOptions offlineContentOptions)
    {
        List<OfflineOptionEntry> offlineOptionEntries = new ArrayList<OfflineOptionEntry>(offlineContentOptions.getVideoOptions());
        offlineOptionEntries.addAll(offlineContentOptions.getAudioOptions());
        offlineOptionEntries.addAll(offlineContentOptions.getTextOptions());
        ThumbnailOfflineOptionEntry thumbnailOfflineOptionEntry = offlineContentOptions.getThumbnailOption();
        if (thumbnailOfflineOptionEntry != null)
        {
            offlineOptionEntries.add(thumbnailOfflineOptionEntry);
        }
        return offlineOptionEntries;
    }
}
