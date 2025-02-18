package it.polimi.tiw.project.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import it.polimi.tiw.project.beans.CartElement;
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.beans.Supplier;

@WebServlet("/AddToCart")
@MultipartConfig
public class AddToCart extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public AddToCart() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			// Retrieves cart-session (can be empty, but always not null)
			ArrayList<CartElement> cartClient = null;
			int codeProduct;
			int codeSupplier;
			int cartqty;

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
				codeProduct = Integer.parseInt(jsonObjectReq.getAsJsonObject("form").get("codeProduct").getAsString());
				codeSupplier = Integer.parseInt(jsonObjectReq.getAsJsonObject("form").get("codeSupplier").getAsString());
				cartqty = Integer.parseInt(jsonObjectReq.getAsJsonObject("form").get("cartqty").getAsString());
				
				//Parameters cannot be < 0
				try {
					if(codeProduct <= 0 || codeSupplier <= 0 || cartqty <= 0) {
						throw new Exception();
					}
				} catch (Exception e) {
					e.printStackTrace();
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().println("Incorrect param values");
					return;
				}

				// Cart
				if (jsonObjectReq.get("cart").isJsonNull()) {
					cartClient = new ArrayList<CartElement>();
				} else {
					String jsonCart = jsonObjectReq.get("cart").toString();

					Type cartElementType = new TypeToken<ArrayList<CartElement>>() {
					}.getType();
					cartClient = gson.fromJson(jsonCart, cartElementType);
				}
			} catch (Exception e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Could not retrieve cart or form values");
				return;
			}

			Supplier suppl = new Supplier();
			suppl.setCodeSupplier(codeSupplier);

			Product prod = new Product();
			prod.setCodeProduct(codeProduct);
			prod.setCartQty(cartqty);

			// Checks cart to add or update product quantities
			boolean noSupplierInCart = true;
			if (!cartClient.isEmpty()) {

				// Cart has some elements inside
				for (CartElement ce : cartClient) {
					if (ce.getSupplier().getCodeSupplier() == codeSupplier) {
						// Supplier for this product is already in cart
						noSupplierInCart = false;

						// Finds product inside supplier cart if present
						int productIndex = -1;
						for (Product p : ce.getProductsDet()) {
							if (p.getCodeProduct() == codeProduct) {
								productIndex = ce.getProductsDet().indexOf(p);
							}
						}

						if (productIndex == -1) {
							// Product not in cart -> adds product to cart
							ce.addProduct(prod);
						} else {
							// Product already in cart -> updates quantities
							prod.setCartQty(ce.getProductsDet().get(productIndex).getCartQty() + cartqty);

							// Substitutes product with updated product
							ce.getProductsDet().set(productIndex, prod);
						}
					}
				}
			}

			if (cartClient.isEmpty() || noSupplierInCart) {
				// Cart is empty or supplier is not in cart -> Adds new CartElement
				CartElement ce = new CartElement();

				ce.setSupplier(suppl);
				ce.addProduct(prod);

				cartClient.add(ce);
			}

			// Returns updated cart
			String json = gson.toJson(cartClient);

			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(json);
			return;
	}
}
