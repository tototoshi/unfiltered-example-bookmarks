package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.filter._
import unfiltered.request._
import unfiltered.response._
import unfiltered.scalate._

import org.clapper.avsl.Logger

import org.apache.http.impl.cookie.DateUtils

import PartialFunction._
import collection.immutable.Map
import java.util.Date

class RootPlan extends Plan {
  val logger = Logger(classOf[RootPlan])
  val creationDate = new java.util.Date 
  val eTag = hashCode.toString

  val Head = Ok ~> Vary("Accept-Charset", "Accept-Encoding", "Accept-Language", "Accept")

  val Caching = 
    CacheControl("max-age=3600") ~> 
    LastModified(DateUtils.formatDate(creationDate)) ~> 
    ETag(eTag) 

  def intent = {
    case OPTIONS(_) => Ok ~> Allow("GET", "HEAD", "OPTIONS")

    case HEAD(Path(Seg(Nil))) => {
      logger.debug("HEAD /")
      Head
    }

    case req @ GET(Path(Seg(Nil))) => {
      logger.debug("GET /")
      val cached = 
        req match {
          case IfNoneMatch(xs) => xs contains eTag
          case IfModifiedSince(xs) => {
            val sinceDate = DateUtils.parseDate(xs mkString ", ")
            ! creationDate.after(sinceDate)
          }
          case _ => false
      }
      if (cached)
        Head ~> Caching ~> NotModified
      else
        Head ~> Caching ~> HtmlContent ~> ResponseString(
          """<html><head></head><body>""" +
          """<form action="/users" method="POST">""" +
          """<label>id</label><input type="text" name="user[id]"/>""" +
          """<label>password</label><input type="text" name="user[password]"/>""" +
          """<label>email</label><input type="text" name="user[email]"/>""" +
          """<label>full name</label><input type="text" name="user[full_name]"/>""" +
          """<input type="submit"/>""" +
          """</form>""" +
          """</body></html>"""
        )
    }

//    case req @ POST(Path(Seg("users" :: Nil))) => try {
//      val Params(form) = req
//      val name = form("user[id]")(0)
//      val None = repository findUser name
//      storeUserFromForm(name, form) map { Created ~> renderer(req)(_) } get
//    } catch { case _ => BadRequest }    
  }
}
  
abstract class BookmarksRepositoryPlan(val repository: BookmarksRepository) extends Plan {
  val authSvc = new SimpleLocalAuthService
  def Fail(name: String) = Unauthorized ~> WWWAuthenticate("""Basic realm="/""" + name + "\"")
  def verifyLocally(u: String, p: String, user: User) = authSvc.verify(u, p, user)
  def basicAuthVerified[R](req: HttpRequest[R]) = 
    BasicAuthVerified unapply req map { _ => true } getOrElse false

  object BasicAuthVerified {
    def unapply[R](req: HttpRequest[R]) = for {
      Path(Seg("users" :: name :: _)) <- Some(req)
      BasicAuth(u, p) <- Some(req)
      if name == u
      user <- repository findUser u
      if verifyLocally(u, p, user)
    } yield user
  }
}

class UserPlan(override val repository: BookmarksRepository, val renderer: Renderer[User])
  extends BookmarksRepositoryPlan(repository) {
  val logger = Logger(classOf[UserPlan])

  def storeUserFromForm(name: String, form: Map[String, Seq[String]]) = {
    val user = User(name,
      form("user[password]")(0),
      form("user[email]")(0),
      form("user[full_name]")(0))
    if (repository.storeUser(user).isEmpty) Some(user) else None
  }

  
  
  def intent = {
  // TODO add OPTION
  // TODO add HEAD
    
    case req @ GET(Path(Seg("users" :: name :: Nil))) => {
      logger.debug("GET /users/%s" format name)
      // TODO add hypermedia
      // TODO add caching
      repository findUser name map { Ok ~> renderer(req)(_) } getOrElse NotFound
    }
    
    case req @ PUT(Path(Seg("users" :: name :: Nil))) => try {
      logger.debug("PUT /users/%s" format name)
      // in general: want at most one interaction with repo per HTTP request
      // hard because it would push too much of the application-specific
      // service semantics into the repo
      val Params(form) = req
      (for (user <- repository findUser name) yield
        (for {
          BasicAuth(u, p) <- Some(req)
          if verifyLocally(u, p, user)
        } yield {
          storeUserFromForm(name, form)
          NoContent
        }) getOrElse Fail(name)
      ) getOrElse 
        (storeUserFromForm(name, form) map { Created ~> renderer(req)(_) } get)
    } catch { case _ => BadRequest }

    case req @ DELETE(Path(Seg("users" :: name :: Nil))) => {
      logger.debug("DELETE /users/%s" format name)
      (for (BasicAuth(u, p) <- Some(req)) yield
        (for (user <- repository findUser name) yield 
          if (verifyLocally(u, p, user)) {
            repository.removeUser(name)
            NoContent
          } else
            Fail(name)
        ) getOrElse NotFound
      ) getOrElse Fail(name)
    }
  }
}

class BookmarksPlan(override val repository: BookmarksRepository)
  extends BookmarksRepositoryPlan(repository) {
  val logger = Logger(classOf[BookmarksPlan])

  def intent = {
    case req @ GET(Path(Seg("users" :: name :: "bookmarks" :: Nil))) => {
      logger.debug("GET /users/%s/bookmarks" format name)
      // TODO representation based on content negotiation
      (for (bs <- repository findBookmarks name) yield {
        val authorized = ! bs.exists(_._2.restricted) || basicAuthVerified(req)
        Ok ~> ResponseString((if (authorized) bs else bs filter { ! _._2.restricted }) toString)
      }) getOrElse NotFound
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
    if (repository.storeBookmark(name, bookmark).isEmpty) Some(bookmark) else None
  }

  def intent = {
    case req @ GET(Path(Seg("users" :: name :: "bookmarks" :: uri))) => {
      val uriString = uri mkString "/"
      logger.debug("GET /users/%s/bookmarks/%s" format (name, uriString))
      (for {
        b <- repository findBookmark(name, uriString)
        if ! b.restricted || basicAuthVerified(req)
       } yield Ok ~> ResponseString(b toString)) getOrElse NotFound
    }
    
    case req @ PUT(Path(Seg("users" :: name :: "bookmarks" :: uri))) => try {
      val uriString = uri mkString "/"
      logger.debug("PUT /users/%s/bookmarks/%s" format (name, uriString))
      (for (BasicAuthVerified(_) <- Some(req)) yield {
        val Params(form) = req
        (for (b <- storeBookmarkFromForm(name, uriString, form)) yield 
          Created ~> ResponseString(b toString)) getOrElse NoContent
      }) getOrElse NotFound
    } catch { case _ => BadRequest }

    case req @ DELETE(Path(Seg("users" :: name :: "bookmarks" :: uri))) => {
      val uriString = uri mkString "/"
      logger.debug("DELETE /users/%s/bookmarks/%s" format (name, uriString))
      (for {
        BasicAuthVerified(_) <- Some(req)
        _ <- repository.removeBookmark(name, uriString)
      } yield NoContent) getOrElse NotFound  
    }
  }
}
