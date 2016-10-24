package org.datalift.core.project;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Entity;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.SqlDatabaseSource;
import org.datalift.fwk.project.SqlQuerySource;
import org.datalift.fwk.util.CloseableIterator;

import static org.datalift.fwk.util.StringUtils.isSet;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

/**
 * Default implementation of the {@link SqlQuerySource} interface.
 *
 * @author csuglia
 */
@Entity
@RdfsClass("datalift:SqlDatabaseSource")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="http://www.datalift.org/core/projects")
public class SqlDatabaseSourceImpl extends BaseSource implements SqlDatabaseSource {
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The scheme for JDBC URLs. */
    public final static String JDBC_URL_SCHEME  = "jdbc:";
	
    private final static String COL_TABLE_NAME = "TABLE_NAME";
    private final static String COL_TABLE_COLUMN = "COLUMN_NAME";

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:userName")
    private String user;
    @RdfProperty("datalift:password")
    private String password;

    private List<String> tableList;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new SQL source.
     */
    public SqlDatabaseSourceImpl() {
        super(SourceType.SqlDatabaseSource);
    }
    
    /**
     * Creates a new SQL source with the specified identifier and
     * owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    public SqlDatabaseSourceImpl(String uri, Project project) {
        super(SourceType.SqlDatabaseSource, uri, project);
    }


    /** {@inheritDoc} */
	@Override
	public String getConnectionUrl() {
		return this.getSourceUrl();
	}

    /** {@inheritDoc} */
	@Override
	public void setConnectionUrl(String connectionUrl) {
        // Check URL.
        this.checkDatabaseType(connectionUrl);
        // Store URL.
        this.setSourceUrl(connectionUrl);
	}

	private void checkDatabaseType(String connectionUrl) {
		if (connectionUrl.startsWith(JDBC_URL_SCHEME) && connectionUrl.contains("/")) {
            String[] urlElts = connectionUrl.split(":");
            if (!((urlElts.length > 1) && (urlElts[1] != null))) {
            	throw new TechnicalException("invalid.jdbc.url", connectionUrl);
            }
        }else{
        	throw new TechnicalException("invalid.jdbc.url", connectionUrl);
        }
	}

    /** {@inheritDoc} */
	@Override
	public String getUser() {
		return this.user;
	}

    /** {@inheritDoc} */
	@Override
	public void setUser(String user) {
		this.user = user;
	}

    /** {@inheritDoc} */
	@Override
	public String getPassword() {
		return this.password;
	}

    /** {@inheritDoc} */
	@Override
	public void setPassword(String password) {
		this.password = password;
	}
	
    /** {@inheritDoc} */
    @Override
    protected StringBuilder toString(StringBuilder b) {
        b.append(this.getConnectionUrl());
        return super.toString(b);
    }
    
    /** {@inheritDoc} */
    @Override
    public String getDatabaseName(){
    	String sourceUrl = this.getSourceUrl();
    	return sourceUrl.substring(sourceUrl.lastIndexOf("/")+ 1);
    }
    
    /** {@inheritDoc} */
    @Override
    public String getDatabasePath(){
    	String sourceUrl = this.getSourceUrl();
    	return sourceUrl.substring(0,sourceUrl.lastIndexOf("/")+ 1);
    }
    
    private void initJdbcDriver(){
        try {
            // Force loading of database driver.
            String databaseType = this.getDatabaseType();
            Class.forName(DatabaseType.valueOf(databaseType).getDriver());
            log.debug("Database driver loaded for {}", databaseType);
        } catch (Exception e) {
            throw new TechnicalException("jdbc.driver.not.found", e);
        }
    }
    
    /** {@inheritDoc} */
	@Override
	public List<String> getTableNames() throws SQLException{
		if(tableList == null) {
			initJdbcDriver();
//			try {
				String dbType = this.getDatabaseType();
				String query = (DatabaseType.postgresql.name().equals(dbType))?
				                "SELECT * FROM pg_catalog.pg_tables;": "SHOW TABLES;";
				Connection sqlCon=DriverManager.getConnection(this.getConnectionUrl(),this.getUser(), 
						this.getPassword());
				Statement stmt=sqlCon.createStatement();
				ResultSet tableStmt=stmt.executeQuery(query);
				tableList = new ArrayList<String>();
				while(tableStmt.next()){
					tableList.add(tableStmt.getString(1));
				}
		/*	} catch (SQLException e) {
				throw new TechnicalException("tables.retrieval.failed", e);
			}*/
		}
		return tableList;
	}

    /** {@inheritDoc} */
	@Override
	public CloseableIterator<Row<Object>> getTableIterator(String tableName) {
		if(tableName == null){
			throw new IllegalArgumentException();
		}
		if(tableList.contains(tableName)){
			// Now I will create the query object that will be used to get the iterator. 
			SqlQuerySourceImpl tableSource = new SqlQuerySourceImpl(null,this.getProject());
			tableSource.setTitle(tableName);
			tableSource.setUser(this.user);
			tableSource.setPassword(this.password);
			tableSource.setConnectionUrl(this.getConnectionUrl());
			tableSource.setQuery("SELECT * FROM " + tableName);
			return tableSource.iterator();
		}
		return null;
	}
	


	/** {@inheritDoc} */
	@Override
	public HashMap<String, List<String>> getTablesColumnNames() {
		HashMap<String, List<String>> columnsPerTable = null;
		initJdbcDriver();
	    try{
	   		Connection sqlCon=DriverManager.getConnection(this.getConnectionUrl(),this.getUser(), 
					this.getPassword());
			Statement stmt=sqlCon.createStatement();
			ResultSet tableStmt=stmt.executeQuery("SELECT " + COL_TABLE_NAME+"," +  COL_TABLE_COLUMN +" FROM " +
					"INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + this.getDatabaseName() + "' ORDER BY " +
					COL_TABLE_NAME + ",ORDINAL_POSITION;");
			columnsPerTable = new HashMap<String, List<String>>();
			String table = new String();
			List<String> columns = null;
			while(tableStmt.next()){
				if(table.equalsIgnoreCase(tableStmt.getString(COL_TABLE_NAME))){
					columns.add(tableStmt.getString(COL_TABLE_COLUMN));
				}else{
					if(!table.isEmpty()){
						//do not put a null array during the first interaction
						columnsPerTable.put(table, columns);
					}
					table = tableStmt.getString(COL_TABLE_NAME);
					columns = new ArrayList<String>();
					columns.add(tableStmt.getString(COL_TABLE_COLUMN));
				}
			}
			if(columns!=null){
				columnsPerTable.put(table, columns);
			}
		} catch (SQLException e) {
			throw new TechnicalException("tables.retrieval.failed", e);
		}
	    return columnsPerTable;
	}

	/** {@inheritDoc} */
	@Override
	public int getTableCount() {
		try {
			getTableNames();
			return tableList.size();
		} catch (SQLException e) {
			throw new TechnicalException("tables.retrieval.failed", e);
		}
		
	}

	/** {@inheritDoc} */
	@Override
	public String getDatabaseType() {
	    return getDatabaseType(this.getConnectionUrl());
	}

    private static String getDatabaseType(String connectionUrl) {
        if ((isSet(connectionUrl)) &&
            (connectionUrl.startsWith(JDBC_URL_SCHEME))) {
            String[] urlElts = connectionUrl.split(":");
            if ((urlElts.length > 1) && (urlElts[1] != null)) {
                return urlElts[1];
            }
        }
        throw new TechnicalException("invalid.jdbc.url", connectionUrl);
    }
}
	


