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

import it.polimi.tiw.project.utils.ConnectionHandler;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.dao.ProductDAO;

@WebServlet("/GoToHome")
public class GoToHome extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Connection to database
	private Connection connection = null;
	// Thymeleaf engine
	private TemplateEngine templateEngine;

	public GoToHome() {
		super();
	}

	public void init() throws ServletException {
		ServletContext context = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(context);

		// Attempt to establish connection to DB
		connection = ConnectionHandler.getConnection(context);

		// Template engine setup
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(false);
		ProductDAO prodDAO = new ProductDAO(connection);

		String codeUser = ((User) session.getAttribute("user")).getUsername();
		ArrayList<Product> lastVisualized = null;

		// Removes results keyword from session if present
		session.removeAttribute("keyword");

		try {
			lastVisualized = prodDAO.getLastVisualizedByUser(codeUser);

			if(lastVisualized == null || lastVisualized.size() < 5) {
				throw new Exception();
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in retrieving last visualized products");
		}
		
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("products", lastVisualized);

		String path = "/WEB-INF/Home.html";
		templateEngine.process(path, ctx, response.getWriter());
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
