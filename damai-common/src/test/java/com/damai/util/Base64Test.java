package com.damai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @description: Base64 单元测试
 **/
class Base64Test {

    @Test
    @DisplayName("encode：与JDK内置Base64编码结果一致（3字节整倍数据）")
    void testEncodeAlignedWithJdk() {
        byte[] data = "damai!".getBytes(StandardCharsets.UTF_8);
        assertEquals(java.util.Base64.getEncoder().encodeToString(data), Base64.encode(data));
    }

    @Test
    @DisplayName("encode：余1字节时末尾补两个'='，与JDK结果一致")
    void testEncodeOneByteRemainder() {
        byte[] data = "d".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.encode(data);
        assertEquals(java.util.Base64.getEncoder().encodeToString(data), encoded);
        assertEquals("ZA==", encoded);
    }

    @Test
    @DisplayName("encode：余2字节时末尾补一个'='，与JDK结果一致")
    void testEncodeTwoBytesRemainder() {
        byte[] data = "da".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.encode(data);
        assertEquals(java.util.Base64.getEncoder().encodeToString(data), encoded);
        assertEquals("ZGE=", encoded);
    }

    @Test
    @DisplayName("encode：包含负字节（高位为1）的二进制数据编码正确")
    void testEncodeBinaryDataWithNegativeBytes() {
        byte[] data = new byte[]{(byte) 0xFF, (byte) 0x80, 0x00, 0x7F, (byte) 0xFE};
        assertEquals(java.util.Base64.getEncoder().encodeToString(data), Base64.encode(data));
    }

    @Test
    @DisplayName("encode：null输入返回null，空数组返回空串")
    void testEncodeEdgeCases() {
        assertNull(Base64.encode(null));
        assertEquals("", Base64.encode(new byte[0]));
    }
}
