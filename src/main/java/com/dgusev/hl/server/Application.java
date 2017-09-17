package com.dgusev.hl.server;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Arrays;


/**
 * Created by dgusev on 25.07.2017.
 */
@SpringBootApplication
public class Application {

    public static void main(String... args) throws IOException {
        SpringApplication.run(Application.class, args);
    }


}
