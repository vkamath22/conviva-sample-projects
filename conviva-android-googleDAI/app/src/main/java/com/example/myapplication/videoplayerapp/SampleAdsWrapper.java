/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.myapplication.videoplayerapp;
/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.conviva.sdk.ConvivaAnalytics;
import com.conviva.sdk.ConvivaSdkConstants;
import com.example.myapplication.helper.ConvivaHelper;
import com.example.myapplication.samplevideoplayer.SampleVideoPlayer;
import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.CuePoint;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.StreamDisplayContainer;
import com.google.ads.interactivemedia.v3.api.StreamManager;
import com.google.ads.interactivemedia.v3.api.StreamRequest;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.ads.interactivemedia.v3.api.player.VideoStreamPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.conviva.sdk.ConvivaSdkConstants.ASSET_NAME;
import static com.conviva.sdk.ConvivaSdkConstants.DURATION;
import static com.conviva.sdk.ConvivaSdkConstants.POD_DURATION;
import static com.conviva.sdk.ConvivaSdkConstants.POD_INDEX;
import static com.conviva.sdk.ConvivaSdkConstants.POD_POSITION;
import static com.conviva.sdk.ConvivaSdkConstants.STREAM_URL;
import static com.example.myapplication.helper.ConvivaHelper.sConvivaAdAnalytics;
import static com.example.myapplication.helper.ConvivaHelper.sConvivaVideoAnalytics;
import static com.conviva.sdk.ConvivaSdkConstants.ASSET_NAME;
import static com.conviva.sdk.ConvivaSdkConstants.DURATION;
import static com.conviva.sdk.ConvivaSdkConstants.ENCODED_FRAMERATE;
import static com.conviva.sdk.ConvivaSdkConstants.IS_LIVE;
import static com.conviva.sdk.ConvivaSdkConstants.PLAYER_NAME;
import static com.conviva.sdk.ConvivaSdkConstants.POD_DURATION;
import static com.conviva.sdk.ConvivaSdkConstants.POD_INDEX;
import static com.conviva.sdk.ConvivaSdkConstants.POD_POSITION;
import static com.conviva.sdk.ConvivaSdkConstants.STREAM_URL;
import static com.conviva.sdk.ConvivaSdkConstants.VIEWER_ID;

/**
 * This class adds ad-serving support to Sample HlsVideoPlayer
 */
