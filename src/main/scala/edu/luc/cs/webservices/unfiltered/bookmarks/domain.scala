package edu.luc.cs.webservices.unfiltered.bookmarks

import java.util.Date

case class User(
  val name: String, 
  password: String, 
  email: String, 
  fullName: String, 
  bookmarks: Bookmark*
)

case class Bookmark(
  uri: String, 
  dateTime: Date, 
  shortDescription: String, 
  longDescription: String, 
  restrict: Boolean, 
  val user: User
)
