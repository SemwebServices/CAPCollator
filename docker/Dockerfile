FROM adoptopenjdk:11-jdk-openj9-bionic
MAINTAINER Ian.Ibbotson@semweb.co
VOLUME /tmp
# ADD service-0.1.war mockncipserver.war
# RUN apt-get install bash
COPY ./CAPAggregator.war /CAPAggregator.war
# ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/CAPAggregator.war"]
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xshareclasses", "-Xscmx50M", "-Xtune:virtualized", "-jar","/CAPAggregator.war"]
EXPOSE 8080/tcp
