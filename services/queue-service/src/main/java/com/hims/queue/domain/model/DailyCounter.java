package com.hims.queue.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Daily-reset token counter (one row per queue date). Not an audit entity. */
@Entity
@Table(name = "daily_counter")
public class DailyCounter {

    @Id
    @Column(name = "counter_date")
    private LocalDate counterDate;

    @Column(name = "last_no", nullable = false)
    private Long lastNo = 0L;

    public LocalDate getCounterDate() { return counterDate; }
    public void setCounterDate(LocalDate counterDate) { this.counterDate = counterDate; }
    public Long getLastNo() { return lastNo; }
    public void setLastNo(Long lastNo) { this.lastNo = lastNo; }
}
