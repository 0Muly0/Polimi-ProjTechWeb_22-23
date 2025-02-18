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

import it.polimi.tiw.project.beans.Advertisement;
import it.polimi.tiw.project.beans.CartElement;
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.beans.Supplier;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.ProductDAO;
import it.polimi.tiw.project.dao.SupplierDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/GoToResults")
public class GoToResults extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Connection to database
	private Connection connection = null;
	// Thymeleaf engine
	private TemplateEngine templateEngine;

	public GoToResults() {
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
		SupplierDAO supplDAO = new SupplierDAO(connection);
		
		ArrayList<Product> searchedProdList = new ArrayList<Product>();
		Product prodDetails = null;
		ArrayList<Advertisement> addsList = null;

		String codeProduct = null;
		String codeUser = ((User) session.getAttribute("user")).getUsername();
		String keyword = (String) request.getSession().getAttribute("keyword");
		
		try {
			codeProduct = request.getParameter("codeProduct");
			String newKW = request.getParameter("keyword");
			
			if(newKW != null) {
				keyword = newKW;
			}
		} catch (NumberFormatException | NullPointerException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect params values");
			return;
		}
		
		if(keyword != null && keyword != "") {
			//Coming from an interaction with the search bar or an old keyword from a previous search
			try {
				searchedProdList = prodDAO.getSearchedProducts(keyword);
				request.getSession(false).setAttribute("keyword", keyword);
			} catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database access failed");
			}
		}	
		
		if(codeProduct != null) {
			//Coming from an interaction with the home list or the result list -> Load details
			int codeP;
			try {
				codeP = Integer.parseInt(codeProduct);
				
				if(codeP <= 0) {
					throw new Exception();
				}
			} catch (Exception e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect product code");
				return;
			}
			
			try {
				prodDetails = prodDAO.getProductDetails(codeP, codeUser);
				addsList = supplDAO.getAdvsSuppliersByProduct(codeP);
				
				if(prodDetails == null || addsList == null) {
					throw new Exception();
				}
				
				//Coming from an interaction with the home list -> Adding product to result list
				if(prodDetails != null && searchedProdList.isEmpty()) {
					searchedProdList.add(prodDetails);
				}
			} catch (Exception e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve product details or advertisements");
				return;
			}

			//Advertisements <-> Cart-session linking
			ArrayList<CartElement> cart = (ArrayList<CartElement>) session.getAttribute("cart");
			if(!cart.isEmpty()) {				
				for(Advertisement add : addsList) {
					
					int cartQty = 0;
					
					for(CartElement ce: cart) {
						
						if(add.getCodeSupplier() == ce.getSupplier().getCodeSupplier()) {
							
							Supplier suppl = null;
							//Retrieving supplier for pricing calculations
							try {
								suppl = supplDAO.getSupplierDetails(ce.getSupplier().getCodeSupplier());
								if(suppl == null) {
									throw new Exception();
								}
							} catch (Exception e) {
								e.printStackTrace();
								response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Supplier retrieving failed");
								return;
							}
							ce.setSupplier(suppl);
							
							
							for(Product p: ce.getProductsDet()) {
								
								cartQty += p.getCartQty();
								
								//Retrieving advertisement for pricing calculations
								Advertisement adv = null;
								try {
									adv = prodDAO.getAdvDetails(p.getCodeProduct(), ce.getSupplier().getCodeSupplier());
									if(adv == null) {
										throw new Exception();
									}
								} catch (Exception e) {
									e.printStackTrace();
									response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Advertisement retrieving failed");
									return;
								}
								p.setCartUnitPrice(adv.getPrice());
							}
							//Pricing and shipping calculations
							ce.updatePrices();

							add.setCartQty(cartQty);
							add.setCartTot(ce.getProductPrice());
						}
					}
				}
			}
		}	
		
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("products", searchedProdList);
		ctx.setVariable("prodDetails", prodDetails);
		ctx.setVariable("prodAdds", addsList);
		
		String path = "/WEB-INF/Results.html";
		templateEngine.process(path, ctx, response.getWriter());
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
