package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger

/** embedded server */
object Main {
  val logger = Logger(Main.getClass)
  val userRepository = new InMemoryUserRepository

  def main(args: Array[String]) {
    logger.info("starting unfiltered app at localhost on port %s" format 8080)
    unfiltered.jetty.Http(8080)
      .filter(new UserResource(userRepository))
      .filter(new BookmarksResource(userRepository))
      .filter(new BookmarkResource(userRepository))
      .run
  }
}
