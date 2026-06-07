package com.videviewer.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

/**
 * VidViewerGlideModule - Custom Glide configuration
 * Sets thumbnail cache size to 50MB for fast startup
 */
@GlideModule
public class VidViewerGlideModule extends AppGlideModule {

    private static final int MEMORY_CACHE_MB = 20;
    private static final int DISK_CACHE_MB = AppConstants.THUMBNAIL_CACHE_SIZE_MB;

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Memory cache: 20 MB
        builder.setMemoryCache(new LruResourceCache(MEMORY_CACHE_MB * 1024 * 1024));
        // Disk cache: 50 MB
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context,
            "thumbnail_cache", DISK_CACHE_MB * 1024 * 1024));
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide,
                                   @NonNull Registry registry) {
        // No custom components needed
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false; // Disable for performance
    }
}
