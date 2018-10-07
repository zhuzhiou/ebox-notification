package ebox.notification.ems;

import ebox.notification.payload.SubmitMessage;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class Sender {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @GetMapping("/sender")
    public void send() {
        SubmitMessage submitMessage = new SubmitMessage();
        submitMessage.setId(StringUtils.remove(UUID.randomUUID().toString(), "-"));
        submitMessage.setReceiver("13828406168");
        submitMessage.setMessage("取件码" + RandomStringUtils.randomNumeric(6) + "，请移步到冠庭园快递柜及时取走您的包裹。");
        amqpTemplate.convertAndSend("submitMessages", submitMessage);
    }
}
