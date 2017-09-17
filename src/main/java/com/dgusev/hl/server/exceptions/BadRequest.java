package com.dgusev.hl.server.exceptions;


/**
 * Created by dgusev on 12.08.2017.
 */

public class BadRequest extends RuntimeException {

    public BadRequest() {

    }

    public BadRequest(String msg) {
        super(msg);
    }
}
