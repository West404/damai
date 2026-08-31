package com.damai.util;

import com.damai.exception.DaMaiFrameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: RsaSignTool RSA签名/验签工具 单元测试
 **/
class RsaSignToolTest {

    private Map<String, String> buildParams() {
        Map<String, String> map = new HashMap<>(8);
        //基础参数
        map.put("code", "0001");
        //业务参数
        map.put("businessBody", "{\"id\":\"1111\",\"sleepTime\":10}");
        return map;
    }

    @Test
    @DisplayName("私钥签名、公钥验签：签名结果验证通过")
    void testSignAndVerifySuccess() {
        Map<String, String> map = buildParams();
        String sign = RsaSignTool.rsaSign256(map, RsaSignTool.signPrivateKey);
        assertNotNull(sign);

        map.put("sign", sign);
        assertTrue(RsaSignTool.verifyRsaSign256(map, RsaSignTool.signPublicKey));
    }

    @Test
    @DisplayName("验签后参数被篡改：签名验证不通过")
    void testVerifyFailWhenParamTampered() {
        Map<String, String> map = buildParams();
        String sign = RsaSignTool.rsaSign256(map, RsaSignTool.signPrivateKey);
        map.put("sign", sign);

        //模拟中间人篡改业务参数
        map.put("businessBody", "{\"id\":\"9999\",\"sleepTime\":10}");

        assertFalse(RsaSignTool.verifyRsaSign256(map, RsaSignTool.signPublicKey));
    }

    @Test
    @DisplayName("files参数不参与签名验签：额外加入files参数后验签仍通过")
    void testFilesParamIgnored() {
        Map<String, String> map = buildParams();
        String sign = RsaSignTool.rsaSign256(map, RsaSignTool.signPrivateKey);
        map.put("sign", sign);
        map.put("files", "some-file-content");

        assertTrue(RsaSignTool.verifyRsaSign256(map, RsaSignTool.signPublicKey));
    }

    @Test
    @DisplayName("使用不匹配的公钥验签：签名验证不通过")
    void testVerifyFailWithWrongPublicKey() {
        Map<String, String> map = buildParams();
        String sign = RsaSignTool.rsaSign256(map, RsaSignTool.signPrivateKey);
        map.put("sign", sign);

        Map<String, String> wrongKeyPair = RsaTool.getKey();
        assertFalse(RsaSignTool.verifyRsaSign256(map, wrongKeyPair.get(RsaTool.PUBLIC_KEY)));
    }

    @Test
    @DisplayName("使用非法私钥签名时抛出 DaMaiFrameException")
    void testSignWithIllegalPrivateKey() {
        Map<String, String> map = buildParams();
        assertThrows(DaMaiFrameException.class,
                () -> RsaSignTool.rsaSign256(map, "illegal-private-key"));
    }

    @Test
    @DisplayName("验签后原参数map中的sign被移除")
    void testVerifyRemovesSignFromParams() {
        Map<String, String> map = buildParams();
        String sign = RsaSignTool.rsaSign256(map, RsaSignTool.signPrivateKey);
        map.put("sign", sign);

        assertTrue(RsaSignTool.verifyRsaSign256(map, RsaSignTool.signPublicKey));
        //验签方法内部会将sign、files从参数map中移除
        assertFalse(map.containsKey("sign"));
    }

    @Test
    @DisplayName("对相同参数多次签名：SHA256withRSA签名结果一致")
    void testSignDeterministic() {
        Map<String, String> map = buildParams();
        String sign1 = RsaSignTool.rsaSign256(map, RsaSignTool.signPrivateKey);
        String sign2 = RsaSignTool.rsaSign256(map, RsaSignTool.signPrivateKey);
        assertEquals(sign1, sign2);
    }
}
