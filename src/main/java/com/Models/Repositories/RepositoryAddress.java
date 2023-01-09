package com.Models.Repositories;

import com.Models.DataBase.ConnectionForDatabase;
import com.Models.Entities.Address;
import com.Models.Entities.Person;
import com.Models.Entities.TelephoneNumber;

import javax.sql.rowset.CachedRowSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RepositoryAddress implements  Repository<Address>{

    private ConnectionForDatabase connection;

    public RepositoryAddress(ConnectionForDatabase connection) {
        this.connection = connection;
    }

    @Override
    public Set<Address> getAll(){
        String queryText ="SELECT * \n" +
                "FROM\n" +
                "Addresses;\n";

        Set <Address> setOfAddresses = new HashSet<>();

        ConnectionForDatabase.Query query = new ConnectionForDatabase.Query(queryText, null);
        ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, null);

        try {
            ResultSet addressesResultSet = resultSet.get(0);

            while (addressesResultSet.next()) {
                setOfAddresses.add(new Address(addressesResultSet.getInt("id"), addressesResultSet.getString("address")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return setOfAddresses;
    }

    @Override
    public Address get(int id) {

        String queryText ="SELECT * \n" +
                "FROM\n" +
                "Addresses\n" +
                "WHERE Addresses.id = ?;\n";

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        arrayListOfParameters.add(id);
        ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, arrayListOfParameters);
        Address address = null;

        try {
            ResultSet addressesResultSet = resultSet.get(0);

          if (addressesResultSet.next()) {
               address = new Address(id, addressesResultSet.getString("address"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return address;
    }
    

    @Override
    public Integer insert(Address address) {

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        String queryText = "";

        if (address.getId() == null) {

            Integer idMaxAddress = 1;

            queryText += "SELECT MAX (id) + 1 as idMax\n" +
                    "FROM\n" +
                    "Addresses;\n";
            ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, arrayListOfParameters);
            ResultSet addressId = resultSet.get(0);
            try {
                if (addressId.next()) {
                    Integer idMax = addressId.getInt("idMax");
                    if (address.getId() == null) {
                        address.setId(idMax);
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }

        queryText =
        "merge into Addresses \n" +
        "using (VALUES (?,?)) as Source (id, Address)\n" +
                "ON Addresses.id = Source.id\n" +
                "WHEN MATCHED THEN\n" +
                "UPDATE set address = Source.address\n"+
                "WHEN NOT MATCHED THEN\n" +
                "INSERT (id, Address)\n" +
                "VALUES(Source.id, Source.Address)";


        arrayListOfParameters.add(address.getId());
        arrayListOfParameters.add(address.getAddress());

        connection.executeQuery(queryText, arrayListOfParameters);
        return address.getId();
    }

    @Override
    public void delete(Integer id) {

        String queryText = " DELETE FROM Addresses WHERE id = ?;\n" +
                "DELETE FROM AddressesOfPersons WHERE Person_id = ?;\n";

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        arrayListOfParameters.add(id);
        arrayListOfParameters.add(id);
        connection.executeQuery(queryText, arrayListOfParameters);


    }

    @Override
    public void deleteALL() {

        String queryText = " DELETE FROM Addresses;\n";

        connection.executeQuery(queryText, null);
    }
}
