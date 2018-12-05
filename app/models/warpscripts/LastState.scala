package models.warpscripts
object LastState {
    def lastState(token: String, selector: String, duration: String, alert: String, op: String): String = {
        s"""
            ${duration} 'duration' STORE
            ${alert} 'alert' STORE
            ${op} 'op' STORE
            1535188676443422 'now' STORE
            '${selector}' 'selector' STORE
            '${token}' 'token' STORE
            [ $token $selector PARSESELECTOR $now $duration ] FETCH

            [
                SWAP
                []
                $alert
                'filter.last.' $op + EVAL
            ] FILTER
            NONEMPTY

            [ SWAP mapper.last MAXLONG 0 -1 ] MAP
        """
    }
}


