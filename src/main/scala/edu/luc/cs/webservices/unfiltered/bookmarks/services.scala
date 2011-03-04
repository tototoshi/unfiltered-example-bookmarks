package edu.luc.cs.webservices.unfiltered.bookmarks

import scala.collection.mutable.{Map, HashMap}

abstract class UserRepository {
  def findByName(name: String): Option[User]
  def store(user: User): Unit
  def remove(name: String): Option[User]
}

class InMemoryUserRepository extends UserRepository {
  val users: Map[String, User] = new HashMap
  override def findByName(name: String) = users.get(name)
  override def store(user: User) = users.put(user.name, user)
  override def remove(name: String) = users.remove(name)
}

trait AuthService {
  def verify(login: String, password: String): Boolean
}

class UserRepositoryAuthService(val userRepository: UserRepository) extends AuthService {
  def verify(login: String, password: String) = 
	userRepository.findByName(login) match {
    case Some(user) => user.password == password
    case _ => false
  }
}
