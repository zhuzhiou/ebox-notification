package ebox.notification.ems;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@lombok.Data
@ConfigurationProperties(prefix = "sms.vendor")
public class VendorProperties {

    private String enterpriseID;

    private String loginName;

    private String password;

    private Map<String, String> actions;
}
