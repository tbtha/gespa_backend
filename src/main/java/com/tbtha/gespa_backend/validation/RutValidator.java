package com.tbtha.gespa_backend.validation;

import com.tbtha.gespa_backend.utils.RutUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RutValidator implements ConstraintValidator<ValidRut, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return RutUtils.isValid(value);
    }
}
