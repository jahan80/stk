package com.starterkit.notif.notif.application.listener;

import com.starterkit.notif.notif.api.dto.EmailRequest;
import com.starterkit.notif.notif.api.dto.NotifEventMessage;
import com.starterkit.notif.notif.api.dto.PushRequest;
import com.starterkit.notif.notif.api.dto.SmsRequest;
import com.starterkit.notif.notif.application.NotificationService;
import com.starterkit.notif.notif.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMqConfig.NOTIF_QUEUE)
    public void onNotifEvent(NotifEventMessage message) {

        log.info("Received notif event: type={}, eventId={}, source={}, traceId={}",
                message.getEventType(),
                message.getEventId(),
                message.getSource(),
                message.getTraceId());

        try {
            switch (message.getEventType()) {
                case "SEND_SMS" -> handleSms(message);
                case "SEND_EMAIL" -> handleEmail(message);
                case "SEND_PUSH" -> handlePush(message);
                default -> log.warn("Unknown notif event type: {}", message.getEventType());
            }
        } catch (Exception ex) {
            log.error("Failed to process notif event: eventId={}, type={}",
                    message.getEventId(),
                    message.getEventType(),
                    ex);
            throw ex;
        }
    }

    private void handleSms(NotifEventMessage message) {
        Map<String, Object> data = message.getData();

        SmsRequest request = new SmsRequest();
        request.setTo((String) data.get("to"));
        request.setMessage((String) data.get("message"));

        notificationService.sendSms(request);
    }

    private void handleEmail(NotifEventMessage message) {
        Map<String, Object> data = message.getData();

        EmailRequest request = new EmailRequest();
        request.setTo((String) data.get("to"));
        request.setSubject((String) data.get("subject"));
        request.setBody((String) data.get("body"));

        notificationService.sendEmail(request);
    }

    private void handlePush(NotifEventMessage message) {
        Map<String, Object> data = message.getData();

        PushRequest request = new PushRequest();
        request.setDeviceToken((String) data.get("deviceToken"));
        request.setTitle((String) data.get("title"));
        request.setBody((String) data.get("body"));

        notificationService.sendPush(request);
    }
}
