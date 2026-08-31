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
 * @description: RsaTool 单元测试
 **/
class RsaToolTest {

    @Test
    @DisplayName("getKey：生成的密钥对包含公钥和私钥，且每次生成不同")
    void testGetKey() {
        Map<String, String> keyPair = RsaTool.getKey();
        assertNotNull(keyPair.get(RsaTool.PUBLIC_KEY));
        assertNotNull(keyPair.get(RsaTool.PRIVATE_KEY));

        Map<String, String> anotherKeyPair = RsaTool.getKey();
        assertNotEquals(keyPair.get(RsaTool.PUBLIC_KEY), anotherKeyPair.get(RsaTool.PUBLIC_KEY));
    }

    @Test
    @DisplayName("encrypt/decrypt：短数据加密后可正确解密还原")
    void testEncryptDecryptShortData() {
        Map<String, String> keyPair = RsaTool.getKey();
        // 注意：encrypt 使用平台默认字符集、decrypt 固定 UTF-8，
        // 在非 UTF-8 平台（如 Windows GBK）下中文数据无法正确还原，此处使用 ASCII 数据
        String data = "{\"id\":\"1111\",\"name\":\"damai\"}";
        String encrypt = RsaTool.encrypt(data, keyPair.get(RsaTool.PUBLIC_KEY));
        assertNotNull(encrypt);
        String decrypt = RsaTool.decrypt(encrypt, keyPair.get(RsaTool.PRIVATE_KEY));
        assertEquals(data, decrypt);
    }

    @Test
    @DisplayName("encrypt/decrypt：超过117字节的长数据分段加密后可正确解密还原")
    void testEncryptDecryptLongData() {
        Map<String, String> keyPair = RsaTool.getKey();
        // 构造超过 MAX_ENCRYPT_BLOCK(117) 的长数据，验证分段加解密逻辑
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append("a");
        }
        String data = sb.toString();
        String encrypt = RsaTool.encrypt(data, keyPair.get(RsaTool.PUBLIC_KEY));
        String decrypt = RsaTool.decrypt(encrypt, keyPair.get(RsaTool.PRIVATE_KEY));
        assertEquals(data, decrypt);
    }

    @Test
    @DisplayName("decrypt：使用错误私钥解密抛出 DaMaiFrameException(RSA_DECRYPT_ERROR)")
    void testDecryptWithWrongKey() {
        Map<String, String> keyPair1 = RsaTool.getKey();
        Map<String, String> keyPair2 = RsaTool.getKey();
        String encrypt = RsaTool.encrypt("secret-data", keyPair1.get(RsaTool.PUBLIC_KEY));
        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> RsaTool.decrypt(encrypt, keyPair2.get(RsaTool.PRIVATE_KEY)));
        assertEquals(BaseCode.RSA_DECRYPT_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("decrypt：密文为非法base64时抛出 DaMaiFrameException(RSA_DECRYPT_ERROR)")
    void testDecryptWithInvalidData() {
        Map<String, String> keyPair = RsaTool.getKey();
        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> RsaTool.decrypt("not-valid-cipher-text", keyPair.get(RsaTool.PRIVATE_KEY)));
        assertEquals(BaseCode.RSA_DECRYPT_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("encrypt：公钥串非法时抛出 DaMaiFrameException(RSA_ENCRYPT_ERROR)")
    void testEncryptWithInvalidPublicKey() {
        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> RsaTool.encrypt("data", "invalid-public-key"));
        assertEquals(BaseCode.RSA_ENCRYPT_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("getPublicKey/getPrivateKey：由密钥串还原出的Key对象算法为RSA")
    void testGetKeys() throws Exception {
        Map<String, String> keyPair = RsaTool.getKey();
        assertTrue(RsaTool.getPublicKey(keyPair.get(RsaTool.PUBLIC_KEY)).getAlgorithm().equals("RSA"));
        assertTrue(RsaTool.getPrivateKey(keyPair.get(RsaTool.PRIVATE_KEY)).getAlgorithm().equals("RSA"));
    }
}
