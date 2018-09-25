package ebox.notification.log;

import java.util.List;

/**
 * @author zhuzhiou
 */
public interface LogMessageTempService {

    void addMessage(LogMessageTemp logMessageTemp);

    List<LogMessageTemp> getMessagesByIds(List<String> ids);

    void deleteMessagesByIds(List<String> ids);
}
