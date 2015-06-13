package com.importsaad.job;

import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excelecm.common.job.admin.JobExecutionDetail;
import com.importsaad.job.impl.LimpiaTemporalJobImpl;

public class LimpiaTemporalJob implements Job {
	
	private static Logger log = LoggerFactory.getLogger(LimpiaTemporalJob.class);
	private final static String KEY_OSNAME = "osName";
	private final static String KEY_DAYSOLD = "daysOld";
	
	public void execute(JobExecutionContext context) throws JobExecutionException
    {
    	JobKey jobKey = null;   	
    	long startTime = 0;
    	long endTime = 0;
    	
    	try {
    		
    		startTime = System.currentTimeMillis();
    		
	    	jobKey = context.getJobDetail().getKey();
	    	JobExecutionDetail.getInstance().setLastResult(jobKey, JobExecutionDetail.RESULT_RUNNING);
	    	log.info((new StringBuilder()).append("Executing job: ").append(jobKey).append(" executing at ").append(new Date()).append(", fired by: ").append(context.getTrigger().getKey()).toString());
	    	
	    	// Get Job Parameters
	    	String osName = null;
	    	int addDays = 0;
        	if (context.getMergedJobDataMap().containsKey(KEY_DAYSOLD)) {
        		addDays = context.getMergedJobDataMap().getInt(KEY_DAYSOLD);
        		log.info("Job days old: " + addDays);
        	} else {
        		log.info("No days old parameter has been provided.");
        	}
        	if (context.getMergedJobDataMap().containsKey(KEY_OSNAME)) {
        		osName = context.getMergedJobDataMap().getString(KEY_OSNAME);
        		log.info("Job object store name: " + osName);
        	} else {
        		throw new JobExecutionException("No job object store name parameter has been provided");
        	}
	    	
	    	// Job Implementation
	    	LimpiaTemporalJobImpl impl = new LimpiaTemporalJobImpl();
	    	impl.setOsName(osName);
	    	impl.setAddDays(addDays);
	    	impl.doExec();
	    	Thread.sleep(1000); // timeout para reflejar progress bar en administrador
	    	context.setResult(JobExecutionDetail.RESULT_DONE + " (" + impl.getDocEliminados() + ")");
    	
    	} catch (Exception e) {
    		log.error("Error.", e);
    		context.setResult(JobExecutionDetail.RESULT_ERROR + ". " + e.getMessage());
    		
    	} finally {
    		endTime = System.currentTimeMillis();
    		JobExecutionDetail.getInstance().setLastResult(jobKey, context.getResult());
    		JobExecutionDetail.getInstance().setLastRunTime(jobKey, endTime - startTime);
    	}
    }	    

}

