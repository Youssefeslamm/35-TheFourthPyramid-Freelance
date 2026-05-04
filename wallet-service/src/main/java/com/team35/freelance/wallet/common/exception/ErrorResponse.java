package com.team35.freelance.wallet.common.exception;

public record ErrorResponse(
        int status,
        String message
) {}
