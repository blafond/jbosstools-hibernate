package org.jboss.tools.hibernate.runtime.v_6_0.internal.legacy;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;

public class TextType extends AbstractSingleColumnStandardBasicType<String> implements AdjustableBasicType<String> {
	public static final TextType INSTANCE = new TextType();

	public TextType() {
		super(LongVarcharJdbcType.INSTANCE, StringJavaTypeDescriptor.INSTANCE);
	}

	public String getName() {
		return "text";
	}
}
