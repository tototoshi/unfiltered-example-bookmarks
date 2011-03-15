package edu.luc.cs.webservices.unfiltered.bookmarks

import collection.mutable.{Map, HashMap}
import collection.immutable.{Map => IMap}
import actors.Actor
import actors.Actor._
import scala.concurrent.stm._

class UserRepositoryAuthService(val repository: BookmarksRepository) extends AuthService {
  def verify(login: String, password: String) =
    repository findUser login match {
    case Some(user) => user.password == password
    case _ => false
  }
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

trait STMBookmarksRepository extends BookmarksRepository {
  val concurrency = Ref(0)
  val debug = false

  abstract override def findUser(name: String) = atomic { implicit txn => {
    if (debug) {
      concurrency += 1
      if (concurrency() > 1) throw new RuntimeException("STM bug?")
    }
    val result = super.findUser(name)
    if (debug) concurrency -= 1
    result
  }}
  abstract override def storeUser(user: User) = 
    atomic { implicit txn => super.storeUser(user) }
  abstract override def removeUser(name: String) = 
    atomic { implicit txn => super.removeUser(name) }
  abstract override def findBookmarks(name: String) = 
    atomic { implicit txn => super.findBookmarks(name) }
  abstract override def findBookmark(name: String, uri: String) = 
    atomic { implicit txn => super.findBookmark(name, uri) }
  abstract override def storeBookmark(name: String, bookmark: Bookmark) = 
    atomic { implicit txn => super.storeBookmark(name, bookmark) }
  abstract override def removeBookmark(name: String, uri: String) = 
    atomic { implicit txn => super.removeBookmark(name, uri) }
}

trait ActorBookmarksRepository extends BookmarksRepository {

  // TODO try this using reflection

  case class FindUser(name: String)
  case class StoreUser(user: User)
  case class RemoveUser(name: String)
  case class FindBookmarks(name: String)
  case class FindBookmark(name: String, uri: String)
  case class StoreBookmark(name: String, bookmark: Bookmark)
  case class RemoveBookmark(name: String, uri: String)

  def ia = actor {
    loop {
      react {
        case FindUser(n) => {
          if (debug) {
            var c = 0
            atomic { implicit txn => { concurrency += 1 ; c = concurrency() } }
            if (c > 1) throw new RuntimeException("Scala native actors bug?")
          }
          sender ! { super.findUser(n) }
          if (debug) atomic { implicit txn => concurrency -= 1 }
        }
        case StoreUser(u) => sender ! { super.storeUser(u) }
        case RemoveUser(n) => sender ! { super.removeUser(n) }
        case FindBookmarks(n) => sender ! { super.findBookmarks(n) }
        case FindBookmark(n, s) => sender ! { super.findBookmark(n, s) }
        case StoreBookmark(n, b) => sender ! { super.storeBookmark(n, b) }
        case RemoveBookmark(n, s) => sender ! { super.removeBookmark(n, s) }
      }
    }
  }

  val concurrency = Ref(0)
  val debug = false
  def sync[R](msg: Any) = { ia ! msg ; self receive { case x: R => x } }

  abstract override def findUser(name: String) = sync[Option[User]](FindUser(name))
  abstract override def storeUser(user: User) = sync[Option[User]](StoreUser(user))
  abstract override def removeUser(name: String) = sync[Option[User]](RemoveUser(name)) 
  abstract override def findBookmarks(name: String) = sync[Option[IMap[String, Bookmark]]](FindBookmarks(name))
  abstract override def findBookmark(name: String, uri: String) = sync[Option[Bookmark]](FindBookmark(name, uri))
  abstract override def storeBookmark(name: String, bookmark: Bookmark) = sync[Option[Bookmark]](StoreBookmark(name, bookmark))
  abstract override def removeBookmark(name: String, uri: String) = sync[Option[Bookmark]](RemoveBookmark(name, uri))
}  
