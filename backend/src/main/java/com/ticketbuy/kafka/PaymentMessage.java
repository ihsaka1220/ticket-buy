package com.ticketbuy.kafka;

import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentMessage implements Serializable {
    private String orderNo;
    private String tradeNo;
    private BigDecimal amount;
    private String payStatus;
    private Long userId;

    public PaymentMessage() {}

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getTradeNo() { return tradeNo; }
    public void setTradeNo(String tradeNo) { this.tradeNo = tradeNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final PaymentMessage msg = new PaymentMessage();
        public Builder orderNo(String orderNo) { msg.orderNo = orderNo; return this; }
        public Builder tradeNo(String tradeNo) { msg.tradeNo = tradeNo; return this; }
        public Builder amount(BigDecimal amount) { msg.amount = amount; return this; }
        public Builder payStatus(String payStatus) { msg.payStatus = payStatus; return this; }
        public Builder userId(Long userId) { msg.userId = userId; return this; }
        public PaymentMessage build() { return msg; }
    }
}
