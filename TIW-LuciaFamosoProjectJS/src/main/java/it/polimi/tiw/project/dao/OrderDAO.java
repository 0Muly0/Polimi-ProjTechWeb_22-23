package it.polimi.tiw.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import it.polimi.tiw.project.beans.CartElement;
import it.polimi.tiw.project.beans.Order;
import it.polimi.tiw.project.beans.Product;
import it.polimi.tiw.project.beans.Supplier;
import it.polimi.tiw.project.beans.User;

public class OrderDAO {

	private Connection connection;

	public OrderDAO(Connection connection) {
		this.connection = connection;
	}
	
	public void createOrder(CartElement ce, User u, Timestamp datetime) throws SQLException {
		String query1 = "UPDATE advertising SET quantity = quantity - ? WHERE codeProduct = ? AND codeSupplier = ?";
		String query2 = "SELECT * FROM advertising WHERE codeProduct = ? AND codeSupplier = ?";
		String query3 = "INSERT into cart_order (codeSupplier, codeUser, date, total) VALUES(?, ?, ?, ?)";
		String query4 = "INSERT into sold_items (codeOrder, codeProduct, quantity) VALUES(?, ?, ?)";
		
		connection.setAutoCommit(false); 

		int orderId = -1;
		PreparedStatement pstatement1 = null;
		PreparedStatement pstatement2 = null;
		ResultSet result2 = null;
		PreparedStatement pstatement3 = null;
		ResultSet result3 = null;
		PreparedStatement pstatement4 = null;
		
		try {
			pstatement1 = connection.prepareStatement(query1);
			pstatement2 = connection.prepareStatement(query2);
			pstatement3 = connection.prepareStatement(query3, Statement.RETURN_GENERATED_KEYS);
			pstatement4 = connection.prepareStatement(query4);
			
			pstatement3.setInt(1, ce.getSupplier().getCodeSupplier());
			pstatement3.setString(2, u.getUsername());
			pstatement3.setTimestamp(3, datetime);
			pstatement3.setFloat(4, (ce.getProductPrice() + ce.getShippingPrice()));
			
			pstatement3.executeUpdate();
			
			// Retrieves oder auto-generated id
			result3 = pstatement3.getGeneratedKeys();
			while (result3.next()) {
				orderId = result3.getInt(1);
			}
			
			for(Product p: ce.getProductsDet()) {
				// Subtract quantity from advertisement
				pstatement1.setInt(1, p.getCartQty());
				pstatement1.setInt(2, p.getCodeProduct());
				pstatement1.setInt(3, ce.getSupplier().getCodeSupplier());
				pstatement1.executeUpdate();
				
				// Retrieves advertisement data to check final quantity
				pstatement2.setInt(1, p.getCodeProduct());
				pstatement2.setInt(2, ce.getSupplier().getCodeSupplier());
				result2 = pstatement2.executeQuery();
				
				// Check valid subtraction
				while(result2.next()) {
					if(result2.getInt("quantity") < 0) {
						throw new SQLException();
					}
				}
				
				//Adds product to order
				pstatement4.setInt(1, orderId);
				pstatement4.setInt(2, p.getCodeProduct());
				pstatement4.setInt(3, p.getCartQty());
				pstatement4.executeUpdate();
			}		
		} catch (SQLException e) {
		    e.printStackTrace();
			connection.rollback();
			throw new SQLException(e);
		} finally {
			try {
				if (result2 != null) {
					result2.close();
				}
			} catch (Exception e1) {
				connection.rollback();
				throw new SQLException(e1);
			}
			try {
		        if (result3 != null) {
		            result3.close();
		        }
		    } catch (Exception e2) {
				connection.rollback();
		        throw new SQLException(e2);
		    }
			try {
				pstatement1.close();
				pstatement2.close();
				pstatement3.close();
				pstatement4.close();
			} catch (Exception e2) {
				connection.rollback();
				throw new SQLException(e2);
			}
		}
		connection.commit();
		connection.setAutoCommit(true);
	}
	
	public ArrayList<Order> getOrdersByUser(String codeUser) throws SQLException {
		ArrayList<Order> orders = new ArrayList<Order>();
		
		String query1 = "SELECT * FROM cart_order AS O JOIN supplier AS S ON O.codeSupplier = S.codeSupplier WHERE O.codeUser = ? ORDER BY date DESC";
		String query2 = "SELECT * FROM sold_items AS SI JOIN product AS P on SI.codeProduct = P.codeProduct WHERE SI.codeOrder = ?";
		
		ResultSet result1 = null;
		PreparedStatement pstatement1 = null;
		ResultSet result2 = null;
		PreparedStatement pstatement2 = null;
		
		try {
			pstatement1 = connection.prepareStatement(query1);
			pstatement2 = connection.prepareStatement(query2);
			
			pstatement1.setString(1, codeUser);
			result1 = pstatement1.executeQuery();
			
			while(result1.next()) {
				Order ord = new Order();
				Supplier supp = new Supplier();
				ArrayList<Product> prods = new ArrayList<Product>();
				
				ord.setCodeOrder(result1.getInt("codeOrder"));
				ord.setDate(result1.getTimestamp("date"));
				ord.setTotal(result1.getFloat("total"));
				
				supp.setCodeSupplier(result1.getInt("codeSupplier"));
				supp.setName(result1.getString("name"));
				ord.setSupplier(supp);
				
				// Get products list
				pstatement2.setInt(1, result1.getInt("codeOrder"));
				result2 = pstatement2.executeQuery();
				
				while(result2.next()) {
					Product prod = new Product();
					
					prod.setCodeProduct(result2.getInt("codeProduct"));
					prod.setName(result2.getString("name"));
					prod.setDescription(result2.getString("description"));
					prod.setCategory(result2.getString("codeCategory"));
					prod.setCartQty(result2.getInt("quantity"));
					
					prods.add(prod);
				}
				
				ord.setProducts(prods);
				orders.add(ord);
			}
			
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);

		} finally {
			try {
				if (result1 != null) {
					result1.close();
				}
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
		        if (result2 != null) {
		            result2.close();
		        }
		    } catch (Exception e2) {
		        throw new SQLException(e2);
		    }
			try {
				pstatement1.close();
				pstatement2.close();
			} catch (Exception e3) {
				throw new SQLException(e3);
			}
		}		
		return orders;
	}
}
