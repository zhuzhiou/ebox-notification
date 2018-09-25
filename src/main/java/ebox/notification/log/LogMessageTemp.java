package ebox.notification.log;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zhuzhiou
 */
@lombok.Data
public class LogMessageTemp implements Serializable {

    private String id;

    private LocalDateTime createdDate;
}
