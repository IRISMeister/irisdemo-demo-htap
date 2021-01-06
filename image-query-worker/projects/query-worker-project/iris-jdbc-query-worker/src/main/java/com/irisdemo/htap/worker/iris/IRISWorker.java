package com.irisdemo.htap.worker.iris;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.irisdemo.htap.config.Config;

import com.irisdemo.htap.workersrv.WorkerSemaphore;
import com.irisdemo.htap.workersrv.AccumulatedMetrics;
import com.irisdemo.htap.workersrv.IWorker;

@Component("worker")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class IRISWorker implements IWorker 
{
    Logger logger = LoggerFactory.getLogger(IRISWorker.class);

    @Autowired
    WorkerSemaphore workerSemaphore;
    
    @Autowired 
    AccumulatedMetrics accumulatedMetrics;
    
    @Autowired
    Config config;    

    DriverManagerDataSource dataSourceCache;

	/**
	 * I could not make this a Bean. It would get created before we had fetched the connection information from
	 * the master.
	 * 
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	synchronized public DriverManagerDataSource getDataSource() throws SQLException, IOException
	{
		if (dataSourceCache==null)
		{
	        logger.info("Creating data source for '" + config.getConsumptionJDBCURL() + "'...");
	        Properties connectionProperties = new Properties();
	        connectionProperties.setProperty("user", config.getConsumptionJDBCUserName());
	        connectionProperties.setProperty("password", config.getConsumptionJDBCPassword());

	        dataSourceCache = new DriverManagerDataSource(config.getConsumptionJDBCURL(), connectionProperties);
		}
        
        return dataSourceCache;
    }

	@Async("workerExecutor")
    public CompletableFuture<Long> startOneConsumer(int threadNum) throws IOException, SQLException
    {	
		PreparedStatement preparedStatement;
		ResultSet rs;
		ResultSetMetaData rsmd;
		Connection connection = getDataSource().getConnection();
		double t0, t1, t2, t3, rowCount;
		int idIndex, rowSizeInBytes, colnumCount;
		
		logger.info("Starting Consumer thread "+threadNum+"...");
		accumulatedMetrics.incrementNumberOfActiveQueryThreads();
		
		String[] IDs = new String[config.getConsumptionNumOfKeysToFetch()];

		for (idIndex = 0; idIndex<config.getConsumptionNumOfKeysToFetch(); idIndex++)
		{
			IDs[idIndex]="W1A"+idIndex;
		}		 
		
		/*
		 *  Each thread will run queries on the first 4 elements of the array.
		 *  Harry randomly shuffled the array so that each thread would be fetching different records.
		 */

		Random rnd = ThreadLocalRandom.current();
		
		for (idIndex = IDs.length - 1; idIndex > 0; idIndex--)
		{
			int index = rnd.nextInt(idIndex + 1);
		    // Simple swap
		    String id = IDs[index];
		    IDs[index] = IDs[idIndex];
		    IDs[idIndex] = id;
		}
		
		try 
		{

			preparedStatement = connection.prepareStatement(config.getQueryByIdStatement());
			String sql2="SELECT max(seqno) FROM SpeedTest.Account";
			PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
			String sql3="SELECT * FROM SpeedTest.Account where seqno>?-10 and seqno<?";
			PreparedStatement preparedStatement3 = connection.prepareStatement(sql3);
			String sql4="SELECT sum(load_version_no) FROM SpeedTest.Account where seqno>?-10 and seqno<?";
			PreparedStatement preparedStatement4 = connection.prepareStatement(sql4);
			long maxseqno=0;
			while(workerSemaphore.green())
			{
				for (idIndex = 0; idIndex<4; idIndex++)
				{					
					t0 = System.currentTimeMillis();
					preparedStatement.setString(1, IDs[idIndex]);
					rs = preparedStatement.executeQuery();
					
					t1= System.currentTimeMillis();
										
					rsmd = rs.getMetaData();
	                rowSizeInBytes=0;
	                rowCount=0;
					
	                t2= System.currentTimeMillis();                
	                
	                colnumCount = rsmd.getColumnCount();
	                
	                while (rs.next()) 
	                {
	                	rowCount++;
	                    for (int column=1; column<=colnumCount; column++) 
	                    {
	                    	// Approximate size
	                    	rowSizeInBytes += rs.getString(column).getBytes().length;
	                    }
	                 }
					 t3= System.currentTimeMillis();
					 
					 accumulatedMetrics.addToStats(t3-t0, rowCount, rowSizeInBytes);
				}

				t0 = System.currentTimeMillis();
				rs = preparedStatement2.executeQuery();
				rs.next();
				maxseqno=rs.getLong(1);

				preparedStatement3.setLong(1, maxseqno);
				preparedStatement3.setLong(2, maxseqno);
				rs = preparedStatement3.executeQuery();
				rsmd = rs.getMetaData();
				rowSizeInBytes=0;
				rowCount=0;

				colnumCount = rsmd.getColumnCount();
				while (rs.next()) 
				{
					rowCount++;
					for (int column=1; column<=colnumCount; column++) 
					{
						// Approximate size
						rowSizeInBytes += rs.getString(column).getBytes().length;
					}
				 }

				 preparedStatement4.setLong(1, maxseqno);
				 preparedStatement4.setLong(2, maxseqno);
				 rs = preparedStatement4.executeQuery();
				 rs.next();

				t3= System.currentTimeMillis();
				accumulatedMetrics.addToStats(t3-t0, rowCount, rowSizeInBytes);

				if (config.getConsumptionTimeBetweenQueriesInMillis()>0)
				{
					Thread.sleep(config.getConsumptionTimeBetweenQueriesInMillis());
				}
			}

		} 
		catch (SQLException sqlException) 
		{
			logger.error("Exception while running consumer query: " + sqlException.getMessage());
			throw sqlException;
		} 
		catch (InterruptedException e) 
		{
			logger.warn("Thread has been interrupted. Maybe the master asked it to stop: " + e.getMessage());
		} 
		finally
		{
			connection.close();
		}
		
		accumulatedMetrics.decrementNumberOfActiveQueryThreads();

		return null;
	}
	
}
