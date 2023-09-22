package io.apistax.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.apistax.models.*;
import io.mikael.urlbuilder.UrlBuilder;
import jdk.jfr.ContentType;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class APIstaxClientImpl implements APIstaxClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String host;
    private final String apiKey;

    public APIstaxClientImpl(String apiKey, String host) {
        this.apiKey = apiKey;
        this.host = host;

        httpClient = HttpClient.newHttpClient();

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new JsonNullableModule());
    }

    @Override
    public byte[] convertHtmlToPdf(HtmlPayload payload) throws APIstaxException {
        return requestBinary("/v1/html-to-pdf", payload, "application/pdf");
    }

    @Override
    public byte[] convertHtmlToPdf(String content) throws APIstaxException {
        return convertHtmlToPdf(new HtmlPayload().content(content));
    }

    @Override
    public byte[] generateEpcQrCode(EpcQrCodePayload payload) throws APIstaxException {
        return requestBinary("/v1/epc-qr-code", payload, "image/png");
    }

    @Override
    public byte[] generateEpcQrCode(String iban, String recipient) throws APIstaxException {
        return generateEpcQrCode(new EpcQrCodePayload().iban(iban).recipient(recipient));
    }

    @Override
    public VatVerificationResult verifyVatId(VatVerificationPayload payload) throws APIstaxException {
        return requestJson("/v1/vat-verification", payload, VatVerificationResult.class);
    }

    @Override
    public VatVerificationResult verifyVatId(String vatId) throws APIstaxException {
        return verifyVatId(new VatVerificationPayload().vatId(vatId));
    }

    @Override
    public GeocodeResult geocodeSearch(GeocodeSearchPayload payload) throws APIstaxException {
        return requestJson("/v1/geocode/search", payload, GeocodeResult.class);
    }

    @Override
    public GeocodeResult geocodeSearch(String query) throws APIstaxException {
        return geocodeSearch(new GeocodeSearchPayload().query(query));
    }

    @Override
    public GeocodeResult geocodeReverse(GeocodeReversePayload payload) throws APIstaxException {
        return requestJson("/v1/geocode/reverse", payload, GeocodeResult.class);
    }

    @Override
    public GeocodeResult geocodeReverse(double latitude, double longitude) throws APIstaxException {
        return geocodeReverse(new GeocodeReversePayload().latitude(latitude).longitude(longitude));
    }

    @Override
    public IndexResult fetchIndex(Index index, IndexFrequency frequency) throws APIstaxException {
        var query = Collections.singletonMap("frequency", frequency.getValue());
        return requestJson("/v1/indexes/" + index.getValue(), query, IndexResult.class);
    }

    @Override
    public byte[] generateSwissQrInvoice(SwissQrInvoicePayload payload) throws APIstaxException {
        return generateSwissQrInvoice(payload, null);
    }

    @Override
    public byte[] generateSwissQrInvoice(SwissQrInvoicePayload payload, SwissQrInvoiceFormat format) throws APIstaxException {
        String accept = "application/pdf";

        if(format == SwissQrInvoiceFormat.SVG) {
            accept = "image/svg+xml";
        } else if(format == SwissQrInvoiceFormat.PNG) {
            accept = "image/png";
        }

        return requestBinary("/v1/swiss-qr-invoice", payload, accept);
    }

    private byte[] requestBinary(String path, Object body, String accept) {
        return request(path, body, accept, null, inputStream -> {
            try {
                return inputStream.readAllBytes();
            } catch (IOException e) {
                throw new APIstaxException(e);
            }
        });
    }

    private <T> T requestJson(String path, Object body, Class<T> type) {
        return requestJson(path, body, null, type);
    }

    private <T> T requestJson(String path, Map<String, String> query, Class<T> type) {
        return requestJson(path, null, query, type);
    }

    private <T> T requestJson(String path, Object body, Map<String, String> query, Class<T> type) {
        return request(path, body, "application/json", query, inputStream -> {
            try {
                return objectMapper.readValue(inputStream, type);
            } catch (IOException e) {
                throw new APIstaxException(e);
            }
        });
    }

    private <T> T request(String path, Object body, String accept, Map<String, String> query, Function<InputStream, T> mapper) {
        try {
            var request = createRequestBuilder(path, body, accept, query).build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() > 299) {
                try {
                    var errorMessage = objectMapper.readValue(response.body(), ErrorMessage.class);
                    throw new APIstaxException(errorMessage.getMessages());
                } catch (IOException e) {
                    throw new APIstaxException(List.of("message.unknownError"));
                }
            }

            return mapper.apply(response.body());
        } catch (IOException e) {
            throw new APIstaxException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new APIstaxException(e);
        }
    }

    private HttpRequest.Builder createRequestBuilder(String path, Object body, String accept, Map<String, String> query) {
        try {
            var builder = UrlBuilder.fromString(host + path);

            if (query != null && !query.isEmpty()) {
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    builder = builder.addParameter(entry.getKey(), entry.getValue());
                }
            }

            var requestBuilder = HttpRequest.newBuilder();
            requestBuilder.uri(builder.toUri());
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.header("Authorization", "Bearer " + apiKey);
            requestBuilder.header("User-Agent", "apistax-java-client");

            if(accept != null) {
                requestBuilder.header("Accept", accept);
            }

            if (body != null) {
                var bodyData = objectMapper.writeValueAsBytes(body);
                requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(bodyData));
            } else {
                requestBuilder.GET();
            }

            return requestBuilder;
        } catch (IOException e) {
            throw new APIstaxException(e);
        }
    }
}
