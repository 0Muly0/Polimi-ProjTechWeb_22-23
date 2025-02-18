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
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.beans.Supplier;

@WebServlet("/AddToCart")
public class AddToCart extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public AddToCart() {
		super();
	}

	public void init() throws ServletException {
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession(false);
		// Retrieves cart-session (can be empty, but always not null)
		ArrayList<CartElement> cart = (ArrayList<CartElement>) session.getAttribute("cart");

		int codeProduct;
		int codeSupplier;
		int cartqty;

		try {
			codeProduct = Integer.parseInt(request.getParameter("codeProduct"));
			codeSupplier = Integer.parseInt(request.getParameter("codeSupplier"));
			cartqty = Integer.parseInt(request.getParameter("cartqty"));
			
			//Parameters cannot be < 0
			if(codeProduct <= 0 || codeSupplier <= 0 || cartqty <= 0) {
				throw new Exception();
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect params values");
			return;
		}

		Supplier suppl = new Supplier();
		suppl.setCodeSupplier(codeSupplier);

		Product prod = new Product();
		prod.setCodeProduct(codeProduct);
		prod.setCartQty(cartqty);

		// Checks cart to add or update product quantities
		boolean noSupplierInCart = true;
		if (!cart.isEmpty()) {

			// Cart has some elements inside
			for (CartElement ce : cart) {
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

		if (cart.isEmpty() || noSupplierInCart) {
			// Cart is empty or supplier is not in cart -> Adds new CartElement
			CartElement ce = new CartElement();

			ce.setSupplier(suppl);
			ce.addProduct(prod);

			cart.add(ce);
		}

		// Updates session with new cart -> quantity checks operated in the rendering phase
		session.setAttribute("cart", cart);

		// Redirects to rendering cart
		String path = getServletContext().getContextPath() + "/GoToCart";
		response.sendRedirect(path);
	}
}
