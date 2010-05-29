package com.aptana.syncing.core.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite(AllTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(Local2LocalSyncingTest.class);
		suite.addTestSuite(Local2FTPSyncingTest.class);
		suite.addTestSuite(Local2SFTPSyncingTest.class);
		// $JUnit-END$
		return suite;
	}
}
