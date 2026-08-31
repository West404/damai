package com.damai.pay.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.damai.entity.PayBill;
import com.damai.enums.BaseCode;
import com.damai.enums.PayBillStatus;
import com.damai.exception.DaMaiFrameException;
import com.damai.pay.PayResult;
import com.damai.pay.RefundResult;
import com.damai.pay.TradeResult;
import com.damai.pay.alipay.config.AlipayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 支付宝支付策略处理器单元测试
 */
@ExtendWith(MockitoExtension.class)
class AlipayStrategyHandlerTest {

    @Mock
    private AlipayClient alipayClient;

    @Mock
    private AlipayTradePagePayResponse pagePayResponse;

    @Mock
    private AlipayTradeQueryResponse tradeQueryResponse;

    @Mock
    private AlipayTradeRefundResponse tradeRefundResponse;

    private AlipayProperties alipayProperties;

    private AlipayStrategyHandler alipayStrategyHandler;

    @BeforeEach
    void setUp() {
        alipayProperties = new AlipayProperties();
        alipayProperties.setAppId("app-id-001");
        alipayProperties.setSellerId("seller-001");
        alipayProperties.setAlipayPublicKey("invalid-public-key");
        alipayStrategyHandler = new AlipayStrategyHandler(alipayClient, alipayProperties);
    }

    @Test
    @DisplayName("getChannel 返回支付宝渠道标识 alipay")
    void getChannelReturnsAlipay() {
        assertEquals("alipay", alipayStrategyHandler.getChannel());
    }

    @Test
    @DisplayName("pay 调用SDK成功时返回成功结果和页面表单body")
    void paySuccess() throws AlipayApiException {
        when(alipayClient.pageExecute(any(AlipayTradePagePayRequest.class), eq("POST")))
                .thenReturn(pagePayResponse);
        when(pagePayResponse.isSuccess()).thenReturn(true);
        when(pagePayResponse.getBody()).thenReturn("<form>pay-form</form>");

        PayResult payResult = alipayStrategyHandler.pay(
                "order-1", new BigDecimal("100.00"), "门票", "http://notify", "http://return");

        assertTrue(payResult.isSuccess());
        assertEquals("<form>pay-form</form>", payResult.getBody());
    }

