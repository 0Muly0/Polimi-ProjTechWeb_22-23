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

import com.google.gson.Gson;

import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.dao.ProductDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/GetSearchResults")
@MultipartConfig
public class GetSearchResults extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Connection to database
	private Connection connection = null;

	public GetSearchResults() {
		super();
	}

	public void init() throws ServletException {
		// Attempt to establish connection to DB
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		ProductDAO prodDAO = new ProductDAO(connection);
		
		ArrayList<Product> searchedProdList = new ArrayList<Product>();
		String codeProduct = null;
		String keyword = null;
		
		try {
			codeProduct = request.getParameter("codeProduct");
			keyword = request.getParameter("keyword");
		} catch (NumberFormatException | NullPointerException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect params values");
			return;
		}
		
		if(keyword != null && keyword != "") {
			//Coming from an interaction with the menu -> load searched products 
			try {
				searchedProdList = prodDAO.getSearchedProducts(keyword);
			} catch (SQLException e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Could not retrieve product list");
			}	
		}
		
		if(codeProduct != null) {
			//Coming from an interaction with the home list -> load specific product inside result list
			Product prodDetails = null;
			
			int codeP;
			try {
				codeP = Integer.parseInt(codeProduct);
				
				if(codeP <= 0) {
					throw new Exception();
				}
			} catch (Exception e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Incorrect product code");
				return;
			}
			
			try {
				prodDetails = prodDAO.getProductDetails(codeP, null);
				 
				if(prodDetails == null) {
					throw new Exception();
				}
			} catch (Exception e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Could not retrieve product details");
				return;
			}
			
			//Adds product to results list
			searchedProdList.add(prodDetails);
		}
		
		Gson gson = new Gson();
		String json = gson.toJson(searchedProdList);
		
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
