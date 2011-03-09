package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty.Server

import org.clapper.avsl.Logger

/** embedded server */
object Main {
  val logger = Logger(Main.getClass)
  val userRepository = new InMemoryUserRepository
  val routers = Seq(rootRouter, 
                    new UserRouter(userRepository),
                    new BookmarksRouter(userRepository),
                    new BookmarkRouter(userRepository))
  def applyRouters = routers.foldLeft(_: Server){_ filter _}
  
  def main(args: Array[String]) {
    logger.info("starting unfiltered app at localhost on port %s" format 8080)
    applyRouters(unfiltered.jetty.Http(8080)).run() 
  }
}
