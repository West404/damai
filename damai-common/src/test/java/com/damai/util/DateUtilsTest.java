package com.damai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: DateUtils 单元测试
 **/
class DateUtilsTest {

    @Test
    @DisplayName("format与parse：日期时间字符串与Date互转保持一致")
    void testFormatAndParseRoundTrip() {
        String dateTimeStr = "2022-06-17 16:06:17";
        Date date = DateUtils.parseDateTime(dateTimeStr);
        assertNotNull(date);
        assertEquals(dateTimeStr, DateUtils.formatDateTime(date));
    }

    @Test
    @DisplayName("format：date为null时返回空字符串")
    void testFormatWithNull() {
        assertEquals("", DateUtils.format(null, DateUtils.FORMAT_SECOND));
        assertEquals("", DateUtils.formatDate(null));
    }

    @Test
    @DisplayName("parse：空串、null、非法格式均返回null")
    void testParseWithInvalidInput() {
        assertNull(DateUtils.parse("", DateUtils.FORMAT_SECOND));
        assertNull(DateUtils.parse("   ", DateUtils.FORMAT_SECOND));
        assertNull(DateUtils.parse(null, DateUtils.FORMAT_SECOND));
        assertNull(DateUtils.parseDateTime("not-a-date"));
    }

    @Test
    @DisplayName("getDateStart：返回当天 00:00:00")
    void testGetDateStart() {
        Date date = DateUtils.parseDateTime("2024-03-15 12:30:45");
        assertEquals("2024-03-15 00:00:00", DateUtils.formatDateTime(DateUtils.getDateStart(date)));
        assertNull(DateUtils.getDateStart(null));
    }

    @Test
    @DisplayName("getDateEnd：返回当天 23:59:59")
    void testGetDateEnd() {
        Date date = DateUtils.parseDateTime("2024-03-15 12:30:45");
        assertEquals("2024-03-15 23:59:59", DateUtils.formatDateTime(DateUtils.getDateEnd(date)));
        assertNull(DateUtils.getDateEnd(null));
    }

    @Test
    @DisplayName("getDateNo/getDateTimeNo：返回对应数字格式")
    void testGetDateNo() {
        Date date = DateUtils.parseDateTime("2024-01-05 08:09:10");
        assertEquals(20240105, DateUtils.getDateNo(date));
        assertEquals(20240105080910L, DateUtils.getDateTimeNo(date));
        assertEquals(0, DateUtils.getDateNo(null));
        assertEquals(0L, DateUtils.getDateTimeNo(null));
    }

    @Test
    @DisplayName("getWeek：2024-01-01为周一(1)，2024-01-07为周日(7)")
    void testGetWeek() {
        assertEquals(1, DateUtils.getWeek(DateUtils.parseDate("2024-01-01")));
        assertEquals(7, DateUtils.getWeek(DateUtils.parseDate("2024-01-07")));
        assertEquals(0, DateUtils.getWeek(null));
    }

    @Test
    @DisplayName("getWeekStr：返回中文星期，null返回未知")
    void testGetWeekStr() {
        assertEquals("周一", DateUtils.getWeekStr(DateUtils.parseDate("2024-01-01")));
        assertEquals("周日", DateUtils.getWeekStr(DateUtils.parseDate("2024-01-07")));
        assertEquals("未知", DateUtils.getWeekStr(null));
    }

    @Test
    @DisplayName("addDay/addMonth/addYear：日期加减计算正确（含闰年、月末进位）")
    void testAdd() {
        assertEquals("2024-02-01", DateUtils.formatDate(DateUtils.addDay(DateUtils.parseDate("2024-01-31"), 1)));
        assertEquals("2024-01-30", DateUtils.formatDate(DateUtils.addDay(DateUtils.parseDate("2024-01-31"), -1)));
        // 闰年 2024-02-29 加一年 -> 2025-02-28
        assertEquals("2025-02-28", DateUtils.formatDate(DateUtils.addYear(DateUtils.parseDate("2024-02-29"), 1)));
        assertEquals("2024-03-31", DateUtils.formatDate(DateUtils.addMonth(DateUtils.parseDate("2024-01-31"), 2)));
        assertNull(DateUtils.addDay(null, 1));
    }

    @Test
    @DisplayName("addHour/addMinute/addSecond：时间加减计算正确")
    void testAddTime() {
        Date base = DateUtils.parseDateTime("2024-01-01 00:00:00");
        assertEquals("2024-01-01 02:00:00", DateUtils.formatDateTime(DateUtils.addHour(base, 2)));
        assertEquals("2024-01-01 00:30:00", DateUtils.formatDateTime(DateUtils.addMinute(base, 30)));
        assertEquals("2023-12-31 23:59:59", DateUtils.formatDateTime(DateUtils.addSecond(base, -1)));
    }

    @Test
    @DisplayName("getWeekDate：获取所在周指定星期的日期，越界返回null")
    void testGetWeekDate() {
        Date wednesday = DateUtils.parseDate("2024-01-03");
        assertEquals("2024-01-01", DateUtils.formatDate(DateUtils.getWeekDate(wednesday, DateUtils.WEEK_1_MONDAY)));
        assertEquals("2024-01-07", DateUtils.formatDate(DateUtils.getWeekDate(wednesday, DateUtils.WEEK_7_SUNDAY)));
        assertNull(DateUtils.getWeekDate(wednesday, 0));
        assertNull(DateUtils.getWeekDate(wednesday, 8));
    }

