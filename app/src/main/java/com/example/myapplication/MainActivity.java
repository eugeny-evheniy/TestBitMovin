/*
 * Bitmovin Player Android SDK
 * Copyright (C) 2017, Bitmovin GmbH, All Rights Reserved
 *
 * This source code and its use and distribution, is subject to the terms
 * and conditions of the applicable license agreement.
 */

package com.example.myapplication;

import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.bitmovin.player.DrmLicenseKeyExpiredException;
import com.bitmovin.player.IllegalOperationException;
import com.bitmovin.player.NoConnectionException;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.config.drm.DRMConfiguration;
import com.bitmovin.player.config.drm.WidevineConfiguration;
import com.bitmovin.player.config.media.AdaptiveSource;
import com.bitmovin.player.config.media.DASHSource;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.config.track.MimeTypes;
import com.bitmovin.player.offline.OfflineContentManager;
import com.bitmovin.player.offline.OfflineContentManagerListener;
import com.bitmovin.player.offline.OfflineSourceItem;
import com.bitmovin.player.offline.options.OfflineContentOptions;
import com.bitmovin.player.offline.options.OfflineOptionEntry;
import com.bitmovin.player.offline.options.OfflineOptionEntryAction;
import com.bitmovin.player.offline.options.OfflineOptionEntryState;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OfflineContentManagerListener, ListItemActionListener
{
    private File rootFolder;
    private List<ListItem> listItems;
    private ListView listView;
    private ListAdapter listAdapter;
    private Gson gson;

    private boolean retryOfflinePlayback = true;
    private ListItem listItemForRetry = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        this.gson = new Gson();
        this.listView = (ListView) findViewById(R.id.listview);

        // Get the folder into which the downloaded offline content will be stored.
        // There can be multiple of such root folders and every can contain several offline contents.
        this.rootFolder = this.getDir("offline", ContextWrapper.MODE_PRIVATE);

        // Creating the ListView containing 2 example streams, which can be downloaded using this app.
        this.listItems = getListItems();
        this.listAdapter = new ListAdapter(this, 0, this.listItems, this);
        this.listView.setAdapter(this.listAdapter);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                onListItemClicked((ListItem) parent.getItemAtPosition(position));
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        requestOfflineContentOptions(this.listItems);
    }

    @Override
    protected void onStop()
    {
        for (ListItem listItem : this.listItems)
        {
            listItem.getOfflineContentManager().release();
        }
        this.gson = null;
        this.listItems = null;
        this.listAdapter = null;
        this.listView.setOnItemClickListener(null);
        super.onStop();
    }

    private void requestOfflineContentOptions(List<ListItem> listItems)
    {
        for (ListItem listItem : listItems)
        {
            // Request OfflineContentOptions from the OfflineContentManager.
            // Note that the getOptions call is asynchronous, and that the result will be delivered to the according listener method onOptionsAvailable
            listItem.getOfflineContentManager().getOptions();
        }
    }

    private void onListItemClicked(ListItem listItem)
    {
        playSource(listItem);
    }

    private void playSource(ListItem listItem)
    {
        SourceItem sourceItem = null;
        try
        {
            // First we try to get an OfflineSourceItem from the OfflineContentManager, as we prefer offline content
            sourceItem = listItem.getOfflineContentManager().getOfflineSourceItem();
        }
        catch (IOException e)
        {
            // If it fails to load needed files
        }
        catch (DrmLicenseKeyExpiredException e)
        {
            try
            {
                this.listItemForRetry = listItem;
                this.retryOfflinePlayback = true;
                listItem.getOfflineContentManager().renewOfflineLicense();
            }
            catch (NoConnectionException e1)
            {
                Toast.makeText(this, "The DRM license expired, but there is no network connection", Toast.LENGTH_LONG).show();
            }
        }

        // If no offline content is available, or it fails to get an OfflineSourceItem, we take the original SourceItem for online streaming
        if (sourceItem == null)
        {
//            return;
            sourceItem = listItem.getSourceItem();
        }
        startPlayerActivity(sourceItem);
    }

    private void startPlayerActivity(SourceItem sourceItem)
    {
        Intent playerActivityIntent = new Intent(this, PlayerActivity.class);

        // Add the SourceItem to the Intent
        String extraName = sourceItem instanceof OfflineSourceItem ? PlayerActivity.OFFLINE_SOURCE_ITEM : PlayerActivity.SOURCE_ITEM;
        playerActivityIntent.putExtra(extraName, gson.toJson(sourceItem));

        //Start the PlayerActivity
        startActivity(playerActivityIntent);
    }

    /*
     * Implementation of the OfflineContentManagerListener methods
     */

    @Override
    public void onCompleted(SourceItem sourceItem, OfflineContentOptions offlineContentOptions)
    {
        ListItem listItem = getListItemWithSourceItem(sourceItem);
        if (listItem != null)
        {
            // Update the OfflineContentOptions, reset progress and notify the ListAdapter to update the views
            listItem.setOfflineContentOptions(offlineContentOptions);
            listItem.setProgress(0);
            this.listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onError(SourceItem sourceItem, ErrorEvent errorEvent)
    {
        Toast.makeText(this, errorEvent.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgress(SourceItem sourceItem, float progress)
    {
        ListItem listItem = getListItemWithSourceItem(sourceItem);
        if (listItem != null)
        {
            float oldProgress = listItem.getProgress();
            listItem.setProgress(progress);

            // Only show full progress changes
            if ((int) oldProgress != (int) progress)
            {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onOptionsAvailable(SourceItem sourceItem, OfflineContentOptions offlineContentOptions)
    {
        ListItem listItem = getListItemWithSourceItem(sourceItem);
        if (listItem != null)
        {
            // Update the OfflineContentOptions and notify the ListAdapter to update the views
            listItem.setOfflineContentOptions(offlineContentOptions);
            this.listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDrmLicenseUpdated(SourceItem sourceItem)
    {
        if (this.retryOfflinePlayback)
        {
            if (this.listItemForRetry.getSourceItem() == sourceItem)
            {
                // At the last try, the license was expired
                // so we try it now again
                ListItem listItem = this.listItemForRetry;
                this.retryOfflinePlayback = false;
                this.listItemForRetry = null;
                playSource(listItem);
            }
        }
    }

    @Override
    public void onSuspended(SourceItem sourceItem)
    {
        Toast.makeText(this, "Suspended: " + sourceItem.getTitle(),Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResumed(SourceItem sourceItem)
    {
        Toast.makeText(this, "Resumed: " + sourceItem.getTitle(),Toast.LENGTH_SHORT).show();
    }

    /*
     * Listener methods for the two buttons every ListItem has
     */

    @Override
    public void showSelectionDialog(ListItem listItem)
    {
        OfflineContentOptions offlineContentOptions = listItem.getOfflineContentOptions();

        // Generating the needed lists, to create an AlertDialog, listing all options
        List<OfflineOptionEntry> entries = Util.getAsOneList(offlineContentOptions);
        String[] entriesAsText = new String[entries.size()];
        boolean[] entriesCheckList = new boolean[entries.size()];
        for (int i = 0; i < entriesAsText.length; i++)
        {
            OfflineOptionEntry oh = entries.get(i);
            try
            {
                // Resetting the Action if set
                oh.setAction(null);
            }
            catch (IllegalOperationException e)
            {
                // Won't happen
            }
            entriesAsText[i] = oh.getId() + "-" + oh.getMimeType();
            entriesCheckList[i] = oh.getState() == OfflineOptionEntryState.DOWNLOADED || oh.getAction() == OfflineOptionEntryAction.DOWNLOAD;
        }

        // Building and showing the AlertDialog
        AlertDialog.Builder dialogBuilder = generateAlertDialogBuilder(listItem, entries, entriesAsText, entriesCheckList);
        dialogBuilder.show();
    }

    @Override
    public void delete(ListItem listItem)
    {
        // To delete everything of a specific OfflineContentManager, we call deleteAll
        listItem.getOfflineContentManager().deleteAll();
        Toast.makeText(this, "Deleting " + listItem.getSourceItem().getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void suspend(ListItem listItem)
    {
        listItem.getOfflineContentManager().suspend();
    }

    @Override
    public void resume(ListItem listItem)
    {
        listItem.getOfflineContentManager().resume();
    }

    public void download(ListItem listItem)
    {
        OfflineContentManager offlineContentManager = listItem.getOfflineContentManager();
        if (offlineContentManager == null)
        {
            return;
        }

        try
        {
            // Passing the OfflineContentOptions with set OfflineOptionEntryActions to the OfflineContentManager
            offlineContentManager.process(listItem.getOfflineContentOptions());
        }
        catch (NoConnectionException e)
        {
            e.printStackTrace();
        }
    }

    private AlertDialog.Builder generateAlertDialogBuilder(final ListItem listItem, final List<OfflineOptionEntry> entries, String[] entriesAsText, boolean[] entryCheckList)
    {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this).setMultiChoiceItems(entriesAsText, entryCheckList, new DialogInterface.OnMultiChoiceClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked)
            {
                try
                {
                    // Set an Download/Delete action, if the user changes the checked state
                    OfflineOptionEntry offlineOptionEntry = entries.get(which);
                    offlineOptionEntry.setAction(isChecked ? OfflineOptionEntryAction.DOWNLOAD : OfflineOptionEntryAction.DELETE);
                }
                catch (IllegalOperationException e)
                {
                }

            }
        });
        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                download(listItem);
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.cancel, null);
        return dialogBuilder;
    }

    private ListItem getListItemWithSourceItem(SourceItem sourceItem)
    {
        // Find the matching SourceItem in the List, containing all our SourceItems
        for (ListItem listItem : this.listItems)
        {
            if (listItem.getSourceItem() == sourceItem)
            {
                return listItem;
            }
        }
        return null;
    }

    private List<ListItem> getListItems()
    {
        List<ListItem> listItems = new ArrayList<>();

        // Initialize a SourceItem with a DRM configuration
        AdaptiveSource adaptiveSource = new DASHSource("https://zavideoplatform.streaming.mediaservices.windows.net///2c856c2a-b469-4a0d-a6c1-984fee7017c9/03. Creating Numpy Arrays.ism/manifest(format=mpd-time-csf,encryption=cenc)");
        SourceItem sourceItem = new SourceItem(adaptiveSource);
        sourceItem.setTitle("Zenva");
        // setup DRM handling
        String drmLicenseUrl = "https://zavideoplatform.keydelivery.eastus.media.azure.net/Widevine/?kid=29d061a3-82f0-4b19-8add-0a1395f17851";

        // Create a new source item
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2FjYWRlbXkuemVudmEuY29tIiwiaWF0IjoxNTU1Nzg5ODI0LCJleHAiOjE1NTU4MDQyNTQsImF1ZCI6Imh0dHBzOi8vYWNhZGVteS56ZW52YS5jb20iLCJzdWIiOjAsInp2YWlwIjoiMTA0LjE1MS42LjUyIiwienZhcG9zdCI6MH0.jfWk8QGdcKjXTxp8oj4Lfehr5mgE5JDq5KvHPnasakY");

        DRMConfiguration config = new WidevineConfiguration(drmLicenseUrl);
        config.setHttpHeaders(headers);
        sourceItem.addDRMConfiguration(config);

        sourceItem.addSubtitleTrack("https://zavideoplatform.blob.core.windows.net/closed-captions/lesson-635112-en.vtt", MimeTypes.TYPE_VTT ,"", null, true, "hi");

        // Initialize an OfflineContentManager in the rootFolder with the id "artOfMotionDrm"
        OfflineContentManager zenvaDrmOfflineContentManager = OfflineContentManager.getOfflineContentManager(sourceItem, this.rootFolder.getPath(), "Zenva", this, this);

        // Create a ListItem from the SourceItem and the OfflienContentManager
        ListItem zenvaDrmListItem = new ListItem(sourceItem, zenvaDrmOfflineContentManager);

        // Add the ListItem to the List
        listItems.add(zenvaDrmListItem);


        return listItems;
    }
}
