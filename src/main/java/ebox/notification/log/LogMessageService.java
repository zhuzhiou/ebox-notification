package ebox.notification.log;

import java.util.List;

/**
 * @author zhuzhiou
 */
public interface LogMessageService {

    void addMessage(LogMessage logMessage);

    // 更新短信的状态
    void updateMessages(List<LogMessage> logMessagesHis);
}
