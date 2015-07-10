package test.jobs;

import org.junit.Before;
import org.junit.Test;
import com.importsaad.job.impl.LimpiaTemporalJobImpl;
import com.importsaad.job.impl.LimpiaUnfiledJobImpl;
import com.importsaad.job.impl.UpdateSemaforosJobImpl;

public class JobsTest {

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testLimpiaTemporalJob() {
		try 
		{
			LimpiaTemporalJobImpl job = new LimpiaTemporalJobImpl();
			job.setAddDays(-1);
			job.doExec();
			System.out.println("Documentos elimindados: " + job.getDocEliminados());
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Test
	public void testLimpiaUnfiledJob() {
		try 
		{
			LimpiaUnfiledJobImpl job = new LimpiaUnfiledJobImpl();
			job.doExec();
			System.out.println("Documentos elimindados: " + job.getDocEliminados());
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}	

	@Test
	public void testUpdateSemaforosJobJob() {
		try 
		{
			UpdateSemaforosJobImpl job = new UpdateSemaforosJobImpl();
			job.setOsName("ImportSaadOS");
			job.setAddDays(-30);
			job.doExec();
			System.out.println("Contenedores procesados: " + job.getProcesados());
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}	

}
