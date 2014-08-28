package com.petpet.c3po;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ FitsIntegrationTests.class, TikaIntegrationTests.class })
public class AllIntegrationTests {

}