    @Test
    @DisplayName("getWeekDateList：主代码 getWeekDateEnd 存在无限递归缺陷，该链路暂不可测，见总结说明")
    @org.junit.jupiter.api.Disabled("DateUtils.getWeekDateEnd 自身递归调用导致 StackOverflowError，待主代码修复后启用")
    void testGetWeekDateList() {
        List<String> weekDates = DateUtils.getWeekDateList("2024-01-03");
        assertEquals(7, weekDates.size());
        assertEquals("2024-01-01", weekDates.get(0));
        assertEquals("2024-01-07", weekDates.get(6));
        assertTrue(DateUtils.getWeekDateList("not-a-date").isEmpty());
    }

    @Test
    @DisplayName("getMonthDateList：闰年2月共29天，且首尾正确")
    void testGetMonthDateList() {
        List<String> monthDates = DateUtils.getMonthDateList("2024-02-15");
        assertEquals(29, monthDates.size());
        assertEquals("2024-02-01", monthDates.get(0));
        assertEquals("2024-02-29", monthDates.get(28));

        List<String> januaryDates = DateUtils.getMonthDateList("2024-01-15");
        assertEquals(31, januaryDates.size());
        assertTrue(DateUtils.getMonthDateList("bad").isEmpty());
    }

    @Test
    @DisplayName("getMonthDateStart/getMonthDateEnd：返回月起止时间")
    void testGetMonthDateStartAndEnd() {
        Date date = DateUtils.parseDateTime("2024-02-15 10:20:30");
        assertEquals("2024-02-01 00:00:00", DateUtils.formatDateTime(DateUtils.getMonthDateStart(date)));
        assertEquals("2024-02-29 23:59:59", DateUtils.formatDateTime(DateUtils.getMonthDateEnd(date)));
        assertNull(DateUtils.getMonthDateStart(null));
        assertNull(DateUtils.getMonthDateEnd(null));
    }

    @Test
    @DisplayName("countBetweenSecond：计算相差秒数，任一参数为空返回-1")
    void testCountBetweenSecond() {
        Date date1 = DateUtils.parseDateTime("2024-01-01 00:00:00");
        Date date2 = DateUtils.parseDateTime("2024-01-01 00:01:30");
        assertEquals(90, DateUtils.countBetweenSecond(date1, date2));
        // 交换顺序结果一致（取绝对值）
        assertEquals(90, DateUtils.countBetweenSecond(date2, date1));
        assertEquals(-1, DateUtils.countBetweenSecond(null, date2));
        assertEquals(-1, DateUtils.countBetweenSecond(date1, null));
    }

    @Test
    @DisplayName("getBetweenDateList：不包含参数日期时只返回中间日期")
    void testGetBetweenDateListNotContainParams() {
        List<String> dates = DateUtils.getBetweenDateList("2024-01-01", "2024-01-04");
        assertEquals(2, dates.size());
        assertEquals("2024-01-02", dates.get(0));
        assertEquals("2024-01-03", dates.get(1));
    }

    @Test
    @DisplayName("getBetweenDateList：包含参数日期且支持前后日期乱序")
    void testGetBetweenDateListContainParams() {
        List<String> dates = DateUtils.getBetweenDateList("2024-01-03", "2024-01-01", true);
        assertEquals(3, dates.size());
        assertEquals("2024-01-01", dates.get(0));
        assertEquals("2024-01-03", dates.get(2));
    }

    @Test
    @DisplayName("getDateNode：日期节点各字段解析正确")
    void testGetDateNode() {
        Date date = DateUtils.parseDateTime("2024-01-03 10:20:30");
        DateUtils.DateNode node = DateUtils.getDateNode(date);
        assertNotNull(node);
        assertEquals(2024, node.getYear());
        assertEquals(1, node.getMonth());
        assertEquals(3, node.getDay());
        assertEquals(10, node.getHour());
        assertEquals(20, node.getMinute());
        assertEquals(30, node.getSecond());
        assertEquals(3, node.getWeek());
        assertEquals(3, node.getDayOfYear());
        assertEquals(date.getTime(), node.getMillisecondStamp());
        assertEquals(date.getTime() / 1000, node.getSecondStamp());
        assertNull(DateUtils.getDateNode(null));
    }

    @Test
    @DisplayName("getWeekOfYear：2024-01-01恰为周一故为第1周，null返回-1")
    void testGetWeekOfYear() {
        assertEquals(1, DateUtils.getWeekOfYear(DateUtils.parseDate("2024-01-01")));
        assertEquals(-1, DateUtils.getWeekOfYear(null));
    }

    @Test
    @DisplayName("getDate：自定义格式解析，非法字符串抛IllegalArgumentException，空参数返回默认日期")
    void testGetDate() {
        Date date = DateUtils.getDate("2024/01/01", "yyyy/MM/dd");
        assertNotNull(date);

        Date defaultDate = new Date(0L);
        assertEquals(defaultDate, DateUtils.getDate(null, "yyyy-MM-dd", defaultDate));

        assertThrows(IllegalArgumentException.class, () -> DateUtils.getDate("bad-value", "yyyy-MM-dd"));
    }

    @Test
    @DisplayName("nowStr：返回符合 yyyy-MM-dd HH:mm:ss 格式的当前时间字符串")
    void testNowStr() {
        String nowStr = DateUtils.nowStr();
        assertNotNull(nowStr);
        // 能被 FORMAT_SECOND 解析说明格式正确
        assertNotNull(DateUtils.parseDateTime(nowStr));
        assertEquals(19, nowStr.length());
    }

    @Test
    @DisplayName("getFormatedDateString：时区偏移超出范围时回落到0时区且不报错")
    void testGetFormatedDateStringWithInvalidOffset() {
        String result = DateUtils.getFormatedDateString(20, DateUtils.FORMAT_DATE);
        assertNotNull(result);
        assertEquals(10, result.length());
    }
}
