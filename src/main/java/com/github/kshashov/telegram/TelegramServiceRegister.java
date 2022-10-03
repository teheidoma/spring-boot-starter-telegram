package com.github.kshashov.telegram;

import com.github.kshashov.telegram.api.TelegramMvcController;
import com.github.kshashov.telegram.config.TelegramBotGlobalProperties;
import com.github.kshashov.telegram.config.TelegramBotProperties;
import com.github.kshashov.telegram.handler.TelegramPollingService;
import com.github.kshashov.telegram.handler.TelegramService;
import com.github.kshashov.telegram.handler.TelegramUpdatesHandler;
import com.github.kshashov.telegram.handler.TelegramWebhookService;
import com.pengrad.telegrambot.TelegramBot;
import io.javalin.Javalin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.env.Environment;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class TelegramServiceRegister {
    private final ConfigurableBeanFactory configurableBeanFactory;
    private final TelegramBotGlobalProperties telegramBotGlobalProperties;
    private final Environment environment;
    private final TelegramUpdatesHandler updatesHandler;

    private Optional<Javalin> server = Optional.empty();


    public void register(TelegramMvcController controller) {
        TelegramBotProperties properties = createDefaultBotProperties(controller.getToken(), telegramBotGlobalProperties);
        TelegramBot bot = properties.getBotBuilder().build();

        if (telegramBotGlobalProperties.getBotProcessors().containsKey(bot.getToken())) {
            telegramBotGlobalProperties.getBotProcessors().get(bot.getToken()).accept(bot);
        }
        TelegramService telegramService;
        if (properties.getWebhook() != null) {
            createServer();
            if (server.isPresent()) {
                telegramService = new TelegramWebhookService(properties, bot, updatesHandler, server.get());
            } else {
                throw new RuntimeException("failed to init webhook server");
            }
        } else {
            telegramService = new TelegramPollingService(properties, bot, updatesHandler);
        }

        configurableBeanFactory.registerSingleton(telegramService.getClass().getName(), telegramService);
    }

    private TelegramBotProperties createDefaultBotProperties(@NotNull String token, @NotNull TelegramBotGlobalProperties globalProperties) {
        return TelegramBotProperties.builder(token)
                .configure(builder -> builder
                        .apiUrl("https://api.telegram.org/bot")
                        .updateListenerSleep(environment.getProperty("telegram.bot.update-listener-sleep", Long.class, 300L))
                        .okHttpClient(new OkHttpClient.Builder()
                                .dispatcher(new Dispatcher(globalProperties.getTaskExecutor()))
                                .build()))
                .build();
    }

    private void createServer() {
        if (server.isPresent()) return;
        try {
            server = Optional.of(Javalin.create().start(telegramBotGlobalProperties.getWebserverPort()));
            log.info("Javalin server has been started on {} port", telegramBotGlobalProperties.getWebserverPort());
        } catch (Exception ex) {
            log.error("An unexpected error occured while starting Javalin server", ex);
        }
    }
}
