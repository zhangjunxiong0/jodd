// Copyright (c) 2003-2014, Jodd Team (jodd.org). All Rights Reserved.

package jodd.db.oom;

import jodd.bean.BeanUtil;
import jodd.db.oom.naming.ColumnNamingStrategy;
import jodd.db.oom.naming.TableNamingStrategy;
import jodd.introspector.ClassIntrospector;
import jodd.introspector.FieldDescriptor;
import jodd.util.StringPool;
import jodd.util.sort.FastSort;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Holds all information about some entity type, such as table name and {@link DbEntityColumnDescriptor columns data}.
 */
public class DbEntityDescriptor<E> {

	public DbEntityDescriptor(Class<E> type, String schemaName, TableNamingStrategy tableNamingStrategy, ColumnNamingStrategy columnNamingStrategy) {
		this.type = type;
		this.entityName = type.getSimpleName();
		this.isAnnotated = DbMetaUtil.resolveIsAnnotated(type);
		this.schemaName = DbMetaUtil.resolveSchemaName(type, schemaName);
		this.tableName = DbMetaUtil.resolveTableName(type, tableNamingStrategy);
		this.columnNamingStrategy = columnNamingStrategy;
		this.mappedTypes = DbMetaUtil.resolveMappedTypes(type);
	}

	// ---------------------------------------------------------------- type and table

	private final Class<E> type;
	private final String entityName;
	private final boolean isAnnotated;
	private final String tableName;
	private final String schemaName;
	private final ColumnNamingStrategy columnNamingStrategy;
	private final Class[] mappedTypes;

	/**
	 * Returns entity type.
	 */
	public Class<E> getType() {
		return type;
	}

	/**
	 * Returns <code>true</code> if type is annotated with {@link jodd.db.oom.meta.DbTable}.
	 */
	public boolean isAnnotated() {
		return isAnnotated;
	}

	/**
	 * Returns table name to which the entity is mapped.
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * Returns type name.
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * Returns schema name or <code>null</code> if not available.
	 */
	public String getSchemaName() {
		return schemaName;
	}

	/**
	 * Returns mapped types.
	 */
	public Class[] getMappedTypes() {
		return mappedTypes;
	}

	// ---------------------------------------------------------------- columns and fields

	private DbEntityColumnDescriptor[] columnDescriptors;
	private DbEntityColumnDescriptor[] idColumnDescriptors;

	/**
	 * Returns the array of column descriptors.
	 */
	public DbEntityColumnDescriptor[] getColumnDescriptors() {
		init();
		return columnDescriptors;
	}

	// ---------------------------------------------------------------- initialization

	/**
	 * Lazy initialization of descriptor.
	 */
	protected void init() {
		if (columnDescriptors == null) {
			resolveColumnsAndProperties(type);
		}
	}

	/**
	 * Resolves list of all columns and properties.
	 */
	private void resolveColumnsAndProperties(Class type) {
		FieldDescriptor[] fields = ClassIntrospector.lookup(type).getAllFieldDescriptors();
		List<DbEntityColumnDescriptor> decList = new ArrayList<DbEntityColumnDescriptor>(fields.length);
		int idcount = 0;

		HashSet<String> names = new HashSet<String>(fields.length);

		for (FieldDescriptor fieldDescriptor : fields) {
			Field field = fieldDescriptor.getField();

			DbEntityColumnDescriptor dec = DbMetaUtil.resolveColumnDescriptors(this, field, isAnnotated, columnNamingStrategy);
			if (dec != null) {
				if (names.add(dec.getColumnName()) == false) {
					throw new DbOomException("Duplicate column name: " + dec.getColumnName());
				}

				decList.add(dec);

				if (dec.isId) {
					idcount++;
				}
			}
		}
		if (decList.isEmpty()) {
			throw new DbOomException("No column mappings in entity: " + type);
		}
		columnDescriptors = decList.toArray(new DbEntityColumnDescriptor[decList.size()]);
		FastSort.sort(columnDescriptors);

		// extract ids from sorted list
		if (idcount > 0) {
			idColumnDescriptors = new DbEntityColumnDescriptor[idcount];
			idcount = 0;
			for (DbEntityColumnDescriptor dec : columnDescriptors) {
				if (dec.isId) {
					idColumnDescriptors[idcount++] = dec;
				}
			}
		}
	}

