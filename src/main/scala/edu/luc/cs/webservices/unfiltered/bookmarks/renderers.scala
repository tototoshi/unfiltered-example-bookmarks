package edu.luc.cs.webservices.unfiltered.bookmarks

import unfiltered.request._
import unfiltered.response._

trait Renderer {
  def preferredMediaType(req: HttpRequest[Any]) = req match {
    case Accepts.Json(_) => 'json
    case Accepts.Html(_) => 'html
    case _ => 'text
  }
}

object userRenderer extends Renderer {
  def render(user: User) = {
	def as(variant: Symbol) = variant match {
	  case 'json => 5
	  case 'html => 7
	  case 'text => 9
	}
  }
}