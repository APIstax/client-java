package io.apistax.client;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.apistax.models.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
public class TestAPIstaxClientImpl {

    @Test
    void testError(WireMockRuntimeInfo runtimeInfo) {
        var client = getClient(runtimeInfo);

        var response = WireMock.aResponse()
                .withBody("{\"messages\": [\"errorMessage\"]}")
                .withStatus(500);
        stub(runtimeInfo, () -> WireMock.post("/v1/geocode/search").willReturn(response));

        try {
            client.geocodeSearch("An address");
            fail("No APIstaxException thrown.");
        } catch (APIstaxException e) {
            assertNotNull(e.getMessages());
            assertEquals(1, e.getMessages().size());
            assertEquals("errorMessage", e.getMessages().get(0));
        }
    }

    @Test
    void testConvertHtmlToPdf(WireMockRuntimeInfo runtimeInfo) {
        var client = getClient(runtimeInfo);

        var response = WireMock.aResponse()
                .withBody("PDF" .getBytes(StandardCharsets.UTF_8))
                .withHeader("Content-Type", "application/pdf")
                .withStatus(200);

        var requestJson = "{" +
                "\"content\":\"content\"," +
                "\"header\":\"header\"," +
                "\"footer\":\"footer\"," +
                "\"width\":1.1," +
                "\"height\":1.2," +
                "\"marginTop\":1.3," +
                "\"marginBottom\":1.4," +
                "\"marginStart\":1.5," +
                "\"marginEnd\":1.6," +
                "\"landscape\":true," +
                "\"printBackground\":true" +
                "}";

        var mappingBuilder = WireMock.post("/v1/html-to-pdf")
                .withRequestBody(WireMock.equalToJson(requestJson))
                .willReturn(response);

        stub(runtimeInfo, () -> mappingBuilder);

        var result = client.convertHtmlToPdf(new HtmlPayload()
                .content("content")
                .header("header")
                .footer("footer")
                .width(1.1f)
                .height(1.2f)
                .marginTop(1.3f)
                .marginBottom(1.4f)
                .marginStart(1.5f)
                .marginEnd(1.6f)
                .landscape(true)
                .printBackground(true)
        );

        assertNotNull(result);
        assertArrayEquals("PDF" .getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void testGenerateEpcQrCode(WireMockRuntimeInfo runtimeInfo) {
        var client = getClient(runtimeInfo);

        var response = WireMock.aResponse()
                .withBody("EPC-QR-CODE" .getBytes(StandardCharsets.UTF_8))
                .withHeader("Content-Type", "image/png")
                .withStatus(200);

        var requestJson = "{" +
                "\"bic\":\"bic\"," +
                "\"iban\":\"iban\"," +
                "\"recipient\":\"recipient\"," +
                "\"currency\":\"currency\"," +
                "\"amount\":1.1," +
                "\"reference\":\"reference\"," +
                "\"text\":\"text\"," +
                "\"size\":500," +
                "\"frame\":true," +
                "\"message\":\"message\"" +
                "}";

        var mappingBuilder = WireMock.post("/v1/epc-qr-code")
                .withRequestBody(WireMock.equalToJson(requestJson))
                .willReturn(response);

        stub(runtimeInfo, () -> mappingBuilder);

        var result = client.generateEpcQrCode(new EpcQrCodePayload()
                .bic("bic")
                .iban("iban")
                .recipient("recipient")
                .currency("currency")
                .amount(1.1f)
                .reference("reference")
                .text("text")
                .size(500)
                .frame(true)
                .message("message")
        );

        assertNotNull(result);
        assertArrayEquals("EPC-QR-CODE" .getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void testVerifyVatId(WireMockRuntimeInfo runtimeInfo) {
        var client = getClient(runtimeInfo);

        var response = WireMock.aResponse()
                .withBody("{\"valid\":true,\"name\":\"name\",\"address\":\"address\",\"countryCode\":\"countryCode\"}")
                .withHeader("Content-Type", "application/json")
                .withStatus(200);

        var mappingBuilder = WireMock.post("/v1/vat-verification")
                .withRequestBody(WireMock.equalToJson("{\"vatId\": \"vatId\"}"))
                .willReturn(response);

        stub(runtimeInfo, () -> mappingBuilder);

        var result = client.verifyVatId(new VatVerificationPayload().vatId("vatId"));

        assertNotNull(result);
        assertEquals(true, result.getValid());
        assertEquals("name", result.getName());
        assertEquals("address", result.getAddress());
        assertEquals("countryCode", result.getCountryCode());
    }

    @Test
    void testGeocodeSearch(WireMockRuntimeInfo runtimeInfo) {
        var client = getClient(runtimeInfo);
        assertGeocodeResult(
                runtimeInfo,
                "/v1/geocode/search",
                mappingBuilder -> mappingBuilder.withRequestBody(WireMock.equalToJson("{\"query\":\"query\", \"language\": \"language\"}")),
                () -> client.geocodeSearch(new GeocodeSearchPayload().query("query").language("language"))
        );
    }

    @Test
    void testGeocodeReverse(WireMockRuntimeInfo runtimeInfo) {
        var client = getClient(runtimeInfo);
        assertGeocodeResult(
                runtimeInfo,
                "/v1/geocode/reverse",
                mappingBuilder -> mappingBuilder.withRequestBody(WireMock.equalToJson("{\"latitude\":1.1,\"longitude\":2.2, \"language\": \"language\"}")),
                () -> client.geocodeReverse(new GeocodeReversePayload().latitude(1.1).longitude(2.2).language("language"))
        );
    }

    private void assertGeocodeResult(WireMockRuntimeInfo runtimeInfo, String path, Consumer<MappingBuilder> mapping, Supplier<GeocodeResult> supplier) {
        var response = WireMock.aResponse()
                .withBody("{\"position\":{\"latitude\":1.1,\"longitude\":2.2},\"address\":{\"houseNumber\":\"houseNumber\",\"street\":\"street\",\"city\":\"city\",\"postalCode\":\"postalCode\",\"country\":\"country\",\"countryCode\":\"countryCode\"}})")
                .withHeader("Content-Type", "application/json")
                .withStatus(200);

        var mappingBuilder = WireMock.post(path).willReturn(response);
        mapping.accept(mappingBuilder);

        stub(runtimeInfo, () -> mappingBuilder);

        var result = supplier.get();

        assertNotNull(result);

        var position = result.getPosition();
        assertNotNull(position);
        assertEquals(1.1, position.getLatitude());
        assertEquals(2.2, position.getLongitude());

        var address = result.getAddress();
        assertNotNull(address);
        assertEquals("houseNumber", address.getHouseNumber());
        assertEquals("street", address.getStreet());
        assertEquals("city", address.getCity());
        assertEquals("postalCode", address.getPostalCode());
        assertEquals("country", address.getCountry());
        assertEquals("countryCode", address.getCountryCode());
    }

    @Test
    void testFetchIndex(WireMockRuntimeInfo runtimeInfo) {
        var client = getClient(runtimeInfo);

        var response = WireMock.aResponse()
                .withBody("{\"id\":\"at-cpi-1\",\"name\":\"name\",\"source\":\"source\",\"frequency\":\"YEARLY\",\"values\":[{\"year\":1,\"month\":2,\"value\":10.10}]}")
                .withHeader("Content-Type", "application/json")
                .withStatus(200);
        stub(runtimeInfo, () ->
                WireMock.get("/v1/indexes/at-cpi-1?frequency=YEARLY")
                        .willReturn(response)
        );

        var result = client.fetchIndex(Index.AT_CPI_1, IndexFrequency.YEARLY);

        assertNotNull(result);
        assertEquals("at-cpi-1", result.getId());
        assertEquals("name", result.getName());
        assertEquals("source", result.getSource());
        assertEquals(IndexFrequency.YEARLY, result.getFrequency());
        assertNotNull(result.getValues());
        assertEquals(1, result.getValues().size());

        var value = result.getValues().get(0);

        assertEquals(1, value.getYear());
        assertEquals(2, value.getMonth());
        assertEquals(10.10f, value.getValue());
    }

    private APIstaxClient getClient(WireMockRuntimeInfo runtimeInfo) {
        return new APIstaxClientImpl("API_KEY", runtimeInfo.getHttpBaseUrl());
    }

    private void stub(WireMockRuntimeInfo runtimeInfo, Supplier<MappingBuilder> supplier) {
        runtimeInfo.getWireMock()
                .register(supplier.get());
    }
}
