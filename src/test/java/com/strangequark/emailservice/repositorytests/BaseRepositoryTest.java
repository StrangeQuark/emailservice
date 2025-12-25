package com.strangequark.emailservice.repositorytests;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class BaseRepositoryTest {
    static {
        System.setProperty("ENCRYPTION_KEY", "8C636049C7763F06A35A17E86A542B15");
        System.setProperty("SERVICE_SECRET_EMAIL", "testClientPassword");
        System.setProperty("ACCESS_SECRET_KEY", "4C96564053ADF2405FA490EDE8DE779CA8568689F47BBBF63BE58313CE1C0531");
    }
}
