package com.ledikom.service;

import com.ledikom.model.Coupon;
import com.ledikom.model.PollOption;
import com.ledikom.model.User;
import com.ledikom.repository.UserRepository;
import com.ledikom.utils.BotResponses;
import com.ledikom.utils.UserResponseState;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UserService {
    private static final int INIT_REFERRAL_COUNT = 0;
    private static final boolean INIT_RECEIVE_NEWS = true;
    private static final UserResponseState INIT_RESPONSE_STATE = UserResponseState.NONE;

    private final UserRepository userRepository;
    private final CouponService couponService;
    private final PollService pollService;
    private final BotUtilityService botUtilityService;

    public UserService(final UserRepository userRepository, @Lazy final CouponService couponService, final PollService pollService, final BotUtilityService botUtilityService) {
        this.userRepository = userRepository;
        this.couponService = couponService;
        this.pollService = pollService;
        this.botUtilityService = botUtilityService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void saveAll(final List<User> users) {
        userRepository.saveAll(users);
    }

    public void addNewUser(final Long chatId) {
        User user = new User(chatId, INIT_REFERRAL_COUNT, INIT_RECEIVE_NEWS, INIT_RESPONSE_STATE);
        userRepository.save(user);
        couponService.addCouponsToUser(user);
    }

    public void saveUser(final User user) {
        userRepository.save(user);
    }

    public User findByChatId(final Long chatId) {
        return userRepository.findByChatId(chatId).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void processPoll(final Poll telegramPoll) {
        // check if not a re-vote
        if (telegramPoll.getTotalVoterCount() == 1) {
            com.ledikom.model.Poll pollToUpdate = pollService.findByQuestion(telegramPoll.getQuestion());

            List<PollOption> pollOptionList = IntStream.range(0, telegramPoll.getOptions().size())
                    .mapToObj(index -> new PollOption(
                            pollToUpdate.getOptions().get(index).getText(),
                            pollToUpdate.getOptions().get(index).getVoterCount() + telegramPoll.getOptions().get(index).getVoterCount()))
                    .toList();
            pollToUpdate.setOptions(pollOptionList);
            pollToUpdate.setTotalVoterCount(pollToUpdate.getTotalVoterCount() + 1);
            pollToUpdate.setLastVoteTimestamp(LocalDateTime.now());

            pollService.savePoll(pollToUpdate);
        }
    }

    public String processStatefulUserResponse(final String text, final Long chatId) {
        User user = findByChatId(chatId);
        if (user.getResponseState() == UserResponseState.SENDING_NOTE) {
            user.setNote(text);
            user.setResponseState(UserResponseState.NONE);
            saveUser(user);
            return BotResponses.noteAdded();
        } else if (user.getResponseState() == UserResponseState.SENDING_DATE) {
            user.setSpecialDate(text);
            user.setResponseState(UserResponseState.NONE);
            saveUser(user);
            return BotResponses.dateAdded();
        }

        return "Нет такой команды!";
    }

    public void removeCouponFromUser(final User user, final Coupon coupon) {
        user.getCoupons().remove(coupon);
        userRepository.save(user);
    }

    public void addNewRefUser(final long chatIdFromRefLink, final long chatId) {
        final boolean selfLinkOrUserExists = chatIdFromRefLink == chatId || userExistsByChatId(chatId);
        if (!selfLinkOrUserExists) {
            User user = userRepository.findByChatId(chatIdFromRefLink).orElseThrow(() -> new RuntimeException("User not found"));
            user.setReferralCount(user.getReferralCount() + 1);
            userRepository.save(user);
        }
    }

    public boolean userExistsByChatId(final long chatId) {
        return userRepository.findByChatId(chatId).isPresent();
    }

    public List<User> getAllUsersToReceiveNews() {
        return userRepository.findAll().stream().filter(User::getReceiveNews).collect(Collectors.toList());
    }

    public List<SendMessage> processNoteRequestAndBuildSendMessageList(final long chatId) {
        User user = findByChatId(chatId);
        user.setResponseState(UserResponseState.SENDING_NOTE);
        saveUser(user);

        if (user.getNote() != null && !user.getNote().isBlank()) {
            SendMessage smNote = botUtilityService.buildSendMessage(user.getNote(), chatId);
            SendMessage smInfo = botUtilityService.buildSendMessage(BotResponses.editNote(), chatId);
            return List.of(smInfo, smNote);
        }

        SendMessage sm = botUtilityService.buildSendMessage(BotResponses.addNote(), chatId);
        return List.of(sm);
    }

    public SendMessage createUserSpecialDate(final long chatId) {
        User user = findByChatId(chatId);
        user.setResponseState(UserResponseState.SENDING_DATE);
        saveUser(user);

        if (user.getSpecialDate() != null) {
            return botUtilityService.buildSendMessage(BotResponses.specialDateAlreadyCreated(), chatId);
        }

        return botUtilityService.buildSendMessage(BotResponses.addSpecialDate(), chatId);
    }

    public void sendPrizeIfSpecialDate() {
        List<User> users = getAllUsers();

        users.forEach(user -> {
            LocalDate date = LocalDate.now(ZoneId.of("Europe/Moscow"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
            String now = date.format(formatter);

            if (Objects.equals(user.getSpecialDate(), now)) {
                System.out.println("user " + user.getId() + " has special date today");
            }
        } );
    }

    public boolean userIsInActiveState(final Long chatId) {
        return findByChatId(chatId).getResponseState() != UserResponseState.NONE;
    }
}
