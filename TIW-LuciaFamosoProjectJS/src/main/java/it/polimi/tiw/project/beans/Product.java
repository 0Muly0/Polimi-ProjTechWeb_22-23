package it.polimi.tiw.project.beans;

public class Product {

	private int codeProduct = -1;
	private String name = null;
	private String description = null;
	private String category = null;
	private String photo = null;
	private float minprice = 0.0f;

	// Cart variables
	private int cartQty = 0;
	private float cartUnitPrice = 0.0f;

	public int getCodeProduct() {
		return codeProduct;
	}

	public void setCodeProduct(int codeProduct) {
		this.codeProduct = codeProduct;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public float getMinprice() {
		return minprice;
	}

	public void setMinprice(float minprice) {
		this.minprice = minprice;
	}

	// Cart Method

	public int getCartQty() {
		return cartQty;
	}

	public void setCartQty(int cartQty) {
		this.cartQty = cartQty;
	}

	public float getCartUnitPrice() {
		return cartUnitPrice;
	}

	public void setCartUnitPrice(float cartUnitPrice) {
		this.cartUnitPrice = cartUnitPrice;
	}
	
	public boolean equalizeCartQty(int difference, int cartqty) {
		boolean equalized = false;
		
		if (difference > 0) {
			// Product quantity not in stock anymore, reset cart by difference
			this.cartQty = cartqty - difference;
			equalized = true;
		} else {
			this.cartQty = cartqty;
		}
		
		return equalized;
	}

}
