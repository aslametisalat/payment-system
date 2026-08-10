package com.payment.iso8583.service;

import com.payment.iso8583.dto.AuthorizationRequestData;
import com.payment.iso8583.dto.AuthorizationResponseData;
import com.payment.iso8583.model.ISO8583Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ISO 8583 is the message format every card network speaks: a Message Type
 * Indicator (MTI) plus a bitmap-addressed set of numbered fields. This is
 * the wire format POSTransactionService builds in "Step 4" of the flow.
 */
class ISO8583MessageBuilderTest {

    private final ISO8583MessageBuilder builder = new ISO8583MessageBuilder();

    private AuthorizationRequestData.AuthorizationRequestDataBuilder sampleRequestData() {
        return AuthorizationRequestData.builder()
                .pan("4111111111111111")
                .amountInCents(12345L)
                .expiryDate("2812")
                .posEntryMode("051")
                .stan("000123")
                .acquirerId("123456")
                .retrievalReferenceNumber("000000012345")
                .terminalId("TERM0001")
                .merchantId("MERCH0001")
                .merchantNameLocation("Coffee Shop / Austin US")
                .currencyCode("840");
    }

    @Test
    void buildAuthorizationRequest_mapsFieldsToTheirISO8583Positions() {
        ISO8583Message message = builder.buildAuthorizationRequest(sampleRequestData().build());

        assertThat(message.getMti()).isEqualTo("0100"); // Authorization Request
        assertThat(message.getField(2)).isEqualTo("4111111111111111"); // PAN
        assertThat(message.getField(3)).isEqualTo("000000"); // Processing code: purchase
        assertThat(message.getField(4)).isEqualTo("000000012345"); // Amount, 12 digits
        assertThat(message.getField(11)).isEqualTo("000123"); // STAN
        assertThat(message.getField(14)).isEqualTo("2812"); // Card expiry
        assertThat(message.getField(22)).isEqualTo("051"); // POS entry mode
        assertThat(message.getField(25)).isEqualTo("00"); // POS condition code
        assertThat(message.getField(32)).isEqualTo("123456"); // Acquiring institution
        assertThat(message.getField(37)).isEqualTo("000000012345"); // RRN
        assertThat(message.getField(41)).isEqualTo("TERM0001"); // Terminal ID
        assertThat(message.getField(42)).isEqualTo("MERCH0001"); // Merchant ID
        assertThat(message.getField(43)).isEqualTo("Coffee Shop / Austin US");
        assertThat(message.getField(49)).isEqualTo("840"); // Currency code
    }

    @Test
    void buildAuthorizationRequest_omitsOptionalFieldsWhenNotProvided() {
        ISO8583Message message = builder.buildAuthorizationRequest(sampleRequestData().build());

        assertThat(message.hasField(35)).isFalse(); // no track 2 data supplied
        assertThat(message.hasField(52)).isFalse(); // no PIN supplied
        assertThat(message.hasField(55)).isFalse(); // no EMV data supplied
    }

    @Test
    void buildAuthorizationRequest_includesPinAndEmvFieldsWhenProvided() {
        ISO8583Message message = builder.buildAuthorizationRequest(
                sampleRequestData()
                        .encryptedPIN("AABBCCDDEEFF0011")
                        .emvData("9F260812345678")
                        .track2Data("4111111111111111=2812101000000000000")
                        .build());

        assertThat(message.getField(35)).isEqualTo("4111111111111111=2812101000000000000");
        assertThat(message.getField(52)).isEqualTo("AABBCCDDEEFF0011");
        assertThat(message.getField(55)).isEqualTo("9F260812345678");
    }

    @Test
    void buildAuthorizationResponse_echoesRequestFieldsAndAddsTheResult() {
        ISO8583Message request = builder.buildAuthorizationRequest(sampleRequestData().build());

        ISO8583Message response = builder.buildAuthorizationResponse(request,
                AuthorizationResponseData.builder()
                        .responseCode("00")
                        .authorizationCode("654321")
                        .build());

        assertThat(response.getMti()).isEqualTo("0110"); // Authorization Response
        assertThat(response.getField(2)).isEqualTo(request.getField(2));
        assertThat(response.getField(11)).isEqualTo(request.getField(11));
        assertThat(response.getField(37)).isEqualTo(request.getField(37));
        assertThat(response.getField(41)).isEqualTo(request.getField(41));
        assertThat(response.getField(42)).isEqualTo(request.getField(42));
        assertThat(response.getField(38)).isEqualTo("654321"); // Approval code
        assertThat(response.getField(39)).isEqualTo("00"); // Approved
    }

    @Test
    void buildAuthorizationResponse_omitsApprovalCodeWhenDeclined() {
        ISO8583Message request = builder.buildAuthorizationRequest(sampleRequestData().build());

        ISO8583Message response = builder.buildAuthorizationResponse(request,
                AuthorizationResponseData.builder()
                        .responseCode("51") // insufficient funds
                        .authorizationCode(null)
                        .build());

        assertThat(response.getField(39)).isEqualTo("51");
        assertThat(response.hasField(38)).isFalse();
    }

    @Test
    void messageToString_startsWithMtiAndAFullBitmap() {
        ISO8583Message message = builder.buildAuthorizationRequest(sampleRequestData().build());

        String wireFormat = builder.messageToString(message);

        // MTI (4 chars) + bitmap (128 bits = 16 bytes = 32 hex chars)
        assertThat(wireFormat).startsWith("0100");
        assertThat(wireFormat.substring(4, 36)).matches("[0-9A-F]{32}");
        assertThat(wireFormat).contains("4111111111111111"); // PAN still present in the field data
    }
}
