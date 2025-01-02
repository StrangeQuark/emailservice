// Integration file: Auth

package com.strangequark.emailservice.utility;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class AuthUtility {
    public static void enableUser(String email) {
        //Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //Create the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);

        //Compile the HttpEntity
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        String url = Boolean.parseBoolean(System.getenv("DOCKER_DEPLOYMENT")) ?
                "http://auth-app:6001/user/enableUser" : "http://localhost:6001/user/enableUser";

        new RestTemplate().postForObject(url, requestEntity, String.class);
    }
}
