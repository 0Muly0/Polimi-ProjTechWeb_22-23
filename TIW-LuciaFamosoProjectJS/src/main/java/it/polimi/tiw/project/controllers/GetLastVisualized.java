package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;

import it.polimi.tiw.project.utils.ConnectionHandler;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.dao.ProductDAO;

@WebServlet("/GetLastVisualized")
@MultipartConfig
public class GetLastVisualized extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Connection to database
	private Connection connection = null;
       
    public GetLastVisualized() {
        super();
    }
    
    public void init() throws ServletException {
		//Attempt to establish connection to DB
		connection = ConnectionHandler.getConnection(getServletContext());
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession(false);
		ProductDAO prodDAO = new ProductDAO(connection);

		String codeUser = ((User) session.getAttribute("user")).getUsername();
		ArrayList<Product> lastVisualized = null;
		
		try {
			lastVisualized = prodDAO.getLastVisualizedByUser(codeUser);

			if(lastVisualized == null || lastVisualized.size() < 5) {
				throw new Exception();
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in retrieving last visualized products");
		}
		
		Gson gson = new Gson();
		String json = gson.toJson(lastVisualized);
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
		return;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
