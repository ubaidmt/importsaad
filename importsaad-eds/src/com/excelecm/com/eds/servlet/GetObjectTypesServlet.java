package com.excelecm.com.eds.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONArray;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.NavigatorSettings;

/**
 * Servlet implementation class GetObjectTypesServlet
 */
@WebServlet("/types")
public class GetObjectTypesServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//InputStream objectTypesStream = this.getServletContext().getClassLoader().getResourceAsStream("ObjectTypes.json");
		NavigatorSettings navigatorSettings = ConfigurationSettings.getInstance().getNavigatorSettings();
		InputStream objectTypesStream = new FileInputStream(navigatorSettings.getEdsPath() + java.io.File.separator + "ObjectTypes.json");
		PrintWriter writer = response.getWriter();
		JSONArray jsonResponse = JSONArray.parse(objectTypesStream);
		jsonResponse.serialize(writer);
	}

}
