package com.Controllers.Servlets.AddressServices;

import com.Models.DataBase.ConnectionForDatabase;
import com.Models.Repositories.RepositoryAddress;


import java.util.ArrayList;

public class ServiceDeleteAddresses {

        ConnectionForDatabase connection;
        RepositoryAddress repositoryAddress;

    public ServiceDeleteAddresses(ConnectionForDatabase connection) {
        this.connection = connection;
        this.repositoryAddress = new RepositoryAddress(connection);
    }



        public void execute(ArrayList<Integer> idList){

            for (Integer id : idList) {
                repositoryAddress.delete(id);
            }


        }




}
