package it.polimi.tiw.project.beans;

import java.util.ArrayList;

public class CartElement {
	
	private Supplier supplier;
	private ArrayList<Product> productsDet = new ArrayList<Product>();
	private float productPrice = 0f;
	private float shippingPrice = 0f;

	public Supplier getSupplier() {
		return supplier;
	}

	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}

	public ArrayList<Product> getProductsDet() {
		return productsDet;
	}

	public void setProductsDet(ArrayList<Product> productsDet) {
		this.productsDet = productsDet;
	}
	
	public void addProduct(Product product) {
		productsDet.add(product);
	}

	public float getProductPrice() {
		return productPrice;
	}

	public void setProductPrice(float productPrice) {
		this.productPrice = productPrice;
	}

	public float getShippingPrice() {
		return shippingPrice;
	}

	public void setShippingPrice(float shippingPrice) {
		this.shippingPrice = shippingPrice;
	}
	
	public void updatePrices() {
		productPrice = 0f;
		shippingPrice = 0f;
		
		int totalProducts = 0;
		for(Product p: productsDet) {
			totalProducts += p.getCartQty();
			productPrice += p.getCartQty() * p.getCartUnitPrice();
		}
		
		if(supplier.getTreshold() == null || productPrice < supplier.getTreshold()) {
			for(PriceRange pr : supplier.getShippingPolicy()) {
				if(totalProducts >= pr.getMin() && totalProducts <= pr.getMax()) {
					shippingPrice = pr.getShippingPrice();
				}
			}
		}
	}

}
