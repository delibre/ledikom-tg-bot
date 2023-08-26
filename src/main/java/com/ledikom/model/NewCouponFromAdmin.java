package com.ledikom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class NewCouponFromAdmin {
    private LocalDateTime expirationDate;
    private int discount;
    private String name;
    private String couponDescription;
    private String couponAnnouncementText;
}
