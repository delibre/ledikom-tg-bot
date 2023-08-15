package com.ledikom.service;

import com.ledikom.model.Coupon;
import com.ledikom.model.User;
import com.ledikom.repository.CouponRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    @Value("${hello-coupon.discount}")
    private int helloCouponDiscount;
    @Value("${hello-coupon.name}")
    private String helloCouponName;
    private final static String HELLO_COUPON_TEXT = "Приветственный купон -5% на вашу следующую покупку.";

    private final CouponRepository couponRepository;
    private final UserService userService;

    public CouponService(final CouponRepository couponRepository, final UserService userService) {
        this.couponRepository = couponRepository;
        this.userService = userService;
    }

    @PostConstruct
    public void createHelloCoupon() {
        Coupon coupon = new Coupon(HELLO_COUPON_TEXT, helloCouponName, helloCouponDiscount);
        couponRepository.save(coupon);

        List<User> users = userService.getAllUsers();

        if (!users.isEmpty()) {
            users.forEach(user -> user.getCoupons().add(coupon));
            userService.saveAll(users);
        }
    }

    public Coupon createNewCoupon(final int discount, final String name, final String text, final LocalDateTime expiryDateTime) {
        Coupon newCoupon = new Coupon(text, name, discount, expiryDateTime);
        couponRepository.save(newCoupon);

        couponRepository.save(newCoupon);

        List<User> users = userService.getAllUsers();

        if (!users.isEmpty()) {
            users.forEach(user -> user.getCoupons().add(newCoupon));
            userService.saveAll(users);
        }

        return newCoupon;
    }

    public void addCouponsToUser(final User user) {
        List<Coupon> coupons = couponRepository.findAll();
        user.getCoupons().addAll(coupons);
        userService.saveUser(user);
    }

    public Coupon findByName(final String helloCouponName) {
        return couponRepository.findByName(helloCouponName).orElseThrow(() -> new RuntimeException("Coupon not found"));
    }

    public Coupon findCouponForUser(final User user, final String couponCommand) {
        int couponId = Integer.parseInt(couponCommand.split("_")[1]);
        return user.getCoupons().stream().filter(c -> c.getId() == couponId).findFirst().orElse(null);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public void deleteCoupon(Coupon coupon) {
        couponRepository.delete(coupon);
    }
}
