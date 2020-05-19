package com.juntai.wisdom.basecomponent.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.juntai.wisdom.basecomponent.app.BaseApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Calendar;

/**
 * 文件
 *
 * @aouther Ma
 * @date 2019/3/21
 */
public class FileCacheUtils {

    /**
     * 获取app文件地址
     *
     * @return
     */
    public static String getAppPath() {
        File destDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + BaseAppUtils.getAppName());
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return destDir.getAbsolutePath() + "/";
    }

    /**
     * 压缩图片存放目录
     *
     * @return
     */
    public static String getAppImagePath() {
        File destDir = new File(getAppPath() + "image/");
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return destDir.getAbsolutePath() + "/";
    }

    /**
     * 获取video缓存目录
     *
     * @return
     */
    public static String getAppVideoPath() {
        File destDir = new File(getAppPath() + "video/");
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return destDir.getAbsolutePath() + "/";
    }

    public static String getAppRecordVideoPath() {
        File destDir = new File(getAppPath() + "recordVideo/");
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return destDir.getAbsolutePath() + "/";
    }


    public static String saveBitmap(Bitmap bmp) {
        FileOutputStream out;
        Calendar calendar = Calendar.getInstance();
        String bitmapName = String.valueOf(calendar.get(Calendar.YEAR)) + String.valueOf(calendar.get(Calendar.MONTH)) + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH) + 1) + String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)) + String.valueOf(calendar.get(Calendar.MINUTE)) + String.valueOf(calendar.get(Calendar.SECOND)) + ".jpg";
        File file;
        String path = null;
        try {
            // 获取SDCard指定目录下
            String sdCardDir = getAppImagePath();
            File dirFile = new File(sdCardDir);  //目录转化成文件夹
            if (!dirFile.exists()) {              //如果不存在，那就建立这个文件夹
                dirFile.mkdirs();
            }
            file = new File(sdCardDir, bitmapName);
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            path = sdCardDir + bitmapName;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }


    /**
     * 创建文件夹
     *
     * @param fileName
     */
    public static void creatFile(String fileName) {

        File file = new File(fileName);

        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 清除图片缓存
     */
    public static void clearImage() {
        try {
            deleteFile(new File(getAppImagePath()));
        } catch (Exception e) {
            LogUtil.e("image-删除缓存文件失败=" + e.toString());
        }
    }

    /**
     * 清除视频缓存
     */
    public static void clearVideo() {
        try {
            deleteFile(new File(getAppVideoPath()));
        } catch (Exception e) {
            LogUtil.e("video-删除缓存文件失败=" + e.toString());
        }
    }

    /**
     * 清除所有缓存
     */
    public static void clearAll() {
        try {
            deleteFile(new File(getAppPath()));
            deleteFile(new File(getAppImagePath()));
            deleteFile(new File(getAppVideoPath()));
        } catch (Exception e) {
            LogUtil.e("all-删除缓存文件失败=" + e.toString());
        }
    }

    /**
     * 删除文件
     *
     * @param file
     */
    private static void deleteFile(File file) {
        //判断是否为目录
        for (File ff : file.listFiles()) {
            if (ff.isDirectory()) {
                deleteFile(ff);
            }
            ff.delete();
        }
    }


    /**
     * 文件大小
     */
    private static float cacheSize = 0.00f;

    /**
     * 获取文件缓存大小
     *
     * @return
     */
    public static String getCacheSize() {
        File file = new File(getAppPath());
        cacheSize = 0.00f;
        getCacheSize(file);
        //LogUtil.e("fff文件缓存大小 = "+ String.format("%.2f",(cacheSize / 1024)) + " k");
        return String.format("%.2f", (cacheSize / 1024 / 1024)) + "m";
    }

    /**
     * 递归查看文件大小
     *
     * @param file
     */
    private static void getCacheSize(File file) {
        //判断是否为目录
        for (File ff : file.listFiles()) {
            cacheSize += ff.length();
            if (ff.isDirectory()) {
                getCacheSize(ff);
            }
        }
    }



    /**
     * 将字符串写入到文本文件中
     * @param strcontent   要保存的字符串
     * @param filePath  保存的路径
     * @param fileName  文件的名称
     */
    public static void writeToTxtFile(String strcontent, String filePath, String fileName) {



        filePath =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/.dwh/";
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName);

        String strFilePath = filePath + fileName;
        String  writedContent = getFileContent(fileName);
        if (!TextUtils.isEmpty(writedContent)) {
            return;
        }

        // 每次写入时，都换行写
        String strContent = MD5.md5(strcontent);
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }

    /**
     * 生成文件
     * @param filePath
     * @param fileName
     * @return
     */
    public static File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    //生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }

    //读取指定目录下的所有TXT文件的文件内容
    public static String getFileContent(String fileName) {
        String path =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/.dwh/";
        makeFilePath(path, fileName);
        File file = new File(path+fileName);
        if (!file.exists()) {
            file.mkdirs();
        }
        String content = "";
        if (!file.isDirectory()) { //检查此路径名的文件是否是一个目录(文件夹)
            if (file.getName().endsWith("txt")) {//文件格式为""文件
                try {
                    InputStream instream = new FileInputStream(file);
                    if (instream != null) {
                        InputStreamReader inputreader = new InputStreamReader(instream, "UTF-8");
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line = "";
                        //分行读取
                        while ((line = buffreader.readLine()) != null) {
                            content += line;
                            //                            content += line + "\n";
                        }
                        instream.close();//关闭输入流
                    }
                } catch (java.io.FileNotFoundException e) {
                    Log.d("TestFile", "The File doesn't not exist.");
                } catch (IOException e) {
                    Log.d("TestFile", e.getMessage());
                }
            }
        }
        return content;
    }

}
