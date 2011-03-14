package edu.luc.cs.webservices.unfiltered.bookmarks

import java.util.Date
import collection.mutable.Map

case class User(
  val name: String, 
  password: String, 
  email: String, 
  fullName: String
)

case class Bookmark(
  uri: String, 
  dateTime: Date, 
  shortDescription: String, 
  longDescription: String, 
  restricted: Boolean 
)
