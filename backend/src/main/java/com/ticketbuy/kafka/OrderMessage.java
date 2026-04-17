package com.ticketbuy.kafka;

import java.io.Serializable;
import java.math.BigDecimal;

public class OrderMessage implements Serializable {
    private String orderNo;
    private Long userId;
    private Long eventId;
    private Long ticketTypeId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer type;

    public OrderMessage() {}

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public Long getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final OrderMessage msg = new OrderMessage();
        public Builder orderNo(String orderNo) { msg.orderNo = orderNo; return this; }
        public Builder userId(Long userId) { msg.userId = userId; return this; }
        public Builder eventId(Long eventId) { msg.eventId = eventId; return this; }
        public Builder ticketTypeId(Long ticketTypeId) { msg.ticketTypeId = ticketTypeId; return this; }
        public Builder quantity(Integer quantity) { msg.quantity = quantity; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { msg.totalAmount = totalAmount; return this; }
        public Builder type(Integer type) { msg.type = type; return this; }
        public OrderMessage build() { return msg; }
    }
}
