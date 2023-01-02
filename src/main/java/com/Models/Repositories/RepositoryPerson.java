package com.Models.Repositories;

import com.Models.DataBase.ConnectionForDatabase;
import com.Models.Entities.Address;
import com.Models.Entities.Person;
import com.Models.Entities.TelephoneNumber;

import javax.sql.rowset.CachedRowSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/*
tables:
    Persons (id, Name)
    Addresses(id, Address)
    AddressesOfPersons(Person_id, Address_id)
    TelephonesOfPersons (id, Person_id, Telephone)
 */

public class RepositoryPerson implements Repository<Person> {


    private ConnectionForDatabase connection;

    public RepositoryPerson(ConnectionForDatabase connection) {
        this.connection = connection;
    }

    @Override
    public Set<Person> getAll() {
        String queryText = "SELECT * \n" +
                "FROM\n" +
                "Persons ORDER BY Persons.id;\n" +

                "SELECT * \n" +
                "FROM\n" +
                "Addresses;\n" +

                "SELECT * \n" +
                "FROM\n" +
                "AddressesOfPersons;\n" +

                "SELECT * \n" +
                "FROM\n" +
                "TelephonesOfPersons;\n";

        Set<Person> setOfPersons = new HashSet<>();

        ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, null);

        HashMap<Integer, Address> addresses = new HashMap<>();
        HashMap<Integer, Person> persons = new HashMap<>();

