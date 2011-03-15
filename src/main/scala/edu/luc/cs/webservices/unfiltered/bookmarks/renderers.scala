package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._
import net.liftweb.json.Serialization._

trait Renderer[T] {
  def apply[R](req: HttpRequest[R])(resource: T): ResponseFunction[Any]
}

object userRenderer extends Renderer[User] {
  implicit val formats = net.liftweb.json.DefaultFormats
  def apply[R](req: HttpRequest[R])(user: User) = req match {
    // TODO remove password and think about bookmarks
    case Accepts.Json(_) => JsonContent ~> ResponseString(write(user))
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

// TODO renderers for the other plans and resources