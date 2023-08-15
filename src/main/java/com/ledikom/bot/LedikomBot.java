package com.ledikom.bot;

import com.ledikom.model.Coupon;
import com.ledikom.model.User;
import com.ledikom.model.UserCouponKey;
import com.ledikom.model.UserCouponRecord;
import com.ledikom.service.CouponService;
import com.ledikom.service.UserService;
import com.ledikom.utils.BotResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class LedikomBot extends TelegramLongPollingBot {

    @Value("${bot_username}")
    private String botUsername;
    @Value("${bot_token}")
    private String botToken;
    @Value("${tech_admin_id}")
    private Long techAdminId;
    @Value("${admin_id}")
    private Long adminId;
    @Value("${coupon.time-in-minutes}")
    private int couponTimeInMinutes;
    private final BotService botService;
    private final Logger log = LoggerFactory.getLogger(LedikomBot.class);
    private final UserService userService;
    private final CouponService couponService;


    public LedikomBot(BotService botService, UserService userService, CouponService couponService) {
        this.botService = botService;
        this.userService = userService;
        this.couponService = couponService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            var msg = update.getMessage();
            var chatId = msg.getChatId();

            if (Objects.equals(chatId, adminId)) {
                processAdminMessage(msg.getText());
                return;
            }

            processMessage(msg.getText(), chatId);

        } else if (update.hasCallbackQuery()) {
            var qry = update.getCallbackQuery();
            var chatId = qry.getMessage().getChatId();

            if (Objects.equals(chatId, adminId)) {
                processAdminMessage(qry.getData());
                return;
            }

            processMessage(qry.getData(), chatId);
        }

    }

    public void processMessage(String command, Long chatId) {

        if(command.startsWith("couponPreview_")) {
            generateCouponAcceptMessageIfNotUsed(command, chatId);
            return;
        }
        if (command.startsWith("couponAccept_")) {
            generateCouponIfNotUsed(command, chatId);
            return;
        }

        switch (command) {
            case "/start" -> sendMessage(botService.addUserAndGenerateHelloMessage(chatId));

            case "/activecoupons" -> sendMessage(botService.showAllCoupons(chatId));

            case "/setreminder" -> System.out.println("setnotification");

            case "/removereminder" -> System.out.println("deletenotification");

            case "/showpharmacies" -> System.out.println("pharmacies");

            default -> {
                log.error("Illegal State", new IllegalStateException());
                sendMessage(new IllegalStateException().toString(), techAdminId);
            }
        }
    }

    public void processAdminMessage(String command) {

        if(command.startsWith("coupon")) {
            broadcastMessage(botService.createNewCoupon(command));
            return;
        }

        switch (command) {
            case "/start" -> sendMessage(BotResponses.helloAdmin(), adminId);

            case "/createpoll" -> System.out.println("createpoll");

            default -> {
                log.error("Illegal State", new IllegalStateException());
                sendMessage(new IllegalStateException().toString(), techAdminId);
            }
        }
    }

    public void broadcastMessage(SendMessage sm) {
        List<User> registeredUsers = userService.getAllUsers();

        for (User user : registeredUsers) {
            sm.setChatId(user.getChatId());

            try {
                execute(sm);
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void generateCouponIfNotUsed(final String couponCommand, final Long chatId) {
        Coupon coupon = botService.generateCouponIfNotUsed(couponCommand, chatId);

        if (coupon == null) {
            sendMessage(BotResponses.couponUsedMessage(), chatId);
        } else {
            String couponTextWithUniqueSign = botService.generateSignedCoupon(coupon);
            UserCouponKey userCouponKey = sendCoupon("Времени осталось: " + couponTimeInMinutes + ":00" +
                    "\n\n" +
                    couponTextWithUniqueSign, chatId);
            botService.addCouponToMap(userCouponKey, couponTextWithUniqueSign);
        }
    }

    private void generateCouponAcceptMessageIfNotUsed(final String couponCommand, final long chatId) {
        sendMessage(botService.generateCouponAcceptMessageIfNotUsed(couponCommand, chatId));
    }

    private void sendMessage(String botReply, Long chatId) {
        var sm = SendMessage.builder()
                .chatId(chatId)
                .text(botReply).build();

        try {
            execute(sm);
        } catch (Exception e) {
            log.trace(e.getMessage());
        }
    }

    private void sendMessage(SendMessage sm) {
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private UserCouponKey sendCoupon(String couponText, Long chatId) {
        var sm = SendMessage.builder()
                .chatId(chatId)
                .text(couponText).build();
        try {
            Message sentMessage = execute(sm);
            return new UserCouponKey(sentMessage.getChatId(), sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    private void editMessage(final Long chatId, final Integer messageId, final String editedMessage) {
        var editMessageText = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(editedMessage)
                .build();

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void updateCouponTimerAndMessage(final UserCouponKey userCouponKey, final UserCouponRecord userCouponRecord) {
        long timeLeftInSeconds = (userCouponRecord.getExpiryTimestamp() - System.currentTimeMillis()) / 1000;

        if (timeLeftInSeconds >= 0) {
            editMessage(userCouponKey.getChatId(), userCouponKey.getMessageId(), botService.generateCouponText(userCouponRecord, timeLeftInSeconds));
        } else {
            editMessage(userCouponKey.getChatId(), userCouponKey.getMessageId(), "Время вашего купона истекло.");
        }

    }

    @Scheduled(fixedRate = 1000)
    public void processCouponsInMap() {
        BotService.userCoupons.forEach(this::updateCouponTimerAndMessage);
        botService.removeExpiredCouponsFromMap();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteCouponsIfExpired() {
        List<Coupon> coupons = couponService.getAllCoupons();

        LocalDateTime currentDateTime = LocalDateTime.now();

        for (Coupon coupon : coupons) {
            if(coupon.getId() != 1 && currentDateTime.isAfter(coupon.getExpiryDateTime())) {
                couponService.deleteCoupon(coupon);
            }
        }
    }
}
