package uz.learnx.learnxapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * The envelope every endpoint answers with. Errors carry a stable, machine-readable {@code meta.code} the UI
 * translates; messages are English and meant for developers.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private HttpStatus status;
    private String message;
    @Builder.Default
    private Map<String, Object> meta = new LinkedHashMap<>();
    private Object data;
    private Long elements;
    private Integer pages;

    public static ApiResponse ok(Object data) {
        return ApiResponse.builder().status(HttpStatus.OK).message("success").data(data).build();
    }

    public static ApiResponse ok() {
        return ok(null);
    }

    public static ApiResponse created(Object data) {
        return ApiResponse.builder().status(HttpStatus.CREATED).message("success").data(data).build();
    }

    public static ApiResponse accepted() {
        return ApiResponse.builder().status(HttpStatus.ACCEPTED).message("success").build();
    }

    public static ApiResponse error(HttpStatus status, String code, String message) {
        ApiResponse response = ApiResponse.builder().status(status).message(message).build();
        response.getMeta().put("code", code);
        return response;
    }

    /** Leaves out an empty meta, so success responses stay small. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getMeta() {
        return meta;
    }
}