public class SampleAdsWrapper implements AdEvent.AdEventListener, AdErrorEvent.AdErrorListener,
        AdsLoader.AdsLoadedListener {

    private static final String PLAYER_TYPE = "DAISamplePlayer";

    /**
     * Log interface, so we can output the log commands to the UI or similar.
     */
    public interface Logger {
        void log(String logMessage);
    }
    private HashMap<String, Object> adInfo = new HashMap<>();
    private ImaSdkFactory mSdkFactory;
    private AdsLoader mAdsLoader;
    private StreamManager mStreamManager;
    private StreamDisplayContainer mDisplayContainer;
    private List<VideoStreamPlayer.VideoStreamPlayerCallback> mPlayerCallbacks;
    private boolean isAdBreakStarted = false;
    private SampleVideoPlayer mVideoPlayer;
    private Context mContext;
    private ViewGroup mAdUiContainer;

    private double mBookMarkContentTime; // Bookmarked content time, in seconds.
    private double mSnapBackTime; // Stream time to snap back to, in seconds.
    private boolean mAdsRequested;
    private String mFallbackUrl;
    private Logger mLogger;
    private boolean isAdSessionCreated = false;
    // Initializing the Conviva Helper for a video playback request.
    private int podIndexCount = 0;
    private String mPodPosition;
    private int absoluteIndex;
    /**
     * Creates a new SampleAdsWrapper that implements IMA direct-ad-insertion.
     * @param context the app's context.
     * @param videoPlayer underlying HLS video player.
     * @param adUiContainer ViewGroup in which to display the ad's UI.
     */
    public SampleAdsWrapper(Context context, SampleVideoPlayer videoPlayer,
                            ViewGroup adUiContainer) {
        mVideoPlayer = videoPlayer;
        mContext = context;
        mAdUiContainer = adUiContainer;
        mSdkFactory = ImaSdkFactory.getInstance();
        mPlayerCallbacks = new ArrayList<>();
        mDisplayContainer = mSdkFactory.createStreamDisplayContainer();
        createAdsLoader();

    }

    private void createAdsLoader() {
        // Change any settings as necessary here.
        mAdsLoader = mSdkFactory.createAdsLoader(mContext,mSdkFactory.createImaSdkSettings(), mDisplayContainer);
    }

    public void requestAndPlayAds(VideoListFragment.VideoListItem videoListItem,
                                  double bookMarkTime) {
        sConvivaAdAnalytics = ConvivaAnalytics.buildAdAnalytics(mContext, sConvivaVideoAnalytics);
        StreamRequest request = buildStreamRequest(videoListItem);
        mBookMarkContentTime = bookMarkTime;
        mAdsLoader.addAdErrorListener(this);
        mAdsLoader.addAdsLoadedListener(this);
        mAdsLoader.requestStream(request);
        mAdsRequested = true;
    }

    private StreamRequest buildStreamRequest(VideoListFragment.VideoListItem videoListItem) {

        VideoStreamPlayer videoStreamPlayer = createVideoStreamPlayer();

        // Set the license URL.
        mVideoPlayer.setLicenseUrl(videoListItem.getLicenseUrl());
        HashMap<String, Object> contentInfo = new HashMap<>();

        contentInfo.put(ASSET_NAME, videoListItem.getTitle());

        contentInfo.put(STREAM_URL, "something");
        contentInfo.put(PLAYER_NAME, "ConvivaSSAISample");
        contentInfo.put(VIEWER_ID, "ConvivaSSAISample");
        contentInfo.put(ENCODED_FRAMERATE, 30);
        contentInfo.put(ConvivaSdkConstants.IS_LIVE, videoListItem.getAssetKey() != null);
        sConvivaVideoAnalytics.reportPlaybackRequested(contentInfo);
        //sConvivaAdAnalytics.

        HashMap<String, Object> adInfo = new HashMap<>();
        adInfo.put("c3.ad.adManagerVersion", "3.18.1");
        adInfo.put(ConvivaSdkConstants.IS_LIVE, videoListItem.getAssetKey() != null);
        sConvivaAdAnalytics.setAdInfo(adInfo);

        Map<String, Object> adinfo = new HashMap<>();
        adinfo.put(ConvivaSdkConstants.FRAMEWORK_VERSION, "3.18.1");
        sConvivaAdAnalytics.setAdPlayerInfo(adinfo);

        Map<String, Object> adMetadata = new HashMap<>();
        adMetadata.put(ConvivaSdkConstants.AD_PLAYER, ConvivaSdkConstants.AdPlayer.CONTENT.toString());
        adMetadata.put(ConvivaSdkConstants.AD_TAG_URL, "something");
        sConvivaAdAnalytics.setAdListener(mAdsLoader, adMetadata);
        mVideoPlayer.setSampleVideoPlayerCallback(
                new SampleVideoPlayer.SampleVideoPlayerCallback() {
                    @Override
                    public void onUserTextReceived(String userText) {
                        for (VideoStreamPlayer.VideoStreamPlayerCallback callback : mPlayerCallbacks) {
                            callback.onUserTextReceived(userText);
                        }
                    }
                    @Override
                    public void onSeek(int windowIndex, long positionMs) {
                        double timeToSeek = positionMs;
                        if (mStreamManager != null) {
                            CuePoint cuePoint =
                                    mStreamManager.getPreviousCuePointForStreamTime(positionMs / 1000);
                            double bookMarkStreamTime =
                                    mStreamManager.getStreamTimeForContentTime(mBookMarkContentTime);
                            if (cuePoint != null && !cuePoint.isPlayed()
                                    && cuePoint.getEndTime() > bookMarkStreamTime) {
                                mSnapBackTime = timeToSeek / 1000.0; // Update snap back time.
                                // Missed cue point, so snap back to the beginning of cue point.
                                timeToSeek = cuePoint.getStartTime() * 1000;
                                Log.i("IMA", "SnapBack to " + timeToSeek);
                                mVideoPlayer.seekTo(windowIndex, Math.round(timeToSeek));
                                mVideoPlayer.setCanSeek(false);

                                return;
                            }
                        }
                        mVideoPlayer.seekTo(windowIndex, Math.round(timeToSeek));
                    }
                });
        mDisplayContainer.setVideoStreamPlayer(videoStreamPlayer);
        mDisplayContainer.setAdContainer(mAdUiContainer);

        StreamRequest request;
        // Live stream request.
        if (videoListItem.getAssetKey() != null) {
            request = mSdkFactory.createLiveStreamRequest(videoListItem.getAssetKey(),
                    videoListItem.getApiKey());
        } else { // VOD request.
            request = mSdkFactory.createVodStreamRequest(videoListItem.getContentSourceId(),
                    videoListItem.getVideoId(), videoListItem.getApiKey());
        }
        // Set the stream format (HLS or DASH).
        request.setFormat(videoListItem.getStreamFormat());

        return request;
    }

    private VideoStreamPlayer createVideoStreamPlayer() {
        return new VideoStreamPlayer() {
            @Override
            public void loadUrl(String url, List<HashMap<String, String>> subtitles) {
                mVideoPlayer.setStreamUrl(url);
                mVideoPlayer.play();
                Object mPlayer = mVideoPlayer.getmPlayer();
                sConvivaVideoAnalytics.setPlayer(mPlayer);

                ((SimpleExoPlayer)mPlayer).addListener(new Player.EventListener() {
                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

                    }
                });

                // Bookmarking
                if (mBookMarkContentTime > 0) {
                    double streamTime =
                            mStreamManager.getStreamTimeForContentTime(mBookMarkContentTime);
                    mVideoPlayer.seekTo((long) (streamTime * 1000.0)); // s to ms.
                }
            }
            @Override
            public void seek(long l) {

            }

            @Override
            public void onAdPeriodStarted() {

            }

            @Override
            public void onAdPeriodEnded() {

            }

            @Override
            public int getVolume() {
                return 0;
            }

            @Override
            public void addCallback(VideoStreamPlayerCallback videoStreamPlayerCallback) {
                mPlayerCallbacks.add(videoStreamPlayerCallback);
            }

            @Override
            public void removeCallback(VideoStreamPlayerCallback videoStreamPlayerCallback) {
                mPlayerCallbacks.remove(videoStreamPlayerCallback);
            }

            @Override
            public void onAdBreakStarted() {
                // Disable player controls.
                mVideoPlayer.setCanSeek(false);
                mVideoPlayer.enableControls(false);
                log("Ad Break Started\n");
            }

            @Override
            public void onAdBreakEnded() {
                // Re-enable player controls.
                mVideoPlayer.setCanSeek(true);
                mVideoPlayer.enableControls(true);
                if (mSnapBackTime > 0) {
                    Log.i("IMA", "SampleAdsWrapper seeking " + mSnapBackTime);
                    mVideoPlayer.seekTo(Math.round(mSnapBackTime * 1000));
                }
                mSnapBackTime = 0;
                log("Ad Break Ended\n");
            }

            @Override
            public VideoProgressUpdate getContentProgress() {
                if (mVideoPlayer == null) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(mVideoPlayer.getCurrentPositionPeriod(),
                        mVideoPlayer.getDuration());
            }
        };
    }

    public void pauseContentMonitoring(){
        if(sConvivaVideoAnalytics != null){
            sConvivaVideoAnalytics.reportPlaybackEvent(ConvivaSdkConstants.Events.USER_WAIT_STARTED.toString());
        }
    }
    public void resumeContentMonitoring(){
        if(sConvivaVideoAnalytics != null){
                sConvivaVideoAnalytics.reportPlaybackEvent(ConvivaSdkConstants.Events.USER_WAIT_ENDED.toString());
            //sending additional play state just in case if there is no event change after resume monitoring.
            // otherwise it will stay in unknown state until next player state change
            sConvivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING);
        }
    }


    public double getContentTime() {
        if (mStreamManager != null) {
            return mStreamManager.getContentTimeForStreamTime(
                    mVideoPlayer.getCurrentPositionPeriod() / 1000.0);
        }
        return 0.0;
    }

    public double getStreamTimeForContentTime(double contentTime) {
        if (mStreamManager != null) {
            return mStreamManager.getStreamTimeForContentTime(contentTime);
        }
        return 0.0;
    }

    public void setSnapBackTime(double snapBackTime) {
        mSnapBackTime = snapBackTime;
    }

    public boolean getAdsRequested() {
        return mAdsRequested;
    }

    /** AdErrorListener implementation **/
    @Override
    public void onAdError(AdErrorEvent event) {
        log(String.format("Error: %s\n", event.getError().getMessage()));
        // play fallback URL.
        log("Playing fallback Url\n");
        mVideoPlayer.setStreamUrl(mFallbackUrl);
        mVideoPlayer.play();
        Map<String, Object> info = new HashMap<>();
        info.put(ASSET_NAME, "Fallback");
        ConvivaHelper.sConvivaVideoAnalytics.setContentInfo(info);
    }

    private String getConvivaSessionStartTag(AdEvent event) {

        switch (event.getType()) {

            case LOADED:
                return "loaded";
            case STARTED:
                return "start";
            case AD_PROGRESS:
                return "progress";
            case FIRST_QUARTILE:
                return "firstQuartile";
            case MIDPOINT:
                return "midpoint";
            case THIRD_QUARTILE:
                return "thirdQuartile";
            case COMPLETED:
                return "complete";

        }
        return null;
    }

    private void setupAdInfo(AdEvent event) {

        if (!isAdSessionCreated) {

            isAdSessionCreated = true;
            Ad eventAd = event.getAd();

            adInfo.put("c3.ad.adManagerName", "Google IMA DAI SDK");
            adInfo.put("c3.ad.adManagerVersion", "3.9.0");
            adInfo.put("c3.ad.technology", "Server Side");
            adInfo.put("c3.ad.type", "NA");
            adInfo.put("c3.ad.mediaFileApiFramework", "NA");
            adInfo.put("c3.ad.position", "NA");
            adInfo.put("c3.ad.adStitcher", "Google DAI");
            adInfo.put("c3.ad.sessionStartEvent", getConvivaSessionStartTag(event));

            if (eventAd != null) {

                adInfo.put(ASSET_NAME, eventAd.getTitle());
                adInfo.put(DURATION, (int) eventAd.getDuration());

                adInfo.put("c3.ad.system", eventAd.getAdSystem());
                adInfo.put("c3.ad.creativeId", eventAd.getCreativeId());
                adInfo.put("c3.ad.id", eventAd.getAdId());
                adInfo.put("c3.ad.sequence", String.valueOf(eventAd.getAdPodInfo().getAdPosition()));

            } else {

                adInfo.put(ASSET_NAME, "NONE");
                adInfo.put("c3.ad.system", "NA");
                adInfo.put("c3.ad.id", "NA");
            }

            adInfo.put(STREAM_URL, "mStreamURL");
        }
    }

    /** AdEventListener implementation **/
    @Override
    public void onAdEvent(AdEvent event) {
        switch (event.getType()) {
            case AD_BREAK_STARTED:

                break;
            case AD_BREAK_ENDED:

                break;
            case STARTED:
                Log.v("SampleAdsWrapper", "pause content monitor");

                break;
            case COMPLETED:
                Log.v("SampleAdsWrapper", "resume content monitor");
                isAdSessionCreated = false;
                adInfo.clear();
                break;
            case AD_PROGRESS:
                break;
            case LOADED:
                Log.v("SampleAdsWrapper", "LOADED");
                setupAdInfo(event);
                break;
            default:
                log(String.format("Event: %s\n", event.getType()));
                break;
        }
    }
    private void setUpAdBreakStart(AdEvent event) {
        if (!isAdBreakStarted) {
            isAdBreakStarted = true;
            HashMap<String, Object> attrsPodStart = new HashMap<>();
            Ad ad = event.getAd();
            if (ad != null) {
                AdPodInfo podInfo = ad.getAdPodInfo();
                attrsPodStart.put(POD_DURATION, String.valueOf(podInfo.getMaxDuration()));
                absoluteIndex = podInfo.getPodIndex() + 1;
                mPodPosition = podInfo.getPodIndex() == 0 ? "Pre-roll" : (podInfo.getPodIndex() == -1 ? "Post-roll": "Mid-roll");
                attrsPodStart.put(POD_POSITION, mPodPosition);
            }
            // Setting podPosition to UNKNOWN if not available.
            attrsPodStart.put(POD_INDEX, String.valueOf(++podIndexCount));
            sConvivaVideoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.CONTENT, ConvivaSdkConstants.AdType.SERVER_SIDE, attrsPodStart);
        }
    }
    private void setUpAdBreakEnd() {

        HashMap<String, Object> attrsPodEnd = new HashMap<>();

        attrsPodEnd.put("absoluteIndex", String.valueOf(absoluteIndex));
        attrsPodEnd.put("podPosition", mPodPosition); // Setting podPosition to UNKNOWN if not available.
        attrsPodEnd.put("podIndex", String.valueOf(podIndexCount));

        sConvivaVideoAnalytics.reportAdBreakEnded();

        isAdBreakStarted = false;
    }


    /** AdsLoadedListener implementation **/
    @Override
    public void onAdsManagerLoaded(AdsManagerLoadedEvent event) {
        mStreamManager = event.getStreamManager();
        mStreamManager.addAdErrorListener(this);
        mStreamManager.addAdEventListener(this);
        mStreamManager.init();
    }

    /** Sets fallback URL in case ads stream fails. **/
    public void setFallbackUrl(String url) {
        mFallbackUrl = url;
    }

    /** Sets logger for displaying events to screen. Optional. **/
    public void setLogger(Logger logger) {
        mLogger = logger;
    }

    private void log(String message) {
        if (mLogger != null) {
            mLogger.log(message);
        }
    }

    public void release() {
        if (mStreamManager != null) {
            mStreamManager.destroy();
        }
        mStreamManager = null;
        if (mVideoPlayer != null) {
            mVideoPlayer.release();
        }
        mVideoPlayer = null;
        mAdsRequested = false;
    }
}
