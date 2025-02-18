package it.polimi.tiw.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;

import it.polimi.tiw.project.exceptions.CartQuantityException;
import it.polimi.tiw.project.beans.Advertisement;
import it.polimi.tiw.project.beans.Product;

public class ProductDAO {

	private Connection connection;

	public ProductDAO(Connection connection) {
		this.connection = connection;
	}
	
	public Product getProductDetails(int codeProduct, String codeUser) throws SQLException {
		Product prod = null;
		
		String query1 = "SELECT P.codeProduct, P.name, P.description, P.photo, P.codeCategory, MIN(price) AS minprice FROM product AS P JOIN advertising AS A ON P.codeProduct = A.codeProduct WHERE P.codeProduct = ? GROUP BY P.codeProduct, P.name, P.description, P.codeCategory, P.photo";
		String query2 = "REPLACE into visualized_items (codeUser, codeProduct, dateTime) VALUES(?,?,?)";
		
		// If visualization == true and error in visualization -> Rollback
		connection.setAutoCommit(false); 
		PreparedStatement pstatement1 = null;
		PreparedStatement pstatement2 = null;
		ResultSet result1 = null;
		
		try {
			pstatement1 = connection.prepareStatement(query1);
			pstatement2 = connection.prepareStatement(query2);
			pstatement1.setInt(1, codeProduct);
			
			result1 = pstatement1.executeQuery();
			if (result1.next()) {
				prod = new Product();
				prod.setCodeProduct(result1.getInt("codeProduct"));
				prod.setName(result1.getString("name"));
				prod.setDescription(result1.getString("description"));
				prod.setCategory(result1.getString("codeCategory"));
				
				byte[] img = result1.getBytes("photo");
				String encodedImg = Base64.getEncoder().encodeToString(img);
				prod.setPhoto(encodedImg);
				
				prod.setMinprice(result1.getFloat("minprice"));
				
				if(codeUser != null) {
					pstatement2.setString(1, codeUser);
					pstatement2.setInt(2, codeProduct);
					pstatement2.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
					
					pstatement2.executeUpdate();
				}
			}
		} catch (SQLException e) {
		    e.printStackTrace();
		    connection.rollback();
			throw new SQLException(e);

		} finally {
			try {
				if (result1 != null) {
					result1.close();
				}
			} catch (Exception e1) {
			    connection.rollback();
				throw new SQLException(e1);
			}
			try {
				pstatement1.close();
				pstatement2.close();
			} catch (Exception e3) {
			    connection.rollback();
				throw new SQLException(e3);
			}
		}	
		connection.commit();
		connection.setAutoCommit(true);
		
		return prod;
	}
	
	public Advertisement getAdvDetails(int codeProduct, int codeSupplier) throws SQLException {
		Advertisement adv = null;
		
		String query = "SELECT * FROM advertising WHERE codeProduct = ? AND codeSupplier = ?";
		ResultSet result = null;
		PreparedStatement pstatement = null;
		
		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setInt(1, codeProduct);
			pstatement.setInt(2, codeSupplier);
			
			result = pstatement.executeQuery();
			
			while(result.next()) {
				adv = new Advertisement();
				
				adv.setPrice(result.getFloat("Price"));
				adv.setCartQty(result.getInt("quantity"));
			}
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				result.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
			try {
				pstatement.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
		}		
		return adv;
	}
	
