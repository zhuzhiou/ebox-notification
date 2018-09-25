package ebox.notification.log;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zhuzhiou
 */
@lombok.Data
public class LogMessage implements Serializable {

    public static final String STATUS_SENT = "sent";

    public static final String STATUS_SENT_SUCCESS = "sent-success";

    public static final String STATUS_SENT_FAIL = "sent-fail";

    public static final String STATUS_REPORT_SUCCESS = "report-success";

    public static final String STATUS_REPORT_FAIL = "report-fail";

    private String id;

    private String mobile;

    private String content;

    // 已发送、发送成功、发送失败、回执成功、回执失败
    private String status;

    private String result;

    private String comment;

    private String vendor;

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;
}
