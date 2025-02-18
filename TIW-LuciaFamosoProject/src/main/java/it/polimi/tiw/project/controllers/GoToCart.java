package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

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
import it.polimi.tiw.project.dao.ProductDAO;
import it.polimi.tiw.project.dao.SupplierDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/GoToCart")
public class GoToCart extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Connection to database
	private Connection connection = null;
	// Thymeleaf engine
	private TemplateEngine templateEngine;

	public GoToCart() {
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
		
		//Cart-session keeps less data inside to avoid conflicts with data from database
		ArrayList<CartElement> cartSession = (ArrayList<CartElement>) session.getAttribute("cart");
		//Cart-render stores the complete data retrieved from the database
		ArrayList<CartElement> cartRender = new ArrayList<CartElement>();
		
		boolean quantityError = false;
		
		//From create order quantity checking
		if(session.getAttribute("quantityError") != null) {
			quantityError = (boolean) session.getAttribute("quantityError");
		}
		
		// Setup render-cart with products and suppliers details from databases
		if(!cartSession.isEmpty()) {
			
			for(CartElement ce: cartSession) {
				//New cart element with full details to render
				CartElement ceRender = new CartElement();
				Supplier suppl = null;
				
				//Retrieves supplier details
				try {
					suppl = supplDAO.getSupplierDetails(ce.getSupplier().getCodeSupplier());
					if(suppl == null) {
						throw new Exception();
					}
				} catch (Exception e) {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve supplier");
					e.printStackTrace();
					return;
				}
				ceRender.setSupplier(suppl);

				//Retrieves each products details and correct prices and keeps track of out of stock products
				ArrayList<Integer> outOfStockProds = new ArrayList<Integer>();
				
				for(Product p: ce.getProductsDet()) {
					
					Product prod = null;
					Advertisement adv = null;
					try {
						prod = prodDAO.getProductDetails(p.getCodeProduct(), null);
						adv = prodDAO.getAdvDetails(p.getCodeProduct(), ce.getSupplier().getCodeSupplier());
						if(prod == null || adv == null) {
							throw new Exception();
						}
					} catch (Exception e) {
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve product or advertisement details");
						e.printStackTrace();
						return;
					}
					prod.setCartUnitPrice(adv.getPrice());
					
					//Checks cart quantity to avoid selecting more than in stock
					boolean outOfStock = false;
					try {
						int difference = prodDAO.checkAvailability(prod.getCodeProduct(), ce.getSupplier().getCodeSupplier(), p.getCartQty());
						boolean qe = prod.equalizeCartQty(difference, p.getCartQty());
						
						if(qe) {
							//Not enough in stock
							quantityError = true;
							//Updates cart-session product quantity
							p.setCartQty(prod.getCartQty());
							
							//Zero in stock
							if(prod.getCartQty() == 0) {
								outOfStock = true;
								outOfStockProds.add(ce.getProductsDet().indexOf(p));
							}
						}						
					} catch (SQLException e) {
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not check product availability");
						e.printStackTrace();
						return;
					}
					
					//Adds to cart-render products details list if not out of stock
					if(!outOfStock) {
						ceRender.addProduct(prod);	
					}
				}
				
				if(!ceRender.getProductsDet().isEmpty()) {
					//Update prices based on supplier shipping policies and products quantities
					ceRender.updatePrices();				
					//Adds element to cart-render 
					cartRender.add(ceRender);
				}
				
				//Removes out of stock products from cart-session element
				Collections.sort(outOfStockProds, Collections.reverseOrder());
				for(Integer OFSidx : outOfStockProds) {
					ce.getProductsDet().remove(OFSidx.intValue());
				}
			}
			
			//Updates session with new cart
			session.setAttribute("cart", cartSession);	
		}

		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("cart", cartRender);
		if(quantityError) {
			ctx.setVariable("quantityError", "Some quantities inside your cart have been changed due to limited supply in stock");
			session.removeAttribute("quantityError");
		}
	
		String path = "/WEB-INF/Cart.html";
		templateEngine.process(path,ctx,response.getWriter());
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
