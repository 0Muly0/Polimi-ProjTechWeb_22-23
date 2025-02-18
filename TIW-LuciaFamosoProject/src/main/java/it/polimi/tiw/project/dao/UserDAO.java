package it.polimi.tiw.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import it.polimi.tiw.project.beans.User;

public class UserDAO {
	private Connection connection;
	
	public UserDAO(Connection connection) {
		this.connection = connection;
	}
	
	public User checkCredentials(String username, String password) throws SQLException {
		User user = null;
		
		String query = "SELECT * FROM user WHERE codeUser = ? and password = ?";
		ResultSet result = null;
		PreparedStatement pstatement = null;
		
		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setString(1, username);
			pstatement.setString(2, password);
			
			result = pstatement.executeQuery();
			while (result.next()) {
				user = new User();
				user.setUsername(result.getString("codeUser"));
				user.setName(result.getString("name"));
				user.setSurname(result.getString("surname"));
				user.setAddress(result.getString("address"));
				user.setEmail(result.getString("email"));
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
		return user;
	}

}
