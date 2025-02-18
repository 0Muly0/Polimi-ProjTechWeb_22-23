package it.polimi.tiw.project.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
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
import it.polimi.tiw.project.dao.ProductDAO;
import it.polimi.tiw.project.dao.SupplierDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/RenderCart")
@MultipartConfig
public class RenderCart extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Connection to database
	private Connection connection = null;

	public RenderCart() {
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
		ProductDAO prodDAO = new ProductDAO(connection);
		SupplierDAO supplDAO = new SupplierDAO(connection);

		// Cart-client keeps less data inside to avoid conflicts with data from database
		ArrayList<CartElement> cartClient = null;
		// Cart-render stores the complete data retrieved from the database
		ArrayList<CartElement> cartRender = new ArrayList<CartElement>();
		Integer codeSupplier = null;

		boolean quantityError = false;
		// From create order quantity check
		if (session.getAttribute("quantityError") != null) {
			quantityError = (boolean) session.getAttribute("quantityError");
		}

		// Extracting cart client from request body
		Gson gson = new GsonBuilder().create();
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
		String cartJson = (jb.toString()).replace("\\", "");

		try {
			JsonObject jsonObjectReq = gson.fromJson(cartJson, JsonObject.class);

			if (jsonObjectReq.get("cart").isJsonNull()) {
				cartClient = new ArrayList<CartElement>();
			} else {
				String jsonCart = jsonObjectReq.get("cart").toString();

				Type cartElementType = new TypeToken<ArrayList<CartElement>>() {
				}.getType();
				cartClient = gson.fromJson(jsonCart, cartElementType);
			}
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Could not retrieve cart");
			return;
		}

		// Extracting possible supplier code from url
		try {
			String cs = request.getParameter("codeSupplier");
			if (cs != null) {
				codeSupplier = Integer.parseInt(cs);
				
				if(codeSupplier < 0) {
					throw new Exception();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect params values");
			return;
		}

		// Setup render-cart with products and suppliers details from databases
		if (!cartClient.isEmpty()) {

			for (CartElement ce : cartClient) {
				/*
				 * Going to cart page -> codeSupplier = null -> Load details for each element
				 * Rendering already in cart box -> codeSupplier = X -> Load details just for element X
				 */
				if (codeSupplier == null || codeSupplier == ce.getSupplier().getCodeSupplier()) {
					// New cart element with full details to render
					CartElement ceRender = new CartElement();
					Supplier suppl = null;

					// Retrieves supplier details
					try {
						suppl = supplDAO.getSupplierDetails(ce.getSupplier().getCodeSupplier());
						if(suppl == null) {
							throw new Exception();
						}
					} catch (Exception e) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						response.getWriter().println("Could not retrieve supplier");
						return;
					}
					ceRender.setSupplier(suppl);

					// Retrieves each products details and correct prices and keeps track of out of
					// stock products
					ArrayList<Integer> outOfStockProds = new ArrayList<Integer>();

					for (Product p : ce.getProductsDet()) {

						Product prod = null;
						Advertisement adv = null;
						try {
							prod = prodDAO.getProductDetails(p.getCodeProduct(), null);
							adv = prodDAO.getAdvDetails(p.getCodeProduct(), ce.getSupplier().getCodeSupplier());
							if(prod == null || adv == null) {
								throw new Exception();
							}
						} catch (Exception e) {
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							response.getWriter().println("Could not retrieve product or advertisement details");
							return;
						}
						prod.setCartUnitPrice(adv.getPrice());

						// Checks cart quantity to avoid selecting more than in stock
						boolean outOfStock = false;
						try {
							int difference = prodDAO.checkAvailability(prod.getCodeProduct(),
									ce.getSupplier().getCodeSupplier(), p.getCartQty());
							boolean qe = prod.equalizeCartQty(difference, p.getCartQty());

							if (qe) {
								// Not enough in stock
								quantityError = true;
								// Updates cart-session product quantity
								p.setCartQty(prod.getCartQty());

								// Zero in stock
								if (prod.getCartQty() == 0) {
									outOfStock = true;
									outOfStockProds.add(ce.getProductsDet().indexOf(p));
								}
							}
						} catch (SQLException e) {
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							response.getWriter().println("Could not check product availability");
							return;
						}

						// Adds to cart-render products details list if not out of stock
						if (!outOfStock) {
							ceRender.addProduct(prod);
						}
					}

					if (!ceRender.getProductsDet().isEmpty()) {
						// Update prices based on supplier shipping policies and products quantities
						ceRender.updatePrices();
						// Adds element to cart-render
						cartRender.add(ceRender);
					}

					// Removes out of stock products from cart-session element
					Collections.sort(outOfStockProds, Collections.reverseOrder());
					for (Integer OFSidx : outOfStockProds) {
						ce.getProductsDet().remove(OFSidx.intValue());
					}
					
					//Break cycle when supplier element is retrieved
					if(codeSupplier!= null && codeSupplier == ce.getSupplier().getCodeSupplier()) {
						break;
					}
				}
			}
		}

		HashMap<String, Object> cartAndError = new HashMap<String, Object>();
		cartAndError.put("cart", cartRender);
		cartAndError.put("quantityError", quantityError);
		if (quantityError) {
			session.removeAttribute("quantityError");
		}

		String json = gson.toJson(cartAndError);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
		return;
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException sqle) {
		}
	}

}
