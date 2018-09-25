package ebox.notification.log;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 *     短信日志
 * </p>
 *
 * @author zhuzhiou
 */
@Service
public class LogMessageTempServiceImpl implements LogMessageTempService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void addMessage(LogMessageTemp logMessageTemp) {
        jdbcTemplate.update("insert into log_message_temp(id, created_date) values(?, ?)", ps -> {
            ps.setString(1, logMessageTemp.getId());
            ps.setTimestamp(2, Timestamp.valueOf(logMessageTemp.getCreatedDate()));
        });
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public List<LogMessageTemp> getMessagesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbcTemplate.query(StringUtils.join("select id, created_date from log_message_temp where id in (", Strings.repeat("?, ", ids.size()).substring(0, ids.size() * 3 - 2),")"), ps -> {
            for (int i = 0; i < ids.size(); i ++) {
                ps.setString(i + 1, ids.get(0));
            }
        }, (rs, rowNum) -> {
            LogMessageTemp logMessage = new LogMessageTemp();
            logMessage.setId(rs.getString("id"));
            logMessage.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
            return logMessage;
        });
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public void deleteMessagesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("delete from log_message_temp where id = ?", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, ids.get(i));
            }

            @Override
            public int getBatchSize() {
                return ids.size();
            }
        });
    }
}
