package com.damai.jwt;

import com.alibaba.fastjson.JSONObject;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import io.jsonwebtoken.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @description: TokenUtil 单元测试
 **/
class TokenUtilTest {

    private static final String TOKEN_SECRET = "CSYZWECHAT";

    @Test
    @DisplayName("createToken/parseToken：创建的token可解析出原始subject内容")
    void testCreateAndParseToken() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("001key", "001value");
        jsonObject.put("002key", "002value");

        String token = TokenUtil.createToken("1", jsonObject.toJSONString(), 60000, TOKEN_SECRET);
        assertNotNull(token);

        String subject = TokenUtil.parseToken(token, TOKEN_SECRET);
        assertEquals(jsonObject.toJSONString(), subject);
    }

    @Test
    @DisplayName("createToken：ttlMillis为负数时不设置过期时间，token仍可正常解析")
    void testCreateTokenWithoutExpiration() {
        String token = TokenUtil.createToken("2", "no-expire-content", -1, TOKEN_SECRET);
        assertEquals("no-expire-content", TokenUtil.parseToken(token, TOKEN_SECRET));
    }

    @Test
    @DisplayName("parseToken：token过期后抛出 DaMaiFrameException(TOKEN_EXPIRE)")
    void testParseExpiredToken() throws InterruptedException {
        String token = TokenUtil.createToken("3", "expire-content", 100, TOKEN_SECRET);
        // 等待token过期
        Thread.sleep(1200);
        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> TokenUtil.parseToken(token, TOKEN_SECRET));
        assertEquals(BaseCode.TOKEN_EXPIRE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("parseToken：使用错误密钥解析抛出 SignatureException")
    void testParseTokenWithWrongSecret() {
        String token = TokenUtil.createToken("4", "content", 60000, TOKEN_SECRET);
        assertThrows(SignatureException.class,
                () -> TokenUtil.parseToken(token, "WRONG_SECRET"));
    }

    @Test
    @DisplayName("parseToken：解析非法token串抛出异常")
    void testParseMalformedToken() {
        assertThrows(Exception.class, () -> TokenUtil.parseToken("not.a.token", TOKEN_SECRET));
    }
}
