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
	private RecheckExtension extension;

	private static class RecheckDummy {

		private RecheckLifecycle recheck;
		private Runnable someField;
	}

	private static class EmptyTest {

	}

	@BeforeEach
	void beforeEach() {
		context = mock( ExtensionContext.class );
		recheckDummy = new RecheckDummy();
		recheckDummy.recheck = mock( RecheckLifecycle.class );
		recheckDummy.someField = mock( Runnable.class );
		configure( recheckDummy );
		when( context.getDisplayName() ).thenReturn( testName );

		extension = new RecheckExtension();
	}

	private void configure( final Object testInstance ) {
		when( context.getRequiredTestInstance() ).thenReturn( testInstance );
		when( context.getRequiredTestClass() ).then( i -> testInstance.getClass() );
	}

	@Test
	void startsTest() throws Exception {
		extension.beforeTestExecution( context );

		verify( recheckDummy.recheck ).startTest( testName );
	}

	@Test
	void capsTest() throws Exception {
		extension.afterTestExecution( context );

		verify( recheckDummy.recheck ).capTest();
	}

	@Test
	void capsAfterAll() throws Exception {
		extension.afterAll( context );

		verify( recheckDummy.recheck ).cap();
	}

	@Test
	void callsNothingOnOtherMembers() throws Exception {
		extension.beforeTestExecution( context );
		extension.afterTestExecution( context );
		extension.afterAll( context );

		verifyZeroInteractions( recheckDummy.someField );
	}

	@Test
	void doesNotFailOnEmptyTest() throws Exception {
		configure( new EmptyTest() );

		extension.beforeTestExecution( context );
		extension.afterTestExecution( context );
		extension.afterAll( context );
	}
}
