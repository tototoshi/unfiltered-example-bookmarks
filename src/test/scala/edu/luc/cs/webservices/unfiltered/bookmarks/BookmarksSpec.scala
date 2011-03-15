package edu.luc.cs.webservices.unfiltered.bookmarks

import collection.immutable.{Map => IMap}
import java.net.URLEncoder
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.params.HttpProtocolParams

// http://code.google.com/p/specs/wiki/QuickStart
import org.specs._

// http://dispatch.databinder.net/Common_Tasks
import dispatch._

object BookmarksSpec extends Specification with unfiltered.spec.jetty.Served {

  def setup = Main applyPlans _ 

  // requested this to be added to Databinder as dispatch.Request.<<<
  def putForm(request: Request, values: Map[String, Any]): Request = request.next {
    val m = new HttpPut
    m setEntity new UrlEncodedFormEntity(Http map2ee values, request defaultCharset)
    Request.mimic(m)_
  }
    
  def enc = URLEncoder encode (_: String, Request factoryCharset)
    	
  val user1 = "user1"
  val user2 = "user2"

  val http = new Http

  "The example app" should {

    "provide something useful at root" in {
      val status = try {
        http x (host as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }

    "have no user that has not been created" in {
      val status = try {
        http x (host / "users" / user2 as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 404
    }

    "allow unauthenticated creation below root" in {
      val form = Map("user[password]" -> user1, 
                     "user[email]" -> "laufer@cs.luc.edu",
                     "user[full_name]" -> "Konstantin Laufer")  	
      val status = try {
        http x (putForm(host / "users" / user1, form) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 201
    }

    "allow retrieval of the user that has been created" in {
      val status = try {
        http x (host / "users" / user1 as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }

    "expose an empty list of bookmarks" in {
      println()
      val status = try {
        http x (host / "users" / user1 / "bookmarks" as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }

    "allow authenticated bookmark creation" in {
      val form = Map("bookmark[short_description]" -> "etl@luc",
                     "bookmark[long_description]" -> "Loyola Emerging Technologies Lab",
                     "bookmark[restrict]" -> "false")    		        
      val status = try {
        http x (putForm(host / "users" / user1 / "bookmarks" / "http://www.etl.luc.edu/", form) 
          as_! (user1, user1) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 201
    }
    
    "allow retrieval of created bookmark" in {
      val status = try {
        http x (host / "users" / user1 / "bookmarks" / "http://www.etl.luc.edu/" as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }
    
    "allow authenticated creation of a private bookmark" in {
      val form = Map("bookmark[short_description]" -> "cs@luc",
                     "bookmark[long_description]" -> "Loyola CS Dept",
                     "bookmark[restrict]" -> "true")    		        
      val status = try {
        http x (putForm(host / "users" / user1 / "bookmarks" / "http://www.cs.luc.edu/", form) 
          as_! (user1, user1) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 201
    }

    "allow authenticated retrieval of private bookmark" in {
      val status = try {
        http x (host / "users" / user1 / "bookmarks" / "http://www.cs.luc.edu/" as_! (user1, user1) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 200
    }
    
    "allow authenticated deletion of bookmark" in {
      val status = try {
        http x ((host / "users" / user1 / "bookmarks" / "http://www.cs.luc.edu/" DELETE) as_! (user1, user1) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 204
    }
    
    "allow authentication for deletion" in {
      val status = try {
        http x ((host / "users" / user1 DELETE) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 401
    }

    "allow authenticated deletion of user" in {
      val status = try {
        http x ((host / "users" / user1 DELETE) as_! (user1, user1) as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 204
    }
  }
}