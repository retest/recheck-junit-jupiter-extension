package de.retest.recheck.junit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import de.retest.recheck.RecheckLifecycle;

public class RecheckExtensionTest {

	private static final String testName = "testName";
	private ExtensionContext context;
	private RecheckDummy recheckDummy;

	private static class RecheckDummy {

		private RecheckLifecycle recheck;
		private Runnable someField;
	}

	@BeforeEach
	void beforeEach() {
		context = mock( ExtensionContext.class );
		recheckDummy = new RecheckDummy();
		recheckDummy.recheck = mock( RecheckLifecycle.class );
		recheckDummy.someField = mock( Runnable.class );
		when( context.getRequiredTestInstance() ).thenReturn( recheckDummy );
		when( context.getRequiredTestClass() ).then( i -> recheckDummy.getClass() );
		when( context.getDisplayName() ).thenReturn( testName );
	}

	@Test
	void startsTest() throws Exception {
		new RecheckExtension().beforeTestExecution( context );

		verify( recheckDummy.recheck ).startTest( testName );
	}

	@Test
	void capsTest() throws Exception {
		new RecheckExtension().afterTestExecution( context );

		verify( recheckDummy.recheck ).capTest();
	}

	@Test
	void capsAfterAll() throws Exception {
		new RecheckExtension().afterAll( context );

		verify( recheckDummy.recheck ).cap();
	}

	@Test
	void callsNothingOnOtherMembers() throws Exception {
		new RecheckExtension().afterAll( context );

		verifyZeroInteractions( recheckDummy.someField );
	}
}
