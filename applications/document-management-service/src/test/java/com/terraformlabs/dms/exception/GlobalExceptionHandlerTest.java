package com.terraformlabs.dms.exception;

import com.terraformlabs.dms.dto.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null") // body is asserted non-null before access; MethodParameter intentionally null in handler tests
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBadRequest_returns400WithMessage() {
        ResponseEntity<ApiError> response =
                handler.handleBadRequest(new BadRequestException("bad input"));

        ApiError body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("bad input");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void handleUnauthorized_returns401WithMessage() {
        ResponseEntity<ApiError> response =
                handler.handleUnauthorized(new UnauthorizedException("not allowed"));

        ApiError body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("not allowed");
    }

    @Test
    void handleNotFound_returns404WithMessage() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("not found"));

        ApiError body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("not found");
    }

    @Test
    void handleValidation_returns400WithFirstConstraintMessage() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors())
                .thenReturn(List.of(new ObjectError("field", "must not be blank")));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        ApiError body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("must not be blank");
    }

    @Test
    void handleValidation_returnsDefaultMessageWhenNoErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        ApiError body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Validation failed");
    }

    @Test
    void handleGeneric_returns500WithFixedMessage() {
        ResponseEntity<ApiError> response =
                handler.handleGeneric(new RuntimeException("something went wrong"));

        ApiError body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Unexpected server error");
    }
}
