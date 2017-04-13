package org.jboss.tools.hibernate.runtime.v_5_2;

import org.junit.Assert;
import org.junit.Test;

public class VersionTest {
	
	@Test
	public void testToolsVersion() {
		Assert.assertEquals("5.2.3.Final", org.hibernate.tool.Version.VERSION);
	}

	@Test
	public void testCoreVersion() {
		Assert.assertEquals("5.2.9.Final", org.hibernate.Version.getVersionString());
	}

}
