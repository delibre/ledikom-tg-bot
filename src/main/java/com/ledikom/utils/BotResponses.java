package com.ledikom.utils;

import com.ledikom.model.Coupon;
import com.ledikom.model.User;
import com.ledikom.model.UserCouponRecord;

import java.time.format.DateTimeFormatter;

public final class BotResponses {

    public static String startMessage() {
        return """
                Приветствуем, это бот сетки аптек Ледиком.
                                
                Здесь вы сможете:
                - поставить напоминание о приеме лекарств
                - найти ближайшую аптеку в своем городе
                                
                Также здесь регулярно будут появляться опросы и действует реферальная программа. Будьте активными и
                выигрывайте призы в виде скидок на наши товары.
                                
                Сейчас вы можете активировать купон со скидкой 5% на покупки.
                """;
    }

    public static String couponAcceptMessage(final Coupon coupon, final boolean inAllPharmacies, final int durationInMinutes) {
        StringBuilder sb = new StringBuilder(coupon.getText() + "\n\n");

        if (inAllPharmacies) {
            sb.append("Действует во всех аптках сети.");
        } else {
            sb.append("Действует в аптках:\n");
            coupon.getPharmacies().forEach(pharmacy -> {
                sb.append(pharmacy.getName()).append(" - ").append(pharmacy.getCity().label).append(", ").append(pharmacy.getAddress()).append("\n");
            });
        }
        sb.append("\n");

        if (coupon.getStartDate() != null && coupon.getEndDate() != null) {
            sb.append("С ").append(coupon.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append(" по ").append(coupon.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");
        }

        sb.append("Купон действует в течение ").append(durationInMinutes).append(" минут. Активируйте его при кассе.");

        return sb.toString();
    }

    public static String referralMessage(final String refLink, final int referralCount) {
        return "Ваша реферальная ссылка:\n\n\n" + refLink + "\n\n\nКоличество приглашенных вами пользователей: " + referralCount + "\n\n\nПоделитесь с другом и получайте бонусы: НАПИСАТЬ КАКИЕ БОНУСЫ И ДОАБВИТЬ АВТО ФУНКЦИОНАЛ";
    }

    public static String couponExpiredMessage() {
        return "Время вашего купона истекло.";
    }

    public static String triggerReceiveNewsMessage(final User user) {
        return "Подписка на рассылку новостей и акций " + (user.getReceiveNews() ? "включена." : "отключена.");
    }

    public static String listOfCouponsMessage() {
        return "Ваши купоны:";
    }

    public static String noActiveCouponsMessage() {
        return "У вас нету купонов";
    }

    public static String initialCouponText(final String couponTextWithBarcode, final long couponDurationInMinutes) {
        return "Времени осталось: " + UtilityHelper.convertIntToTimeInt(couponDurationInMinutes) + ":00" +
                "\n\n" +
                couponTextWithBarcode;
    }

    public static String updatedCouponText(final UserCouponRecord userCouponRecord, final long timeLeftInSeconds) {
        return "Времени осталось: " + UtilityHelper.convertIntToTimeInt(timeLeftInSeconds / 60) + ":" + UtilityHelper.convertIntToTimeInt(timeLeftInSeconds % 60) +
                "\n\n" +
                userCouponRecord.getText();
    }

    public static String couponButton(final Coupon coupon) {
        return coupon.getName();
    }

    public static String noteAdded() {
        return "Заметка записана, можете редактировать через меню";
    }

    public static String editNote() {
        return "*Чтобы редактировать заметку скопируйте свою заметку из сообщения выше, вставьте в поле ввода, измените и отправте";
    }

    public static String addNote() {
        return "Введите нотатку и вышлите сообщение";
    }

    public static String musicMenu() {
        return "Это меню музыки для сна. Выберите стиль:";
    }

    public static String musicDurationMenu() {
        return "Выберите продолжительность, музыка остановится автоматически";
    }

    public static String goodNight() {
        return "Good night!";
    }

    public static String chooseYourCity() {
        return "Выбери город";
    }

    public static String cityAdded(final String cityName) {
        return "Вы выбрали: " + City.valueOf(cityName).label;
    }

    public static String newCoupon(final Coupon coupon) {
        return coupon.getNews() + "\n\n" + coupon.getText();
    }

    public static String couponIsNotActive() {
        return "Купон неактивен!";
    }

    public static String yourCityCanUpdate(final City city) {
        return "Ваш город" + (city == null ?
                " неуказан.\n\nУстановите в меню ниже, чтобы получать актуальные новости и акции только для вашего города!"
                :
                ": " + city.label + ".\n\nМожете изменить в меню ниже.");
    }

    public static String cityAddedNewCoupons(final String cityName) {
        return "Вы выбрали: " + City.valueOf(cityName).label + "\n\nПроверьте и воспользуйтесь вашими новыми купонами!";
    }
}
