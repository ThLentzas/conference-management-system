package com.example.conference_management_system.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;

public final class WebUtils {
    private  WebUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("WebUtils is a utility class and cannot be instantiated");
    }

    /*
        The cookie in the response Header(SET_COOKIE) is in the form of
        SESSION=OTU2ODllODktYjZhMS00YmUxLTk1NGEtMDk0ZTBmODg0Mzhm; Path=/; HttpOnly; SameSite=Lax

        By splitting with ";" we get the first value which then we set it in the Cookie request header. The expected
        value is SESSION= plus some value.
     */
    public static String getSessionId(HttpHeaders headers) {
        String sessionCookie = headers.getFirst(HttpHeaders.SET_COOKIE);

        return sessionCookie.split(";")[0];
    }

    /*
        The cookie in the response Header(SET_COOKIE) is in the form of
        XSRF-TOKEN=546c0cc0-c895-4c9f-80df-06f589ce3378; Path=/

        We are using a double submit cookie pattern. The csrfCookie is XSRF-TOKEN=546c0cc0-c895-4c9f-80df-06f589ce3378
        which will be sent as a Cookie request header, and also we need to send an X-XSRF-TOKEN with the value of the
        cookie. Splitting by "=" XSRF-TOKEN=546c0cc0-c895-4c9f-80df-06f589ce3378 gives us the value
        546c0cc0-c895-4c9f-80df-06f589ce3378, then we can set the value.
     */
    public static Map<String, String> getCsrfToken(HttpHeaders headers) {
        Map<String, String> responseHeaders = new HashMap<>();
        String csrfCookie = headers.getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
        String csrfToken = csrfCookie.split("=")[1];
        responseHeaders.put("csrfCookie", csrfCookie);
        responseHeaders.put("csrfToken", csrfToken);

        return responseHeaders;
    }
}
