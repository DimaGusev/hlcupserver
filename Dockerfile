FROM centos:7
RUN yum update -y && \
yum install -y wget && \
yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel && \
yum install -y sysstat && \
yum clean all
# Set environment variables.
ENV HOME /root
WORKDIR /root
ADD target/hlcupserver-1.0.jar app.jar
WORKDIR /
#ADD data.zip /tmp/data/data.zip
ENV JAVA_OPTS="-server -Xmx3000m -Xms3000m -XX:MaxNewSize=2000m -XX:NewSize=2000m -XX:CompileThreshold=1 -XX:+PrintGC -XX:+PrintGCDateStamps -Duser.timezone=UTC -Dfile.encoding=UTF-8"
EXPOSE 80
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /root/app.jar" ]