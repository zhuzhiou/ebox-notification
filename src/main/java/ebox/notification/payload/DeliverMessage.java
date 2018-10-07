package ebox.notification.payload;

import java.io.Serializable;

@lombok.Data
public class DeliverMessage implements Serializable {

    private String id;

    private String code;

    private String message;

    private boolean report;
}
