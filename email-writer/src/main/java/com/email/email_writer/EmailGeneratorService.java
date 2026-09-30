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
        prompt.append(" You are a professional AI email-writing assistant.\n" +
                "\n" +
                "            Analyze the original email and generate a natural, context-aware reply.\n" +
                "\n" +
                "            REQUIREMENTS:\n" +
                "            - Reply directly to the original email.\n" +
                "            - Preserve the meaning and intent of the conversation.\n" +
                "            - Use the requested tone: %s.\n" +
                "            - Be professional and human-like.\n" +
                "            - Keep the reply concise unless the original email requires more detail.\n" +
                "            - Address every important question or request from the sender.\n" +
                "            - Do not invent information or make unsupported commitments.\n" +
                "            - Do not change facts from the original email.\n" +
                "            - Do not include a subject line.\n" +
                "            - Do not include multiple versions.\n" +
                "            - Do not provide explanations or commentary.\n" +
                "            - Do not say \"Here is the email\" or similar phrases.\n" +
                "            - Do not use placeholders unless they exist in the original email.\n" +
                "            - Return ONLY the final email reply in plain text.\n" +
                "\n" +
                "            ORIGINAL EMAIL:\n" +
                "            %s\n" +
                "\n" +
                "            Generate the final reply now.");

        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a").append(emailRequest.getTone()).append(" tone.");

        }
        prompt.append("Original email : \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}