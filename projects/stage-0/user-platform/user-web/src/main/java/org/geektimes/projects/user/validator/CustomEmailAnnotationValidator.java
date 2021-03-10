package org.geektimes.projects.user.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.util.DomainNameUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @Author: menglinggang
 * @Date: 2021-03-10
 * @Time: 7:59 下午
 */
public class CustomEmailAnnotationValidator implements ConstraintValidator<CustomEmail, String> {

    private Pattern pattern;

    private static final Pattern LOCAL_PART_PATTERN = Pattern.compile("([a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uffff-]+|\"([a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\u0080-\uffff-]|\\\\\\\\|\\\\\\\")+\")(\\.([a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uffff-]+|\"([a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\u0080-\uffff-]|\\\\\\\\|\\\\\\\")+\"))*", 2);

    @Override
    public void initialize(CustomEmail constraintAnnotation) {
        jakarta.validation.constraints.Pattern.Flag[] flags = constraintAnnotation.flags();
        int intFlag = 0;
        jakarta.validation.constraints.Pattern.Flag[] var4 = flags;
        int var5 = flags.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            jakarta.validation.constraints.Pattern.Flag flag = var4[var6];
            intFlag |= flag.getValue();
        }
        if (!".*".equals(constraintAnnotation.regexp()) || constraintAnnotation.flags().length > 0) {
            try {
                this.pattern = Pattern.compile(constraintAnnotation.regexp(), intFlag);
            } catch (PatternSyntaxException var8) {
                throw new RuntimeException(var8);
            }
        }

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        } else {
            boolean isValid = isValid(value.subSequence(0, value.length()), constraintValidatorContext);
            if (this.pattern != null && isValid) {
                Matcher m = this.pattern.matcher(value);
                return m.matches();
            } else {
                return isValid;
            }
        }
    }

    private boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value != null && value.length() != 0) {
            String stringValue = value.toString();
            int splitPosition = stringValue.lastIndexOf(64);
            if (splitPosition < 0) {
                return false;
            } else {
                String localPart = stringValue.substring(0, splitPosition);
                String domainPart = stringValue.substring(splitPosition + 1);
                return !this.isValidEmailLocalPart(localPart) ? false : DomainNameUtil.isValidEmailDomainAddress(domainPart);
            }
        } else {
            return true;
        }
    }

    private boolean isValidEmailLocalPart(String localPart) {
        if (localPart.length() > 64) {
            return false;
        } else {
            Matcher matcher = LOCAL_PART_PATTERN.matcher(localPart);
            return matcher.matches();
        }
    }

}
