package com.email.email_writer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;


@Service

public class EmailGeneratorService {
    private final WebClient webClient;
    private final String apiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder,
                                 @Value("${gemini.api.url}") String baseUrl,
                                 @Value("${gemini.api.key}") String geminiApiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = geminiApiKey;
    }


    public String generateEmailReply(EmailRequest emailRequest) throws Exception {

        // build a prompt
        String prompt = buildPromt(emailRequest);

        // build a JSON

//
//        String requestBody = String.format("""
//                {
//                    "model": "gemini-3.5-flash-lite",
//                    "input": "%s"
//                }""", prompt);

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode requestBody = mapper.createObjectNode();

        requestBody.put("model", "gemini-3.5-flash-lite");
        requestBody.put("input", prompt);


        //Send Request

        String response = webClient.post().uri(uriBuilder -> uriBuilder.path("/v1beta/interactions")
                        .build()).header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println(errorBody);
                                    return Mono.error(
                                            new RuntimeException(errorBody)
                                    );
                                })
                )
                .bodyToMono(String.class)
                .block();


        // extract response

        return extractResponseContent(response);
    }


    private String extractResponseContent(String response) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response);

        for (JsonNode step : root.path("steps")) {

            if ("model_output".equals(step.path("type").asText())) {

                for (JsonNode content : step.path("content")) {

                    if ("text".equals(content.path("type").asText())) {
                        return content.path("text").asText();
                    }
                }
            }
        }

        return "No text response found.";
    }

    private String buildPromt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("  You are helping Aryan Mishra write a reply to an email.\n" +
                "\n" +
                "            Write in a natural, clear, concise style that sounds like a real person.\n" +
                "            Be professional and friendly when appropriate. Follow the requested tone\n" +
                "            if one is provided; otherwise, use a polite, neutral tone.\n" +
                "\n" +
                "            Rules:\n" +
                "            - Preserve the original email's meaning and the reply's intended message.\n" +
                "            - Address the sender's important questions and requests.\n" +
                "            - Do not invent facts, personal details, dates, promises, experience, or commitments.\n" +
                "            - Do not add a job title, department, company, phone number, or email address\n" +
                "              unless it appears in the information provided.\n" +
                "            - Do not include generic placeholders such as [Your Name] or [Department].\n" +
                "            - Sign off as \"Aryan Mishra\" only when a sign-off fits the email.\n" +
                "            - Return only the email reply. Do not include analysis or a subject line.\n" +
                "\n" +
                "            Requested tone:\n" +
                "            %s\n" +
                "\n" +
                "            Original email:\n" +
                "            %s");

        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a").append(emailRequest.getTone()).append(" tone.");

        }
        prompt.append("Original email : \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}