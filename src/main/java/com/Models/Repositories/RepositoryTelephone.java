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

public class RepositoryTelephone implements Repository<TelephoneNumber>{

    private ConnectionForDatabase connection;

    public RepositoryTelephone(ConnectionForDatabase connection) {
        this.connection = connection;
    }
    @Override
    public Set<TelephoneNumber> getAll() {

        String queryText = "SELECT TelephonesOfPersons.id as id, \n" +
                "TelephonesOfPersons.Person_id as Person_id, \n" +
                "TelephonesOfPersons.Telephone as Telephone, \n" +
                "Persons.Name as Name \n" +
                "FROM\n" +
                "TelephonesOfPersons\n" +
                "LEFT JOIN Persons ON\n" +
                "TelephonesOfPersons.Person_id = Persons.id;\n";

        Set<TelephoneNumber> telephoneNumbers = new HashSet<>();
        ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, null);

        try {
            ResultSet telephoneResultSet = resultSet.get(0);

            while (telephoneResultSet.next()) {
                telephoneNumbers.add(
                        new TelephoneNumber(
                                telephoneResultSet.getInt("id"),
                                telephoneResultSet.getString("Telephone"),
                                new Person(telephoneResultSet.getInt("Person_id"), telephoneResultSet.getString("Name"))));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return telephoneNumbers;

    }

    @Override
    public TelephoneNumber get(int id) {


        String queryText = "SELECT TelephonesOfPersons.id as id, \n" +
                "TelephonesOfPersons.Person_id as Person_id, \n" +
                "TelephonesOfPersons.Telephone as Telephone, \n" +
                "Persons.Name as Name \n" +
                "FROM\n" +
                "TelephonesOfPersons\n" +
                "LEFT JOIN Persons ON\n" +
                "TelephonesOfPersons.Person_id = Persons.id\n" +
                "WHERE TelephonesOfPersons.id = ?;\n";

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        arrayListOfParameters.add(id);
        ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, arrayListOfParameters);
        TelephoneNumber telephoneNumber = null;
        try {
            ResultSet telephoneResultSet = resultSet.get(0);

            if (telephoneResultSet.next()) {
                telephoneNumber =  new TelephoneNumber(
                                telephoneResultSet.getInt("id"),
                                telephoneResultSet.getString("Telephone"),
                                new Person(telephoneResultSet.getInt("Person_id"), telephoneResultSet.getString("Name")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return telephoneNumber;
    }

    @Override
    public Integer insert(TelephoneNumber telephoneNumber) {

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        String queryText = "";

        if (telephoneNumber.getId() == null) {

            arrayListOfParameters.add(telephoneNumber.getPerson().getId());
            ArrayList<ConnectionForDatabase.Query> queries = new ArrayList<>();

            queries.add(new ConnectionForDatabase.Query( "SELECT MAX (id) + 1 as idMax\n" +
                    "FROM\n" +
                    "TelephonesOfPersons;\n", null));
            queries.add(new ConnectionForDatabase.Query("SELECT id FROM Persons WHERE id = ?\n"  , arrayListOfParameters));


            ArrayList<CachedRowSet> resultSet = connection.executeQuery(queries);
            ResultSet telephoneId = resultSet.get(0);
            ResultSet person = resultSet.get(1);
            try {
                if (telephoneId.next()) {
                    Integer idMax = telephoneId.getInt("idMax");
                    if (telephoneNumber.getId() == null) {
                        telephoneNumber.setId(idMax);
                    }
                }

                if(!person.next()) {
                    return null;
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }

        arrayListOfParameters.clear();

        queryText =
                "merge into TelephonesOfPersons \n" +
                        "using (VALUES (?,?,?)) as Source (id, telephone, Person_id)\n" +
                        "ON TelephonesOfPersons.id = Source.id\n" +
                        "WHEN MATCHED THEN\n" +
                        "UPDATE set telephone = Source.telephone, Person_id = Source.Person_id\n"+
                        "WHEN NOT MATCHED THEN\n" +
                        "INSERT (id, telephone, Person_id)\n" +
                        "VALUES(Source.id, Source.telephone, Source.Person_id);";
        arrayListOfParameters = new ArrayList<>();
        arrayListOfParameters.add(telephoneNumber.getId());
        arrayListOfParameters.add(telephoneNumber.getTelephoneNumber());
        arrayListOfParameters.add(telephoneNumber.getPerson().getId());

        connection.executeQuery(queryText, arrayListOfParameters);
        return telephoneNumber.getId();
    }


    @Override
    public void delete(Integer id) {
        String queryText = " DELETE FROM TelephonesOfPersons WHERE id = ?;\n";

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        arrayListOfParameters.add(id);
        connection.executeQuery(queryText, arrayListOfParameters);


    }

    @Override
    public void deleteALL() {
        String queryText = " DELETE FROM TelephonesOfPersons;\n";
        connection.executeQuery(queryText, null);

    }
}
