package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.polimi.tiw.project.beans.CartElement;

@WebServlet("/RemoveFromCart")
public class RemoveFromCart extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public RemoveFromCart() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession(false);
		
		ArrayList<CartElement> cartOld = (ArrayList<CartElement>) session.getAttribute("cart");
		ArrayList<CartElement> cartNew = new ArrayList<CartElement>();
		int cartElementIdx = -1;
		int prodIdx = -1;
		
		try {
			cartElementIdx = Integer.parseInt(request.getParameter("cartElementIdx"));
			prodIdx = Integer.parseInt(request.getParameter("productIdx"));
			
			// Removes element from old cart
			cartOld.get(cartElementIdx).getProductsDet().remove(prodIdx);
		} catch ( NumberFormatException | NullPointerException | IndexOutOfBoundsException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect params values");
			return;
		}
		
		// Removes supplier if all products have been removed
		for(CartElement ce: cartOld) {
			if(ce.getProductsDet().size() > 0) {
				cartNew.add(ce);
			}
		}
		
		session.setAttribute("cart", cartNew);
		
		// Redirects to rendering cart
		String path = getServletContext().getContextPath() + "/GoToCart";
		response.sendRedirect(path);
	}
}