	public ArrayList<Product> getLastVisualizedByUser(String codeUser) throws SQLException {
		ArrayList<Product> lastVisualized = new ArrayList<Product>();

		String query = "SELECT P.* FROM product AS P JOIN ( SELECT codeProduct, MAX(dateTime) AS latestDateTime FROM ( SELECT codeProduct, dateTime FROM visualized_items WHERE codeUser = ? UNION SELECT codeProduct, NULL AS dateTime FROM product WHERE codeCategory = 'photography' ) AS combined_data GROUP BY codeProduct ) AS M ON P.codeProduct = M.codeProduct ORDER BY M.latestDateTime DESC limit 5";
		ResultSet result = null;
		PreparedStatement pstatement = null;

		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setString(1, codeUser);
			
			result = pstatement.executeQuery();
			while (result.next()) {
				Product prod = new Product();
				prod.setCodeProduct(result.getInt("codeProduct"));
				prod.setName(result.getString("name"));
				prod.setDescription(result.getString("description"));
				prod.setCategory(result.getString("codeCategory"));
				
				byte[] img = result.getBytes("photo");
				String encodedImg = Base64.getEncoder().encodeToString(img);
				prod.setPhoto(encodedImg);
				
				lastVisualized.add(prod);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(e);
			
		} finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
				
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}
		return lastVisualized;
	}

	public ArrayList<Product> getSearchedProducts(String keyword) throws SQLException {
		ArrayList<Product> products = new ArrayList<Product>();
		
		String query = "SELECT P.codeProduct, P.name, MIN(price) AS minprice FROM product AS P JOIN advertising AS A ON P.codeProduct = A.codeProduct WHERE P.name LIKE ? OR P.description LIKE ? GROUP BY P.codeProduct, P.name ORDER BY minprice ASC";
		ResultSet result = null;
		PreparedStatement pstatement = null;
		
		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setString(1, "%" + keyword + "%");
			pstatement.setString(2, "%" + keyword + "%");
			
			result = pstatement.executeQuery();
			while (result.next()) {
				Product prod = new Product();
				prod.setCodeProduct(result.getInt("codeProduct"));
				prod.setName(result.getString("name"));
				prod.setMinprice(result.getFloat("minprice"));
				
				products.add(prod);
			}
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);

		} finally {
			try {
				result.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				pstatement.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}		
		return products;
	}

	public int checkAvailability(int codeProduct, int codeSupplier, int quantity)  throws SQLException {
		int available = 0;
		
		String query = "SELECT quantity FROM advertising WHERE codeProduct = ? AND codeSupplier = ?";
		ResultSet result = null;
		PreparedStatement pstatement = null;
		
		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setInt(1, codeProduct);
			pstatement.setInt(2, codeSupplier);
			
			result = pstatement.executeQuery();
			
			while(result.next()) {
				available = quantity - result.getInt("quantity");
			}
		} catch (SQLException e) {
		    e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				pstatement.close();
			} catch (Exception e1) {
				throw new SQLException(e1);
			}
			try {
				result.close();
			} catch (Exception e2) {
				throw new SQLException(e2);
			}
		}
		return available;
	}
	
	public void buyProduct(int codeProduct, int codeSupplier, int quantity) throws SQLException, CartQuantityException {
		String query1 = "UPDATE advertising SET quantity = quantity - ? WHERE codeProduct = ? AND codeSupplier = ?";
		String query2 = "SELECT * FROM advertising WHERE codeProduct = ? AND codeSupplier = ?";
		
		connection.setAutoCommit(false); 
		PreparedStatement pstatement1 = null;
		PreparedStatement pstatement2 = null;
		ResultSet result2 = null;
		
		try {
			pstatement1 = connection.prepareStatement(query1);
			pstatement1.setInt(1, quantity);
			pstatement1.setInt(2, codeProduct);
			pstatement1.setInt(3, codeSupplier);
			pstatement1.executeUpdate();
			
			pstatement2 = connection.prepareStatement(query2);
			pstatement2.setInt(1, codeProduct);
			pstatement2.setInt(2, codeSupplier);
			result2 = pstatement2.executeQuery();
			
			while(result2.next()) {
				if(result2.getInt("quantity") < 0) {
					connection.rollback();
					throw new CartQuantityException();
				}
			}
		} catch (SQLException e) {
			connection.rollback();
		    e.printStackTrace();
			throw new SQLException(e);
		} finally {
			try {
				result2.close();
			} catch (Exception e2) {
				connection.rollback();
				throw new SQLException(e2);
			}
			try {
				pstatement1.close();
				pstatement2.close();
			} catch (Exception e1) {
				connection.rollback();
				throw new SQLException(e1);
			}
		}	
		connection.commit();
		connection.setAutoCommit(true);
	}
}
