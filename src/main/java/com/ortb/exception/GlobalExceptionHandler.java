package com.ortb.exception;

import com.ortb.model.openrtb.BidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BidResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Invalid bid request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BidResponse.noBid("unknown", 2));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BidResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed bid request JSON: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BidResponse.noBid("unknown", 2));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BidResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error processing bid request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BidResponse.noBid("unknown", 1));
    }
}
