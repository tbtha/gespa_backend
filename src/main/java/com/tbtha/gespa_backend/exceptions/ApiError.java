package com.tbtha.gespa_backend.exceptions;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        String code,
        String message,
        List<String> details,
        OffsetDateTime timestamp
) {
}
