package edu.luc.cs.webservices.unfiltered.bookmarks

import java.util.Date
import collection.mutable.Map

case class User(
  val name: String, 
  password: String, 
  email: String, 
  fullName: String, 
  val bookmarks: Map[String, Bookmark] = Map.empty
)

case class Bookmark(
  uri: String, 
  dateTime: Date, 
  shortDescription: String, 
  longDescription: String, 
  restricted: Boolean, 
  val user: User
) {
  override def toString =
	if (user != null)
      Bookmark(uri, dateTime, shortDescription, longDescription, restricted, null).toString
    else
      super.toString
}
