![Build status](https://github.com/joerghoh/aem-httpclient/actions/workflows/github-actions-build.yml/badge.svg?branch=main)
![Apache 2 license](https://img.shields.io/badge/License-Apache_2.0-blue.svg)

# aem-httpclient

AEM ships out-of-the-box with a version of the Apache httpclient 4.x libraries, which are a fine piece of software. But I experienced a number of situations where features of the httpclients were not used, which led to problems for both customers, endusers and operation.

For example:
* timeouts are often not set, which can lead to problems in situations where synchronous HTTP calls are made.
* a configured proxy is not used by default. This is problematic in the case of AEM as a Cloud Service when using Advanced Networking and a dedicated egress IP. In such situations this dedicated egress IP is not used.
* insufficient resource handling leading to stability issues.
* insufficient handling of error situation leading to problems in the application; for example because an internal server error on the remote side did not return any response body, leading to exceptions on the client side.
* lack of observability when it comes to log the number of outgoing requests and timing information.


To address these reasons I decided to create a project, which provides a HTTP client with all those goodies included. My goal is to:

* provide reasonable defaults in the context of AEM
* utilize aggressive timeouts for any type of connection
* have these settings configurable via OSGI
* enforce explicitly the handling of error situations like networking issues, timeouts and unexpected HTTP status codes
* provide metrics and logging in case of issues to analyze problems quickly
* be easy to include into custom applications




