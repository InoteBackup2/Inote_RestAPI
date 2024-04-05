package fr.inote.inoteApi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class InoteApiApplicationTests {

	/*
	 * The contextLoads() method is an empty test that simply checks if the
	 * application context can be started without errors. This is a basic sanity
	 * check that ensures your Spring application can be started and that all the
	 * necessary beans are wired correctly.
	 */
	@Test
	void contextLoads() {
	}

}
