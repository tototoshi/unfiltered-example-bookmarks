package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger

/** embedded server */
object Server {
  val logger = Logger(Server.getClass)
  val userRepository = new InMemoryUserRepository

  def main(args: Array[String]) {
    logger.info("starting unfiltered app at localhost on port %s" format 8080)
    unfiltered.jetty.Http(8080)
      .filter(new App(userRepository))
      .run
  }
}
