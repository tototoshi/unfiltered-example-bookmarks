package edu.luc.cs.webservices.unfiltered.bookmarks

import collection.mutable.{Map, HashMap}
import collection.immutable.{Map => IMap}
import actors.Actor
import actors.Actor._

// TODO convert UserRepository to service facade for
// easy replacement with persistent version

trait BookmarksRepository {
  def findUser(name: String): Option[User]
  def storeUser(user: User): Option[User]
  def removeUser(name: String): Option[User]
  def findBookmarks(name: String): Option[IMap[String, Bookmark]]
  def findBookmark(name: String, uri: String): Option[Bookmark]
  def storeBookmark(name: String, bookmark: Bookmark): Option[Bookmark]
  def removeBookmark(name: String, uri: String): Option[Bookmark]
}

class ThreadSafeBookmarksRepository(repository: BookmarksRepository) extends BookmarksRepository {
  // TODO generate this functionality automatically this using reflection
	
  case class FindUser(name: String, requestor: Actor)
  case class StoreUser(user: User, requestor: Actor)
  case class RemoveUser(name: String, requestor: Actor)
  case class FindBookmarks(name: String, requestor: Actor)
  case class FindBookmark(name: String, uri: String, requestor: Actor)
  case class StoreBookmark(name: String, bookmark: Bookmark, requestor: Actor)
  case class RemoveBookmark(name: String, uri: String, requestor: Actor)
	
  val ia = actor {
	loop {
	  react {
	    case FindUser(n, a) => {
	      println("entering")
	      a ! (repository.findUser(n))
	      println("leaving")
	    }
	    case StoreUser(u, a) => a ! (repository.storeUser(u))
	    case RemoveUser(n, a) => a ! (repository.removeUser(n))
	    case FindBookmarks(n, a) => a ! (repository.findBookmarks(n))
	    case FindBookmark(n, s, a) => a ! (repository.findBookmark(n, s))
	    case StoreBookmark(n, b, a) => a ! (repository.storeBookmark(n, b))
	    case RemoveBookmark(n, s, a) => a ! (repository.removeBookmark(n, s))
	  }
	}
  }
  
  override def findUser(name: String) = 
  	{ ia ! (FindUser(name, self)) ; self receive { case x: Option[User] => x } }
  override def storeUser(user: User) = 
    { ia ! (StoreUser(user, self)) ; self receive { case x: Option[User] => x } }
  override def removeUser(name: String) = 
    { ia ! (RemoveUser(name, self)) ; self receive { case x: Option[User] => x } } 
  override def findBookmarks(name: String) = 
    { ia ! (FindBookmarks(name, self)) ; self receive { case x: Option[IMap[String, Bookmark]] => x } }
  override def findBookmark(name: String, uri: String) = 
    { ia ! (FindBookmark(name, uri, self)) ; self receive { case x: Option[Bookmark] => x } }
  override def storeBookmark(name: String, bookmark: Bookmark) = 
    { ia ! (StoreBookmark(name, bookmark, self)) ; self receive { case x: Option[Bookmark] => x } }
  override def removeBookmark(name: String, uri: String) = 
    { ia ! (RemoveBookmark(name, uri, self)) ; self receive { case x: Option[Bookmark] => x } }
}  
	
class InMemoryBookmarksRepository extends BookmarksRepository {
  private val users: Map[String, User] = new HashMap
  private val bookmarks: Map[String, Map[String, Bookmark]] = new HashMap

  override def findUser(name: String) = users.get(name)
  override def storeUser(user: User) = {
	bookmarks.put(user.name, new HashMap)
	users.put(user.name, user)
  }
  override def removeUser(name: String) = {
    bookmarks.remove(name)
	users.remove(name)
  }
  override def findBookmarks(name: String) = bookmarks get name match {
    // TODO make this conversion O(1) 
	case Some(bs) => Some(IMap(bs.toList: _*))
	case None => None
  }	
  override def findBookmark(name: String, uri: String) = bookmarks get name match {
	case Some(bs) => bs get uri
	case None => None
  }
  override def storeBookmark(name: String, bookmark: Bookmark) =
    bookmarks(name).put(bookmark.uri, bookmark)
  override def removeBookmark(name: String, uri: String) =
    bookmarks(name).remove(uri)
}

trait AuthService {
  def verify(login: String, password: String): Boolean
}

class UserRepositoryAuthService(val userRepository: BookmarksRepository) extends AuthService {
  def verify(login: String, password: String) = 
	userRepository findUser login match {
    case Some(user) => user.password == password
    case _ => false
  }
}
