package edu.luc.cs.webservices.unfiltered.bookmarks

import collection.immutable.{Map => IMap}

trait AuthService {
  def verify(login: String, password: String): Boolean
}

trait BookmarksRepository {
  def findUser(name: String): Option[User]
  def storeUser(user: User): Option[User]
  def removeUser(name: String): Option[User]
  def findBookmarks(name: String): Option[IMap[String, Bookmark]]
  def findBookmark(name: String, uri: String): Option[Bookmark]
  def storeBookmark(name: String, bookmark: Bookmark): Option[Bookmark]
  def removeBookmark(name: String, uri: String): Option[Bookmark]
}