        try {
            ResultSet personsResultSet = resultSet.get(0);
            ResultSet addressesResultSet = resultSet.get(1);
            ResultSet addressesOfPersonsResultSet = resultSet.get(2);
            ResultSet telephonesOfPersonsResultSet = resultSet.get(3);

            while (personsResultSet.next()) {
                persons.put(personsResultSet.getInt("id"), new Person(personsResultSet.getInt("id"), personsResultSet.getString("Name")));
            }

            while (addressesResultSet.next()) {
                addresses.put(addressesResultSet.getInt("id"), new Address(addressesResultSet.getInt("id"), addressesResultSet.getString("Address")));
            }

            while (addressesOfPersonsResultSet.next()) {
                Integer personId  = addressesOfPersonsResultSet.getInt("Person_id");
                Integer addressId = addressesOfPersonsResultSet.getInt("Address_id");

                Person person = persons.get(personId);
                Address address = addresses.get(addressId);

                if (person != null && address != null) {
                    person.getAddresses().add(address);
                    address.getPersons().add(person);
                }
            }

            while (telephonesOfPersonsResultSet.next()) {
                Integer id = telephonesOfPersonsResultSet.getInt("id");
                Integer personId = telephonesOfPersonsResultSet.getInt("Person_id");
                String telephone = telephonesOfPersonsResultSet.getString("Telephone");

                Person person = persons.get(personId);

                if (person != null) {
                    person.getTelephoneNumbers().add(new TelephoneNumber(id, telephone, person));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        setOfPersons = new HashSet<Person>(persons.values());
        return setOfPersons;
    }

    @Override
    public Person get(int id) {
        String queryText = "SELECT * \n" +
                "FROM\n" +
                "Persons\n" +
                "WHERE\n" +
                "Persons.id = ?;\n" +

                "SELECT * \n" +
                "FROM\n" +
                "Addresses\n" +
                "WHERE Addresses.id in \n" +
                "(SELECT Address_id\n" +
                "FROM AddressesOfPersons\n" +
                "WHERE AddressesOfPersons.Person_id = ?);\n" +

                "SELECT * \n" +
                "FROM\n" +
                "TelephonesOfPersons\n" +
                "WHERE  TelephonesOfPersons.Person_id = ?\n";

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        arrayListOfParameters.add(id);
        arrayListOfParameters.add(id);
        arrayListOfParameters.add(id);

        ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, arrayListOfParameters);

        try {
            ResultSet personsResultSet = resultSet.get(0);
            ResultSet addressesResultSet = resultSet.get(1);
            ResultSet telephonesOfPersonsResultSet = resultSet.get(2);

            if (personsResultSet.next()) {
                Person resultPerson = new Person(personsResultSet.getInt("id"), personsResultSet.getString("Name"));

                while (addressesResultSet.next()) {
                    Address address = new Address(addressesResultSet.getInt("id"), addressesResultSet.getString("Address"));
                    resultPerson.getAddresses().add(address);
                    address.getPersons().add(resultPerson);
                }

                while (telephonesOfPersonsResultSet.next()) {
                    resultPerson.getTelephoneNumbers().add(new TelephoneNumber(telephonesOfPersonsResultSet.getInt("id"), telephonesOfPersonsResultSet.getString("Telephone"), resultPerson));
                }

                return resultPerson;
            } else {
                return null;
            }


        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer insert(Person person) {

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        String queryText = "";
        Integer idMaxAddress = 1;
        Integer idMaxTelephone = 1;

        queryText += "SELECT MAX (id) + 1 as idMax\n" +
                "FROM\n" +
                "Persons;\n" +
                "SELECT MAX (id) + 1 as idMax\n" +
                "FROM\n" +
                "TelephonesOfPersons;\n"+
                "SELECT MAX (id) + 1 as idMax\n" +
                "FROM\n" +
                "Addresses;\n";
        ArrayList<CachedRowSet> resultSet = connection.executeQuery(queryText, arrayListOfParameters);
        ResultSet personId = resultSet.get(0);

        try {
            if (personId.next()) {
                Integer idMax = personId.getInt("idMax");
                if(person.getId() == null) {
                    person.setId(idMax);
                }
            }
            ResultSet telephoneId = resultSet.get(1);
            if (telephoneId.next()) {
                idMaxTelephone = telephoneId.getInt("idMax");
            }
            ResultSet addressId = resultSet.get(2);
            if (addressId.next()) {
                idMaxAddress = addressId.getInt("idMax");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for(Address address : person.getAddresses()) {
            if(address.getId() == null) {
                address.setId(idMaxAddress);
                idMaxAddress++;
            }
        }

        for(TelephoneNumber telephoneNumber : person.getTelephoneNumbers()) {
            if(telephoneNumber.getId() == null) {
                telephoneNumber.setId(idMaxTelephone);
                idMaxTelephone++;
            }
        }

        queryText += "INSERT \n" +
                "INTO\n" +
                "Persons (id, Name)\n" +
                "VALUES\n" +
                "(?, ?)\n" +
                "ON CONFLICT (id) DO UPDATE \n" +
                "SET Name = excluded.Name;\n";
        arrayListOfParameters.add(person.getId());
        arrayListOfParameters.add(person.getName());

        for (Address address : person.getAddresses()) {
            queryText += "INSERT \n" +
                    "INTO\n" +
                    "Addresses (id, Address)\n" +
                    "VALUES\n" +
                    "(?, ?)\n" +
                    "ON CONFLICT (id) DO UPDATE\n" +
                    "SET Address = excluded.Address;\n";
            arrayListOfParameters.add(address.getId());
            arrayListOfParameters.add(address.getAddress());

            queryText += "INSERT \n" +
                    "INTO\n" +
                    "AddressesOfPersons (Person_id, Address_id)\n" +
                    "VALUES\n" +
                    "(?, ?)\n" +
                    "ON CONFLICT (Person_id, Address_id) DO NOTHING;\n";

            arrayListOfParameters.add(person.getId());
            arrayListOfParameters.add(address.getId());
        }

        for (TelephoneNumber telephoneNumber: person.getTelephoneNumbers()) {
            queryText += "INSERT \n" +
                    "INTO\n" +
                    "TelephonesOfPersons (id, person_id, telephone)\n" +
                    "VALUES\n" +
                    "(?, ?, ?)\n" +
                    "ON CONFLICT (id) DO UPDATE \n" +
                    "SET Telephone = excluded.Telephone,\n" +
                    "Person_id = excluded.Person_id;\n";
            arrayListOfParameters.add(telephoneNumber.getId());
            arrayListOfParameters.add(person.getId());
            arrayListOfParameters.add(telephoneNumber.getTelephoneNumber());
        }

        connection.executeQuery(queryText, arrayListOfParameters);
        return person.getId();
    }

    @Override
    public void delete(Integer id) {

        String queryText = " DELETE FROM Persons WHERE id = ?;\n" +
                "DELETE FROM AddressesOfPersons WHERE Person_id = ?;\n" +
                "DELETE FROM TelephonesOfPersons WHERE Person_id = ?;\n";

        ArrayList<Object> arrayListOfParameters = new ArrayList<>();
        arrayListOfParameters.add(id);
        arrayListOfParameters.add(id);
        arrayListOfParameters.add(id);
        connection.executeQuery(queryText, arrayListOfParameters);

    }

    @Override
    public void deleteALL() {

        String queryText = "DELETE FROM Persons;" +
                "DELETE FROM AddressesOfPersons;" +
                "DELETE FROM TelephonesOfPersons;";

        connection.executeQuery(queryText, null);

    }

}
