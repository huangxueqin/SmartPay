package com.android.smartpay.utilities;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xueqin on 2015/12/14 0014.
 */
public final class StorageUtils {
    private static final String APP_FOLDER = "smartpay";
    private static final String LOG_FILE_NAME = "log";

    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static void dumpLog(String msg) {
        if(!isExternalStorageWritable()) {
            L("external Storage not writable");
            return;
        }

        File homeDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + APP_FOLDER);
        if(!homeDir.exists()) {
            homeDir.mkdirs();
        }

        File logFile = new File(homeDir, "log.txt");
        logFile.setReadable(true);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(logFile, logFile.exists()));
            writer.print(getTime());
            writer.println(msg);
            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static String getTime() {
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss: ").format(new Date());
    }

    private static void L(String msg) {
        Log.d("Dump-------->", msg);
    }
}
