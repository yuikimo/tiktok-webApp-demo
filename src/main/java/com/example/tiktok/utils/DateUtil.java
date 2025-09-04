package com.example.tiktok.utils;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    private static final String dateFormat = "yyyy-MM-dd";

    /**
     * 格式化日期
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(date);
    }

    /**
     * 在日期Date上增加x天
     * @param date 处理的日期，非null
     * @param amount 要加的天数，可能为负数
     * @return
     */
    public static Date addDays(Date date, int amount) {
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        now.set(Calendar.DATE, now.get(Calendar.DATE) + amount);
        return now.getTime();
    }

    /**
     * 在日期Date上减少X天
     * @param date 处理的日期，非null
     * @param amount 要加的天数，可能为负数
     * @return
     */
    public static Date redDays(Date date, int amount) {
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        now.set(Calendar.DATE, now.get(Calendar.DATE) - amount);
        return now.getTime();
    }

    /**
     * 对日期的[秒]进行加/减
     * @param date 日期
     * @param seconds 秒数，负数为减
     * @return 加/减几秒后的日期
     */
    public static Date addDateSeconds(Date date, int seconds){
        DateTime dateTime = new DateTime(date);
        return dateTime.plusSeconds(seconds).toDate();
    }

    /**
     * 对日期的[分钟]进行加/减
     * @param date 日期
     * @param minutes 分钟数，负数为减
     * @return 加/减几分钟后的日期
     */
    public static Date addDateMinutes(Date date, int minutes) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusMinutes(minutes).toDate();
    }

    /**
     * 对日期的[小时]进行加/减
     * @param date  日期
     * @param hours 小时数，负数为减
     * @return 加/减几小时后的日期
     */
    public static Date addDateHours(Date date, int hours) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusHours(hours).toDate();
    }

    /**
     * 对日期的【天】进行加/减
     * @param date 日期
     * @param days 天数，负数为减
     * @return 加/减几天后的日期
     */
    public static Date addDateDays(Date date, int days) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusDays(days).toDate();
    }

    /**
     * 对日期的【周】进行加/减
     * @param date  日期
     * @param weeks 周数，负数为减
     * @return 加/减几周后的日期
     */
    public static Date addDateWeeks(Date date, int weeks) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusWeeks(weeks).toDate();
    }

    /**
     * 对日期的【月】进行加/减
     * @param date   日期
     * @param months 月数，负数为减
     * @return 加/减几月后的日期
     */
    public static Date addDateMonths(Date date, int months) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusMonths(months).toDate();
    }

    /**
     * 对日期的【年】进行加/减
     * @param date  日期
     * @param years 年数，负数为减
     * @return 加/减几年后的日期
     */
    public static Date addDateYears(Date date, int years) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusYears(years).toDate();
    }

    /**
     * 判断给定日期是否是当月的最后一天
     * @param date
     * @return
     */
    public static boolean isLastDayOfMonth(Date date) {
        // 1.创建日历类
        Calendar calendar = Calendar.getInstance();
        // 2.设置当前传递的时间，不设置为当前系统日期
        calendar.setTime(date);
        // 3.date的日期为N，N+1【假设当月是30天，30+1=31，如果当月只有30天，那么最终结果为1，也就是下月的1号】
        calendar.set(Calendar.DATE, (calendar.get(Calendar.DATE) + 1));
        // 4.判断是否为当月最后一天【1==1那么就表明当天是当月的最后一天返回true】
        return calendar.get(Calendar.DAY_OF_MONTH) == 1;
    }

    /**
     * 获取上个月月份
     * @return
     */
    public static final String getLastMonth() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        // 设置为当前时间
        calendar.setTime(date);
        // 在 calendar 对象中将月份减去 1，表示将日期调整为上一个月
        calendar.add(Calendar.MONTH, -1);
        // 设置为上一个月
        date = calendar.getTime();
        return format.format(date);
    }

    public static String getLastMonth(String repeatDate){
        String lastMonth = "";
        SimpleDateFormat dft = new SimpleDateFormat("yyyyMM");

        Calendar calendar = Calendar.getInstance();

        int year = Integer.parseInt(repeatDate.substring(0, 4));
        String monthsString = repeatDate.substring(4, 6);

        int month;
        if ("0".equals(monthsString.substring(0, 1))) {
            month = Integer.parseInt(monthsString.substring(1, 2));
        } else {
            month = Integer.parseInt(monthsString.substring(0, 2));
        }

        // 将日历对象设置为指定年份和月份（month-2 是因为月份从0开始），并将日期设置为该月的最后一天。
        calendar.set(year,month-2, Calendar.DATE);
        lastMonth = dft.format(calendar.getTime());
        return lastMonth;
    }
}
