package org.jboss.tools.hibernate.orm.runtime.exp.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.spi.QueryImplementor;
import org.jboss.tools.hibernate.runtime.common.IFacade;
import org.jboss.tools.hibernate.runtime.common.IFacadeFactory;
import org.jboss.tools.hibernate.runtime.spi.ICriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

public class SessionFacadeTest {
	
	private static IFacadeFactory FACADE_FACTORY = new FacadeFactoryImpl();
	
	private static final String ENTITY_NAME = "entity_name";
	private static final SessionFactoryImplementor SESSION_FACTORY = createSessionFactory();
	private static final QueryImplementor<?> QUERY_IMPLEMENTOR = createQueryImplementor();
	private static final Root<?> ROOT = createRoot();
	
	private Session sessionTarget = null;
	private SessionFacadeImpl sessionFacade = null;
	
	@BeforeEach
	public void beforeEach() {
		sessionTarget = new TestSession();
		sessionFacade = new SessionFacadeImpl(FACADE_FACTORY, sessionTarget);
	}
	
	@Test
	public void testGetEntityName() {
		assertSame(ENTITY_NAME, sessionFacade.getEntityName(new Object()));
	}
	
	@Test
	public void testGetSessionFactory() {
		assertSame(SESSION_FACTORY, ((IFacade)sessionFacade.getSessionFactory()).getTarget());
	}
	
	@Test
	public void testCreateQuery() {
		assertSame(QUERY_IMPLEMENTOR, ((IFacade)sessionFacade.createQuery("foobar")).getTarget());
	}
	
	@Test
	public void testIsOpen() {
		assertFalse(sessionFacade.isOpen());
		((TestSession)sessionTarget).isOpen = true;
		assertTrue(sessionFacade.isOpen());		
	}
	
	@Test
	public void testClose() {
		((TestSession)sessionTarget).isOpen = true;
		sessionFacade.close();
		assertFalse(((TestSession)sessionTarget).isOpen);
	}
	
	@Test
	public void testContains() {
		assertFalse(sessionFacade.contains("foo"));
		assertTrue(sessionFacade.contains("someFakeEntity"));
		assertFalse(sessionFacade.contains("anotherFakeEntity"));
		try { 
			sessionFacade.contains("bar");
		} catch (IllegalArgumentException e) {
			assertEquals("illegal", e.getMessage());
		}
	}
	
	@Test
	public void testCreateCriteria() {
		assertNull(((TestSession)sessionTarget).criteriaBuilder);
		assertNull(((TestSession)sessionTarget).fromClass);
		assertNull(((TestSession)sessionTarget).root);
		assertNull(((TestSession)sessionTarget).criteriaQuery);
		ICriteria criteria = sessionFacade.createCriteria(Object.class);
		assertNotNull(((TestSession)sessionTarget).criteriaBuilder);
		assertSame(Object.class, ((TestSession)sessionTarget).fromClass);
		assertSame(ROOT, ((TestSession)sessionTarget).root);
		assertNotNull(((TestSession)sessionTarget).criteriaQuery);
		assertSame(QUERY_IMPLEMENTOR, ((IFacade)criteria).getTarget());
	}
	
	private static class TestSession extends SessionDelegatorBaseImpl {

		private static final long serialVersionUID = 1L;
		
		private static SessionImplementor createDelegate() {
			return (SessionImplementor)Proxy.newProxyInstance(
					TestSession.class.getClassLoader(),
					new Class[] { SessionImplementor.class }, 
					new InvocationHandler() {						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							return null;
						}
					});
		}
		
		private boolean isOpen = false;
		private HibernateCriteriaBuilder criteriaBuilder = null;
		private CriteriaQuery<?> criteriaQuery = null;
		private Class<?> fromClass = null;
		private Root<?> root = null;
		
		public TestSession() {
			super(createDelegate());
		}
		
		@Override
		public String getEntityName(Object o) {
			return ENTITY_NAME;
		}
		
		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return SESSION_FACTORY;
		}
		
		@Override
		public QueryImplementor<?> createQuery(String queryString) {
			return QUERY_IMPLEMENTOR;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public QueryImplementor<?> createQuery(CriteriaQuery criteria) {
			criteriaQuery = criteria;
			return QUERY_IMPLEMENTOR;
		}
		
		@Override
		public boolean isOpen() {
			return isOpen;
		}
		
		@Override
		public void close() {
			isOpen = false;
		}
		
		@Override
		public boolean contains(Object object) {
			if (object.equals("foo")) {
				throw new IllegalArgumentException("Not an entity [" + object + "]");
			} else if (object.equals("bar")) {
				throw new IllegalArgumentException("illegal");
			} else if (object.equals("someFakeEntity")) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public HibernateCriteriaBuilder getCriteriaBuilder() {
			criteriaBuilder = createCriteriaBuilder();
			return criteriaBuilder;
		}
		
		private HibernateCriteriaBuilder createCriteriaBuilder() {
			return (HibernateCriteriaBuilder)Proxy.newProxyInstance(
					SessionFacadeTest.class.getClassLoader(), 
					new Class[] { HibernateCriteriaBuilder.class }, 
					new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("createQuery".equals(method.getName())) {
							return createCriteriaQuery();
						}
						return null;
					}
			});
		}
		
		private CriteriaQuery<?> createCriteriaQuery() {
			return (CriteriaQuery<?>)Proxy.newProxyInstance(
					SessionFacadeTest.class.getClassLoader(), 
					new Class[] { CriteriaQuery.class }, 
					new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("from".equals(method.getName())) {
							fromClass = (Class<?>)args[0];
							return ROOT;
						} else if ("select".equals(method.getName())) {
							root = (Root<?>)args[0];							
						}
						return null;
					}
			});
		}
		
	}
	
	private static SessionFactoryImplementor createSessionFactory() {
		return (SessionFactoryImplementor)Proxy.newProxyInstance(
				SessionFacadeTest.class.getClassLoader(), 
				new Class[] { SessionFactoryImplementor.class }, 
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
		});
	}
	
	private static QueryImplementor<?> createQueryImplementor() {
		return (QueryImplementor<?>)Proxy.newProxyInstance(
				SessionFacadeTest.class.getClassLoader(), 
				new Class[] { QueryImplementor.class }, 
				new InvocationHandler() {		
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
				});
	}
	
	private static Root<?> createRoot() {
		return (Root<?>)Proxy.newProxyInstance(
				SessionFacadeTest.class.getClassLoader(), 
				new Class[] { Root.class }, 
				new InvocationHandler() {		
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
				});
	}
	
}
