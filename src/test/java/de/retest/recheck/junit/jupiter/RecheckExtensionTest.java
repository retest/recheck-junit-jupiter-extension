package de.retest.recheck.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.execution.DefaultTestInstances;

import de.retest.recheck.RecheckLifecycle;

class RecheckExtensionTest {

	private static final String displayName = "testName()";
	private static final String testName = "testName";

	private ExtensionContext context;
	private RecheckTest receheckTest;
	private RecheckExtension extension;

	private static class RecheckTest {

		private RecheckLifecycle recheck;
		private Runnable someField;

	}

	private static class EmptyTest {

	}

	@BeforeEach
	void beforeEach() {
		context = mock( ExtensionContext.class );
		receheckTest = new RecheckTest();
		receheckTest.recheck = mock( RecheckLifecycle.class );
		receheckTest.someField = mock( Runnable.class );
		configure( receheckTest );
		when( context.getDisplayName() ).thenReturn( displayName );

		extension = new RecheckExtension();
	}

	private void configure( final Object testInstance ) {
		when( context.getRequiredTestInstance() ).thenReturn( testInstance );
		when( context.getRequiredTestClass() ).then( i -> testInstance.getClass() );
	}

	@Test
	void startsTest() throws Exception {
		extension.beforeTestExecution( context );

		verify( receheckTest.recheck ).startTest( testName );
	}

	@Test
	void capsTest() throws Exception {
		extension.afterTestExecution( context );

		verify( receheckTest.recheck ).capTest();
	}

	@Test
	void capsAfterAll() throws Exception {
		addJUnitAfterAllBehaviour();
		final TestInstances instances = DefaultTestInstances.of( receheckTest );
		when( context.getTestInstances() ).thenReturn( Optional.of( instances ) );

		extension.afterAll( context );

		verify( receheckTest.recheck ).cap();
	}

	private void addJUnitAfterAllBehaviour() {
		doThrow( IllegalStateException.class ).when( context ).getRequiredTestInstance();
	}

	@Test
	void callsNothingOnOtherMembers() throws Exception {
		extension.beforeTestExecution( context );
		extension.afterTestExecution( context );
		extension.afterAll( context );

		verifyNoInteractions( receheckTest.someField );
	}

	@Test
	void doesNotFailOnEmptyTest() throws Exception {
		assertThatCode( () -> {
			configure( new EmptyTest() );

			extension.beforeTestExecution( context );
			extension.afterTestExecution( context );
			extension.afterAll( context );
		} ).doesNotThrowAnyException();
	}

	@Test
	void doesNotFailOnCapTestAssertionError() throws Exception {
		doThrow( new AssertionError() ).when( receheckTest.recheck ).capTest();

		assertThatCode( () -> extension.afterTestExecution( context ) ).isInstanceOf( AssertionError.class );

		verify( receheckTest.recheck ).cap();
	}
}
