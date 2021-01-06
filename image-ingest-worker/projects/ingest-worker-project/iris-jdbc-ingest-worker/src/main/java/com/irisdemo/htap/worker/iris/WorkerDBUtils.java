package com.irisdemo.htap.worker.iris;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import com.irisdemo.htap.config.Config;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class WorkerDBUtils 
{
	protected static Logger logger = LoggerFactory.getLogger(WorkerDBUtils.class);
    
    @Autowired
    protected Config config;    
	
    protected static Object[][] paramRandomValues;
	
    protected static int[] paramDataTypes;
	
    protected static long[][] paramSizeInBytes;
	
    protected static boolean randomMappingInitialized = false;
    
    protected static DriverManagerDataSource dataSourceCache = null;
	
	/**
	 * I could not make this a Bean. It would get created before we had fetched the connection information from
	 * the master.
	 * 
	 * This method can't be static because it relies on the auto-wiring of config to work.
	 * 
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	synchronized public DriverManagerDataSource getDataSource() throws SQLException
	{
		if (dataSourceCache==null)
		{
	        logger.info("Creating data source for '" + config.getIngestionJDBCURL() + "'...");
	        Properties connectionProperties = new Properties();
	        connectionProperties.setProperty("user", config.getIngestionJDBCUserName());
	        connectionProperties.setProperty("password", config.getIngestionJDBCPassword());
	
	        dataSourceCache = new DriverManagerDataSource(config.getIngestionJDBCURL(), connectionProperties);
		}
        
        return dataSourceCache;
    }
	
	/*
	 * This method will prepare the statement on TABLE_SELECT and use that to:
	 * - See how many columns the table has
	 * - Create the paramRandomValues and paramDataTypes based on the number of columns
	 * - Loop on each column and create 1000 random values for it.
	 */
	public synchronized void initializeRandomMapping(Connection connection) throws SQLException, IOException
	{
		int type;
		int precision;
		int shorterPrecision;
		//String typeName;
		
		if (randomMappingInitialized)
			return;
		
		PreparedStatement preparedStatement = connection.prepareStatement(config.getQueryStatement());
		ResultSet resultSet = preparedStatement.executeQuery();
		
		ResultSetMetaData metaData = resultSet.getMetaData();
		
		int columnCount = metaData.getColumnCount();
		
		// There is up to 40 types in java.sql.Types. We won't use all of these slots.
		paramRandomValues = new Object[columnCount+1][1000];
		paramSizeInBytes = new long[columnCount+1][1000];
		paramDataTypes = new int[columnCount+1];
		
		for(int column=1; column <= columnCount; column++)
		{
			type = metaData.getColumnType(column);
			paramDataTypes[column]=type;
			
			precision = metaData.getPrecision(column);
			//typeName = metaData.getColumnTypeName(column);
			
			switch (type)
			{
			case java.sql.Types.VARCHAR:
				for (int i=0; i<1000; i++)
				{
					shorterPrecision = (int) (precision*Math.random());
					if (shorterPrecision==0) shorterPrecision=1;
					paramRandomValues[column][i] = Util.randomAlphaNumeric(shorterPrecision);
					paramSizeInBytes[column][i] = ((String)paramRandomValues[column][i]).getBytes().length;
				}
				break;
				
			case java.sql.Types.TIMESTAMP:
				
				for (int i=0; i<1000; i++)
				{
					paramRandomValues[column][i] = Util.randomTimeStamp();
					// Not sure if that is the correct size that goes on the wire. But it must be a good approximation?
					paramSizeInBytes[column][i] = ((java.sql.Timestamp)paramRandomValues[column][i]).toString().getBytes().length;
				}
				break;
				
			case java.sql.Types.BIGINT:
				for (int i=0; i<1000; i++)
				{
					paramRandomValues[column][i] = Math.round(Math.random()*10000);
					paramSizeInBytes[column][i] = Long.BYTES;
				}
				break;
				
			}
		}
				
		randomMappingInitialized = true;
	}
	
	/*
	 * Populates a prepared statement with appropriate random data for each field. Random data is get from
	 * the paramRandomValues array that was initialized by initializeRandomMapping().
	 * This prevents us from generating too many random objects which would cause the Garbage Collector to panic.
	 */
	public static long pupulatePreparedStatement(int parameterCount, long recordNum, String threadPrefix, PreparedStatement preparedStatement) throws SQLException
	{
		Object randomValue = null;
		String param1;
		int numberOfRandomValues = 999;
		int randomIndex = (int) (numberOfRandomValues*Math.random());
		
		// param 1:
		param1 = threadPrefix + "" + recordNum;
		preparedStatement.setObject(1, param1);
		long recordSize=Character.BYTES+Long.BYTES;

		// param 2:
		preparedStatement.setLong(2,recordNum);
		recordSize+=Long.BYTES;

		// rest of parameters:
		for(int param=3; param <= parameterCount; param++)
		{
			randomValue = paramRandomValues[param][randomIndex];
			
			preparedStatement.setObject(param, randomValue);
			
			recordSize+=paramSizeInBytes[param][randomIndex];
		}
		
		return recordSize;
	}
    

    
	public void createIRISDisableJournalProc(Connection connection) throws SQLException, IOException
	{
		PreparedStatement statement;
		
		try
		{
		    statement = connection.prepareStatement(config.getIrisProcDisableJournalDrop());
		    statement.execute();
		    statement.close();

		}
		catch (SQLException exception)
		{
			if (exception.getErrorCode()!=362) //Method '???' does not exist in any class
			{
				throw exception;
			}
		}
		
	    statement = connection.prepareStatement(config.getIrisProcDisableJournal());
	    statement.execute();
	    statement.close();

    }
    
	public void createTable(Connection connection) throws SQLException
	{
		PreparedStatement statement = connection.prepareStatement(config.getTableCreateStatement());
	    statement.execute();
		statement.close();

	    String sql="CREATE INDEX idx1 on SpeedTest.Account (repteamno)";
		statement = connection.prepareStatement(sql);
	    statement.execute();
		statement.close();

        sql="CREATE INDEX idx2 on SpeedTest.Account (seqno)";
		statement = connection.prepareStatement(sql);
	    statement.execute();
		statement.close();

	    // Populate dummy master table
	    String sql1="DROP TABLE SpeedTest.MASTER";
		PreparedStatement statement1 = connection.prepareStatement(sql1);
		try
		{
		    statement1.execute();
		}
		catch (SQLException exception)
		{
			if (exception.getErrorCode()==30) //Table or view not found
			{
				
			}
			else if (exception.getMessage().startsWith("Unknown table"))
			{
				
			}
			else
			{
				throw exception;
			}
			
		}
	    statement1.close();
	    
	    String sql2="CREATE TABLE SpeedTest.MASTER (load_version_no BIGINT PRIMARY KEY, NAME VARCHAR(50), p1 BIGINT, p2 BIGINT, p3 BIGINT, p4 BIGINT, p5 BIGINT, p6 BIGINT, p7 BIGINT, p8 BIGINT, p9 BIGINT, p10 BIGINT)";
		PreparedStatement statement2 = connection.prepareStatement(sql2);
	    statement2.execute();
	    statement2.close();

	    String sql3="INSERT INTO SpeedTest.MASTER (load_version_no, NAME,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement statement3 = connection.prepareStatement(sql3);
		for (int i=1;i<=10000;i++) {
			statement3.setInt(1, i);
			statement3.setString(2, "AAAAA"+i);
			for (int j=0;j<10;j++) {
				statement3.setLong(3+j, Math.round(Math.random()*Long.MAX_VALUE));
			}
			statement3.execute();
		}
	    statement3.close();
	}
    
	public void dropTable(Connection connection) throws SQLException
	{
		PreparedStatement statement = connection.prepareStatement(config.getTableDropStatement());
	    statement.execute();
	    statement.close();
	}
	
	public void truncateTable(Connection connection) throws SQLException, IOException
	{
		PreparedStatement statement = connection.prepareStatement(config.getTableTruncateStatement());
	    statement.execute();
	    statement.close();
	}
	
	public static void disableJournalForConnection(Connection connection, boolean disable) throws SQLException
	{
		CallableStatement disableJournalStatement = connection.prepareCall("{ ? = call IRISDemo.DisableJournalForConnection(?) }");
		disableJournalStatement.registerOutParameter(1, Types.VARCHAR);
		disableJournalStatement.setBoolean(2, disable);

		disableJournalStatement.execute();
		
		String returnMsg = disableJournalStatement.getString(1);
		
		if (!returnMsg.equals("1"))
		{
			throw new SQLException(returnMsg);
		}
	}

	public void createIRISExpandDatabaseProc(Connection connection) throws SQLException, IOException
	{
		PreparedStatement statement;
		
		try
		{
			String sqlCmd = "CREATE FUNCTION IRISDemo.ExpandDatabase(IN pSizeInGB INTEGER) FOR IRISDemo.HTAPDemoAPI2 RETURNS VARCHAR(32000) LANGUAGE OBJECTSCRIPT { \n " +
			"	Set tSC = $$$OK \n" +
			"	Set tNS = $Namespace \n" +
			"	Try { \n" +
			"		Set $Namespace=\"%SYS\" \n" +
			"       Set tSC = ##class(Config.Databases).Get(tNS, .properties) \n"+
			"		Quit:$$$ISERR(tSC)\n" +
			"		\n" +
			"		Set dir=properties(\"Directory\")\n" +
			"		Set db=##class(SYS.Database).%OpenId(dir,,.tSC)\n" +
			"		Quit:$$$ISERR(tSC)\n" +
			"		Set db.Size=pSizeInGB*1024 \n" +
			"		Set tSC = db.%Save()\n" +
			"		\n" +
			"	} Catch (oException) { \n" +
			"		Set tSC = oException.AsStatus() \n" +
			"	} \n" +
			"	Set $Namespace=tNS \n" +
			"	If $$$ISERR(tSC) Quit $System.Status.GetErrorText(tSC) \n" +
			"	Quit 1 \n" +
			"}\n";

		    statement = connection.prepareStatement(sqlCmd);
		    statement.execute();
		    statement.close();

		}
		catch (SQLException exception)
		{
			if (exception.getErrorCode()!=361) //Method or Query name not unique
			{
				throw exception;
			}
		}
    }
	
	public static void expandDatabase(Connection connection, int databaseSizeInGB) throws SQLException
	{
		CallableStatement statement = connection.prepareCall("{ ? = call IRISDemo.ExpandDatabase(?) }");
		String returnMsg;

		for (int i=0; i<2; i++)
		{
			statement.registerOutParameter(1, Types.VARCHAR);
			statement.setInt(2, databaseSizeInGB);

			statement.execute();
			
			returnMsg = statement.getString(1);
			
			if (returnMsg.equals("1"))
			{
				break; //SUCCESS! Exit for loop
			}
			else
			{
				if (returnMsg.contains("the expansion failed to start"))
				{
					continue; //try again
				}
				else
				{
					throw new SQLException(returnMsg); // Unpredicted error. Exist for loop with exception
				}
			}
		}
	}
}
