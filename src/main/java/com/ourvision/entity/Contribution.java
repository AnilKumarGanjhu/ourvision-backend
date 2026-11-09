package com.ourvision.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="contributions")
public class Contribution {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Column(name="contribution_month", nullable=false, length=20) private String contributionMonth;
    @Column(name="payment_mode", nullable=false, length=20) private String paymentMode;
    @Column(name="receipt_no", unique=true, length=50) private String receiptNo;
    @Column(name="is_late", nullable=false) private boolean late;
    @Column(name="fine_amount", nullable=false, precision=12, scale=2) private BigDecimal fineAmount = BigDecimal.ZERO;
    @Column(name="paid_at", nullable=false) private LocalDateTime paidAt = LocalDateTime.now();
    public Contribution() {}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public String getContributionMonth(){return contributionMonth;} public void setContributionMonth(String v){contributionMonth=v;}
    public String getPaymentMode(){return paymentMode;} public void setPaymentMode(String v){paymentMode=v;}
    public String getReceiptNo(){return receiptNo;} public void setReceiptNo(String v){receiptNo=v;}
    public boolean isLate(){return late;} public void setLate(boolean v){late=v;}
    public BigDecimal getFineAmount(){return fineAmount;} public void setFineAmount(BigDecimal v){fineAmount=v;}
    public LocalDateTime getPaidAt(){return paidAt;}
}
