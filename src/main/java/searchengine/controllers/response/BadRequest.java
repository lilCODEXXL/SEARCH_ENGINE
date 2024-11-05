package searchengine.controllers.response;

import lombok.Value;

@Value
public class BadRequest {
    boolean gotResult;
    String error;
}
