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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import it.polimi.tiw.project.beans.CartElement;

@WebServlet("/RemoveFromCart")
@MultipartConfig
public class RemoveFromCart extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public RemoveFromCart() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		ArrayList<CartElement> cartOld = null;
		ArrayList<CartElement> cartNew = new ArrayList<CartElement>();
		int cartElementIdx = -1;
		int prodIdx = -1;

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
			JsonObject jsonObjectReq = (gson.fromJson(jsonReq, JsonElement.class)).getAsJsonObject();

			// Form values
			cartElementIdx = Integer.parseInt(jsonObjectReq.getAsJsonObject("form").get("cartElementIdx").getAsString());
			prodIdx = Integer.parseInt(jsonObjectReq.getAsJsonObject("form").get("productIdx").getAsString());

			// Cart
			if(jsonObjectReq.get("cart").isJsonNull()) {
				cartOld = new ArrayList<CartElement>();
			} else {
				String jsonCart = jsonObjectReq.get("cart").toString();
				
				Type cartElementType = new TypeToken<ArrayList<CartElement>>() {}.getType();
				cartOld = gson.fromJson(jsonCart, cartElementType);
			}
		} catch (NumberFormatException | NullPointerException | JsonSyntaxException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Could not retrieve cart or form values");
			return;
		}

		// Removes element from old cart
		try {
			cartOld.get(cartElementIdx).getProductsDet().remove(prodIdx);
		} catch (IndexOutOfBoundsException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect param values");
			return;
		}

		// Removes supplier if all products have been removed
		for (CartElement ce : cartOld) {
			if (ce.getProductsDet().size() > 0) {
				cartNew.add(ce);
			}
		}

		// Returns new cart without removed product, if cart is empty, returns null
		String json =  gson.toJson(null);;
		if(!cartNew.isEmpty()) {
			json = gson.toJson(cartNew);
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
		return;
	}

}
