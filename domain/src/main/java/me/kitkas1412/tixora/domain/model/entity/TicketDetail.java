package me.kitkas1412.tixora.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_item")
public class TicketDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "stock_initial", nullable = false)
    private int stockInitial;

    @Column(name = "stock_available", nullable = false)
    private int stockAvailable;

    @Column(name = "is_stock_prepared", nullable = false)
    private boolean stockPrepared;

    @Column(name = "price_original", nullable = false)
    private Long priceOriginal;

    @Column(name = "price_flash", nullable = false)
    private Long priceFlash;

    @Column(name = "sale_start_time", nullable = false)
    private LocalDateTime saleStartTime;

    @Column(name = "sale_end_time", nullable = false)
    private LocalDateTime saleEndTime;

    @Column(name = "status", nullable = false)
    private int status;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TicketDetail() {
    }

    public TicketDetail(String name, String description, int stockInitial, int stockAvailable,
                        boolean stockPrepared, Long priceOriginal, Long priceFlash,
                        LocalDateTime saleStartTime, LocalDateTime saleEndTime, int status, Long activityId) {
        this.name = name;
        this.description = description;
        this.stockInitial = stockInitial;
        this.stockAvailable = stockAvailable;
        this.stockPrepared = stockPrepared;
        this.priceOriginal = priceOriginal;
        this.priceFlash = priceFlash;
        this.saleStartTime = saleStartTime;
        this.saleEndTime = saleEndTime;
        this.status = status;
        this.activityId = activityId;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStockInitial() {
        return stockInitial;
    }

    public void setStockInitial(int stockInitial) {
        this.stockInitial = stockInitial;
    }

    public int getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(int stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public boolean isStockPrepared() {
        return stockPrepared;
    }

    public void setStockPrepared(boolean stockPrepared) {
        this.stockPrepared = stockPrepared;
    }

    public Long getPriceOriginal() {
        return priceOriginal;
    }

    public void setPriceOriginal(Long priceOriginal) {
        this.priceOriginal = priceOriginal;
    }

    public Long getPriceFlash() {
        return priceFlash;
    }

    public void setPriceFlash(Long priceFlash) {
        this.priceFlash = priceFlash;
    }

    public LocalDateTime getSaleStartTime() {
        return saleStartTime;
    }

    public void setSaleStartTime(LocalDateTime saleStartTime) {
        this.saleStartTime = saleStartTime;
    }

    public LocalDateTime getSaleEndTime() {
        return saleEndTime;
    }

    public void setSaleEndTime(LocalDateTime saleEndTime) {
        this.saleEndTime = saleEndTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
