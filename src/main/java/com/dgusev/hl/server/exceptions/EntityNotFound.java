package com.dgusev.hl.server.exceptions;


/**
 * Created by dgusev on 12.08.2017.
 */

public class EntityNotFound extends RuntimeException {
    public static final EntityNotFound INSTANCE = new EntityNotFound();

    private EntityNotFound() {
    }
}
