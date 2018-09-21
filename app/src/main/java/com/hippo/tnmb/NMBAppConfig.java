/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.tnmb;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.hippo.tnmb.util.Settings;
import com.hippo.yorozuya.FileUtils;

import java.io.File;

public class NMBAppConfig {

    private static final String APP_DIRNAME = "nmb";

    private static final String CRASH_DIRNAME = "crash";
    private static final String DOODLE_DIRNAME = "doodle";
    private static final String IMAGE_DIRNAME = "image";
    private static final String COOKIES_DIRNAME = "cookies";
    private static final String PHOTO_DIRNAME = "photo";
    private static final String LOGCAT_DIRNAME = "logcat";

    private static Context sContext;

    public static void initialize(Context context) {
        sContext = context.getApplicationContext();
    }

    public static File getNativeLibDir() {
        return new File(sContext.getApplicationInfo().nativeLibraryDir);
    }

    public static @Nullable File getExternalAppDir() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(Environment.getExternalStorageDirectory(), APP_DIRNAME);
            if (FileUtils.ensureDirectory(file)) {
                return file;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * mkdirs and get
     */
    public static @Nullable File getDirInAppDir(String filename) {
        File appFolder = getExternalAppDir();
        if (appFolder != null) {
            File dir = new File(appFolder, filename);
            if (FileUtils.ensureDirectory(dir)) {
                return dir;
            }
        }

        return null;
    }

    /**
     * No dir
     */
    public static @Nullable File getFileInAppDir(String filename) {
        File appFolder = getExternalAppDir();
        if (appFolder != null) {
            File file = new File(appFolder, filename);
            if (FileUtils.ensureFile(file)) {
                return file;
            }
        }

        return null;
    }

    public static @Nullable File getCrashDir() {
        return getDirInAppDir(CRASH_DIRNAME);
    }

    public static @Nullable File getDoodleDir() {
        return getDirInAppDir(DOODLE_DIRNAME);
    }

    public static @Nullable File getImageDir() {
        return getDirInAppDir(IMAGE_DIRNAME);
    }

    public static @Nullable File getCookiesDir() {
        return getDirInAppDir(COOKIES_DIRNAME);
    }

    public static @Nullable File getPhotoDir() {
        return getDirInAppDir(PHOTO_DIRNAME);
    }

    public static @Nullable File getLogcatDir() {
        return getDirInAppDir(LOGCAT_DIRNAME);
    }

    public static @Nullable File getTempDir() {
        File temp = new File(sContext.getCacheDir(), "temp");
        if (FileUtils.ensureDirectory(temp)) {
            return temp;
        } else {
            return null;
        }
    }

    private static @Nullable File getRecordImageDir() {
        File temp = sContext.getDir("record", 0);
        if (FileUtils.ensureDirectory(temp)) {
            return temp;
        } else {
            return null;
        }
    }

    public static @Nullable File createRecordImageFile() {
        return FileUtils.createTempFile(getRecordImageDir(), null);
    }

    public static @Nullable File createTempFile() {
        return createTempFile(null);
    }

    public static @Nullable File createTempFile(String extension) {
        return FileUtils.createTempFile(getTempDir(), extension);
    }

    public static @Nullable File createTempFileWithFilename(String filename) {
        File dir = getTempDir();
        if (dir == null) {
            return null;
        }

        File file = new File(dir, filename);
        if (FileUtils.ensureFile(file)) {
            return file;
        } else {
            return null;
        }
    }

    public static @Nullable File createTempDir() {
        return FileUtils.createTempDir(getTempDir());
    }

    public static boolean needloadImage(Context context) {
        switch (Settings.getImageLoadingStrategy()) {
            case Settings.IMAGE_LOADING_STRATEGY_ALL:
                return true;
            case Settings.IMAGE_LOADING_STRATEGY_WIFI:
                return NMBApplication.isConnectedWifi(context);
            default:
            case Settings.IMAGE_LOADING_STRATEGY_NO:
                return false;
        }
    }
}
