package com.damai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: RsaSignTool 单元测试（使用类内置的签名密钥对）
 **/
class RsaSignToolTest {

    @Test
    @DisplayName("rsaSign256(Map)：对参数Map签名并验签通过")
    void testSignAndVerifyWithMap() {
        Map<String, String> params = new HashMap<>(8);
        params.put("code", "0001");
        params.put("businessBody", "{\"id\":\"1111\",\"sleepTime\":10}");

        String sign = RsaSignTool.rsaSign256(params, RsaSignTool.signPrivateKey);
        assertNotNull(sign);
        params.put("sign", sign);

        assertTrue(RsaSignTool.verifyRsaSign256(params, RsaSignTool.signPublicKey));
    }

    @Test
    @DisplayName("rsaSign256(String)：对字符串内容签名并用字节形式验签通过")
    void testSignAndVerifyWithContent() throws Exception {
        String content = "businessBody={\"id\":\"1111\"}&code=0001";
        String sign = RsaSignTool.rsaSign256(content, RsaSignTool.signPrivateKey);
        assertTrue(RsaSignTool.verifyRsaSign256(content.getBytes("utf-8"), sign, RsaSignTool.signPublicKey));
    }

    @Test
    @DisplayName("verifyRsaSign256：参数被篡改后验签失败返回false")
    void testVerifyWithTamperedParams() {
        Map<String, String> params = new HashMap<>(8);
        params.put("code", "0001");
        params.put("businessBody", "{\"id\":\"1111\"}");
        String sign = RsaSignTool.rsaSign256(params, RsaSignTool.signPrivateKey);
        params.put("sign", sign);

        // 篡改业务参数
        params.put("businessBody", "{\"id\":\"9999\"}");
        assertFalse(RsaSignTool.verifyRsaSign256(params, RsaSignTool.signPublicKey));
    }

    @Test
    @DisplayName("rsaSign256：参数顺序不同但内容相同，生成的签名可互相验签（按key排序拼接）")
    void testSignOrderInsensitive() {
        Map<String, String> params1 = new HashMap<>(8);
        params1.put("b", "2");
        params1.put("a", "1");

        Map<String, String> params2 = new HashMap<>(8);
        params2.put("a", "1");
        params2.put("b", "2");

        String sign1 = RsaSignTool.rsaSign256(params1, RsaSignTool.signPrivateKey);
        // 用 params2 验 params1 的签名，二者拼接内容一致（a=1&b=2），应验签通过
        params2.put("sign", sign1);
        assertTrue(RsaSignTool.verifyRsaSign256(params2, RsaSignTool.signPublicKey));
    }
}
