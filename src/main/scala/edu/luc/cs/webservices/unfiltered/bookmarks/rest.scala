package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.filter._
import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger

class App(val userRepository: UserRepository) extends Plan {
  val logger = Logger(classOf[App])

  val authSvc = new UserRepositoryAuthService(userRepository)
  def verify(u: String, p: String, user: User) = authSvc.verify(u, p) && user.name == u
  def Fail(name: String) = Unauthorized ~> WWWAuthenticate("""Basic realm="/""" + name + "\"")
    
  def intent = {
    case PUT(Path(Seg(name :: Nil))) => {
      logger.debug("PUT %s" format name)
  	  userRepository.findByName(name) match {
	    case Some(user) => Conflict 
	    case _ => {
	      val user = User(name, name, name, name)
          userRepository.store(user)
          Created ~> ResponseString(user.toString) 
	    }
      }
    }
    case r @ GET(Path(Seg(name :: Nil))) => {
      logger.debug("GET %s" format name)
      userRepository.findByName(name) match {
    	case Some(user) => r match {
   	      case BasicAuth(u, p) if verify(u, p, user) =>
   	    	Ok ~> ResponseString(user.toString)
  	      case _ => Fail(name)
    	}
    	case _ => NotFound  
      }
    }
    case r @ DELETE(Path(Seg(name :: Nil))) => {
      logger.debug("DELETE %s" format name)
      userRepository.findByName(name) match {
    	case Some(user) => r match {
   	      case BasicAuth(u, p) if verify(u, p, user) => {
   	    	assert(userRepository.remove(name).isDefined)
	   	    NoContent
   	      } 
  	      case _ => Fail(name)
    	}
    	case _ => NotFound  
      }
    }
  }
}
