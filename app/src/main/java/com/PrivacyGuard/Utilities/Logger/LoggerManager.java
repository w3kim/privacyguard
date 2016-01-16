package com.PrivacyGuard.Utilities.Logger;

import android.os.Environment;
import android.util.Log;

import com.PrivacyGuard.PrivacyGuard;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by justinhu on 16-01-15.
 */
public class LoggerManager {
    //Default dir in case cache file cannot be located
    private static final String DEFAULT_DIRECTORY = "/data/data/com.y59song.UI.PrivacyGuard.PrivacyGuard/Cache/Log";
    //One Logger per Package
    private static final Map<String, Logger> loggerHashMap = new WeakHashMap<String, Logger>();

    /**
     * Returns the Package specific Logger
     *
     * @param packageName the package name relating to the Logger
     * @return the {@link Logger} implementation.
     */
    public static Logger getLogger(String packageName) {
        Logger logger;

        synchronized (loggerHashMap) {
            logger = loggerHashMap.get(packageName);
        }

        if (logger == null) {
            String dir = getDiskCacheDir();
            logger = new Logger(packageName, dir);
            synchronized (loggerHashMap) {
                loggerHashMap.put(logger.getPackageName(), logger);
            }
        }

        return logger;
    }

    /**
     * Returns a dir path for cache storage
     *
     * @return SD storage for cash or internal storage for cash
     */
    public static String getDiskCacheDir() {
        String cachePath;
        File cacheFile;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cacheFile = PrivacyGuard.getAppContext().getExternalCacheDir();
        } else {
            cacheFile = PrivacyGuard.getAppContext().getCacheDir();
        }
        if (cacheFile == null) {
            Log.w("LoggerManager", "Not able to get Disk Cache Directory.");
            cachePath = DEFAULT_DIRECTORY;
        } else {
            cachePath = cacheFile.getAbsolutePath() + File.separator + "Log";
            Log.d("LoggerManager", "log dir path is " + cachePath);
        }
        return cachePath;
    }

}
