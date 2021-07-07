# Docker image for springboot application
# VERSION 0.1.0
# Author: bird

FROM openjdk:11
MAINTAINER wangpeng/20210707
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone


WORKDIR /usr/local/openjdk-11/lib
COPY out/libclib_jiio.so .
COPY out/libopencv_java.so .
WORKDIR /opt/dicom-adapter/config
COPY config/ .
WORKDIR /opt/dicom-adapter
COPY out/artifacts/dicom_gateway_adapter_jar/dicom-gateway-adapter.jar .

ENTRYPOINT ["java","-jar","dicom-gateway-adapter.jar"]
