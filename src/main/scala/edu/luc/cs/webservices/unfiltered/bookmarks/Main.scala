package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty.Server

import org.clapper.avsl.Logger

/** embedded server */
object Main {
  val logger = Logger(Main getClass)
  val userRepository = new InMemoryUserRepository
  val plans = Seq(rootPlan, 
                  new UserPlan(userRepository),
                  new BookmarksPlan(userRepository),
                  new BookmarkPlan(userRepository))
  def applyPlans = plans.foldLeft(_: Server)(_ filter _)
  
  def main(args: Array[String]) {
    logger.info("starting unfiltered app at localhost on port %s" format 8080)
    applyPlans(unfiltered.jetty.Http(8080)).run() 
  }
}
