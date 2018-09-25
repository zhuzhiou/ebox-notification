package ebox.notification.ems;

import ebox.notification.log.LogMessage;
import ebox.notification.log.LogMessageService;
import ebox.notification.log.LogMessageTemp;
import ebox.notification.log.LogMessageTempService;
import ebox.notification.protocol.Notification;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@RabbitListener(queues = "notification")
@EnableConfigurationProperties(VendorProperties.class)
public class SendMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SendMessageHandler.class);

    @Autowired
    private LogMessageTempService logMessageTempService;

    @Autowired
    private LogMessageService logMessageService;

    @Autowired
    private VendorProperties vendorProperties;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private SAXReader saxReader;

    // TODO 日后可以用 Feign 重构接口
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @RabbitHandler
    public void handle(Notification notification) throws IOException {
        if (isBlank(notification.getId()) || StringUtils.length(notification.getReceiver()) < 11 || isBlank(notification.getMessage())) {
            return;
        }
        // 准备参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("enterpriseID", vendorProperties.getEnterpriseID()));
        params.add(new BasicNameValuePair("loginName", vendorProperties.getLoginName()));
        params.add(new BasicNameValuePair("password", vendorProperties.getPassword()));
        params.add(new BasicNameValuePair("mobiles", notification.getReceiver()));
        params.add(new BasicNameValuePair("content", notification.getMessage()));
        params.add(new BasicNameValuePair("smsId", notification.getId()));

        HttpPost httpPost = new HttpPost(vendorProperties.getActions().get("sendSMS"));
        httpPost.setEntity(new UrlEncodedFormEntity(params, Charset.forName("utf-8")));
        HttpResponse httpResponse = httpClient.execute(httpPost);

        StatusLine statusLine = httpResponse.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) {
            // 创建 LogMessage 对象
            LogMessage logMessage = new LogMessage();
            logMessage.setId(notification.getId());
            logMessage.setMobile(notification.getReceiver());
            logMessage.setContent(notification.getMessage());
            logMessage.setStatus(LogMessage.STATUS_SENT);
            logMessage.setVendor("ems");
            LocalDateTime rightNow = LocalDateTime.now();
            logMessage.setCreatedDate(rightNow);
            logMessage.setLastModifiedDate(rightNow);

            // 处理短信接口返回的 xml 内容
            try {
                String entity = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
                if (logger.isInfoEnabled()) {
                    logger.info("短信网关返回发送结果：{}", entity);
                }
                Document document = saxReader.read(new StringReader(entity));
                Node result = document.selectSingleNode("//Result");
                if (result != null) {
                    logMessage.setResult(result.getText());
                    resolveResult(logMessage);
                }
            }
            // 如果返回200但有异常，短信也是有可能收到的，后续的短信回执纠正正常的情况。所以出错也不处理，是避免重复发送的bug，如果真的出错，只能人工干预了。
            catch (IOException | DocumentException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("输入输出出现错误", e);
                }
            }
            logMessageService.addMessage(logMessage);

            // 因为使用了按年份分表，所以在小表存储路由规则。
            LogMessageTemp logMessageTemp = new LogMessageTemp();
            logMessageTemp.setId(logMessage.getId());
            logMessageTemp.setCreatedDate(logMessage.getCreatedDate());
            logMessageTempService.addMessage(logMessageTemp);
        }
        else {
            if (logger.isErrorEnabled()) {
                logger.error("状态码：{}，错误原因：{}", statusCode, statusLine.getReasonPhrase());
            }
            throw new RuntimeException("返回非200状态码");
        }
    }

    private void resolveResult(LogMessage logMessage) {
        if (StringUtils.isBlank(logMessage.getResult())) {
            return;
        }
        switch (logMessage.getResult()) {
            case "0":
                logMessage.setStatus(LogMessage.STATUS_SENT_SUCCESS);
                logMessage.setComment("成功");
                break;
            case "1":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("登录密码错误");
                break;
            case "2":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("企业ID或登录名错误");
                break;
            case "3":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("余额不足");
                break;
            case "4":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("用户归属服务器错误");
                break;
            case "5":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("帐户停用或不存在");
                break;
            case "6":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("内容为空");
                break;
            case "7":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("号码为空");
                break;
            case "8":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("号码超过最大限制数");
                break;
            case "9":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("内容包含关键字");
                break;
            case "10":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("时间格式错误");
                break;
            case "11":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("非法操作导致ip被锁");
                break;
            case "12":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("访问过快");
                break;
            case "13":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("批量一对一参数格式错误");
                break;
            case "14":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("批量一对一出现重复信息(号码，内容同时重复)");
                break;
            case "15":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("签名未报备");
                break;
            case "16":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("单位时间内该号码超过最大发送限制");
                break;
            case "17":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("签名必须在【4，10】字符间");
                break;
            case "18":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("内容涉嫌营销内容");
                break;
            case "27":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("30秒内出现重复发送");
                break;
            case "28":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("内容超过4000字符");
                break;
            case "99":
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
                logMessage.setComment("系统内部错误");
                break;
            default:
                logMessage.setStatus(LogMessage.STATUS_SENT_FAIL);
        }
    }
}
