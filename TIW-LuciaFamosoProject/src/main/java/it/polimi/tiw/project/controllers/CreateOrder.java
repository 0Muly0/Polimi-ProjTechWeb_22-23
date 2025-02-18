package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.project.beans.Advertisement;
import it.polimi.tiw.project.beans.CartElement;
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.beans.Supplier;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.OrderDAO;
import it.polimi.tiw.project.dao.ProductDAO;
import it.polimi.tiw.project.dao.SupplierDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/CreateOrder")
public class CreateOrder extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Connection to database
	private Connection connection = null;
	// Thymeleaf engine
	private TemplateEngine templateEngine;

	public CreateOrder() {
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
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession(false);
		OrderDAO ordDAO = new OrderDAO(connection);
		ProductDAO prodDAO = new ProductDAO(connection);
		SupplierDAO supplDAO = new SupplierDAO(connection);

		ArrayList<CartElement> cart = (ArrayList<CartElement>) session.getAttribute("cart");
		User u = (User) session.getAttribute("user");
		Timestamp datetime = new Timestamp(System.currentTimeMillis());

		int cartElementIdx;
		CartElement ceSession = null;
		CartElement ceOrder = new CartElement();

		try {
			cartElementIdx = Integer.parseInt(request.getParameter("cartElementIdx"));
			ceSession = cart.get(cartElementIdx);
		} catch (NumberFormatException | NullPointerException | IndexOutOfBoundsException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect params values");
			return;
		}

		// Retrieves supplier details for pricing calculations
		Supplier suppl = null;
		try {
			suppl = supplDAO.getSupplierDetails(ceSession.getSupplier().getCodeSupplier());
			if(suppl == null) {
				//An inexistent supplier has been inserted inside the cart 
				throw new Exception();
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve supplier details");
			e.printStackTrace();
			return;
		}
		ceOrder.setSupplier(suppl);

		// Retrieves products pricing from databases
		boolean allAvailable = true;
		ArrayList<Integer> outOfStockProds = new ArrayList<Integer>();

		try {
			for (Product p : ceSession.getProductsDet()) {

				boolean outOfStock = false;

				Product prod = new Product();
				prod.setCodeProduct(p.getCodeProduct());

				// Retrieves each advertisement details and correct prices
				Advertisement adv = null;
				try {
					adv = prodDAO.getAdvDetails(p.getCodeProduct(), ceOrder.getSupplier().getCodeSupplier());
					if(adv == null) {
						// An inexistent product, or a product with inexistent advertisements has been inserted inside the cart 
						throw new Exception();
					}
				} catch (Exception e) {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve advertisement details");
					e.printStackTrace();
					return;
				}
				prod.setCartUnitPrice(adv.getPrice());

				int difference = prodDAO.checkAvailability(prod.getCodeProduct(), ceOrder.getSupplier().getCodeSupplier(), p.getCartQty());
				boolean quantityError = prod.equalizeCartQty(difference, p.getCartQty());
				if (quantityError) {
					// Not enough in stock
					allAvailable = false;
					// Updates cart-session product quantity
					p.setCartQty(prod.getCartQty());

					// Zero in stock
					if (prod.getCartQty() == 0) {
						outOfStock = true;
						outOfStockProds.add(ceSession.getProductsDet().indexOf(p));
					}
				}

				if (!outOfStock) {
					ceOrder.addProduct(prod);
					ceOrder.updatePrices();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve product details");
			return;
		}

		if (allAvailable) {
			try {
				ordDAO.createOrder(ceOrder, u, datetime);
			} catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to create order");
				e.printStackTrace();
				return;
			}

			// Updates cart
			cart.remove(cartElementIdx);
			session.setAttribute("cart", cart);

			// Redirects to rendering orders
			String path = getServletContext().getContextPath() + "/GoToOrders";
			response.sendRedirect(path);
			return;
		} else {
			// Removes all out of stock products from cart-session element
			Collections.sort(outOfStockProds, Collections.reverseOrder());
			for (Integer OFSidx : outOfStockProds) {
				ceSession.getProductsDet().remove(OFSidx.intValue());
			}

			if (ceSession.getProductsDet().isEmpty()) {
				// Removes cart-session element if all products are out of stock
				cart.remove(cartElementIdx);
			} else {
				// Updates cart-session element
				cart.set(cartElementIdx, ceSession);
			}

			// Updates session with new cart
			session.setAttribute("cart", cart);
			session.setAttribute("quantityError", true);

			// Redirects to rendering cart
			String path = getServletContext().getContextPath() + "/GoToCart";
			response.sendRedirect(path);
			return;
		}
	}

}
