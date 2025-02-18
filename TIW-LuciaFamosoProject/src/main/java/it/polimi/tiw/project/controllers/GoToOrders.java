package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.project.beans.Order;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.OrderDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/GoToOrders")
public class GoToOrders extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Connection to database
	private Connection connection = null;
	//Thymeleaf engine 
	private TemplateEngine templateEngine;
       
    public GoToOrders() {
        super();
    }
    
    public void init() throws ServletException {
    	ServletContext context = getServletContext();
    	ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(context);
		
		//Attempt to establish connection to DB
		connection = ConnectionHandler.getConnection(context);
		
		//Template engine setup
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
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
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to retrieve orders");
			e.printStackTrace();
		}
		
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("orders", orders);
		ctx.setVariable("user", u);

		String path = "/WEB-INF/Orders.html";
		templateEngine.process(path, ctx, response.getWriter());
		return;
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void destroy() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException sqle) {
		}
	}

}
