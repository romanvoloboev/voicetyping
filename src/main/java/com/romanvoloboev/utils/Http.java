package com.romanvoloboev.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Service
public class Http {

    private RestTemplate restTemplate;
    private HttpHeaders headers;

    @Value("${text.userkey}")
    private String userkey;

    @Value("${text.url}")
    private String url;


    public Http() {
        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request,body);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        });
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Collections.singletonList(new MediaType("text", "json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET)));
        jsonConverter.setPrettyPrint(true);
        restTemplate.getMessageConverters().add(jsonConverter);


        headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
    }

    public ResultResponseDTO sendText(String text) {
//        MultiValueMap<String, String> reqBody = new LinkedMultiValueMap<>(2);
//        reqBody.add("userkey", userkey);
//        reqBody.add("text", text);
//        HttpEntity<MultiValueMap> entity = new HttpEntity<>(reqBody, headers);
//        ResponseEntity<SendResponseDTO> sendResponseDTOResponseEntity = restTemplate.postForEntity(url, entity, SendResponseDTO.class);
//        String text_uid = sendResponseDTOResponseEntity.getBody().getText_uid();
//
//        log.info("res: {}", text_uid);

        MultiValueMap<String, String> resultBody = new LinkedMultiValueMap<>(3);
        resultBody.add("userkey", userkey);
        resultBody.add("uid", "5a40023d5dca7");
        resultBody.add("jsonvisible", "detail");
        HttpEntity<MultiValueMap> resEntity = new HttpEntity<>(resultBody, headers);

        ResponseEntity<String> resultResponseDTOResponseEntity;
        String json;
        ObjectMapper objectMapper = new ObjectMapper();
        ResultResponseDTO res = null;
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            resultResponseDTOResponseEntity = restTemplate.postForEntity(url, resEntity, String.class);
            json = resultResponseDTOResponseEntity.getBody();
            json = json.replaceAll("\\\"\\{\\\\", "{");
            json = json.replaceAll("\\\\\"", "\"");
            json = json.replaceAll("\"spell_check\":\\\"\\[\\{", "\"spell_check\":[{");
            json = json.replaceAll("]}\"", "]}");
            json = json.replaceAll("\"spell_check\":\"\"", "\"spell_check\":[]");
            json = json.replaceAll("\"spell_check\":\"\\[]\"", "\"spell_check\":[]");
            json = json.replaceAll("\"seo_check\":\"null\"", "\"seo_check\":{}");
            json = json.replaceAll("]\",\"seo_check\":\\{\"", "],\"seo_check\":{\"");
            json = StringEscapeUtils.unescapeJava(json);
            try {
                res = objectMapper.readValue(json, ResultResponseDTO.class);
                String jsonRes = new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(res);
                log.info("RAW JSON PROCESSING RESULT: {}", jsonRes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (res.getError_code() != null);
        return res;
    }

}
