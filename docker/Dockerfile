FROM adoptopenjdk:11-jdk-openj9-bionic
MAINTAINER Ian.Ibbotson@semweb.co
VOLUME /tmp
# ADD service-0.1.war mockncipserver.war
# RUN apt-get install bash
COPY ./CAPAggregator.war /CAPAggregator.war
# See https://dev.to/hugodias/wait-for-mongodb-to-start-on-docker-3h8b
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.6.0/wait /wait
RUN chmod +x /wait
# ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/CAPAggregator.war"]
# ENTRYPOINT ["/wait", "java","-Djava.security.egd=file:/dev/./urandom","-Xshareclasses", "-Xscmx50M", "-Xtune:virtualized", "-jar","/CAPAggregator.war"]
CMD /wait && java -Djava.security.egd=file:/dev/./urandom -Xshareclasses -Xscmx50M -Xtune:virtualized -jar /CAPAggregator.war
EXPOSE 8080/tcp
