package org.jboss.tools.hibernate.runtime.v_6_2.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryDelegatingImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.type.Type;
import org.jboss.tools.hibernate.runtime.common.IFacade;
import org.jboss.tools.hibernate.runtime.spi.ISession;
import org.jboss.tools.hibernate.runtime.spi.IType;
import org.jboss.tools.hibernate.runtime.v_6_2.internal.legacy.StringType;
import org.jboss.tools.hibernate.runtime.v_6_2.internal.util.MockConnectionProvider;
import org.jboss.tools.hibernate.runtime.v_6_2.internal.util.MockDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClassMetadataFacadeTest {
	
	private static final FacadeFactoryImpl FACADE_FACTORY = new FacadeFactoryImpl();
	
	private ClassMetadata classMetadataTarget;
	private ClassMetadataFacadeImpl classMetadataFacade;
	
	@BeforeEach
	public void beforeEach() throws Exception {
		classMetadataTarget = setupFooBarPersister();
		classMetadataFacade = new ClassMetadataFacadeImpl(FACADE_FACTORY, classMetadataTarget);
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(classMetadataTarget);
		assertNotNull(classMetadataFacade);
	}
	
	@Test
	public void testGetEntityName() {
		assertEquals("foobar", classMetadataFacade.getEntityName());
	}
	
	@Test
	public void testGetIdentifierPropertyName() {
		assertEquals("foo", classMetadataFacade.getIdentifierPropertyName());
	}
	
	@Test
	public void testGetPropertyNames() {
		assertSame(PROPERTY_NAMES, classMetadataFacade.getPropertyNames());
	}
	
	@Test
	public void testGetPropertyTypes() {
		IType[] typeFacades = classMetadataFacade.getPropertyTypes();
		assertSame(TYPE_INSTANCE, ((IFacade)typeFacades[0]).getTarget());
 	}
	
	@Test
	public void testGetMappedClass() {
		assertSame(FooBar.class, classMetadataFacade.getMappedClass());
	}
	
	@Test
	public void testGetIdentifierType() {
		assertSame(TYPE_INSTANCE, ((IFacade)classMetadataFacade.getIdentifierType()).getTarget());
	}
	
	@Test
	public void testGetPropertyValue() {
		assertSame(PROPERTY_VALUE, classMetadataFacade.getPropertyValue(null, null));
	}
	
	@Test
	public void testHasIdentifierProperty() {
		assertFalse(classMetadataFacade.hasIdentifierProperty());
		((TestEntityPersister)classMetadataTarget).hasIdentifierProperty = true;
		assertTrue(classMetadataFacade.hasIdentifierProperty());
	}
	
	@Test 
	public void testGetIdentifier() {
		assertNull(((TestEntityPersister)classMetadataTarget).session);
		final SharedSessionContractImplementor sessionTarget = createSession();
		ISession sessionFacade = FACADE_FACTORY.createSession(sessionTarget);
		Object theObject = new Object();
		Object anotherObject = classMetadataFacade.getIdentifier(theObject, sessionFacade);
		assertSame(theObject, anotherObject);
		assertSame(sessionTarget, ((TestEntityPersister)classMetadataTarget).session);
	}
	
	@Test
	public void testIsInstanceOfAbstractEntityPersister() {
		assertTrue(classMetadataFacade.isInstanceOfAbstractEntityPersister());
		classMetadataTarget = (ClassMetadata)Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class[] { ClassMetadata.class }, 
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
				});	
		classMetadataFacade = new ClassMetadataFacadeImpl(FACADE_FACTORY, classMetadataTarget);
		assertFalse(classMetadataFacade.isInstanceOfAbstractEntityPersister());
	}
	
	@Test
	public void testGetTuplizerPropertyValue() {
		assertSame(PROPERTY_VALUE, classMetadataFacade.getTuplizerPropertyValue(null, 0));
	}
	
	@Test
	public void testGetPropertyIndexOrNull() {
		assertSame(0, classMetadataFacade.getPropertyIndexOrNull("bar"));
	}
	
	
	private ClassMetadata setupFooBarPersister() {
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		builder.applySetting(AvailableSettings.DIALECT, MockDialect.class.getName());
		builder.applySetting(AvailableSettings.CONNECTION_PROVIDER, MockConnectionProvider.class.getName());
		StandardServiceRegistry serviceRegistry = builder.build();		
		MetadataBuildingOptionsImpl metadataBuildingOptions = 
				new MetadataBuildingOptionsImpl(serviceRegistry);	
		BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
				serviceRegistry, 
				metadataBuildingOptions);
		metadataBuildingOptions.setBootstrapContext(bootstrapContext);
		InFlightMetadataCollector inFlightMetadataCollector = 
				new InFlightMetadataCollectorImpl(
						bootstrapContext,
						metadataBuildingOptions);
		MetadataBuildingContext metadataBuildingContext = 
				new MetadataBuildingContextRootImpl(
						"JBoss Tools",
						bootstrapContext, 
						metadataBuildingOptions, 
						inFlightMetadataCollector);
		AbstractEntityPersister result = new TestEntityPersister(
				createPersistentClass(metadataBuildingContext), 
				createPersisterCreationContext(
						serviceRegistry,
						bootstrapContext));
		return result;
	}
	
	private PersisterCreationContext createPersisterCreationContext(
			StandardServiceRegistry serviceRegisty,
			BootstrapContext bootstrapContext) {
		MetadataSources metadataSources = new MetadataSources(serviceRegisty);
		return new TestCreationContext(
				bootstrapContext, 
				(MetadataImplementor)metadataSources.buildMetadata());
	}
	
	private PersistentClass createPersistentClass(
			MetadataBuildingContext metadataBuildingContext) {
		RootClass rc = new RootClass(metadataBuildingContext);
		Table t = new Table("tools", "foobar");
		rc.setTable(t);
		Column c = new Column("foo");
		t.addColumn(c);
		ArrayList<Column> keyList = new ArrayList<>();
		keyList.add(c);
		t.createUniqueKey(keyList);
		BasicValue sv = new BasicValue(metadataBuildingContext, t);
		sv.setNullValue("null");
		sv.setTypeName(Integer.class.getName());
		sv.addColumn(c);
		rc.setEntityName("foobar");
		rc.setIdentifier(sv);
		rc.setClassName(FooBar.class.getName());
		rc.setOptimisticLockStyle(OptimisticLockStyle.NONE);
		Property p = new Property();
		p.setName("bar");
		p.setValue(sv);
		rc.addProperty(p);
		return rc;
	}
	
	private class TestCreationContext implements PersisterCreationContext, RuntimeModelCreationContext {
		
		private final BootstrapContext bootstrapContext;
		private final MetadataImplementor metadataImplementor;
		private final SessionFactoryImplementor sessionFactoryImplementor;
		
		TestCreationContext(
				BootstrapContext bootstrapContext,
				MetadataImplementor metadataImplementor) {
			this.bootstrapContext = bootstrapContext;
			this.metadataImplementor = metadataImplementor;
			this.sessionFactoryImplementor = new TestSessionFactory(
					(SessionFactoryImplementor)metadataImplementor.buildSessionFactory());
		}

		@Override
		public MetadataImplementor getBootModel() {
			return null;
		}

		@Override
		public MappingMetamodel getDomainModel() {
			return null;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactoryImplementor;
		}

		@Override
		public BootstrapContext getBootstrapContext() {
			return bootstrapContext;
		}

		@Override
		public MetadataImplementor getMetadata() {
			return metadataImplementor;
		}
		
	}
	
	
	private static final Object PROPERTY_VALUE = new Object();
	private static final String[] PROPERTY_NAMES = new String[] {};
	private static final Type TYPE_INSTANCE = new StringType();
	

	private static class TestEntityPersister extends SingleTableEntityPersister {
		
		private boolean hasIdentifierProperty = false;
		private SharedSessionContractImplementor session = null;
		
		public TestEntityPersister(
				PersistentClass persistentClass, 
				PersisterCreationContext creationContext) {
			super(persistentClass, null, null, creationContext);
		}
		
		@Override
		public Object getPropertyValue(Object object, String propertyName) {
			return PROPERTY_VALUE;
		}
		
		@Override
		public Object getPropertyValue(Object object, int index) {
			return PROPERTY_VALUE;
		}
		
		@Override
		public String getIdentifierPropertyName() {
			return "foo";
		}
		
		@Override
		public String[] getPropertyNames() {
			return PROPERTY_NAMES;
		}
		
		@Override
		public Type[] getPropertyTypes() {
			return new Type[] { TYPE_INSTANCE };
		}
		
		@Override
		public Type getIdentifierType() {
			return TYPE_INSTANCE;
 		}
		
		@Override
		public boolean hasIdentifierProperty() {
			return hasIdentifierProperty;
		}
		
		@Override
		public Object getIdentifier(Object object, SharedSessionContractImplementor s) {
			session = s;
			return object;
		}
		
	}
	
	private class TestSessionFactory extends SessionFactoryDelegatingImpl {
		public TestSessionFactory(SessionFactoryImplementor delegate) { super(delegate); }
		@Override public Generator getGenerator(String rootEntityName) { return new TestGenerator(); }	
	}
	
	private class TestGenerator implements Generator {
		@Override public boolean generatedOnExecution() { return false; }
		@Override public EnumSet<EventType> getEventTypes() { return null; }	
	}
	
	public static SessionImplementor createSession() {
		return (SessionImplementor)Proxy.newProxyInstance(
				ClassMetadataFacadeTest.class.getClassLoader(), 
				new Class[] { SessionImplementor.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
		});
	}
	
	public class FooBar {
		public int id = 1967;
		public int getBar() {
			return 0;
		}
		public void setBar(int b) {}
	}
	
}
