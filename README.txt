Abstract

A fairly complete example of a web service with two levels of CRUD and 
authentication. Intended to be fully compatible (observationally equivalent)
with the Java/Restlet version of the bookmarks example from RESTful Web 
Services by Richardson and Ruby.

Languages

Scala 2.8.1

Dependencies

Simple Build Tool
http://code.google.com/p/simple-build-tool/

Environment

Eclipse Helios (optional)
Ubuntu 10.10 Maverick (for example)

How to run

sbt update
sbt run # now interact with the service using cURL or a browser

or 

sbt test
