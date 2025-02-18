package it.polimi.tiw.project.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

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
@MultipartConfig
public class CreateOrder extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Connection to database
	private Connection connection = null;

	public CreateOrder() {
		super();
	}

	public void init() throws ServletException {
		// Attempt to establish connection to DB
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(false);
		OrderDAO ordDAO = new OrderDAO(connection);
		ProductDAO prodDAO = new ProductDAO(connection);
		SupplierDAO supplDAO = new SupplierDAO(connection);

		ArrayList<CartElement> cartClient = null;
		int cartElementIdx;

		// Extracting cart client and form values from request body
		Gson gson = new GsonBuilder().serializeNulls().create();
		StringBuffer jb = new StringBuffer();
		String line = null;
		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null) {
				jb.append(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Could not retrieve body request");
			return;
		}
		String jsonReq = (jb.toString()).replace("\\", "");

		// Extracting json as object to access keys
		try {
			JsonObject jsonObjectReq = gson.fromJson(jsonReq, JsonObject.class);

			// Form values
			cartElementIdx = Integer.parseInt(jsonObjectReq.getAsJsonObject("form").get("cartElementIdx").getAsString());

			// Cart
			if (jsonObjectReq.get("cart").isJsonNull()) {
				cartClient = new ArrayList<CartElement>();
			} else {
				String jsonCart = jsonObjectReq.get("cart").toString();

				Type cartElementType = new TypeToken<ArrayList<CartElement>>() {
				}.getType();
				cartClient = gson.fromJson(jsonCart, cartElementType);
			}
		} catch (NumberFormatException | NullPointerException | JsonSyntaxException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Could not retrieve cart or form values");
			return;
		}
		

		User u = (User) session.getAttribute("user");
		Timestamp datetime = new Timestamp(System.currentTimeMillis());
		
		CartElement ceSession = null;
		try {
			ceSession = cartClient.get(cartElementIdx);
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect cart element index");
			return;
		}
		CartElement ceOrder = new CartElement();
		HashMap<String, Object> cartError = new HashMap<String, Object>();

		// Retrieves supplier details for pricing calculations
		Supplier suppl = null;
		try {
			suppl = supplDAO.getSupplierDetails(ceSession.getSupplier().getCodeSupplier());
		} catch (SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Could not retrieve supplier details");
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
				} catch (SQLException e) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().println("Could not retrieve advertisement details");
					return;
				}
				prod.setCartUnitPrice(adv.getPrice());

				int difference = prodDAO.checkAvailability(prod.getCodeProduct(),
						ceOrder.getSupplier().getCodeSupplier(), p.getCartQty());
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
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Could not retrieve product details");
		}

		if (allAvailable) {
			try {
				ordDAO.createOrder(ceOrder, u, datetime);
			} catch (SQLException e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Failed to create order");
				return;
			}

			// Updates cart
			cartClient.remove(cartElementIdx);
			cartError.put("quantityError", false);

		} else {
			// Removes all out of stock products from cart-session element
			Collections.sort(outOfStockProds, Collections.reverseOrder());
			for (Integer OFSidx : outOfStockProds) {
				ceSession.getProductsDet().remove(OFSidx.intValue());
			}

			if (ceSession.getProductsDet().isEmpty()) {
				// Removes cart-session element if all products are out of stock
				cartClient.remove(cartElementIdx);
			} else {
				// Updates cart-session element
				cartClient.set(cartElementIdx, ceSession);
			}

			// Updates session with new cart
			session.setAttribute("quantityError", true);
			cartError.put("quantityError", true);
		}

		// Returns updated cart
		if(cartClient.isEmpty()) {
			cartError.put("cart", null);
		} else {
			cartError.put("cart", cartClient);
		}

		String json = gson.toJson(cartError);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
		return;
	}

}
