package edu.luc.cs.webservices.unfiltered.bookmarks

// http://code.google.com/p/specs/wiki/QuickStart
import org.specs._

object BookmarksSpec extends Specification with unfiltered.spec.jetty.Served {

  // TODO update to match SUS
	
  // http://dispatch.databinder.net/Common_Tasks
  import dispatch._

  def setup = {
    val userRepository = new InMemoryUserRepository
    _.filter(new UserResource(new InMemoryUserRepository))
  }

  val user = "blah"
  val user2 = "blah2"

  val http = new Http

  "The example app" should {

    "find nothing at root" in {
      val status = try {
        http x (host as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 404
    }

    "find no user that has not been created" in {
      val status = try {
        http x (host / "users" / user2 as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 404
    }

    "allow unauthenticated creation below root" in {
      val status = try {
        http x ((host / "users" / user <<< "") as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 201
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

    "find empty book" in {
      val status = try {
        http x (host / "users" / user2 as_str) { case (code, _, _, _) => code }
      } catch { case StatusCode(code, _) => code }
      status must_== 404
    }
  }
}