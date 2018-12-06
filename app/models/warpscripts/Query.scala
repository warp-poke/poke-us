package models.warpscripts

object query_module {
  case class Query(token: String, selector: String, duration: String) {

    def setLastEvalTime(String timestamp): String {
      s"""
      ${timestamp} 'now' STORE
      """
    }

    def markAsNotified: String = {
      s"""
        NOW 'now' CSTORE
        [ '${token}' '${selector}' PARSESELECTOR ] FIND
        {
          'lastNotify'
          $$now TOSTRING
        }
        SETATTRIBUTES
        
        ${token} META
      """
    }

    def markAsQuiesce: String = {
      s"""
        [ '${token}' '${selector}' PARSESELECTOR ] FIND
        {
          'quiesce'
          'true
        }
        SETATTRIBUTES
        
        ${token} META
      """
    }

    def markAsUnQuiesce: String = {
      s"""
        [ '${token}' '${selector}' PARSESELECTOR ] FIND
        {
          'quiesce'
          'true
        }
        SETATTRIBUTES
        
        ${token} META
      """
    }

    def fetch: String = {
      s"""
        1535188676443422 'now' CSTORE
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

    def mapperLast(op: String, value: String): String = {
      s"""
        [
          SWAP
          []
          ${value}
          'mapper.' '${op}' + EVAL
          0 0 0
        ] MAP
        NONEMPTY

        [ SWAP mapper.last MAXLONG 0 -1 ] MAP
      """
    }

    def reduce(labelsOption: Option[String]): String = {
      labelsOption.map { labels =>
        s"""
          [ SWAP ${labels} reducer.count ] REDUCE
          [ SWAP mapper.last MAXLONG 0 -1 ] MAP
        """
      }.getOrElse("")
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

    def notQuiesce: String = {
      s"""
        []
        <%
          DUP ATTRIBUTES            
          <%
            'quiesce' GET DUP
            <%
              ISNULL
            %>
            <%
              DROP
              false
            %>
            <%
              'true' ==
            %>
            IFTE
          %>
          <%
            DROP
          %>
          <%
            +
          %>
          IFTE
        %>
        FOREACH
        NONEMPTY
      """
    }

    def checkLastNotify: String =  {
      s"""
        []
        SWAP
        <%
          DUP ATTRIBUTES
          
          <%
            'lastNotify' GET DUP ISNULL
          %>
          <%
            DROP
            +
          %>
          <%
            [ SWAP TOLONG  $$now ] 1 ->LIST CLIP +
          %>
          IFTE
        %>
        FOREACH
        NONEMPTY
      """
    }

    def footer: String = {
      s"""
        DUP TYPEOF <% 'GTS' == %> <% [ SWAP ] %> IFT
      """
    }
  }
}
