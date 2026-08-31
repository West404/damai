package com.damai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: StringUtil 单元测试
 **/
class StringUtilTest {

    @Test
    @DisplayName("isNotEmpty：普通非空字符串返回true")
    void testIsNotEmptyWithNormalString() {
        assertTrue(StringUtil.isNotEmpty("hello"));
    }

    @Test
    @DisplayName("isNotEmpty：null、空串、纯空白字符串均返回false")
    void testIsNotEmptyWithBlankString() {
        assertFalse(StringUtil.isNotEmpty(null));
        assertFalse(StringUtil.isNotEmpty(""));
        assertFalse(StringUtil.isNotEmpty("   "));
    }

    @Test
    @DisplayName("isNotEmpty：字面量 null/undefined/NULL 被视为空")
    void testIsNotEmptyWithLiteralNull() {
        assertFalse(StringUtil.isNotEmpty("null"));
        assertFalse(StringUtil.isNotEmpty("NULL"));
        assertFalse(StringUtil.isNotEmpty("undefined"));
        assertFalse(StringUtil.isNotEmpty(" Undefined "));
    }

    @Test
    @DisplayName("isEmpty：与 isNotEmpty 结果互斥")
    void testIsEmpty() {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertTrue(StringUtil.isEmpty("null"));
        assertFalse(StringUtil.isEmpty("abc"));
    }

    @Test
    @DisplayName("inputStreamConvertString：输入流正确转换为字符串")
    void testInputStreamConvertString() {
        String content = "damai-123";
        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        assertEquals(content, StringUtil.inputStreamConvertString(is));
    }

    @Test
    @DisplayName("inputStreamConvertString：传入null返回null")
    void testInputStreamConvertStringWithNull() {
        assertNull(StringUtil.inputStreamConvertString(null));
    }

    @Test
    @DisplayName("convertQueryStringToMap：标准查询串解析为map")
    void testConvertQueryStringToMap() {
        Map<String, String> map = StringUtil.convertQueryStringToMap("code=0001&name=damai");
        assertEquals(2, map.size());
        assertEquals("0001", map.get("code"));
        assertEquals("damai", map.get("name"));
    }

    @Test
    @DisplayName("convertQueryStringToMap：URL编码的值会被解码")
    void testConvertQueryStringToMapWithUrlEncoded() {
        Map<String, String> map = StringUtil.convertQueryStringToMap("city=%E5%8C%97%E4%BA%AC");
        assertEquals("北京", map.get("city"));
    }

    @Test
    @DisplayName("convertQueryStringToMap：无等号的片段被忽略")
    void testConvertQueryStringToMapWithInvalidSegment() {
        Map<String, String> map = StringUtil.convertQueryStringToMap("a=1&invalid&b=2");
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
    }

    @Test
    @DisplayName("convertQueryStringToMap：传入null抛NullPointerException")
    void testConvertQueryStringToMapWithNull() {
        assertThrows(NullPointerException.class, () -> StringUtil.convertQueryStringToMap(null));
    }
}
