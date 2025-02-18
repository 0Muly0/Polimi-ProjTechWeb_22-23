package it.polimi.tiw.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import it.polimi.tiw.project.beans.Advertisement;
import it.polimi.tiw.project.beans.PriceRange;
import it.polimi.tiw.project.beans.Supplier;

public class SupplierDAO {

	private Connection connection;

	public SupplierDAO(Connection connection) {
		this.connection = connection;
	}

	public Supplier getSupplierDetails(int codeSupplier) throws SQLException {
		Supplier suppl = null;

		String query1 = "SELECT * FROM supplier AS S WHERE S.codeSupplier = ?";
		String query2 = "SELECT * FROM price_range AS PR WHERE PR.codeSupplier = ?";
		
		ResultSet result1 = null;
		PreparedStatement pstatement1 = null;
		ResultSet result2 = null;
		PreparedStatement pstatement2 = null;

		try {
			pstatement1 = connection.prepareStatement(query1);
			pstatement2 = connection.prepareStatement(query2);

			pstatement1.setInt(1, codeSupplier);
			result1 = pstatement1.executeQuery();
			while (result1.next()) {
				suppl = new Supplier();
				
				suppl.setCodeSupplier(codeSupplier);
				suppl.setName(result1.getString("name"));
				suppl.setEvaluation(result1.getFloat("evaluation"));
				
				float tresh = result1.getFloat("treshold");
				if(result1.wasNull()) {
					suppl.setTreshold(null);
				} else {
					suppl.setTreshold(tresh);
				}

				ArrayList<PriceRange> prList = new ArrayList<PriceRange>();

				// Retrieve all price ranges by supplier
				pstatement2.setInt(1, codeSupplier);
				result2 = pstatement2.executeQuery();
				while (result2.next()) {
					PriceRange pr = new PriceRange();
					pr.setMin(result2.getInt("min"));
					pr.setMax(result2.getInt("max"));
					pr.setShippingPrice(result2.getInt("shippingPrice"));

					prList.add(pr);
				}
				// Adds shipping policy to supplier
				suppl.setShippingPolicy(prList);
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
		return suppl;
	}

	public Advertisement getAdvDetails(int codeSupplier, int codeProduct, int quantity) throws SQLException {
		Advertisement add = null;

		String query = "SELECT * FROM advertising AS A WHERE A.codeSupplier = ? AND A.codeProduct = ?";
		
		ResultSet result = null;
		PreparedStatement pstatement = null;

		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setInt(1, codeSupplier);
			pstatement.setInt(2, codeProduct);

			result = pstatement.executeQuery();
			
			while(result.next()) {
				add = new Advertisement();
				
				add.setCodeProduct(codeProduct);
				add.setCodeSupplier(codeSupplier);
				add.setQuantity(quantity);
				add.setPrice(result.getFloat("price"));
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
		return add;
	}

	public ArrayList<Advertisement> getAdvsSuppliersByProduct(int codeProduct) throws SQLException {
		ArrayList<Advertisement> adds = new ArrayList<Advertisement>();;

		String query1 = "SELECT * FROM advertising AS A JOIN supplier AS S ON A.codeSupplier = S.codeSupplier WHERE A.codeProduct = ?";
		String query2 = "SELECT * FROM price_range AS PR WHERE PR.codeSupplier = ?";
		ResultSet result1 = null;
		PreparedStatement pstatement1 = null;
		ResultSet result2 = null;
		PreparedStatement pstatement2 = null;

		try {
			pstatement1 = connection.prepareStatement(query1);
			pstatement2 = connection.prepareStatement(query2);

			pstatement1.setInt(1, codeProduct);
			result1 = pstatement1.executeQuery();
			while (result1.next()) {
				
				Advertisement add = new Advertisement();
				add.setCodeProduct(result1.getInt("codeProduct"));
				add.setCodeSupplier(result1.getInt("codeSupplier"));
				add.setQuantity(result1.getInt("quantity"));
				add.setPrice(result1.getFloat("price"));

				Supplier suppl = new Supplier();
				suppl.setCodeSupplier(result1.getInt("codeSupplier"));
				suppl.setName(result1.getString("name"));
				suppl.setEvaluation(result1.getFloat("evaluation"));

				float tresh = result1.getFloat("treshold");
				if(result1.wasNull()) {
					suppl.setTreshold(null);
				} else {
					suppl.setTreshold(tresh);
				}

				ArrayList<PriceRange> prList = new ArrayList<PriceRange>();

				// Retrieve all price ranges by supplier
				pstatement2.setInt(1, result1.getInt("codeSupplier"));
				result2 = pstatement2.executeQuery();
				while (result2.next()) {
					PriceRange pr = new PriceRange();
					pr.setMin(result2.getInt("min"));
					pr.setMax(result2.getInt("max"));
					pr.setShippingPrice(result2.getInt("shippingPrice"));

					prList.add(pr);
				}
				// Adds shipping policy to supplier
				suppl.setShippingPolicy(prList);
				// Adds supplier to advertisement
				add.setSupplier(suppl);

				adds.add(add);
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
		return adds;
	}

}
