package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._
import net.liftweb.json._

trait Renderer[T] {
  def apply[R](req: HttpRequest[R])(resource: T): ResponseFunction[Any]
}

object userRenderer extends Renderer[User] {
  def apply[R](req: HttpRequest[R])(user: User) = { 
    req match {
      case Accepts.Json(_) => JsonContent ~> ResponseString("""{"name": "blah"}""")
      case Accepts.Html(_) => HtmlContent ~> {
        val langs = req match { case AcceptLanguage(langs) => langs ; case _ => List empty }
        if (langs find (_ startsWith "de") isDefined)
          ResponseString("""<html>mein name ist hase</html>""")
        else
          ResponseString("""<html>my name is blah</html>""")
      	}
      case _ => PlainTextContent ~> ResponseString(user toString)
    }
  }
}