package com.dgusev.hl.server;

import com.dgusev.hl.server.util.CharArrayUtil;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by dgusev on 05.10.2017.
 */
/*
public class TestCase {

    private static final String BASE_URL = "http://192.168.0.104:8080";
    public static class CustomHttpRequestInterceptor implements ClientHttpRequestInterceptor
    {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
        {
            HttpHeaders headers = request.getHeaders();
            headers.remove(HttpHeaders.ACCEPT_CHARSET);
            headers.remove(HttpHeaders.ACCEPT);
            headers.remove(HttpHeaders.CONTENT_TYPE);
            headers.remove("Content-type");
            headers.remove("Accept");

            return execution.execute(request, body);
        }

    }



    public static void main(String ... args) {

        byte[] msg = "HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json;charset=UTF-8\r\nDate: Sat, 19 Aug 2017 11:30:12 GMT\r\nContent-Length: 2\r\n\r\n{}".getBytes();
        System.out.println(msg.length);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new CustomHttpRequestInterceptor()));
        for (int i = 0; i< 20000; i++) {
            String user = "{\"first_name\": \"Пётрафыафы\", \"last_name\": \"Фетатосянвыаывар\", \"birth_date\": -1720915200, \"gender\": \"m\", \"id\": " + i +", \"email\": \"wibylcudestiwukgdsf4523" + i + "@icloud.com\"}\r\n";
            HttpEntity<String> body = new HttpEntity<>(user);
            restTemplate.postForEntity(BASE_URL + "/users/new", body, String.class);
        }
        for (int i = 0; i< 20000; i++) {
            String location = "{\"distance\": 1251, \"city\": \"Лиссагамаавыар-ыварыва-ава\", \"place\": \"Склонвыарыук\", \"id\": " + i + ", \"country\": \"Сейшелывпаыпмщумущупгм\"}\r\n";
            HttpEntity<String> body = new HttpEntity<>(location);
            restTemplate.postForEntity(BASE_URL + "/locations/new", body, String.class);
        }

        for (int i = 0; i< 20000; i++) {
            String visit1 = "{\"user\": " + i + ", \"location\": " + i + ", \"visited_at\": 109" + i + ", \"id\": " + i + ", \"mark\": 5}\r\n";
            String visit2 = "{\"user\": " + i + ", \"location\": " + i + ", \"visited_at\": 110" + i + ", \"id\": " + (30000 + i) + ", \"mark\": 1}\r\n";
            HttpEntity<String> body1 = new HttpEntity<>(visit1);
            HttpEntity<String> body2 = new HttpEntity<>(visit2);
            restTemplate.postForEntity(BASE_URL + "/visits/new", body1, String.class);
            restTemplate.postForEntity(BASE_URL + "/visits/new", body2, String.class);
        }
    }
}*/
