package com.quorum.tessera.api.model;

import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.Test;

public class OpenPojoTest {

    public OpenPojoTest() {
    }



    @Test
    public void executeOpenPojoValidations() {

        Validator pojoValidator = ValidatorBuilder.create()
                .with(new GetterMustExistRule())
                .with(new SetterMustExistRule())
                .with(new SetterTester())
                .with(new GetterTester())
                .build();

        pojoValidator.validate(getClass().getPackage().getName(),
            (pc) -> !pc.getClazz().isAssignableFrom(StoreRawRequest.class));

    }

    
}
