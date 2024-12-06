package net.runelite.client.plugins.flippingcopilot.model;

import java.io.IOException;
import lombok.Getter;

@Getter
public class HttpResponseException extends IOException {
    private final int responseCode;
    private final String responseMessage;

    public HttpResponseException(int responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
        this.responseMessage = message;
    }

}