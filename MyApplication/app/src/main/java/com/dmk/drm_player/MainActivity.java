package com.dmk.drm_player;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionEventListener;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.OfflineLicenseHelper;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor;
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer2.offline.FilteringManifestParser;
import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.chunk.ChunkExtractorWrapper;
import com.google.android.exoplayer2.source.chunk.InitializationChunk;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.dash.manifest.AdaptationSet;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser;
import com.google.android.exoplayer2.source.dash.manifest.Period;
import com.google.android.exoplayer2.source.dash.manifest.RangedUri;
import com.google.android.exoplayer2.source.dash.manifest.Representation;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "OfflineDRM";
    private TextView txtStreamMessage;
    private SimpleExoPlayer simpleExoPlayer;
    private TrackSelector trackSelector;
    private SimpleExoPlayerView exoPlayerView;
    private boolean isPlaying = false;
    private boolean shouldAutoPlay = true;
    private DataSource.Factory dataSourceFactory;
    private FrameworkMediaDrm mediaDrm;
    private DebugTextViewHelper debugViewHelper;
    private SubtitleView subtitles;


    @Override
    public void onPause() {
        super.onPause();

        if(simpleExoPlayer!=null)
            simpleExoPlayer.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (exoPlayerView != null)
            exoPlayerView.getOverlayFrameLayout().removeAllViews();
    }

    /** Returns a new DataSource factory. */
    private DataSource.Factory buildDataSourceFactory() {
        return ((DemoApplication) getApplication()).buildDataSourceFactory();
    }

    private List<StreamKey> getOfflineStreamKeys(Uri uri) {
        return ((DemoApplication) getApplication()).getDownloadTracker().getOfflineStreamKeys(uri);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStreamMessage = findViewById(R.id.txtStreamMessage);
        exoPlayerView = findViewById(R.id.exo_player_view);
        subtitles=(SubtitleView)findViewById(R.id.subtitle);


        dataSourceFactory = buildDataSourceFactory();

        File manifest =  new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/exoplayer/Manifest.mpd");

       // initializeOfflinePlayer(Uri.fromFile(manifest),"https://zavideoplatform.keydelivery.eastus.media.azure.net/Widevine/?kid=29d061a3-82f0-4b19-8add-0a1395f17851","");



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


                    DownloadTask downloadTask = new DownloadTask(MainActivity.this);
                    downloadTask.execute(streamingLocatorUrlWidevine,tokenWidevine);
                   // initializeOfflinePlayer(Uri.fromFile(manifest),"https://zavideoplatform.keydelivery.eastus.media.azure.net/Widevine/?kid=29d061a3-82f0-4b19-8add-0a1395f17851","");


                  //  initializeOfflinePlayer(Uri.fromFile(manifest),streamingLocatorUrlWidevine,tokenWidevine);

                    //   downloadVideoManifestFile(streamingLocatorUrlWidevine,tokenWidevine);

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }



            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });


    }

    private void downloadVideoManifestFile(String streamingLocatorUrlWidevine, String tokenWidevine) {

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization","Bearer "+tokenWidevine);
        client.get(streamingLocatorUrlWidevine, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String raw = new String(responseBody);

                File manifest =  new File(Environment.getExternalStorageDirectory()+"/exoplayer/","KManifest.mpd");

                FileOutputStream stream =null;
                try {
                    manifest.createNewFile();
                   stream = new FileOutputStream(manifest);

                    stream.write(responseBody);
                } catch (FileNotFoundException e){
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }finally {

                    try {
                        if(stream!=null)
                        stream.close();
                    }catch (IOException r){
                        r.printStackTrace();
                    }

                }



            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }


    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializeOfflinePlayer(Uri uri,String drmLicenseUrl,String token) {


        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;

        HashMap<String,String> keyRequestPropertiesArray = new HashMap<>();
               keyRequestPropertiesArray.put("Authorization", "Bearer "+token);

        String[] keyRequestPropertiesArray1 = new String[]{"Authorization", "Bearer "+token};



        boolean multiSession = false;//DRM_MULTI_SESSION_EXTRA
        String errorStringId ="Unknown";
        if (Util.SDK_INT < 18) {
            errorStringId = "DRM Not supported!";
        } else {
            try {

                String drmSchemeExtra = "widevine";

                UUID drmSchemeUuid = Util.getDrmUuid(drmSchemeExtra);
                if (drmSchemeUuid == null) {
                    errorStringId = "Unsupported Scheme";
                } else {
                    drmSessionManager = buildOfflineDrmSessionManager(drmSchemeUuid, drmLicenseUrl,keyRequestPropertiesArray);

                }
            } catch (UnsupportedDrmException e) {
                errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                        ? "Unsupported Scheme" :"Unknown !!";
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DrmSession.DrmSessionException e) {
                e.printStackTrace();
            }

        }
        if (drmSessionManager == null) {
            Toast.makeText(MainActivity.this,errorStringId,Toast.LENGTH_LONG).show();
            finish();
            return;
        }



        ///////////



        RenderersFactory renderersFactory = new DefaultRenderersFactory(this,drmSessionManager);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(bandwidthMeter));

        simpleExoPlayer =  ExoPlayerFactory.newSimpleInstance(
                /* context= */ MainActivity.this, renderersFactory, trackSelector, drmSessionManager);




        DashMediaSource  mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                .setManifestParser(
                        new FilteringManifestParser<>(new DashManifestParser(), getOfflineStreamKeys(Uri.parse(drmLicenseUrl))))
                .createMediaSource(Uri.parse(drmLicenseUrl));



        Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP,
                Format.NO_VALUE,"hi");


        exoPlayerView.setPlayer(simpleExoPlayer);
        exoPlayerView.hideController();


        simpleExoPlayer.prepare(mediaSource);
        simpleExoPlayer.setPlayWhenReady(shouldAutoPlay);
        simpleExoPlayer.addTextOutput(new TextRenderer.Output() {
            @Override
            public void onCues(List<Cue> cues) {
                Log.e("cues are ",cues.toString());
                if(subtitles!=null){
                    subtitles.onCues(cues);
                }
            }
        });

        debugViewHelper = new DebugTextViewHelper(simpleExoPlayer, txtStreamMessage);
        debugViewHelper.start();

        //  ((DemoApplication)getApplication()).getDownloadManager().startDownloads();

    }

    private void initializePlayer(String drmLicenseUrl,String token,String subtitlesUrl) {


        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;

        String[] keyRequestPropertiesArray = new String[]{"Authorization", "Bearer "+token};


            boolean multiSession = false;//DRM_MULTI_SESSION_EXTRA
            String errorStringId ="Unknown";
            if (Util.SDK_INT < 18) {
                errorStringId = "DRM Not supported!";
            } else {
                try {

                    String drmSchemeExtra = "widevine";

                    UUID drmSchemeUuid = Util.getDrmUuid(drmSchemeExtra);
                    if (drmSchemeUuid == null) {
                        errorStringId = "Unsupported Scheme";
                    } else {

                        drmSessionManager =
                                buildDrmSessionManagerV18(
                                        drmSchemeUuid, drmLicenseUrl, keyRequestPropertiesArray, multiSession);


                    }
                } catch (UnsupportedDrmException e) {
                    errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                            ? "Unsupported Scheme" :"Unknown !!";
                }
            }
            if (drmSessionManager == null) {
                Toast.makeText(MainActivity.this,errorStringId,Toast.LENGTH_LONG).show();
                finish();
                return;
            }



        ///////////



        RenderersFactory renderersFactory = new DefaultRenderersFactory(this,drmSessionManager);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(bandwidthMeter));

        simpleExoPlayer =  ExoPlayerFactory.newSimpleInstance(
                /* context= */ MainActivity.this, renderersFactory, trackSelector, drmSessionManager);


      DashMediaSource  mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                .setManifestParser(
                        new FilteringManifestParser<>(new DashManifestParser(), getOfflineStreamKeys(Uri.parse(drmLicenseUrl))))
                .createMediaSource(Uri.parse(drmLicenseUrl));

        Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP,
                Format.NO_VALUE,"hi");

        Uri uriSubtitle = Uri.parse(subtitlesUrl);
       // MediaSource subtitleSource = new SingleSampleMediaSource(uriSubtitle, dataSourceFactory, textFormat, C.TIME_UNSET);


        SingleSampleMediaSource subtitleSource = new SingleSampleMediaSource(Uri.parse(subtitlesUrl), dataSourceFactory,
                Format.createTextSampleFormat(null, MimeTypes.TEXT_VTT,
                        Format.NO_VALUE,"hi"),
                C.TIME_UNSET);
        MergingMediaSource mergedSource = new MergingMediaSource(mediaSource, subtitleSource);


        exoPlayerView.setPlayer(simpleExoPlayer);
        exoPlayerView.hideController();


        simpleExoPlayer.prepare(mergedSource);
        simpleExoPlayer.setPlayWhenReady(shouldAutoPlay);
        simpleExoPlayer.addTextOutput(new TextRenderer.Output() {
            @Override
            public void onCues(List<Cue> cues) {
                Log.e("cues are ",cues.toString());
                if(subtitles!=null){
                    subtitles.onCues(cues);
                }
            }
        });

        debugViewHelper = new DebugTextViewHelper(simpleExoPlayer, txtStreamMessage);
        debugViewHelper.start();

        ((DemoApplication)getApplication()).getDownloadManager().startDownloads();
        ((DemoApplication)getApplication()).getDownloadTracker().getOfflineStreamKeys(Uri.parse(drmLicenseUrl));


    }


    private DefaultDrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(
            UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, boolean multiSession)
            throws UnsupportedDrmException {
        HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSourceFactory( Util.getUserAgent(this, "ExoPlayerDemo"));
        HttpMediaDrmCallback drmCallback =
                new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);



        if (keyRequestPropertiesArray != null) {
            for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                        keyRequestPropertiesArray[i + 1]);
            }
        }
        releaseMediaDrm();

        mediaDrm = FrameworkMediaDrm.newInstance(uuid);

        return new DefaultDrmSessionManager<>(uuid, mediaDrm, drmCallback, null, null, new DefaultDrmSessionEventListener() {
            @Override
            public void onDrmKeysLoaded() {
                Log.e(TAG,"Keys have been loaded");
            }

            @Override
            public void onDrmSessionManagerError(Exception error) {

                Log.e(TAG,"Error=="+error.getMessage());

            }

            @Override
            public void onDrmKeysRestored() {
                Log.e(TAG,"Keys have been restored");

            }

            @Override
            public void onDrmKeysRemoved() {
                Log.e(TAG,"Keys have been removed");


            }
        }, multiSession);

    }


    private OfflineLicenseHelper offlineLicenseHelper;
    private CustomDrmCallback customDrmCallback;
    private DefaultDrmSessionManager<FrameworkMediaCrypto> buildOfflineDrmSessionManager(UUID uuid,
                                                                                  String licenseUrl, HashMap<String, String> keyRequestProperties) throws UnsupportedDrmException, IOException, DrmSession.DrmSessionException, InterruptedException {
        if (Util.SDK_INT < 18) {
            return null;
        }


        HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSourceFactory( Util.getUserAgent(this, "ExoPlayerDemo"));
        HttpMediaDrmCallback drmCallback =
                new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
        drmCallback.setKeyRequestProperty("Authorization",
                keyRequestProperties.get("Authorization"));

        releaseMediaDrm();
        mediaDrm = FrameworkMediaDrm.newInstance(uuid);
        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = new DefaultDrmSessionManager<FrameworkMediaCrypto>(uuid,
                mediaDrm, drmCallback, keyRequestProperties, null, new DefaultDrmSessionEventListener() {
            @Override
            public void onDrmKeysLoaded() {
                Log.e(TAG,"!Keys have been loaded");
            }

            @Override
            public void onDrmSessionManagerError(Exception error) {

                Log.e(TAG,"!Error=="+error.getMessage());

            }

            @Override
            public void onDrmKeysRestored() {
                Log.e(TAG,"!Keys have been restored");

            }

            @Override
            public void onDrmKeysRemoved() {
                Log.e(TAG,"!Keys have been removed");


            }
        });


        DataSource dataSource = licenseDataSourceFactory.createDataSource();

        this.offlineLicenseHelper = OfflineLicenseHelper.newWidevineInstance(licenseUrl,false, licenseDataSourceFactory);

        String offlineAssetKeyIdStr = getSharedPreferences("PREFS",MODE_PRIVATE).getString("KEY_OFFLINE_OFFSET_ID","");
        byte[] offlineAssetKeyId = Base64.decode(offlineAssetKeyIdStr, Base64.DEFAULT);

        Log.v("OfflineDRM"," offlineAssetKeyId:"+offlineAssetKeyIdStr);



//         Pair<Long, Long> remainingSecPair = offlineLicenseHelper.getLicenseDurationRemainingSec(offlineAssetKeyId);
  //      Log.e(TAG," License remaining Play time : "+remainingSecPair.first+", Purchase time : "+remainingSecPair.second);
        if(TextUtils.isEmpty(offlineAssetKeyIdStr) ) {

           //String path = getIntent().getStringExtra(EXTRA_OFFLINE_URI);
           // File file = getUriForManifest(path);
         //   File manifest =  new File(Environment.getExternalStorageDirectory()+"/exoplayer/","Manifest.mpd");


            new AsyncTask<String,String,String>(){
                @Override
                protected String doInBackground(String... strings) {
                    try {

                        String offlineAssetKeyIdStr = getSharedPreferences("PREFS",MODE_PRIVATE).getString("KEY_OFFLINE_OFFSET_ID","");
                        byte[] offlineAssetKeyId = Base64.decode(offlineAssetKeyIdStr, Base64.DEFAULT);

                        //  java.io.File manifestFile = new java.io.File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/exoplayer/Manifest.mpd");

                        DashManifest dashManifest = DashUtil.loadManifest(dataSource, Uri.parse(licenseUrl));

                        // Get DrmInitData
                        // Prefer drmInitData obtained from the manifest over drmInitData obtained from the stream,
                        // as per DASH IF Interoperability Recommendations V3.0, 7.5.3.
                        DrmInitData drmInitData = DashUtil.loadDrmInitData(dataSource, dashManifest.getPeriod(0));


                        Log.e(TAG, "will start download now");

                      //  offlineAssetKeyId = offlineLicenseHelper.downloadLicense(drmInitData);

                        Log.e(TAG, "download done : !!" );
                        //  Pair<Long, Long> p = offlineLicenseHelper.getLicenseDurationRemainingSec(offlineAssetKeyId);


                        //download the content
                        java.io.File downloadFolder = new java.io.File(Environment.getExternalStorageDirectory()+"/exoplayer/");
                        SimpleCache cache = new SimpleCache(downloadFolder, new NoOpCacheEvictor());

                       // DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("EXOPlayer");
                        com.google.android.exoplayer2.offline.DownloaderConstructorHelper helper = new com.google.android.exoplayer2.offline.DownloaderConstructorHelper(cache, licenseDataSourceFactory);
                        com.google.android.exoplayer2.source.dash.offline.DashDownloader dashDownloader = new com.google.android.exoplayer2.source.dash.offline.DashDownloader(Uri.parse(licenseUrl),getOfflineStreamKeys(Uri.parse(licenseUrl)),helper);

                        dashDownloader.download();



                        getSharedPreferences("PREFS", MODE_PRIVATE).edit().putString("KEY_OFFLINE_OFFSET_ID",
                                Base64.encodeToString(offlineAssetKeyId, Base64.DEFAULT)).commit();

                        Log.e(TAG, "download done : ?" );

                    }catch (IOException e){


                        e.printStackTrace();

                        return e.getMessage();
                    }
                    catch (InterruptedException e){

                        e.printStackTrace();

                        return e.getMessage();
                    }

                    return Base64.encodeToString(offlineAssetKeyId, Base64.DEFAULT);


                }
                @Override
                protected void onPostExecute(String string) {
                    super.onPostExecute(string);

                    Log.e(TAG,"onPostExecute "+string);

                }
            }.execute();


        }



       // drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK,offlineAssetKeyId);

     //

        return drmSessionManager;
    }



    private void releasePlayer() {
        if (simpleExoPlayer != null) {
            shouldAutoPlay = simpleExoPlayer.getPlayWhenReady();

            simpleExoPlayer.release();
            simpleExoPlayer = null;
            trackSelector = null;
            isPlaying = false;

        }

        releaseMediaDrm();
    }

    private void releaseMediaDrm() {
        if (mediaDrm != null) {
            mediaDrm.release();
            mediaDrm = null;
        }
    }






}
