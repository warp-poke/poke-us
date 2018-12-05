package models.warpscripts

import query_module.Query

object Checks {
  def last(token: String, selector: String, duration: String, value: String, op: String): String = {
    val query = Query(token, selector, duration)
    List(
      query.fetch,
      query.filterLast(op, value),
      query.footer
    ).mkString("\n")
  }

  def delta(token: String, selector: String, duration: String, range: Integer, value: String, op: String): String = {
    val query = Query(token, selector, duration)
    List(
      query.fetch,
      query.delta(range),
      query.filterLast(op, value),
      query.footer
    ).mkString("\n")
  }
}


