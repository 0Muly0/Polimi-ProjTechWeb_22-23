package it.polimi.tiw.project.beans;

public class Advertisement {

	private int codeProduct;
	private int codeSupplier;
	private Supplier supplier;
	private int quantity;
	private float price;
	
	private int cartQty = 0;
	private float cartTot = 0f;

	public int getCodeProduct() {
		return codeProduct;
	}

	public void setCodeProduct(int codeProduct) {
		this.codeProduct = codeProduct;
	}

	public int getCodeSupplier() {
		return codeSupplier;
	}

	public void setCodeSupplier(int codeSupplier) {
		this.codeSupplier = codeSupplier;
	}

	public Supplier getSupplier() {
		return supplier;
	}

	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public int getCartQty() {
		return cartQty;
	}

	public void setCartQty(int cartQty) {
		this.cartQty = cartQty;
	}

	public float getCartTot() {
		return cartTot;
	}

	public void setCartTot(float cartTot) {
		this.cartTot = cartTot;
	}

}
