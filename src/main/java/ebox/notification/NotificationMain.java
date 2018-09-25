package ebox.notification;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.dom4j.io.SAXReader;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>
 *     该项目处理任务调度
 * </p>
 *
 * @author zhuzhiou
 */
@SpringBootApplication
@EnableScheduling
public class NotificationMain {

    public static void main(String[] args) {
        SpringApplication.run(NotificationMain.class, args);
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClients.createMinimal();
    }

    @Bean
    public SAXReader saxReader() {
        return new SAXReader();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
