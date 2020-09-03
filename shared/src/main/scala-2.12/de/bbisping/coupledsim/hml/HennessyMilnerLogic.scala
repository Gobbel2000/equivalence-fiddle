package de.bbisping.coupledsim.hml

object HennessyMilnerLogic {

  abstract sealed class Formula[A]

  case class And[A](subterms: List[Formula[A]]) extends Formula[A]

  case class Observe[A](action: A, andThen: Formula[A]) extends Formula[A]

  case class Negate[A](andThen: Formula[A]) extends Formula[A]

}