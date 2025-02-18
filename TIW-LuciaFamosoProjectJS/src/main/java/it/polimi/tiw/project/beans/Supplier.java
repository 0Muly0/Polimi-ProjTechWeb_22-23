package it.polimi.tiw.project.beans;

import java.util.ArrayList;

public class Supplier {

	private int codeSupplier = -1;
	private String name = null;
	private float evaluation = 0.0f;
	private ArrayList<PriceRange> shippingPolicy = null;
	private Float treshold = null;
	
	//Just for cart management
	private ArrayList<Advertisement> cart = null;

	public int getCodeSupplier() {
		return codeSupplier;
	}

	public void setCodeSupplier(int codeSupplier) {
		this.codeSupplier = codeSupplier;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getEvaluation() {
		return evaluation;
	}

	public void setEvaluation(float evaluation) {
		this.evaluation = evaluation;
	}

	public ArrayList<PriceRange> getShippingPolicy() {
		return shippingPolicy;
	}

	public void setShippingPolicy(ArrayList<PriceRange> shippingPolicy) {
		this.shippingPolicy = shippingPolicy;
	}

	public Float getTreshold() {
		return treshold;
	}

	public void setTreshold(Float treshold) {
		this.treshold = treshold;
	}

	public ArrayList<Advertisement> getCart() {
		return cart;
	}

	public void setCart(ArrayList<Advertisement> cart) {
		this.cart = cart;
	}

}
