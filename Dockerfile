FROM adoptopenjdk/openjdk11:debianslim-jre

ADD target/universal/stage /opt/app
WORKDIR /opt/app
RUN ["chown", "-R", "daemon:daemon", "."]
RUN ["mkdir", "-p", "/data"]
RUN ["chown", "-R", "daemon:daemon", "/data"]

RUN apt-get update
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:mc3man/trusty-media
RUN apt-get update || true
RUN apt-get install -y ffmpeg
RUN apt-get install -y frei0r-plugins

VOLUME ["/data"]
ENTRYPOINT ["bin/onvif-camera-snapshot-taker"]
CMD []

USER daemon