    @Test
    @DisplayName("pay 调用SDK抛异常时转换为 PAY_ERROR 业务异常")
    void payThrowsBusinessExceptionOnSdkError() throws AlipayApiException {
        when(alipayClient.pageExecute(any(AlipayTradePagePayRequest.class), eq("POST")))
                .thenThrow(new AlipayApiException("sdk error"));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> alipayStrategyHandler.pay("order-1", new BigDecimal("100.00"),
                        "门票", "http://notify", "http://return"));
        assertEquals(BaseCode.PAY_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("signVerify 公钥非法时验签返回false而不是抛出异常")
    void signVerifyReturnsFalseOnInvalidKey() {
        Map<String, String> params = new HashMap<>();
        params.put("sign", "fake-sign");

        assertFalse(alipayStrategyHandler.signVerify(params));
    }

    @Test
    @DisplayName("dataVerify 金额、商户、appId、交易状态全部一致时返回true")
    void dataVerifySuccess() {
        Map<String, String> params = buildValidNotifyParams();
        PayBill payBill = buildPayBill(new BigDecimal("100.00"));

        assertTrue(alipayStrategyHandler.dataVerify(params, payBill));
    }

    @Test
    @DisplayName("dataVerify 回调金额与账单支付金额不一致时返回false")
    void dataVerifyAmountMismatch() {
        Map<String, String> params = buildValidNotifyParams();
        params.put("total_amount", "99.99");
        PayBill payBill = buildPayBill(new BigDecimal("100.00"));

        assertFalse(alipayStrategyHandler.dataVerify(params, payBill));
    }

    @Test
    @DisplayName("dataVerify 回调商户pid与配置不一致时返回false")
    void dataVerifySellerMismatch() {
        Map<String, String> params = buildValidNotifyParams();
        params.put("seller_id", "other-seller");
        PayBill payBill = buildPayBill(new BigDecimal("100.00"));

        assertFalse(alipayStrategyHandler.dataVerify(params, payBill));
    }

    @Test
    @DisplayName("dataVerify 回调appId与配置不一致时返回false")
    void dataVerifyAppIdMismatch() {
        Map<String, String> params = buildValidNotifyParams();
        params.put("app_id", "other-app");
        PayBill payBill = buildPayBill(new BigDecimal("100.00"));

        assertFalse(alipayStrategyHandler.dataVerify(params, payBill));
    }

    @Test
    @DisplayName("dataVerify 交易状态不是TRADE_SUCCESS时返回false")
    void dataVerifyTradeStatusNotSuccess() {
        Map<String, String> params = buildValidNotifyParams();
        params.put("trade_status", "WAIT_BUYER_PAY");
        PayBill payBill = buildPayBill(new BigDecimal("100.00"));

        assertFalse(alipayStrategyHandler.dataVerify(params, payBill));
    }

    @Test
    @DisplayName("queryTrade 查询成功(TRADE_SUCCESS)时映射为已支付账单状态")
    void queryTradeSuccessMapsToPaid() throws AlipayApiException {
        stubQueryTradeSuccess("TRADE_SUCCESS");

        TradeResult tradeResult = alipayStrategyHandler.queryTrade("order-1");

        assertTrue(tradeResult.isSuccess());
        assertEquals("order-1", tradeResult.getOutTradeNo());
        assertEquals(new BigDecimal("100.00"), tradeResult.getTotalAmount());
        assertEquals(PayBillStatus.PAY.getCode(), tradeResult.getPayBillStatus());
    }

    @Test
    @DisplayName("queryTrade 等待买家付款状态时映射为未支付账单状态")
    void queryTradeWaitBuyerPayMapsToNoPay() throws AlipayApiException {
        stubQueryTradeSuccess("WAIT_BUYER_PAY".toLowerCase());

        TradeResult tradeResult = alipayStrategyHandler.queryTrade("order-1");

        assertTrue(tradeResult.isSuccess());
        assertEquals(PayBillStatus.NO_PAY.getCode(), tradeResult.getPayBillStatus());
    }

    @Test
    @DisplayName("queryTrade 交易关闭状态时映射为已取消账单状态")
    void queryTradeClosedMapsToCancel() throws AlipayApiException {
        stubQueryTradeSuccess("trade_closed");

        TradeResult tradeResult = alipayStrategyHandler.queryTrade("order-1");

        assertTrue(tradeResult.isSuccess());
        assertEquals(PayBillStatus.CANCEL.getCode(), tradeResult.getPayBillStatus());
    }

    @Test
    @DisplayName("queryTrade 交易结束状态时映射为已支付账单状态")
    void queryTradeFinishedMapsToPaid() throws AlipayApiException {
        stubQueryTradeSuccess("TRADE_FINISHED");

        TradeResult tradeResult = alipayStrategyHandler.queryTrade("order-1");

        assertTrue(tradeResult.isSuccess());
        assertEquals(PayBillStatus.PAY.getCode(), tradeResult.getPayBillStatus());
    }

    @Test
    @DisplayName("queryTrade 遇到未知交易状态时状态转换失败，账单状态为null")
    void queryTradeUnknownStatusReturnsNullPayBillStatus() throws AlipayApiException {
        stubQueryTradeSuccess("UNKNOWN_STATUS");

        TradeResult tradeResult = alipayStrategyHandler.queryTrade("order-1");

        // 状态转换抛出异常被catch捕获，convertPayBillStatus之前的赋值已生效，
        // 因此 success 为 true 但 payBillStatus 为 null（源码行为如此）
        assertNull(tradeResult.getPayBillStatus());
    }

    @Test
    @DisplayName("queryTrade SDK返回不成功时返回失败结果")
    void queryTradeResponseNotSuccessReturnsFailure() throws AlipayApiException {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(tradeQueryResponse);
        when(tradeQueryResponse.isSuccess()).thenReturn(false);

        TradeResult tradeResult = alipayStrategyHandler.queryTrade("order-1");

        assertFalse(tradeResult.isSuccess());
        assertNull(tradeResult.getOutTradeNo());
    }

    @Test
    @DisplayName("queryTrade SDK抛异常时返回失败结果而不是抛出异常")
    void queryTradeSdkExceptionReturnsFailure() throws AlipayApiException {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenThrow(new AlipayApiException("sdk error"));

        TradeResult tradeResult = alipayStrategyHandler.queryTrade("order-1");

        assertFalse(tradeResult.isSuccess());
    }

    @Test
    @DisplayName("refund 退款成功时返回成功结果")
    void refundSuccess() throws AlipayApiException {
        when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(tradeRefundResponse);
        when(tradeRefundResponse.isSuccess()).thenReturn(true);
        when(tradeRefundResponse.getBody()).thenReturn("refund-body");
        when(tradeRefundResponse.getMsg()).thenReturn("Success");

        RefundResult refundResult =
                alipayStrategyHandler.refund("order-1", new BigDecimal("100.00"), "用户取消");

        assertTrue(refundResult.isSuccess());
        assertEquals("refund-body", refundResult.getBody());
        assertEquals("Success", refundResult.getMessage());
    }

    @Test
    @DisplayName("refund SDK抛异常时转换为 REFUND_ERROR 业务异常")
    void refundThrowsBusinessExceptionOnSdkError() throws AlipayApiException {
        when(alipayClient.execute(any(AlipayTradeRefundRequest.class)))
                .thenThrow(new AlipayApiException("sdk error"));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> alipayStrategyHandler.refund("order-1", new BigDecimal("100.00"), "用户取消"));
        assertEquals(BaseCode.REFUND_ERROR.getCode(), exception.getCode());
    }

    private Map<String, String> buildValidNotifyParams() {
        Map<String, String> params = new HashMap<>();
        params.put("total_amount", "100.00");
        params.put("seller_id", "seller-001");
        params.put("app_id", "app-id-001");
        params.put("trade_status", "TRADE_SUCCESS");
        return params;
    }

    private PayBill buildPayBill(BigDecimal payAmount) {
        PayBill payBill = new PayBill();
        payBill.setId(1L);
        payBill.setOutOrderNo("order-1");
        payBill.setPayAmount(payAmount);
        return payBill;
    }

    private void stubQueryTradeSuccess(String tradeStatus) throws AlipayApiException {
        String body = "{\"alipay_trade_query_response\":{"
                + "\"code\":\"10000\",\"msg\":\"Success\","
                + "\"out_trade_no\":\"order-1\",\"total_amount\":\"100.00\","
                + "\"trade_status\":\"" + tradeStatus + "\"}}";
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(tradeQueryResponse);
        when(tradeQueryResponse.isSuccess()).thenReturn(true);
        when(tradeQueryResponse.getBody()).thenReturn(body);
    }
}
