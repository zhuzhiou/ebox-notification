package ebox.notification.payload;

import java.io.Serializable;

@lombok.Data
public class SubmitMessage implements Serializable {

    private String id;

    private String receiver;

    private String message;
}
