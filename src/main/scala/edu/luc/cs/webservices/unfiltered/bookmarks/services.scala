package edu.luc.cs.webservices.unfiltered.bookmarks

import collection.immutable.{Map => IMap}

trait AuthService {
  def verify(login: String, password: String, user: User): Boolean
}

// TODO consider refactoring to Session Facade/Remote Facade 
// with a single coarse-grained method (including authorization)
// for each HTTP request
// or see next TODO below
trait BookmarksRepository {
  def findUser(name: String): Option[User]
  def storeUser(user: User): Option[User]
  def removeUser(name: String): Option[User]
  def findBookmarks(name: String): Option[IMap[String, Bookmark]]
  def findBookmark(name: String, uri: String): Option[Bookmark]
  def storeBookmark(name: String, bookmark: Bookmark): Option[Bookmark]
  def removeBookmark(name: String, uri: String): Option[Bookmark]
// TODO consider this for efficiency to minimize number of interactions with repo
// (related to Data Transfer Object pattern)
//  def findBookmarks(name: String): Option[(User, IMap[String, Bookmark])]
//  def findBookmark(name: String, uri: String): Option[(User, Bookmark)]
//  def storeBookmark(name: String, bookmark: Bookmark): Option[(User, Bookmark)]
//  def removeBookmark(name: String, uri: String): Option[(User, Bookmark)]  
}
