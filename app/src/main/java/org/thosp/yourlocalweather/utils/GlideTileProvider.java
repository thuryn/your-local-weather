package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.CantContinueException;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.thosp.yourlocalweather.BuildConfig;

import java.util.concurrent.ExecutionException;

public class GlideTileProvider extends MapTileProviderArray {

    public GlideTileProvider(Context context, ITileSource tileSource) {
        super(tileSource, new SimpleRegisterReceiver(context));

        // 1. Local filesystem provider (cache)
        final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(
                new SimpleRegisterReceiver(context), tileSource);
        mTileProviderList.add(fileSystemProvider);

        // 2. Glide-based network provider
        final MapTileModuleProviderBase glideProvider = new GlideTileModuleProvider(context, (OnlineTileSourceBase) tileSource);
        mTileProviderList.add(glideProvider);
    }

    private static class GlideTileModuleProvider extends MapTileModuleProviderBase {
        private final Context context;
        private OnlineTileSourceBase tileSource;

        public GlideTileModuleProvider(Context context, OnlineTileSourceBase tileSource) {
            super(Configuration.getInstance().getTileDownloadThreads(), 
                  Configuration.getInstance().getTileDownloadMaxQueueSize());
            this.context = context.getApplicationContext();
            this.tileSource = tileSource;
        }

        @Override
        public TileLoader getTileLoader() {
            return new GlideTileLoader();
        }

        @Override
        public int getMinimumZoomLevel() {
            return (tileSource != null) ? tileSource.getMinimumZoomLevel() : 0;
        }

        @Override
        public int getMaximumZoomLevel() {
            return (tileSource != null) ? tileSource.getMaximumZoomLevel() : 22;
        }

        @Override
        public String getName() {
            return "GlideTileModuleProvider";
        }

        @Override
        public String getThreadGroupName() {
            return "GlideTileLoader";
        }
        
        @Override
        public boolean getUsesDataConnection() {
            return true;
        }

        @Override
        public void setTileSource(ITileSource tileSource) {
            if (tileSource instanceof OnlineTileSourceBase) {
                this.tileSource = (OnlineTileSourceBase) tileSource;
            }
        }

        private class GlideTileLoader extends TileLoader {
            @Override
            public Drawable loadTile(long pMapTileIndex) throws CantContinueException {
                if (tileSource == null) return null;
                String url = tileSource.getTileURLString(pMapTileIndex);
                try {
                    String userAgent = Configuration.getInstance().getUserAgentValue();
                    if (userAgent == null || userAgent.isEmpty() || userAgent.startsWith("osmdroid") || userAgent.startsWith("org.osmdroid")) {
                        userAgent = String.format("YourLocalWeather/%s (Linux; Android %s)",
                                BuildConfig.VERSION_NAME,
                                Build.VERSION.RELEASE);
                    }
                    GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                            .addHeader("User-Agent", userAgent)
                            .build());
                    Bitmap bitmap = Glide.with(context)
                            .asBitmap()
                            .load(glideUrl)
                            .submit()
                            .get();
                    if (bitmap != null) {
                        return new ExpirableBitmapDrawable(bitmap);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    // Ignore
                }
                return null;
            }
        }
    }
}
