package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.filter._
import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger

import collection.immutable.Map
import java.util.Date

// TODO root resource

class UserResource(val userRepository: UserRepository) extends Plan {
  val logger = Logger(classOf[UserResource])

  val authSvc = new UserRepositoryAuthService(userRepository)
  def verify(u: String, p: String, user: User) = authSvc.verify(u, p) && user.name == u
  def Fail(name: String) = Unauthorized ~> WWWAuthenticate("""Basic realm="/""" + name + "\"")

  def storeUserFromForm(name: String, form: Map[String, Seq[String]]) = {
    val user = User(name, 
    		        form("user[password]")(0), 
    		        form("user[email]")(0), 
    		        form("user[full_name]")(0))
    userRepository.store(user)
    user
  }

  def intent = {
    case GET(Path(Seg("users" :: name :: Nil))) => {
      logger.debug("GET /users/%s" format name)
      userRepository.findByName(name) match {
        // TODO representation based on content negotiation
        case Some(user) => Ok ~> ResponseString(user.toString)
        case _ => NotFound
      }
    }
    case req @ PUT(Path(Seg("users" :: name :: Nil))) => {
      logger.debug("PUT /users/%s" format name)
      userRepository.findByName(name) match {
        case Some(user) => req match {
          case BasicAuth(u, p) if verify(u, p, user) => req match {
            case Params(form) => {
              try {
                storeUserFromForm(name, form)
                NoContent
              } catch { case _ => BadRequest }
            }
            case _ => BadRequest
          }
          case _ => Fail(name)
        }
        case _ => req match {
          case Params(form) => {
            try {
              val user = storeUserFromForm(name, form)
              Created ~> ResponseString(user.toString)
            } catch { case _ => BadRequest }
          }
        }
      }
    }
    case req @ DELETE(Path(Seg("users" :: name :: Nil))) => {
      logger.debug("DELETE /users/%s" format name)
      userRepository.findByName(name) match {
        case Some(user) => req match {
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

class BookmarksResource(val userRepository: UserRepository) extends Plan {
  val logger = Logger(classOf[BookmarksResource])

  val authSvc = new UserRepositoryAuthService(userRepository)
  def verify(u: String, p: String, user: User) = authSvc.verify(u, p) && user.name == u
  def Fail(name: String) = Unauthorized ~> WWWAuthenticate("""Basic realm="/""" + name + "\"")

  def intent = {
    case req @ GET(Path(Seg("users" :: name :: "bookmarks" :: Nil))) => {
      logger.debug("GET /users/%s/bookmarks" format name)
      userRepository.findByName(name) match {
    	case Some(user) => {
          val authorized = req match {
            case BasicAuth(u, p) if verify(u, p, user) => true
            case _ => false
          }
          // TODO representation based on content negotiation
          Ok ~> ResponseString(user.bookmarks.filter((e) => (authorized || ! e._2.restricted)).toString)
    	}
        case _ => NotFound
      }
    }
  }
}

class BookmarkResource(val userRepository: UserRepository) extends Plan {
  val logger = Logger(classOf[BookmarkResource])

  val authSvc = new UserRepositoryAuthService(userRepository)
  def verify(u: String, p: String, user: User) = authSvc.verify(u, p) && user.name == u
  def Fail(name: String) = Unauthorized ~> WWWAuthenticate("""Basic realm="/""" + name + "\"")

  def uriToString(uri: Seq[String]) = uri.mkString("/")
  
  def storeBookmarkFromForm(name: String, uri: String, form: Map[String, Seq[String]]) = {
	val user = userRepository.findByName(name).get
    val bookmark = Bookmark(uri, 
    						new Date,
    		                form("bookmark[short_description]")(0), 
    		                form("bookmark[long_description]")(0), 
    		                form("bookmark[restrict]")(0).toBoolean, 
    		                user)
    user.bookmarks.put(uri, bookmark)
    bookmark
  }

  def intent = {
    case req @ GET(Path(Seg(path))) => path match {
      case "users" :: name :: "bookmarks" :: uri => {
        logger.debug("GET /users/%s/bookmarks/%s".format(name, uri))
        val uriString = uriToString(uri)
        userRepository.findByName(name) match {
          case Some(user) => {
            user.bookmarks.get(uriString) match {
              case Some(bookmark) => {
                val authorized = ! bookmark.restricted || (req match {
                  case BasicAuth(u, p) if verify(u, p, user) => true
                  case _ => false
                })
                if (authorized)
                  // TODO representation based on content negotiation
    	          Ok ~> ResponseString(bookmark.toString)
                else
                  NotFound
              }
              case _ => NotFound
            }
          }
          case _ => NotFound
        }
      }
      case _ => NotFound
    }
    case req @ PUT(Path(Seg(path))) => path match {
      case "users" :: name :: "bookmarks" :: uri => {
        logger.debug("PUT /users/%s/bookmarks/%s".format(name, uri))
        val uriString = uriToString(uri)
        userRepository.findByName(name) match {
          case Some(user) => req match {
            case BasicAuth(u, p) if verify(u, p, user) => req match { 
        	  case Params(form) => user.bookmarks.get(uriString) match {
        	    case Some(bookmark) =>
                  try {
        	        storeBookmarkFromForm(name, uriString, form)
        	        NoContent
                  } catch { case _ => BadRequest }
                case _ => 
                  try {
        	        val bookmark = storeBookmarkFromForm(name, uriString, form)
        	        Created ~> ResponseString(bookmark.toString)
                  } catch { case _ => BadRequest }
        	  }
        	  case _ => BadRequest
            }
            case _ => NotFound
          }
          case _ => NotFound
        }
      }
      case _ => NotFound
    }
    case req @ DELETE(Path(Seg(path))) => path match {
      case "users" :: name :: "bookmarks" :: uri => {
        logger.debug("DELETE /users/%s/bookmarks/%s".format(name, uri))
    	userRepository.findByName(name) match {
          case Some(user) => req match {
    	    case BasicAuth(u, p) if verify(u, p, user) => {
    	      user.bookmarks.remove(uriToString(uri)) match {
    	        case Some(_) => NoContent
    	        case _ => NotFound
    	      }
    	    }
    	    case _ => NotFound
          }
          case _ => NotFound
    	}
      }
      case _ => NotFound
    }
  }
}
