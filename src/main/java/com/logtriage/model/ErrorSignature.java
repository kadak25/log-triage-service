package com.logtriage.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorSignature {
    private String exceptionType;
    private String message;
    private int count;
}
