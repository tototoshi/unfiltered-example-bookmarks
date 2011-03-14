package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.filter._
import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger

import collection.immutable.Map
import java.util.Date

object rootPlan extends Planify ({
  case GET(Path(Seg(Nil))) => {
	Logger(rootPlan getClass).debug("GET /")
	Ok ~> ResponseString(
      "To register: " +
      "curl -X PUT -d 'user[password]=pass' -d 'user[email]=you@host' -d 'user[full_name]=Your%20Name' -v http://localhost:8080/users/you"
    )
  }
})

abstract class BookmarksRepositoryPlan(val repository: BookmarksRepository) extends Plan {
  val authSvc = new UserRepositoryAuthService(repository)
  def verify(u: String, p: String, user: User) = authSvc.verify(u, p) && user.name == u
  def Fail(name: String) = Unauthorized ~> WWWAuthenticate("""Basic realm="/""" + name + "\"")
}

class UserPlan(override val repository: BookmarksRepository, val renderer: Renderer[User]) 
extends BookmarksRepositoryPlan(repository) {
  val logger = Logger(classOf[UserPlan])

  def storeUserFromForm(name: String, form: Map[String, Seq[String]]) = {
    val user = User(name, 
    		        form("user[password]")(0), 
    		        form("user[email]")(0), 
    		        form("user[full_name]")(0))
    repository.storeUser(user)
    user
  }

  def intent = {
    case req @ GET(Path(Seg("users" :: name :: Nil))) => {
      logger.debug("GET /users/%s" format name)
      repository findUser name match {
    	// TODO add hypermedia
        case Some(user) => Ok ~> renderer(req)(user)
        case _ => NotFound
      }
    }
    
    case req @ PUT(Path(Seg("users" :: name :: Nil))) => try {
      logger.debug("PUT /users/%s" format name)
      repository findUser name match {
        case Some(user) => req match {
          case BasicAuth(u, p) if verify(u, p, user) => {
            val Params(form) = req 
            storeUserFromForm(name, form)
            NoContent
          }
          case _ => Fail(name)
        }
        case _ => {
          val Params(form) = req
          val user = storeUserFromForm(name, form)
          Created ~> renderer(req)(user)
        }
      }
    } catch { case _ => BadRequest }
    
    case req @ DELETE(Path(Seg("users" :: name :: Nil))) => {
      logger.debug("DELETE /users/%s" format name)
      repository findUser name match {
        case Some(user) => req match {
          case BasicAuth(u, p) if verify(u, p, user) => {
        	repository.removeUser(name)
            NoContent
          }
          case _ => Fail(name)
        }
        case _ => NotFound
      }
    }
  }
}

class BookmarksPlan(override val repository: BookmarksRepository)
extends BookmarksRepositoryPlan(repository) {
  val logger = Logger(classOf[BookmarksPlan])

  def intent = {
    case req @ GET(Path(Seg("users" :: name :: "bookmarks" :: Nil))) => {
      logger.debug("GET /users/%s/bookmarks" format name)
      repository findUser name match {
    	case Some(user) => {
          val authorized = req match {
            case BasicAuth(u, p) if verify(u, p, user) => true
            case _ => false
          }
          val bookmarks = repository.findBookmarks(name).get
          // TODO representation based on content negotiation
          Ok ~> ResponseString(bookmarks.values filter ((e) => (authorized || ! e.restricted)) toString)
    	}
        case _ => NotFound
      }
    }
  }
}

class BookmarkPlan(override val repository: BookmarksRepository)
extends BookmarksRepositoryPlan(repository) {
  val logger = Logger(classOf[BookmarkPlan])

  def storeBookmarkFromForm(name: String, uri: String, form: Map[String, Seq[String]]) = {
    val bookmark = Bookmark(uri, 
    						new Date,
    		                form("bookmark[short_description]")(0), 
    		                form("bookmark[long_description]")(0), 
    		                form("bookmark[restrict]")(0) toBoolean)
    repository.storeBookmark(name, bookmark)
    bookmark
  }

  def intent = {
    case req @ GET(Path(Seg("users" :: name :: "bookmarks" :: uri))) => try {
      val uriString = uri mkString "/"
      logger.debug("GET /users/%s/bookmarks/%s" format(name, uriString))
      val Some(user) = repository findUser name
      val Some(bookmark) = repository.findBookmark(name, uriString)
      val true = bookmark.restricted || { val BasicAuth(u, p) = req ; verify(u, p, user) }
      Ok ~> ResponseString(bookmark toString)
    } catch { case _ => NotFound }

    case req @ PUT(Path(Seg("users" :: name :: "bookmarks" :: uri))) => try {
      val uriString = uri mkString "/"
      logger.debug("PUT /users/%s/bookmarks/%s" format(name, uriString))
      val Some(user) = repository findUser name
      req match {
        case BasicAuth(u, p) if verify(u, p, user) => {
          val Params(form) = req
          try { 
            repository.findBookmark(name, uriString) match {
        	  case Some(bookmark) => {
        	 	storeBookmarkFromForm(name, uriString, form)
        	 	NoContent
        	  }
              case _ => {
	        	val bookmark = storeBookmarkFromForm(name, uriString, form)
            	Created ~> ResponseString(bookmark toString)
              }
            }
          } catch { case _ => BadRequest }
        }
        case _ => Fail(name)
      }
    } catch { case _ => NotFound }

    case req @ DELETE(Path(Seg("users" :: name :: "bookmarks" :: uri))) => try {
      val uriString = uri mkString "/"
      logger.debug("DELETE /users/%s/bookmarks/%s" format(name, uriString))
      val Some(user) = repository findUser name
      val BasicAuth(u, p) = req
      val true = verify(u, p, user)
      val Some(_) = repository.removeBookmark(name, uriString)
      NoContent
    } catch { case _ => NotFound }
  }
}
