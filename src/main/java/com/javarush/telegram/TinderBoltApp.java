package com.javarush.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = ""; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = ""; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = ""; //TODO: добавь токен ChatGPT в кавычках

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    private final ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentDialogMode;
    private final ArrayList<String> list = new ArrayList<>();

    @Override
    public void onUpdateEventReceived(Update update) {
        String msg = getMessageText();

        if (msg.equals("/start")) {
            currentDialogMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            sendTextMessage(loadMessage("main"));

            showMainMenu(
                    "главное меню бота", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", " /opener",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами \uD83D\uDD25", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt"
            );
            return;
        }

        if (msg.equals("/gpt")) {
            currentDialogMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            sendTextMessage(loadMessage("gpt"));
            return;
        }

        if (currentDialogMode == DialogMode.GPT) {
            String prompt = loadPrompt("gpt");
            String answer = chatGPT.sendMessage(prompt, msg);
            Message serviceMessage = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
            updateTextMessage(serviceMessage, answer);
            return;
        }

        if (msg.equals("/date")) {
            currentDialogMode = DialogMode.DATE;
            sendPhotoMessage("date");

            String prompt = loadMessage("date");
            sendTextButtonsMessage(
                    prompt,
                    "Ариана Гранде", "date_grande",
                    "Марго Роби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райн Гослинг", "date_gosling",
                    "Том Харди", "date_hardy"
            );
            return;
        }

        if (currentDialogMode == DialogMode.DATE) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор");
                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }
            Message serviceMessage = sendTextMessage("Подождите, набирают текст...");
            String answer = chatGPT.addMessage(msg);
            updateTextMessage(serviceMessage, answer);
            return;
        }

        if (msg.equals("/message")) {
            currentDialogMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат вашу переписку",
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }
        if (currentDialogMode == DialogMode.MESSAGE) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                Message serviceMessage = sendTextMessage("Подождите пару секунд - ChatGPT думает...");

                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(serviceMessage, answer);
                return;
            }

            list.add(msg);
            return;
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
