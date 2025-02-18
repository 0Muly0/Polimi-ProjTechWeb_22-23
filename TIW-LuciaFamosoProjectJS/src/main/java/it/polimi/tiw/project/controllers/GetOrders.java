package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;

import it.polimi.tiw.project.beans.Order;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.OrderDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/GetOrders")
@MultipartConfig
public class GetOrders extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Connection to database
	private Connection connection = null;
       
    public GetOrders() {
        super();
    }
    
    public void init() throws ServletException {		
		//Attempt to establish connection to DB
		connection = ConnectionHandler.getConnection(getServletContext());
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		OrderDAO ordDAO = new OrderDAO(connection);
		ArrayList<Order> orders = null;

		HttpSession session = request.getSession(false);
		User u = (User) session.getAttribute("user");
		
		try {
			orders = ordDAO.getOrdersByUser(u.getUsername());
			
			if(orders == null) {
				throw new Exception();
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Failed to retrieve orders");
		}
		
		//Return user data and orders
		HashMap<String, Object> userOrders = new HashMap<String, Object>();
		userOrders.put("user", u);
		userOrders.put("orders", orders);
		
		Gson gson = new Gson();
		String json = gson.toJson(userOrders);
		
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
		} catch (SQLException sqle) {
		}
	}

}
