package ebox.notification.ems;

import ebox.notification.log.LogMessage;
import ebox.notification.log.LogMessageService;
import ebox.notification.log.LogMessageTemp;
import ebox.notification.log.LogMessageTempService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>轮询短信回执接口，更新短信状态。</p>
 *
 * @author zhuzhiou
 */
@Component
@EnableConfigurationProperties(VendorProperties.class)
public class TakeReportsTask {

    private static final Logger logger = LoggerFactory.getLogger(TakeReportsTask.class);

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

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Scheduled(fixedDelay = 30000L)
    public void takeMessageReceipts() throws IOException, DocumentException {
        if (logger.isInfoEnabled())
        {
            logger.info(">>> 开始获取短信回执...");
        }
        int num = 0;
        do {
            // 准备参数
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("enterpriseID", vendorProperties.getEnterpriseID()));
            params.add(new BasicNameValuePair("loginName", vendorProperties.getLoginName()));
            params.add(new BasicNameValuePair("password", vendorProperties.getPassword()));

            // 请求服务器
            HttpPost httpPost = new HttpPost(vendorProperties.getActions().get("getSmsReport"));
            httpPost.setEntity(new UrlEncodedFormEntity(params, Charset.forName("utf-8")));
            HttpResponse httpResponse = httpClient.execute(httpPost);

            // 解释响应
            StatusLine statusLine = httpResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                String entity = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
                Document document = saxReader.read(new StringReader(entity));
                Node result = document.selectSingleNode("//Result");
                if (result == null) {
                    if (logger.isErrorEnabled()) {
                        logger.error("获取短信回执返回xml里不包含//Result节点");
                    }
                }
                else if ("0".equals(result.getText())) {
                    Node report = document.selectSingleNode("//Report");
                    if (report == null) {
                        if (logger.isErrorEnabled()) {
                            logger.error("获取短信回执返回xml里不包含//Report节点");
                        }
                    }
                    else if (StringUtils.isNumeric(report.valueOf("@num"))) {
                        num = Integer.parseInt(report.valueOf("@num"));
                        if (num > 0) {
                            List<Node> items = document.selectNodes("//Item");
                            if (items != null && !items.isEmpty()) {
                                List<String> ids = items.stream().map(item -> item.valueOf("@smsId")).collect(Collectors.toList());
                                // 因为归档表是根据日期拆分的，需要从当前库找到创建日期来做路由
                                List<LogMessageTemp> logMessagesTemp = logMessageTempService.getMessagesByIds(ids);
                                if (logMessagesTemp != null && !logMessagesTemp.isEmpty()) {
                                    List<LogMessage> logMessages = logMessagesTemp.stream().map(logMessageTemp -> {
                                        LogMessage logMessage = new LogMessage();
                                        logMessage.setId(logMessageTemp.getId());
                                        logMessage.setCreatedDate(logMessageTemp.getCreatedDate());
                                        return logMessage;
                                    }).collect(Collectors.toList());
                                    Map<String, LogMessage> map = logMessages.stream().collect(Collectors.toMap(LogMessage::getId, value -> value));
                                    items.forEach(item -> {
                                        String id = item.valueOf("@smsId");
                                        if (map.containsKey(id)) {
                                            LogMessage logMessage = map.get(id);
                                            logMessage.setResult(item.valueOf("@status"));
                                            logMessage.setComment(item.valueOf("@desc"));
                                            logMessage.setLastModifiedDate(LocalDateTime.parse(item.valueOf("@reportTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                                            if ("0".equals(logMessage.getResult())) {
                                                logMessage.setStatus(LogMessage.STATUS_REPORT_SUCCESS);
                                            } else {
                                                logMessage.setStatus(LogMessage.STATUS_REPORT_FAIL);
                                            }
                                        }
                                    });
                                    map.clear();
                                    logMessageService.updateMessages(logMessages);
                                    logMessageTempService.deleteMessagesByIds(logMessagesTemp.stream().map(LogMessageTemp::getId).collect(Collectors.toList()));
                                }
                            }
                        }
                    } // @num
                }
                else {
                    if (logger.isErrorEnabled()) {
                        logger.error("获取短信回执返回错误码：{}", result.getText());
                    }
                }
            }
        } while (num > 0);
        if (logger.isInfoEnabled())
        {
            logger.info("... 获取短信回执完毕 <<<");
        }
    }
}
