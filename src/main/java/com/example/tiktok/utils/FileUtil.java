package com.example.tiktok.utils;

import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.net.URL;

public class FileUtil {

    /**
     * 获取文件后缀
     * @param fileName 文件名
     * @return
     */
    public static String getFormat(String fileName){
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 获取视频时长
     * @param fileUrl 网络url
     * @return
     */
    public static String getVideoDuration(String fileUrl) {
        String [] length = new String[2];

        try {
            URL source = new URL(fileUrl);
            MultimediaObject instance = new MultimediaObject(source);
            MultimediaInfo result = instance.getInfo();

            Long ls = result.getDuration() / 1000;
            length[0] = String.valueOf(ls);
            Integer hour = (int) (ls / 3600);
            Integer minute = (int) (ls % 3600) / 60;
            Integer second = (int) (ls - hour * 3600 - minute * 60);

            String hr = hour.toString();
            String mi = minute.toString();
            String se = second.toString();

            if (hr.length() < 2) {
                hr = "0" + hr;
            }

            if (mi.length() < 2) {
                mi = "0" + mi;
            }

            if (se.length() < 2) {
                se = "0" + se;
            }

            // 如果小时部分为 00，则只返回分钟和秒；否则，返回完整的 hh:mm:ss 格式。
            String noHour = "00";
            if (noHour.equals(hr)) {
                length[1] = mi + ":" + se;
            } else {
                length[1] = hr + ":" + mi + ":" + se;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return length[1];
    }
}
