package com.ticketbuy.dto;

import com.ticketbuy.entity.Event;
import com.ticketbuy.entity.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EventDetailResponse {
    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String venue;
    private String address;
    private LocalDateTime eventDate;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private String description;
    private String coverImage;
    private Integer status;
    private List<TicketTypeInfo> ticketTypes;

    public EventDetailResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public LocalDateTime getSaleStartTime() { return saleStartTime; }
    public void setSaleStartTime(LocalDateTime saleStartTime) { this.saleStartTime = saleStartTime; }
    public LocalDateTime getSaleEndTime() { return saleEndTime; }
    public void setSaleEndTime(LocalDateTime saleEndTime) { this.saleEndTime = saleEndTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<TicketTypeInfo> getTicketTypes() { return ticketTypes; }
    public void setTicketTypes(List<TicketTypeInfo> ticketTypes) { this.ticketTypes = ticketTypes; }

    public static class TicketTypeInfo {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer availableStock;
        private String description;

        public TicketTypeInfo() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getAvailableStock() { return availableStock; }
        public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public static TicketTypeInfo from(TicketType ticketType) {
            TicketTypeInfo info = new TicketTypeInfo();
            info.setId(ticketType.getId());
            info.setName(ticketType.getName());
            info.setPrice(ticketType.getPrice());
            info.setAvailableStock(ticketType.getAvailableStock());
            info.setDescription(ticketType.getDescription());
            return info;
        }
    }

    public static EventDetailResponse from(Event event, String categoryName, List<TicketType> ticketTypes) {
        EventDetailResponse response = new EventDetailResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setCategoryId(event.getCategoryId());
        response.setCategoryName(categoryName);
        response.setVenue(event.getVenue());
        response.setAddress(event.getAddress());
        response.setEventDate(event.getEventDate());
        response.setSaleStartTime(event.getSaleStartTime());
        response.setSaleEndTime(event.getSaleEndTime());
        response.setDescription(event.getDescription());
        response.setCoverImage(event.getCoverImage());
        response.setStatus(event.getStatus());
        response.setTicketTypes(ticketTypes.stream().map(TicketTypeInfo::from).toList());
        return response;
    }
}
