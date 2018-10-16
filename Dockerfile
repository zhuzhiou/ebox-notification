FROM java:8-jre
MAINTAINER 朱志欧<zhuzhiou@qq.com>
ARG JAR_FILE
ADD target/${JAR_FILE} /var/lib/ebox-notification.jar
RUN bash -c 'touch /var/lib/ebox-notification.jar'
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/var/lib/ebox-notification.jar"]