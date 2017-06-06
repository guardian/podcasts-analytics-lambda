package com.gu.contentapi.utils

object PredicateUtils {

  implicit class RichPredicate[A](val f: Function1[A, Boolean]) extends AnyVal {
    def apply(v: A) = f(v)

    def &&(g: Function1[A, Boolean]): Function1[A, Boolean] = {
      (x: A) => f(x) && g(x)
    }

    def ||(g: Function1[A, Boolean]): Function1[A, Boolean] = {
      (x: A) => f(x) || g(x)
    }

    def unary_! : Function1[A, Boolean] = {
      (x: A) => !f(x)
    }
  }
}