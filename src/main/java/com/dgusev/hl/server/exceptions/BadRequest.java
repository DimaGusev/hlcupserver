package com.dgusev.hl.server.exceptions;


/**
 * Created by dgusev on 12.08.2017.
 */

public class BadRequest extends RuntimeException {

    public static final BadRequest INSTANCE = new BadRequest();

    private BadRequest() {

    }

    public BadRequest(String msg) {
        super(msg);
    }
}
