package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty.Server

import org.clapper.avsl.Logger

/** embedded server */
object Main {
  val logger = Logger(Main.getClass)
  val userRepository = new InMemoryUserRepository
  val resources = Seq(rootResource, 
                      new UserResource(userRepository),
                      new BookmarksResource(userRepository),
                      new BookmarkResource(userRepository)
                  )
  def applyResources = resources.foldLeft(_: Server){_ filter _}
  
  def main(args: Array[String]) {
    logger.info("starting unfiltered app at localhost on port %s" format 8080)
    applyResources(unfiltered.jetty.Http(8080)).run() 
  }
}
