package models.warpscripts

object query_module {
  case class Query(token: String, selector: String, duration: String) {
    def fetch: String = {
      s"""
        1535188676443422 'now' STORE
        [ '${token}' '${selector}' PARSESELECTOR $$now ${duration} ] FETCH
      """
    }

    def filterLast(op: String, value: String): String = {
      s"""
        [
          SWAP
          []
          ${value}
          'filter.last.' '${op}' + EVAL
        ] FILTER
        NONEMPTY

        [ SWAP mapper.last MAXLONG 0 -1 ] MAP
      """
    }

    def delta(range: Integer): String = {
      s"""
        [ SWAP mapper.tolong 0 0 0 ] MAP
        [ SWAP -1 mapper.add 0 0 0 ] MAP
        [ SWAP mapper.abs 0 0 0 ] MAP

        [ SWAP mapper.sum ${range} 0 0 ] MAP

        [ SWAP mapper.delta ${range} 0 0 ] MAP
      """
    }
    def footer: String = {
      s"""
        DUP TYPEOF <% 'GTS' == %> <% [ SWAP ] %> IFT
      """
    }
  }
}
