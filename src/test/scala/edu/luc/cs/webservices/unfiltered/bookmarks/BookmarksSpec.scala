package edu.luc.cs.webservices.unfiltered.bookmarks

import collection.immutable.{Map => IMap}
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.params.HttpProtocolParams

// http://code.google.com/p/specs/wiki/QuickStart
import org.specs._

// http://dispatch.databinder.net/Common_Tasks
import dispatch._

object BookmarksSpec extends Specification with unfiltered.spec.jetty.Served {

  // TODO update to match SUS
	  
  def setup = {
    val userRepository = new InMemoryUserRepository
    _.filter(new UserResource(userRepository))
     .filter(new BookmarksResource(userRepository))
     .filter(new BookmarkResource(userRepository))
  }

  // requested this to be added to Databinder as dispatch.Request.<<<
  def putForm(request: Request, values: Map[String, Any]): Request = request.next {
    val m = new HttpPut
    m setEntity new UrlEncodedFormEntity(Http.map2ee(values), request.defaultCharset)
    Request.mimic(m)_
  }    
  
  val user = "blah"
  val user2 = "blah2"

  val http = new Http

  "The example app" should {

    "find something useful at root" in {
      val status = try {
        http x (host as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }

    "find no user that has not been created" in {
      val status = try {
        http x (host / "users" / user2 as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 404
    }

    "allow unauthenticated creation below root" in {
      val form = Map("user[password]" -> "blah", 
    		  		 "user[email]" -> "laufer@cs.luc.edu",
    		  		 "user[full_name]" -> "Konstantin Laufer")  	
      val status = try {
        http x (putForm(host / "users" / user, form) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 201
    }

    "find the user that has been created" in {
      val status = try {
        http x (host / "users" / user as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }

    "find an empty list of bookmarks" in {
      println()
      val status = try {
        http x (host / "users" / user / "bookmarks" as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }
//      val form = Map("bookmark[short_description]=Loyola'   -d 'bookmark[long_description]=Loyola%20home%20page' -d 'bookmark[restrict]=false'     	

    "allow authenticated bookmark creation" in {
      val status = try {
        http x (host / "users" / user / "bookmarks/" as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }
    
    "require authentication for deletion" in {
      val status = try {
        http x ((host / "users" / user DELETE) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 401
    }

    "allow authenticated deletion" in {
      val status = try {
        http x ((host / "users" / user DELETE) as_! (user, user) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 204
    }
  }
}