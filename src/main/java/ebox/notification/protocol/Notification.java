package ebox.notification.protocol;

import java.io.Serializable;

@lombok.Data
public class Notification implements Serializable {

    private String id;

    private String receiver;

    private String message;
}
