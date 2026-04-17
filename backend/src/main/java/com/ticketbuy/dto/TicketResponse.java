package com.ticketbuy.dto;

public class TicketResponse {
    private String orderNo;
    private String eventTitle;
    private String ticketTypeName;
    private Integer quantity;
    private String totalAmount;
    private String payUrl;
    private Long expireTimeSeconds;
    private String status;

    public TicketResponse() {}

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }
    public String getTicketTypeName() { return ticketTypeName; }
    public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
    public Long getExpireTimeSeconds() { return expireTimeSeconds; }
    public void setExpireTimeSeconds(Long expireTimeSeconds) { this.expireTimeSeconds = expireTimeSeconds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final TicketResponse response = new TicketResponse();

        public Builder orderNo(String orderNo) { response.orderNo = orderNo; return this; }
        public Builder eventTitle(String eventTitle) { response.eventTitle = eventTitle; return this; }
        public Builder ticketTypeName(String ticketTypeName) { response.ticketTypeName = ticketTypeName; return this; }
        public Builder quantity(Integer quantity) { response.quantity = quantity; return this; }
        public Builder totalAmount(String totalAmount) { response.totalAmount = totalAmount; return this; }
        public Builder payUrl(String payUrl) { response.payUrl = payUrl; return this; }
        public Builder expireTimeSeconds(Long expireTimeSeconds) { response.expireTimeSeconds = expireTimeSeconds; return this; }
        public Builder status(String status) { response.status = status; return this; }
        public TicketResponse build() { return response; }
    }
}
