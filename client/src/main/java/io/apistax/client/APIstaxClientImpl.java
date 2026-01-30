package io.apistax.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import io.apistax.models.*;
import io.mikael.urlbuilder.UrlBuilder;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.ByteArrayOutputStream;
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

        httpClient = Methanol.create();

        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
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
        return requestBinary("/v1/html-to-pdf", new JsonBodyProvider(payload, objectMapper), "application/pdf");
    }

    @Override
    public byte[] convertHtmlToPdf(String content) throws APIstaxException {
        return convertHtmlToPdf(new HtmlPayload().content(content));
    }

    @Override
    public byte[] generateEpcQrCode(EpcQrCodePayload payload) throws APIstaxException {
        return requestBinary("/v1/epc-qr-code", new JsonBodyProvider(payload, objectMapper), "image/png");
    }

    @Override
    public byte[] generateEpcQrCode(String iban, String recipient) throws APIstaxException {
        return generateEpcQrCode(new EpcQrCodePayload().iban(iban).recipient(recipient));
    }

    @Override
    public VatVerificationResult verifyVatId(VatVerificationPayload payload) throws APIstaxException {
        return requestJson("/v1/vat-verification", new JsonBodyProvider(payload, objectMapper), VatVerificationResult.class);
    }

    @Override
    public VatVerificationResult verifyVatId(String vatId) throws APIstaxException {
        return verifyVatId(new VatVerificationPayload().vatId(vatId));
    }

    @Override
    public GeocodeResult geocodeSearch(GeocodeSearchPayload payload) throws APIstaxException {
        return requestJson("/v1/geocode/search", new JsonBodyProvider(payload, objectMapper), GeocodeResult.class);
    }

    @Override
    public GeocodeResult geocodeSearch(String query) throws APIstaxException {
        return geocodeSearch(new GeocodeSearchPayload().query(query));
    }

    @Override
    public GeocodeResult geocodeReverse(GeocodeReversePayload payload) throws APIstaxException {
        return requestJson("/v1/geocode/reverse", new JsonBodyProvider(payload, objectMapper), GeocodeResult.class);
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

        if (format == SwissQrInvoiceFormat.SVG) {
            accept = "image/svg+xml";
        } else if (format == SwissQrInvoiceFormat.PNG) {
            accept = "image/png";
        }

        return requestBinary("/v1/swiss-qr-invoice", new JsonBodyProvider(payload, objectMapper), accept);
    }

    @Override
    public byte[] generateInvoicePdf(InvoicePayload payload) throws APIstaxException {
        return requestBinary("/v2/invoice-pdf", new JsonBodyProvider(payload, objectMapper), "application/pdf");
    }

    @Override
    public byte[] generateBarcode(BarcodePayload payload) throws APIstaxException {
        return requestBinary("/v1/barcode", new JsonBodyProvider(payload, objectMapper), "image/*");
    }

    @Override
    public byte[] convertPdfToPdfA(InputStream file) throws APIstaxException {
        return requestBinary("/v1/pdf-to-pdf-a", new FileBodyProvider(file), "application/pdf");
    }

    @Override
    public byte[] generateSpaydQrCode(SpaydQrCodePayload payload) throws APIstaxException {
        return requestBinary("/v1/spayd-qr-code", new JsonBodyProvider(payload, objectMapper), "image/png");
    }

    @Override
    public byte[] generateHctQrCode(HctQrCodePayload payload) throws APIstaxException {
        return requestBinary("/v1/hct-qr-code", new JsonBodyProvider(payload, objectMapper), "image/png");
    }

    @Override
    public byte[] generatePayBySquareQrCode(PayBySquareQrCodePayload payload) throws APIstaxException {
        return requestBinary("/v1/pay-by-square-qr-code", new JsonBodyProvider(payload, objectMapper), "image/png");
    }

    @Override
    @Deprecated
    public byte[] generateInvoicePdfV1(InvoicePayloadV1 payload) throws APIstaxException {
        return requestBinary("/v1/invoice-pdf", new JsonBodyProvider(payload, objectMapper), "application/pdf");
    }

    private byte[] requestBinary(String path, BodyProvider body, String accept) {
        return request(path, body, accept, null, inputStream -> {
            try {
                var outputStream = new ByteArrayOutputStream();

                var buffer = new byte[1024 * 4];
                int n;

                while (-1 != (n = inputStream.read(buffer))) {
                    outputStream.write(buffer, 0, n);
                }

                return outputStream.toByteArray();
            } catch (IOException e) {
                throw new APIstaxException(e);
            }
        });
    }

    private <T> T requestJson(String path, BodyProvider body, Class<T> type) {
        return requestJson(path, body, null, type);
    }

    private <T> T requestJson(String path, Map<String, String> query, Class<T> type) {
        return requestJson(path, null, query, type);
    }

    private <T> T requestJson(String path, BodyProvider body, Map<String, String> query, Class<T> type) {
        return request(path, body, "application/json", query, inputStream -> {
            try {
                return objectMapper.readValue(inputStream, type);
            } catch (IOException e) {
                throw new APIstaxException(e);
            }
        });
    }

    private <T> T request(String path, BodyProvider body, String accept, Map<String, String> query, Function<InputStream, T> mapper) {
        try {
            var request = createRequestBuilder(path, body, accept, query).build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() > 299) {
                try {
                    var errorMessage = objectMapper.readValue(response.body(), ErrorMessage.class);
                    throw new APIstaxException(errorMessage.getMessages());
                } catch (IOException e) {
                    if (response.statusCode() == 401) {
                        throw new APIstaxException(List.of("message.forbidden"), e);
                    }

                    throw new APIstaxException(List.of("message.unknownError"), e);
                }
            }

            try (var inputStream = response.body()) {
                return mapper.apply(inputStream);
            }
        } catch (IOException e) {
            throw new APIstaxException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new APIstaxException(e);
        }
    }

    private HttpRequest.Builder createRequestBuilder(String path, BodyProvider body, String accept, Map<String, String> query) throws IOException {
        var builder = UrlBuilder.fromString(host + path);

        if (query != null && !query.isEmpty()) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                builder = builder.addParameter(entry.getKey(), entry.getValue());
            }
        }

        var requestBuilder = HttpRequest.newBuilder();
        requestBuilder.uri(builder.toUri());
        requestBuilder.header("Authorization", "Bearer " + apiKey);
        requestBuilder.header("User-Agent", "apistax-java-client " + BuildConfig.VERSION);

        if (accept != null) {
            requestBuilder.header("Accept", accept);
        }

        if (body != null) {
            requestBuilder.header("Content-Type", body.getContentType());
            requestBuilder.POST(body.getBodyPublisher());
        } else {
            requestBuilder.GET();
        }

        return requestBuilder;
    }

    private interface BodyProvider {

        String getContentType();

        HttpRequest.BodyPublisher getBodyPublisher() throws IOException;
    }

    private static class JsonBodyProvider implements BodyProvider {

        private final Object payload;
        private final ObjectMapper objectMapper;

        public JsonBodyProvider(Object payload, ObjectMapper objectMapper) {
            this.payload = payload;
            this.objectMapper = objectMapper;
        }

        @Override
        public String getContentType() {
            return "application/json";
        }

        @Override
        public HttpRequest.BodyPublisher getBodyPublisher() throws IOException {
            var bodyData = objectMapper.writeValueAsBytes(payload);
            return HttpRequest.BodyPublishers.ofByteArray(bodyData);
        }
    }

    private static class FileBodyProvider implements BodyProvider {

        private final InputStream stream;

        public FileBodyProvider(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public String getContentType() {
            return "multipart/form-data";
        }

        @Override
        public HttpRequest.BodyPublisher getBodyPublisher() {
            var publisher = HttpRequest.BodyPublishers.ofInputStream(() -> stream);

            return MultipartBodyPublisher.newBuilder()
                    .formPart("file", "document.pdf", publisher, MediaType.APPLICATION_OCTET_STREAM)
                    .build();
        }
    }
}
