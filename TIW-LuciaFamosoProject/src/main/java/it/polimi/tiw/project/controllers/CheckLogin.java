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

import it.polimi.tiw.project.beans.CartElement;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.UserDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Connection to database
	private Connection connection = null;
	//Thymeleaf engine 
	private TemplateEngine templateEngine;
       
    public CheckLogin() {
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
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String path = null;
		String usrnm = null;
		String psw = null;
		
		try {
			//Retrieve username from form
			usrnm = request.getParameter("username");
			//Retrieve password from form
			psw = request.getParameter("psw");
			
			//Checks missing parameters
			if (usrnm == null || usrnm.isEmpty() || psw == null || psw.isEmpty()) {
				throw new Exception("Missing or empty credential value");
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Credential");
			return;
		}
		
		UserDAO uDAO = new UserDAO(connection);
		User user = null;
		//Interview database to check login parameters
		try {
			user = uDAO.checkCredentials(usrnm, psw);
		} catch (SQLException e){
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not Possible to check credentials");
			return;
		}
		
		if (user == null) {
			//Username + password combination not existing, redirect to login
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsg", "Incorrect username or password");
			
			path = "/index.html";
			templateEngine.process(path, ctx, response.getWriter());
			return;
		} else {
			//User logged, saving user and initializing empty cart in session
			HttpSession session = request.getSession();
			
			session.setAttribute("user", user);
			session.setAttribute("cart", new ArrayList<CartElement>());
			
			//Redirect to home
			path = getServletContext().getContextPath() + "/GoToHome";
			response.sendRedirect(path);
		}
	}
	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
