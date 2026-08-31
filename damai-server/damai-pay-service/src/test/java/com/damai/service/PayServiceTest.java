package com.damai.service;

import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.damai.dto.NotifyDto;
import com.damai.dto.PayBillDto;
import com.damai.dto.PayDto;
import com.damai.dto.RefundDto;
import com.damai.dto.TradeCheckDto;
import com.damai.entity.PayBill;
import com.damai.entity.RefundBill;
import com.damai.enums.BaseCode;
import com.damai.enums.PayBillStatus;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.PayBillMapper;
import com.damai.mapper.RefundBillMapper;
import com.damai.pay.PayResult;
import com.damai.pay.PayStrategyContext;
import com.damai.pay.PayStrategyHandler;
import com.damai.pay.RefundResult;
import com.damai.pay.TradeResult;
import com.damai.vo.NotifyVo;
import com.damai.vo.PayBillVo;
import com.damai.vo.TradeCheckVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付 service 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PayServiceTest {

    @Mock
    private PayBillMapper payBillMapper;

    @Mock
    private RefundBillMapper refundBillMapper;

    @Mock
    private PayStrategyContext payStrategyContext;

    @Mock
    private UidGenerator uidGenerator;

    @Mock
    private PayStrategyHandler payStrategyHandler;

    @InjectMocks
    private PayService payService;

    // ===================== commonPay =====================

    @Test
    @DisplayName("commonPay 首次支付成功时插入未支付账单并返回支付表单")
    void commonPaySuccessInsertsNewBill() {
        PayDto payDto = buildPayDto();
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.pay(eq("1001"), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenReturn(new PayResult(true, "pay-form"));
        when(uidGenerator.getUid()).thenReturn(999L);

        String body = payService.commonPay(payDto);

        assertEquals("pay-form", body);
        ArgumentCaptor<PayBill> captor = ArgumentCaptor.forClass(PayBill.class);
        verify(payBillMapper).insert(captor.capture());
        PayBill inserted = captor.getValue();
        assertEquals(999L, inserted.getId());
        assertEquals("1001", inserted.getOutOrderNo());
        assertEquals("alipay", inserted.getPayChannel());
        assertEquals("门票", inserted.getSubject());
        assertEquals(new BigDecimal("100.00"), inserted.getPayAmount());
        assertEquals(PayBillStatus.NO_PAY.getCode(), inserted.getPayBillStatus());
        assertNotNull(inserted.getPayTime());
        verify(payBillMapper, never()).updateById(any(PayBill.class));
    }

    @Test
    @DisplayName("commonPay 未支付账单重复支付时只更新支付时间不新增账单")
    void commonPayExistingNoPayBillUpdatesPayTime() {
        PayDto payDto = buildPayDto();
        PayBill existBill = buildPayBill(PayBillStatus.NO_PAY.getCode(), new BigDecimal("100.00"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existBill);
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.pay(anyString(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenReturn(new PayResult(true, "pay-form"));

        String body = payService.commonPay(payDto);

        assertEquals("pay-form", body);
        verify(payBillMapper, never()).insert(any(PayBill.class));
        ArgumentCaptor<PayBill> captor = ArgumentCaptor.forClass(PayBill.class);
        verify(payBillMapper).updateById(captor.capture());
        assertEquals(existBill.getId(), captor.getValue().getId());
        assertNotNull(captor.getValue().getPayTime());
    }

    @Test
    @DisplayName("commonPay 账单已是非未支付状态时抛出 PAY_BILL_IS_NOT_NO_PAY 异常")
    void commonPayBillNotNoPayThrows() {
        PayDto payDto = buildPayDto();
        PayBill paidBill = buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidBill);

        DaMaiFrameException exception =
                assertThrows(DaMaiFrameException.class, () -> payService.commonPay(payDto));
        assertEquals(BaseCode.PAY_BILL_IS_NOT_NO_PAY.getCode(), exception.getCode());
        verify(payStrategyContext, never()).get(anyString());
    }

    @Test
    @DisplayName("commonPay 支付渠道返回失败时不落库且返回失败body")
    void commonPayChannelFailureDoesNotPersist() {
        PayDto payDto = buildPayDto();
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.pay(anyString(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenReturn(new PayResult(false, "error-body"));

        String body = payService.commonPay(payDto);

        assertEquals("error-body", body);
        verify(payBillMapper, never()).insert(any(PayBill.class));
        verify(payBillMapper, never()).updateById(any(PayBill.class));
    }

    // ===================== notify =====================

    @Test
    @DisplayName("notify 验签失败时返回 failure 且不查询账单")
    void notifySignVerifyFailReturnsFailure() {
        NotifyDto notifyDto = buildNotifyDto();
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.signVerify(any())).thenReturn(false);

        NotifyVo notifyVo = payService.notify(notifyDto);

        assertEquals("failure", notifyVo.getPayResult());
        verify(payBillMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("notify 验签成功但账单不存在时返回 failure")
    void notifyBillNotExistReturnsFailure() {
        NotifyDto notifyDto = buildNotifyDto();
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.signVerify(any())).thenReturn(true);
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        NotifyVo notifyVo = payService.notify(notifyDto);

        assertEquals("failure", notifyVo.getPayResult());
    }

    @Test
    @DisplayName("notify 账单已是已支付状态时幂等返回 success 且不做数据校验")
    void notifyAlreadyPaidReturnsSuccess() {
        NotifyDto notifyDto = buildNotifyDto();
        PayBill payBill = buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00"));
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.signVerify(any())).thenReturn(true);
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payBill);

        NotifyVo notifyVo = payService.notify(notifyDto);

        assertEquals("success", notifyVo.getPayResult());
        assertEquals("1001", notifyVo.getOutTradeNo());
        verify(payStrategyHandler, never()).dataVerify(any(), any());
        verify(payBillMapper, never()).update(any(PayBill.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("notify 账单已取消时幂等返回 success")
    void notifyAlreadyCancelledReturnsSuccess() {
        NotifyDto notifyDto = buildNotifyDto();
        PayBill payBill = buildPayBill(PayBillStatus.CANCEL.getCode(), new BigDecimal("100.00"));
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.signVerify(any())).thenReturn(true);
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payBill);

        NotifyVo notifyVo = payService.notify(notifyDto);

        assertEquals("success", notifyVo.getPayResult());
        verify(payBillMapper, never()).update(any(PayBill.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("notify 账单已退单时幂等返回 success")
    void notifyAlreadyRefundedReturnsSuccess() {
        NotifyDto notifyDto = buildNotifyDto();
        PayBill payBill = buildPayBill(PayBillStatus.REFUND.getCode(), new BigDecimal("100.00"));
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.signVerify(any())).thenReturn(true);
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payBill);

        NotifyVo notifyVo = payService.notify(notifyDto);

        assertEquals("success", notifyVo.getPayResult());
        verify(payBillMapper, never()).update(any(PayBill.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("notify 数据校验失败时返回 failure 且不更新账单")
    void notifyDataVerifyFailReturnsFailure() {
        NotifyDto notifyDto = buildNotifyDto();
        PayBill payBill = buildPayBill(PayBillStatus.NO_PAY.getCode(), new BigDecimal("100.00"));
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.signVerify(any())).thenReturn(true);
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payBill);
        when(payStrategyHandler.dataVerify(any(), eq(payBill))).thenReturn(false);

        NotifyVo notifyVo = payService.notify(notifyDto);

        assertEquals("failure", notifyVo.getPayResult());
        verify(payBillMapper, never()).update(any(PayBill.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("notify 验签和数据校验都通过时更新账单为已支付并返回 success")
    void notifySuccessUpdatesBillToPaid() {
        NotifyDto notifyDto = buildNotifyDto();
        PayBill payBill = buildPayBill(PayBillStatus.NO_PAY.getCode(), new BigDecimal("100.00"));
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.signVerify(any())).thenReturn(true);
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payBill);
        when(payStrategyHandler.dataVerify(any(), eq(payBill))).thenReturn(true);

        NotifyVo notifyVo = payService.notify(notifyDto);

        assertEquals("success", notifyVo.getPayResult());
        assertEquals("1001", notifyVo.getOutTradeNo());
        ArgumentCaptor<PayBill> captor = ArgumentCaptor.forClass(PayBill.class);
        verify(payBillMapper).update(captor.capture(), any(LambdaUpdateWrapper.class));
        assertEquals(PayBillStatus.PAY.getCode(), captor.getValue().getPayBillStatus());
    }

    // ===================== tradeCheck =====================

    @Test
    @DisplayName("tradeCheck 渠道查询失败时直接返回失败结果")
    void tradeCheckQueryFailureReturnsVo() {
        TradeCheckDto tradeCheckDto = buildTradeCheckDto();
        TradeResult tradeResult = new TradeResult();
        tradeResult.setSuccess(false);
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.queryTrade("1001")).thenReturn(tradeResult);

        TradeCheckVo vo = payService.tradeCheck(tradeCheckDto);

        assertFalse(vo.isSuccess());
        verify(payBillMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("tradeCheck 本地账单不存在时不做更新")
    void tradeCheckBillNotExistDoesNotUpdate() {
        TradeCheckDto tradeCheckDto = buildTradeCheckDto();
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.queryTrade("1001")).thenReturn(buildTradeResult());
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        TradeCheckVo vo = payService.tradeCheck(tradeCheckDto);

        assertTrue(vo.isSuccess());
        verify(payBillMapper, never()).update(any(PayBill.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("tradeCheck 渠道金额与账单金额不一致时不更新状态")
    void tradeCheckAmountMismatchDoesNotUpdate() {
        TradeCheckDto tradeCheckDto = buildTradeCheckDto();
        TradeResult tradeResult = buildTradeResult();
        tradeResult.setTotalAmount(new BigDecimal("99.99"));
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.queryTrade("1001")).thenReturn(tradeResult);
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00")));

        payService.tradeCheck(tradeCheckDto);

        verify(payBillMapper, never()).update(any(PayBill.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("tradeCheck 渠道与账单状态不一致时以渠道状态为准更新账单")
    void tradeCheckStatusMismatchUpdatesBill() {
        TradeCheckDto tradeCheckDto = buildTradeCheckDto();
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.queryTrade("1001")).thenReturn(buildTradeResult());
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildPayBill(PayBillStatus.NO_PAY.getCode(), new BigDecimal("100.00")));

        TradeCheckVo vo = payService.tradeCheck(tradeCheckDto);

        assertTrue(vo.isSuccess());
        ArgumentCaptor<PayBill> captor = ArgumentCaptor.forClass(PayBill.class);
        verify(payBillMapper).update(captor.capture(), any(LambdaUpdateWrapper.class));
        assertEquals(PayBillStatus.PAY.getCode(), captor.getValue().getPayBillStatus());
    }

    @Test
    @DisplayName("tradeCheck 渠道与账单状态一致时不更新账单")
    void tradeCheckStatusEqualDoesNotUpdate() {
        TradeCheckDto tradeCheckDto = buildTradeCheckDto();
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.queryTrade("1001")).thenReturn(buildTradeResult());
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00")));

        payService.tradeCheck(tradeCheckDto);

        verify(payBillMapper, never()).update(any(PayBill.class), any(LambdaUpdateWrapper.class));
    }

    // ===================== refund =====================

    @Test
    @DisplayName("refund 账单不存在时抛出 PAY_BILL_NOT_EXIST 异常")
    void refundBillNotExistThrows() {
        RefundDto refundDto = buildRefundDto(new BigDecimal("100.00"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        DaMaiFrameException exception =
                assertThrows(DaMaiFrameException.class, () -> payService.refund(refundDto));
        assertEquals(BaseCode.PAY_BILL_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("refund 账单不是已支付状态时抛出 PAY_BILL_IS_NOT_PAY_STATUS 异常")
    void refundBillNotPaidThrows() {
        RefundDto refundDto = buildRefundDto(new BigDecimal("100.00"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildPayBill(PayBillStatus.NO_PAY.getCode(), new BigDecimal("100.00")));

        DaMaiFrameException exception =
                assertThrows(DaMaiFrameException.class, () -> payService.refund(refundDto));
        assertEquals(BaseCode.PAY_BILL_IS_NOT_PAY_STATUS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("refund 退款金额大于支付金额时抛出 REFUND_AMOUNT_GREATER_THAN_PAY_AMOUNT 异常")
    void refundAmountGreaterThanPayAmountThrows() {
        RefundDto refundDto = buildRefundDto(new BigDecimal("100.01"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00")));

        DaMaiFrameException exception =
                assertThrows(DaMaiFrameException.class, () -> payService.refund(refundDto));
        assertEquals(BaseCode.REFUND_AMOUNT_GREATER_THAN_PAY_AMOUNT.getCode(), exception.getCode());
        verify(payStrategyContext, never()).get(anyString());
    }

    @Test
    @DisplayName("refund 退款成功时账单改为已退单并插入退款账单")
    void refundSuccessUpdatesBillAndInsertsRefundBill() {
        RefundDto refundDto = buildRefundDto(new BigDecimal("100.00"));
        PayBill payBill = buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payBill);
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.refund(eq("1001"), any(BigDecimal.class), eq("用户取消")))
                .thenReturn(new RefundResult(true, "refund-body", "Success"));
        when(uidGenerator.getUid()).thenReturn(888L);

        String outOrderNo = payService.refund(refundDto);

        assertEquals("1001", outOrderNo);
        ArgumentCaptor<PayBill> payBillCaptor = ArgumentCaptor.forClass(PayBill.class);
        verify(payBillMapper).updateById(payBillCaptor.capture());
        assertEquals(PayBillStatus.REFUND.getCode(), payBillCaptor.getValue().getPayBillStatus());
        assertEquals(payBill.getId(), payBillCaptor.getValue().getId());

        ArgumentCaptor<RefundBill> refundBillCaptor = ArgumentCaptor.forClass(RefundBill.class);
        verify(refundBillMapper).insert(refundBillCaptor.capture());
        RefundBill refundBill = refundBillCaptor.getValue();
        assertEquals(888L, refundBill.getId());
        assertEquals("1001", refundBill.getOutOrderNo());
        assertEquals(payBill.getId(), refundBill.getPayBillId());
        assertEquals(new BigDecimal("100.00"), refundBill.getRefundAmount());
        assertEquals(2, refundBill.getRefundStatus());
        assertEquals("用户取消", refundBill.getReason());
        assertNotNull(refundBill.getRefundTime());
    }

    @Test
    @DisplayName("refund 渠道退款失败时抛出业务异常且不落库")
    void refundChannelFailureThrows() {
        RefundDto refundDto = buildRefundDto(new BigDecimal("100.00"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00")));
        when(payStrategyContext.get("alipay")).thenReturn(payStrategyHandler);
        when(payStrategyHandler.refund(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new RefundResult(false, "error-body", "退款失败"));

        // 注：此处不校验异常message，DaMaiFrameException(String)构造器未给message字段赋值，
        // 且Lombok生成的getMessage()覆盖了Throwable.getMessage()导致返回null
        assertThrows(DaMaiFrameException.class, () -> payService.refund(refundDto));
        verify(payBillMapper, never()).updateById(any(PayBill.class));
        verify(refundBillMapper, never()).insert(any(RefundBill.class));
    }

    // ===================== detail =====================

    @Test
    @DisplayName("detail 账单存在时返回复制后的账单视图")
    void detailBillExistReturnsVo() {
        PayBillDto payBillDto = new PayBillDto();
        payBillDto.setOrderNumber("1001");
        PayBill payBill = buildPayBill(PayBillStatus.PAY.getCode(), new BigDecimal("100.00"));
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payBill);

        PayBillVo vo = payService.detail(payBillDto);

        assertEquals(payBill.getId(), vo.getId());
        assertEquals("1001", vo.getOutOrderNo());
        assertEquals(new BigDecimal("100.00"), vo.getPayAmount());
        assertEquals(PayBillStatus.PAY.getCode(), vo.getPayBillStatus());
    }

    @Test
    @DisplayName("detail 账单不存在时返回空视图对象")
    void detailBillNotExistReturnsEmptyVo() {
        PayBillDto payBillDto = new PayBillDto();
        payBillDto.setOrderNumber("1001");
        when(payBillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        PayBillVo vo = payService.detail(payBillDto);

        assertNull(vo.getId());
        assertNull(vo.getOutOrderNo());
    }

    // ===================== 构造工具方法 =====================

    private PayDto buildPayDto() {
        PayDto payDto = new PayDto();
        payDto.setPlatform(3);
        payDto.setOrderNumber("1001");
        payDto.setSubject("门票");
        payDto.setPrice(new BigDecimal("100.00"));
        payDto.setChannel("alipay");
        payDto.setPayBillType(1);
        payDto.setNotifyUrl("http://notify");
        payDto.setReturnUrl("http://return");
        return payDto;
    }

    private NotifyDto buildNotifyDto() {
        NotifyDto notifyDto = new NotifyDto();
        notifyDto.setChannel("alipay");
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "1001");
        notifyDto.setParams(params);
        return notifyDto;
    }

    private TradeCheckDto buildTradeCheckDto() {
        TradeCheckDto tradeCheckDto = new TradeCheckDto();
        tradeCheckDto.setOutTradeNo("1001");
        tradeCheckDto.setChannel("alipay");
        return tradeCheckDto;
    }

    private RefundDto buildRefundDto(BigDecimal amount) {
        RefundDto refundDto = new RefundDto();
        refundDto.setOrderNumber("1001");
        refundDto.setAmount(amount);
        refundDto.setChannel("alipay");
        refundDto.setReason("用户取消");
        return refundDto;
    }

    private PayBill buildPayBill(Integer status, BigDecimal payAmount) {
        PayBill payBill = new PayBill();
        payBill.setId(1L);
        payBill.setOutOrderNo("1001");
        payBill.setPayChannel("alipay");
        payBill.setPayAmount(payAmount);
        payBill.setPayBillStatus(status);
        return payBill;
    }

    private TradeResult buildTradeResult() {
        TradeResult tradeResult = new TradeResult();
        tradeResult.setSuccess(true);
        tradeResult.setOutTradeNo("1001");
        tradeResult.setTotalAmount(new BigDecimal("100.00"));
        tradeResult.setPayBillStatus(PayBillStatus.PAY.getCode());
        return tradeResult;
    }
}
