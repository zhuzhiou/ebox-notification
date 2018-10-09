package ebox.notification.payload;

import java.io.Serializable;
import java.util.Date;

@lombok.Data
public class DeliverMessage implements Serializable {

    private String id;

    private String code;

    private String message;

    private Date reportTime;

    private boolean report;
}
