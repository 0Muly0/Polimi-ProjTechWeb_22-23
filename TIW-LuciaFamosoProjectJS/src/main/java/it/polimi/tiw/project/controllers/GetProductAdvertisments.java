package it.polimi.tiw.project.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import it.polimi.tiw.project.beans.Advertisement;
import it.polimi.tiw.project.beans.CartElement;
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.beans.Supplier;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.ProductDAO;
import it.polimi.tiw.project.dao.SupplierDAO;
import it.polimi.tiw.project.utils.ConnectionHandler;

@WebServlet("/GetProductAdvertisments")
@MultipartConfig
public class GetProductAdvertisments extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Connection to database
	private Connection connection = null;

	public GetProductAdvertisments() {
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

		String codeUser = ((User) session.getAttribute("user")).getUsername();

		ArrayList<CartElement> cartClient = null;
		String codeProduct = null;
		Product prodDetails = null;
		ArrayList<Advertisement> addsList = null;

		try {
			codeProduct = request.getParameter("codeProduct");
		} catch (NumberFormatException | NullPointerException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect params values");
			return;
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

		if (codeProduct != null) {
			// Coming from an interaction with the home list or the result list -> Load details
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
			} catch (Exception e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Internal server error, retry later");
				return;
			}

			// Advertisements <-> Cart-session linking
			if (!cartClient.isEmpty()) {
				for (Advertisement add : addsList) {

					int cartQty = 0;

					for (CartElement ce : cartClient) {

						if (add.getCodeSupplier() == ce.getSupplier().getCodeSupplier()) {

							Supplier suppl = null;
							// Retrieving supplier for pricing calculations
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

							for (Product p : ce.getProductsDet()) {

								cartQty += p.getCartQty();

								// Retrieving advertisement for pricing calculations
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
							// Pricing and shipping calculations
							ce.updatePrices();

							add.setCartQty(cartQty);
							add.setCartTot(ce.getProductPrice());
						}
					}
				}
			}
		}

		HashMap<String, Object> prodDetsAdvs = new HashMap<String, Object>();
		prodDetsAdvs.put("details", prodDetails);
		prodDetsAdvs.put("advs", addsList);

		String json = gson.toJson(prodDetsAdvs);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
