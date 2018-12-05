package models.warpscripts

import query_module.Query

object Checks {
  def last(token: String, selector: String, duration: String, value: String, op: String, labels: Option[String] = None): String = {
    val query = Query(token, selector, duration)
    List(
      query.fetch,
      query.filterLast(op, value),
      query.reduce(labels),
      query.footer
    ).mkString("")
  }

  def delta(token: String, selector: String, duration: String, range: Integer, value: String, op: String, labels: Option[String] = None): String = {
    val query = Query(token, selector, duration)
    List(
      query.fetch,
      query.delta(range),
      query.filterLast(op, value),
      query.reduce(labels),
      query.footer
    ).mkString("\n")
  }
}


