package com.ledikom.service;

import com.ledikom.callback.GetFileFromBotCallback;
import com.ledikom.model.Coupon;
import com.ledikom.utils.BotResponses;
import com.ledikom.utils.City;
import com.ledikom.utils.MusicMenuButton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BotUtilityService {

    @Value("${bot.token}")
    private String botToken;

    public String getPhotoFromUpdate(final Message msg, final GetFileFromBotCallback getFileFromBotCallback) {
        PhotoSize photo = null;
        if (msg.hasPhoto()) {
            photo = msg.getPhoto().stream()
                    .max(Comparator.comparingInt(PhotoSize::getWidth))
                    .orElse(null);
        } else if (msg.hasDocument()) {
            photo = msg.getDocument().getThumbnail();
        }

        if (photo != null) {
            GetFile getFileRequest = new GetFile();
            getFileRequest.setFileId(photo.getFileId());
            try {
                File file = getFileFromBotCallback.execute(getFileRequest);
                return "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public SendMessage buildSendMessage(String text, long chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text).build();
    }

    public SendPoll buildSendPoll(Poll poll, long chatId) {
        return SendPoll.builder()
                .chatId(chatId)
                .question(poll.getQuestion())
                .options(poll.getOptions().stream().map(PollOption::getText).collect(Collectors.toList()))
                .isAnonymous(com.ledikom.model.Poll.IS_ANONYMOUS)
                .type(poll.getType())
                .allowMultipleAnswers(poll.getAllowMultipleAnswers())
                .correctOptionId(poll.getCorrectOptionId())
                .explanation(poll.getExplanation())
                .build();
    }

    public void addMusicMenuButtonsToSendMessage(final SendMessage sm) {
        addButtonsToMessage(sm, 2,
                Arrays.stream(MusicMenuButton.values()).map(value -> value.buttonText).collect(Collectors.toList()),
                        Arrays.stream(MusicMenuButton.values()).map(value -> value.callbackDataString).collect(Collectors.toList()));
    }

    public void addMusicDurationButtonsToSendMessage(final SendMessage sm, String musicString) {
        addButtonsToMessage(sm, 2,
                List.of("5 мин", "10 мин", "15 мин", "20 мин"),
                List.of(musicString + "_5", musicString + "_10", musicString + "+15", musicString + "_20"));
    }

    public void addCitiesButtons(final SendMessage sm, final Set<City> cities) {
        addButtonsToMessage(sm, 2,
                cities.stream().map(city -> city.label).collect(Collectors.toList()),
                cities.stream().map(Enum::name).collect(Collectors.toList()));
    }

    private void addButtonsToMessage(final SendMessage sm, final int buttonsInRow, final List<String> buttonTextList, final List<String> callbackDataList) {
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int index = 0; index < buttonTextList.size(); ) {
            var button = new InlineKeyboardButton();
            button.setText(buttonTextList.get(index));
            button.setCallbackData(callbackDataList.get(index));
            row.add(button);
            if (++index % buttonsInRow == 0 || index == buttonTextList.size()) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }

        markup.setKeyboard(keyboard);
        sm.setReplyMarkup(markup);
    }

    public void addCouponButton(final SendMessage sm, final Coupon coupon, final String buttonText, final String callbackData) {
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData + coupon.getId());
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        sm.setReplyMarkup(markup);
    }

    public InlineKeyboardMarkup createListOfCoupons(final Set<Coupon> coupons) {
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Coupon coupon : coupons) {
            var button = new InlineKeyboardButton();
            button.setText(BotResponses.couponButton(coupon));
            button.setCallbackData("couponPreview_" + coupon.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);

        return markup;
    }
}