	// ---------------------------------------------------------------- finders

	/**
	 * Finds column descriptor by column name. Case is ignored.
	 */
	public DbEntityColumnDescriptor findByColumnName(String columnName) {
		if (columnName == null) {
			return null;
		}
		init();
		for (DbEntityColumnDescriptor columnDescriptor : columnDescriptors) {
			if (columnDescriptor.columnName.equalsIgnoreCase(columnName) == true) {
				return columnDescriptor;
			}
		}
		return null;
	}
	/**
	 * Finds column descriptor by property name.
	 */
	public DbEntityColumnDescriptor findByPropertyName(String propertyName) {
		if (propertyName == null) {
			return null;
		}
		init();
		for (DbEntityColumnDescriptor columnDescriptor : columnDescriptors) {
			if (columnDescriptor.propertyName.equals(propertyName) == true) {
				return columnDescriptor;
			}
		}
		return null;
	}


	/**
	 * Returns property name for specified column name.
	 */
	public String getPropertyName(String columnName) {
		DbEntityColumnDescriptor dec = findByColumnName(columnName);
		return dec == null ? null : dec.propertyName;
	}

	/**
	 * Returns column name for specified property name..
	 */
	public String getColumnName(String propertyName) {
		DbEntityColumnDescriptor dec = findByPropertyName(propertyName);
		return dec == null ? null : dec.columnName;
	}

	// ---------------------------------------------------------------- column work

	/**
	 * Returns total number of columns.
	 */
	public int getColumnsCount() {
		init();
		return columnDescriptors.length;
	}

	/**
	 * Returns total number of identity columns.
	 */
	public int getIdColumnsCount() {
		init();
		return idColumnDescriptors == null ? 0 : idColumnDescriptors.length;
	}

	/**
	 * Returns <code>true</code> if entity has one ID column.
	 */
	public boolean hasIdColumn() {
		return getIdColumnsCount() == 1;
	}

	private void ensureSingleIdColumn() {
		init();
		if (idColumnDescriptors == null) {
			throw new DbOomException("No identity column in entity: " + entityName);
		} else if (idColumnDescriptors.length > 1) {
			throw new DbOomException("More then one identity column in entity: " + entityName);
		}
	}

	/**
	 * Returns the identity column name of column marked as identity.
	 * Throws an exception if table has composite primary key.
	 */
	public String getIdColumnName() {
		ensureSingleIdColumn();
		return idColumnDescriptors[0].getColumnName();
	}

	/**
	 * Returns the first property name of column marked as identity.
	 * Throws an exception if table has composite primary key.
	 */
	public String getIdPropertyName() {
		ensureSingleIdColumn();
		return idColumnDescriptors[0].getPropertyName();
	}

	/**
	 * Returns ID value for given entity instance.
	 */
	public Object getIdValue(E object) {
		String propertyName = getIdPropertyName();
		return BeanUtil.getDeclaredProperty(object, propertyName);
	}

	/**
	 * Sets ID value for given entity.
	 */
	public void setIdValue(E object, Object value) {
		String propertyName = getIdPropertyName();
		BeanUtil.setDeclaredProperty(object, propertyName, value);
	}

	/**
	 * Returns unique key for this entity. Returned key
	 * is built from entity class and id value.
	 */
	public String getKeyValue(E object) {
		Object idValue = getIdValue(object);

		String idValueString = idValue == null ?  StringPool.NULL : idValue.toString();

		return type.getName().concat(StringPool.COLON).concat(idValueString);
	}

	// ---------------------------------------------------------------- toString
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DbEntity: ");
		if (schemaName != null) {
			sb.append(schemaName);
			sb.append('.');
		}
		sb.append(tableName);
		return sb.toString();
	}

}