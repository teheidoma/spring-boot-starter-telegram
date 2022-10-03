package com.github.kshashov.telegram.handler;

import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;

/**
 * Is used to listen for telegram events and process them.
 */
public interface TelegramService {

    /**
     * Subscribe on Telegram events.
     */
    void start();

    /**
     * Unsubscribe from Telegram events.
     */
    void stop();

    <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request);
}
