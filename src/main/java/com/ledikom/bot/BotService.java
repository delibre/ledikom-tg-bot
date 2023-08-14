package com.ledikom.bot;

import com.ledikom.model.Coupon;
import com.ledikom.model.User;
import com.ledikom.model.UserCouponKey;
import com.ledikom.model.UserCouponRecord;
import com.ledikom.service.CouponService;
import com.ledikom.service.UserService;
import com.ledikom.utils.BotResponses;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Service
@Setter
@Getter
public class BotService {

    public static final Map<UserCouponKey, UserCouponRecord> userCoupons = new HashMap<>();

    @Value("${hello-coupon.name}")
    private String helloCouponName;
    @Value("${hello-coupon.time-in-minutes}")
    private int helloCouponTimeInMinutes;

    private final UserService userService;
    private final CouponService couponService;

    public BotService(final UserService userService, final CouponService couponService) {
        this.userService = userService;
        this.couponService = couponService;
    }

    public SendMessage addUserAndGenerateHelloMessage(final Long chatId) {
        userService.addNewUser(chatId);

        var sm = SendMessage.builder()
                .chatId(chatId)
                .text(BotResponses.startMessage()).build();

//        User user = userService.findByChatId(chatId);
//        Coupon coupon = user.getCoupons().stream().filter(c -> c.getName().equals(helloCouponName)).findFirst().orElse(null);

        // TODO: extract to a method
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        var button = new InlineKeyboardButton();
        // TODO: text should be from one golden source
        button.setText("Активировать приветственный купон");
        // TODO: callback data can be smth in english to determine user input type
        button.setCallbackData("Активировать приветственный купон");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
        markup.setKeyboard(keyboard);

        sm.setReplyMarkup(markup);

//        sm.setReplyMarkup(createInlineButton(coupon.getText(), "coupon" + coupon.getId()));

        return sm;
    }

//    private InlineKeyboardMarkup createInlineButton(String buttonLabel, String callback) {
//        var markup = new InlineKeyboardMarkup();
//
//        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
//
//        var button = new InlineKeyboardButton();
//        button.setText(buttonLabel);
//        button.setCallbackData(callback);
//        List<InlineKeyboardButton> row = new ArrayList<>();
//        row.add(button);
//        keyboard.add(row);
//
//        markup.setKeyboard(keyboard);
//
//        return markup;
//    }

    public Coupon generateHelloCouponForUserIfNotUsed(final Long chatId) {
        User user = userService.findByChatId(chatId);
        Coupon coupon = user.getCoupons().stream().filter(c -> c.getName().equals(helloCouponName)).findFirst().orElse(null);
        if (coupon != null) {
            userService.removeCouponFromUser(user, coupon);
        }
        return coupon;
    }

    public void addCouponToMap(final UserCouponKey userCouponKey, final String couponText) {
        long expiryTimestamp = System.currentTimeMillis() + helloCouponTimeInMinutes * 60 * 1000L;
        userCoupons.put(userCouponKey, new UserCouponRecord(expiryTimestamp, couponText));
    }

    public SendMessage showAllCoupons(final Long chatId) {
        User user = userService.findByChatId(chatId);
        Set<Coupon> userCoupons = user.getCoupons();
        var sm = new SendMessage();

        if (userCoupons.isEmpty()) {
            sm = SendMessage.builder()
                    .chatId(chatId)
                    .text("У вас нету купонов").build();
        } else {
            sm = SendMessage.builder()
                    .chatId(chatId)
                    .text("Ваши купоны:").build();

            sm.setReplyMarkup(createInlineKeyboardMarkup(userCoupons));
        }

        return sm;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(final Set<Coupon> coupons) {
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Coupon coupon : coupons) {
            var button = new InlineKeyboardButton();
            button.setText(coupon.getName());
            button.setCallbackData("coupon" + coupon.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);

        return markup;
    }


}
