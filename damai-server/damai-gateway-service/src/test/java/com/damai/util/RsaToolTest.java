package com.damai.util;

import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: RsaTool RSA加解密工具 单元测试
 **/
class RsaToolTest {

    @Test
    @DisplayName("生成的密钥对应包含公钥和私钥，且每次生成不相同")
    void testGetKey() {
        Map<String, String> keyPair = RsaTool.getKey();
        assertNotNull(keyPair.get(RsaTool.PUBLIC_KEY));
        assertNotNull(keyPair.get(RsaTool.PRIVATE_KEY));

        Map<String, String> anotherKeyPair = RsaTool.getKey();
        assertNotEquals(keyPair.get(RsaTool.PUBLIC_KEY), anotherKeyPair.get(RsaTool.PUBLIC_KEY));
    }

    @Test
    @DisplayName("公钥加密、私钥解密：短报文加解密往返一致")
    void testEncryptDecryptShortData() {
        Map<String, String> keyPair = RsaTool.getKey();
        String data = "{\"id\":\"1111\",\"sleepTime\":10}";

        String encrypt = RsaTool.encrypt(data, keyPair.get(RsaTool.PUBLIC_KEY));
        assertNotNull(encrypt);
        assertNotEquals(data, encrypt);

        String decrypt = RsaTool.decrypt(encrypt, keyPair.get(RsaTool.PRIVATE_KEY));
        assertEquals(data, decrypt);
    }

    @Test
    @DisplayName("公钥加密、私钥解密：超过117字节的长报文分段加解密往返一致")
    void testEncryptDecryptLongData() {
        Map<String, String> keyPair = RsaTool.getKey();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("大麦网高并发实战项目-测试数据-").append(i).append(";");
        }
        String data = sb.toString();
        // 确保报文超过单次加密分块上限，覆盖分段加解密逻辑
        assertTrue(data.getBytes().length > 117);

        String encrypt = RsaTool.encrypt(data, keyPair.get(RsaTool.PUBLIC_KEY));
        String decrypt = RsaTool.decrypt(encrypt, keyPair.get(RsaTool.PRIVATE_KEY));
        assertEquals(data, decrypt);
    }

    @Test
    @DisplayName("使用不匹配的私钥解密时抛出 DaMaiFrameException(RSA解密失败)")
    void testDecryptWithWrongPrivateKey() {
        Map<String, String> keyPair = RsaTool.getKey();
        Map<String, String> wrongKeyPair = RsaTool.getKey();

        String encrypt = RsaTool.encrypt("hello damai", keyPair.get(RsaTool.PUBLIC_KEY));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> RsaTool.decrypt(encrypt, wrongKeyPair.get(RsaTool.PRIVATE_KEY)));
        assertEquals(BaseCode.RSA_DECRYPT_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("使用非法公钥字符串加密时抛出 DaMaiFrameException(RSA加密失败)")
    void testEncryptWithIllegalPublicKey() {
        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> RsaTool.encrypt("hello damai", "illegal-public-key"));
        assertEquals(BaseCode.RSA_ENCRYPT_ERROR.getCode(), exception.getCode());
    }
}
