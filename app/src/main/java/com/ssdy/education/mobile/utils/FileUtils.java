package com.ssdy.education.mobile.utils;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author 写文件的工具类
 */
public class FileUtils {

    /**
     * 判断SD卡是否挂载
     */
    public static boolean isSDCardAvailable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 根据文件路径保存文件
     *
     * @param bm
     * @param pathName
     * @return
     */
    public static File saveBitmap(Bitmap bm, String pathName) {
        if (isSDCardAvailable()) {
            FileOutputStream fos = null;
            File file = new File(pathName);
            if (file.exists()) {
                file.delete();
            }
            try {
                fos = new FileOutputStream(file);
                if (null != fos) {
                    bm.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.flush();
                    fos.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bm != null && !bm.isRecycled()) {
                bm.recycle();
            }
            return file;
        }
        return null;
    }

    /**
     * 删除文件
     *
     * @param pathname
     */
    public static void delFileByPath(String pathname) {
        if (pathname != null) {
            File file = new File(pathname);
            if (file.isFile()) {
                file.delete();
            }
            file.exists();
        }
    }
}
