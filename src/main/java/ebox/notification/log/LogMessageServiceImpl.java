package ebox.notification.log;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author zhuzhiou
 */
@Service
public class LogMessageServiceImpl implements LogMessageService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void addMessage(LogMessage logMessage) {
        if (logMessage == null) {
            return;
        }
        jdbcTemplate.update(
                StringUtils.join("insert into log_message_",
                        logMessage.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy")),
                        "(id, mobile, content, status, result, comment, created_date, last_modified_date) ",
                        "values(?, ?, ?, ?, ?, ?, ?, ?)"),
                ps -> {
                    ps.setString(1, logMessage.getId());
                    ps.setString(2, logMessage.getMobile());
                    ps.setString(3, logMessage.getContent());
                    ps.setString(4, logMessage.getStatus());
                    ps.setString(5, logMessage.getResult());
                    ps.setString(6, logMessage.getComment());
                    ps.setTimestamp(7, Timestamp.valueOf(logMessage.getCreatedDate()));
                    ps.setTimestamp(8, Timestamp.valueOf(logMessage.getLastModifiedDate()));
                });
    }

    /**
     * 必须提供 status、result、comment、created_date、last_modified_date，不变更 mobile、content
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void updateMessages(List<LogMessage> logMessages) {
        if (logMessages == null || logMessages.isEmpty()) {
            return;
        }
        String[] sqls = new String[logMessages.size()];
        StringBuilder sql = new StringBuilder();
        LogMessage logMessage;
        for (int i = 0; i < logMessages.size(); i++) {
            logMessage = logMessages.get(i);
            sql.setLength(0);
            sql.append("update log_message_").append(logMessage.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy")));
            sql.append(" set status = '").append(logMessage.getStatus()).append("'");
            sql.append(", result = '").append(logMessage.getResult()).append("'");
            // 防止 SQL 注入
            sql.append(", comment = '").append(StringUtils.replace(logMessage.getComment(), "\'", "''")).append("'");
            sql.append(", last_modified_date = '").append(logMessage.getLastModifiedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("'");
            sql.append(" where id = '").append(logMessage.getId()).append("'");
            sqls[i] = sql.toString();
        }
        sql.setLength(0);
        jdbcTemplate.batchUpdate(sqls);
    }
}
